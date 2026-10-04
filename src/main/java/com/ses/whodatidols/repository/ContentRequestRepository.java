package com.ses.whodatidols.repository;

import com.ses.whodatidols.model.ContentRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;

@Repository
@SuppressWarnings("SqlResolve")
public class ContentRequestRepository {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public ContentRequestRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private void ensureTablesExist() {
        String sqlRequests = "IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'ContentRequest') " +
                "CREATE TABLE ContentRequest (" +
                "Id UNIQUEIDENTIFIER PRIMARY KEY DEFAULT NEWID()," +
                "UserId UNIQUEIDENTIFIER NOT NULL," +
                "UserNickname NVARCHAR(100) NOT NULL," +
                "Title NVARCHAR(255) NOT NULL," +
                "ContentType NVARCHAR(20) NOT NULL," +
                "PosterUrl NVARCHAR(500) NULL," +
                "ReleaseYear INT NULL," +
                "TmdbId INT NULL," +
                "TvMazeId INT NULL," +
                "Description NVARCHAR(1000) NULL," +
                "Status NVARCHAR(50) DEFAULT 'PENDING'," +
                "VoteCount INT DEFAULT 1," +
                "CreatedAt DATETIME2 DEFAULT GETUTCDATE()" +
                ")";

        String sqlVotes = "IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'ContentRequestVote') " +
                "CREATE TABLE ContentRequestVote (" +
                "RequestId UNIQUEIDENTIFIER NOT NULL," +
                "UserId UNIQUEIDENTIFIER NOT NULL," +
                "CreatedAt DATETIME2 DEFAULT GETUTCDATE()," +
                "PRIMARY KEY (RequestId, UserId)" +
                ")";

        try {
            jdbcTemplate.execute(sqlRequests);
            jdbcTemplate.execute(sqlVotes);
        } catch (Exception e) {
            System.err.println("Error initializing ContentRequest tables: " + e.getMessage());
        }
    }

    public ContentRequest createRequest(UUID userId, String userNickname, String title,
                                        String contentType, String posterUrl, Integer releaseYear,
                                        Integer tmdbId, Integer tvmazeId, String description) {
        ensureTablesExist();

        UUID requestId = UUID.randomUUID();
        Instant now = Instant.now();
        Timestamp nowTs = Timestamp.from(now);

        String insertRequestSql = "INSERT INTO ContentRequest " +
                "(Id, UserId, UserNickname, Title, ContentType, PosterUrl, ReleaseYear, TmdbId, TvMazeId, Description, Status, VoteCount, CreatedAt) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'PENDING', 1, ?)";

        jdbcTemplate.update(insertRequestSql, requestId, userId, userNickname, title, contentType,
                posterUrl, releaseYear, tmdbId, tvmazeId, description, nowTs);

        String insertVoteSql = "INSERT INTO ContentRequestVote (RequestId, UserId, CreatedAt) VALUES (?, ?, ?)";
        try {
            jdbcTemplate.update(insertVoteSql, requestId, userId, nowTs);
        } catch (Exception ignored) {
        }

        ContentRequest req = new ContentRequest();
        req.setId(requestId);
        req.setUserId(userId);
        req.setUserNickname(userNickname);
        req.setTitle(title);
        req.setContentType(contentType);
        req.setPosterUrl(posterUrl);
        req.setReleaseYear(releaseYear);
        req.setTmdbId(tmdbId);
        req.setTvmazeId(tvmazeId);
        req.setDescription(description);
        req.setStatus("PENDING");
        req.setVoteCount(1);
        req.setCreatedAt(now);
        req.setUserVoted(true);

        return req;
    }

    public Map<String, Object> toggleVote(UUID requestId, UUID userId) {
        ensureTablesExist();

        String checkSql = "SELECT COUNT(*) FROM ContentRequestVote WHERE RequestId = ? AND UserId = ?";
        Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, requestId, userId);
        boolean alreadyVoted = count != null && count > 0;

        boolean nowVoted;
        if (alreadyVoted) {
            jdbcTemplate.update("DELETE FROM ContentRequestVote WHERE RequestId = ? AND UserId = ?", requestId, userId);
            nowVoted = false;
        } else {
            jdbcTemplate.update("INSERT INTO ContentRequestVote (RequestId, UserId, CreatedAt) VALUES (?, ?, GETUTCDATE())",
                    requestId, userId);
            nowVoted = true;
        }

        // Count remaining votes
        Integer newVoteCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ContentRequestVote WHERE RequestId = ?", Integer.class, requestId);
        if (newVoteCount == null) newVoteCount = 0;

        Map<String, Object> result = new HashMap<>();

        // If the number of people requesting drops to 0 or less, auto-delete the request!
        if (newVoteCount <= 0) {
            jdbcTemplate.update("DELETE FROM ContentRequestVote WHERE RequestId = ?", requestId);
            jdbcTemplate.update("DELETE FROM ContentRequest WHERE Id = ?", requestId);
            result.put("voted", false);
            result.put("voteCount", 0);
            result.put("deleted", true);
            return result;
        }

        // Update vote count
        String updateVoteCountSql = "UPDATE ContentRequest SET VoteCount = ? WHERE Id = ?";
        jdbcTemplate.update(updateVoteCountSql, newVoteCount, requestId);

        result.put("voted", nowVoted);
        result.put("voteCount", newVoteCount);
        result.put("deleted", false);
        return result;
    }

    public boolean deleteRequest(UUID requestId) {
        ensureTablesExist();
        jdbcTemplate.update("DELETE FROM ContentRequestVote WHERE RequestId = ?", requestId);
        int rows = jdbcTemplate.update("DELETE FROM ContentRequest WHERE Id = ?", requestId);
        return rows > 0;
    }

    public boolean updateStatus(UUID requestId, String status) {
        ensureTablesExist();
        int rows = jdbcTemplate.update("UPDATE ContentRequest SET Status = ? WHERE Id = ?", status, requestId);
        return rows > 0;
    }

    public List<ContentRequest> getRequests(String filterType, String sortBy, UUID currentUserId, int limit, int offset) {
        ensureTablesExist();

        StringBuilder sql = new StringBuilder();
        List<Object> params = new ArrayList<>();

        sql.append("SELECT r.Id, r.UserId, r.UserNickname, r.Title, r.ContentType, r.PosterUrl, ")
           .append("r.ReleaseYear, r.TmdbId, r.TvMazeId, r.Description, r.Status, r.VoteCount, r.CreatedAt, ");

        if (currentUserId != null) {
            sql.append("CASE WHEN EXISTS (SELECT 1 FROM ContentRequestVote v WHERE v.RequestId = r.Id AND v.UserId = ?) THEN 1 ELSE 0 END AS UserVoted ");
            params.add(currentUserId);
        } else {
            sql.append("0 AS UserVoted ");
        }

        sql.append("FROM ContentRequest r ");

        if ("movie".equalsIgnoreCase(filterType)) {
            sql.append("WHERE r.ContentType = 'movie' ");
        } else if ("series".equalsIgnoreCase(filterType)) {
            sql.append("WHERE r.ContentType = 'series' ");
        }

        if ("newest".equalsIgnoreCase(sortBy)) {
            sql.append("ORDER BY r.CreatedAt DESC ");
        } else {
            // Default to most votes, then newest
            sql.append("ORDER BY r.VoteCount DESC, r.CreatedAt DESC ");
        }

        sql.append("OFFSET ? ROWS FETCH NEXT ? ROWS ONLY");
        params.add(offset);
        params.add(limit);

        return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> {
            ContentRequest r = new ContentRequest();
            r.setId(UUID.fromString(rs.getString("Id")));
            r.setUserId(UUID.fromString(rs.getString("UserId")));
            r.setUserNickname(rs.getString("UserNickname"));
            r.setTitle(rs.getString("Title"));
            r.setContentType(rs.getString("ContentType"));
            r.setPosterUrl(rs.getString("PosterUrl"));

            int year = rs.getInt("ReleaseYear");
            r.setReleaseYear(rs.wasNull() ? null : year);

            int tmdb = rs.getInt("TmdbId");
            r.setTmdbId(rs.wasNull() ? null : tmdb);

            int tvmaze = rs.getInt("TvMazeId");
            r.setTvmazeId(rs.wasNull() ? null : tvmaze);

            r.setDescription(rs.getString("Description"));
            r.setStatus(rs.getString("Status"));
            r.setVoteCount(rs.getInt("VoteCount"));

            Timestamp ts = rs.getTimestamp("CreatedAt");
            if (ts != null) {
                r.setCreatedAt(ts.toInstant());
            }

            r.setUserVoted(rs.getInt("UserVoted") == 1);
            return r;
        }, params.toArray());
    }

    public int countRequests(String filterType) {
        ensureTablesExist();

        String sql = "SELECT COUNT(*) FROM ContentRequest ";
        if ("movie".equalsIgnoreCase(filterType)) {
            sql += "WHERE ContentType = 'movie'";
        } else if ("series".equalsIgnoreCase(filterType)) {
            sql += "WHERE ContentType = 'series'";
        }

        Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
        return count != null ? count : 0;
    }

    public List<ContentRequest> getRequestsForAdmin(String filterType, String status, String search, int limit, int offset) {
        ensureTablesExist();

        StringBuilder sql = new StringBuilder();
        List<Object> params = new ArrayList<>();

        sql.append("SELECT r.Id, r.UserId, r.UserNickname, r.Title, r.ContentType, r.PosterUrl, ")
           .append("r.ReleaseYear, r.TmdbId, r.TvMazeId, r.Description, r.Status, r.VoteCount, r.CreatedAt, 0 AS UserVoted ")
           .append("FROM ContentRequest r WHERE 1=1 ");

        if (filterType != null && !filterType.isBlank() && !"all".equalsIgnoreCase(filterType)) {
            sql.append("AND r.ContentType = ? ");
            params.add(filterType);
        }

        if (status != null && !status.isBlank() && !"all".equalsIgnoreCase(status)) {
            sql.append("AND r.Status = ? ");
            params.add(status);
        }

        if (search != null && !search.isBlank()) {
            sql.append("AND (r.Title LIKE ? OR r.UserNickname LIKE ? OR r.Description LIKE ?) ");
            String wild = "%" + search.trim() + "%";
            params.add(wild);
            params.add(wild);
            params.add(wild);
        }

        sql.append("ORDER BY r.CreatedAt DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY");
        params.add(offset);
        params.add(limit);

        return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> {
            ContentRequest r = new ContentRequest();
            r.setId(UUID.fromString(rs.getString("Id")));
            r.setUserId(UUID.fromString(rs.getString("UserId")));
            r.setUserNickname(rs.getString("UserNickname"));
            r.setTitle(rs.getString("Title"));
            r.setContentType(rs.getString("ContentType"));
            r.setPosterUrl(rs.getString("PosterUrl"));

            int year = rs.getInt("ReleaseYear");
            r.setReleaseYear(rs.wasNull() ? null : year);

            int tmdb = rs.getInt("TmdbId");
            r.setTmdbId(rs.wasNull() ? null : tmdb);

            int tvmaze = rs.getInt("TvMazeId");
            r.setTvmazeId(rs.wasNull() ? null : tvmaze);

            r.setDescription(rs.getString("Description"));
            r.setStatus(rs.getString("Status"));
            r.setVoteCount(rs.getInt("VoteCount"));

            Timestamp ts = rs.getTimestamp("CreatedAt");
            if (ts != null) {
                r.setCreatedAt(ts.toInstant());
            }
            return r;
        }, params.toArray());
    }

    public int countRequestsForAdmin(String filterType, String status, String search) {
        ensureTablesExist();

        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM ContentRequest r WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (filterType != null && !filterType.isBlank() && !"all".equalsIgnoreCase(filterType)) {
            sql.append("AND r.ContentType = ? ");
            params.add(filterType);
        }

        if (status != null && !status.isBlank() && !"all".equalsIgnoreCase(status)) {
            sql.append("AND r.Status = ? ");
            params.add(status);
        }

        if (search != null && !search.isBlank()) {
            sql.append("AND (r.Title LIKE ? OR r.UserNickname LIKE ? OR r.Description LIKE ?) ");
            String wild = "%" + search.trim() + "%";
            params.add(wild);
            params.add(wild);
            params.add(wild);
        }

        Integer count = jdbcTemplate.queryForObject(sql.toString(), Integer.class, params.toArray());
        return count != null ? count : 0;
    }

    public Optional<ContentRequest> findById(UUID requestId) {
        ensureTablesExist();
        String sql = "SELECT Id, UserId, UserNickname, Title, ContentType, PosterUrl, ReleaseYear, TmdbId, TvMazeId, Description, Status, VoteCount, CreatedAt, 0 as UserVoted FROM ContentRequest WHERE Id = ?";
        return jdbcTemplate.query(sql, rs -> {
            if (rs.next()) {
                ContentRequest r = new ContentRequest();
                r.setId(UUID.fromString(rs.getString("Id")));
                r.setUserId(UUID.fromString(rs.getString("UserId")));
                r.setUserNickname(rs.getString("UserNickname"));
                r.setTitle(rs.getString("Title"));
                r.setContentType(rs.getString("ContentType"));
                r.setPosterUrl(rs.getString("PosterUrl"));

                int year = rs.getInt("ReleaseYear");
                r.setReleaseYear(rs.wasNull() ? null : year);

                int tmdb = rs.getInt("TmdbId");
                r.setTmdbId(rs.wasNull() ? null : tmdb);

                int tvmaze = rs.getInt("TvMazeId");
                r.setTvmazeId(rs.wasNull() ? null : tvmaze);

                r.setDescription(rs.getString("Description"));
                r.setStatus(rs.getString("Status"));
                r.setVoteCount(rs.getInt("VoteCount"));

                Timestamp ts = rs.getTimestamp("CreatedAt");
                if (ts != null) {
                    r.setCreatedAt(ts.toInstant());
                }
                return Optional.of(r);
            }
            return Optional.empty();
        }, requestId);
    }
}
