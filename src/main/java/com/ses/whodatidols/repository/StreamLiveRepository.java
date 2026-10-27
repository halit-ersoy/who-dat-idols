package com.ses.whodatidols.repository;

import com.ses.whodatidols.model.StreamLive;
import com.ses.whodatidols.model.StreamLiveChat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

@Repository
@SuppressWarnings({"SqlResolve", "SqlNoDataSourceInspection"})
public class StreamLiveRepository {

    private static final Logger logger = LoggerFactory.getLogger(StreamLiveRepository.class);
    private final JdbcTemplate jdbcTemplate;

    public StreamLiveRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        ensureSchema();
    }

    private void ensureSchema() {
        try {
            jdbcTemplate.execute("""
                IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'StreamLive')
                BEGIN
                    CREATE TABLE StreamLive (
                        ID UNIQUEIDENTIFIER PRIMARY KEY DEFAULT NEWID(),
                        Title NVARCHAR(255) NOT NULL,
                        Description NVARCHAR(MAX) NULL,
                        ContentType NVARCHAR(50) NOT NULL,
                        ContentId UNIQUEIDENTIFIER NOT NULL,
                        ContentName NVARCHAR(255) NULL,
                        Slug NVARCHAR(255) NULL,
                        PosterUrl NVARCHAR(500) NULL,
                        VideoUrl NVARCHAR(500) NOT NULL,
                        ScheduledStartTime DATETIME NOT NULL,
                        DurationMinutes INT NOT NULL DEFAULT 0,
                        EndTime DATETIME NOT NULL,
                        IsActive BIT NOT NULL DEFAULT 1,
                        CreatedAt DATETIME NOT NULL DEFAULT GETDATE()
                    );
                    CREATE NONCLUSTERED INDEX idx_streamlive_schedule ON StreamLive(ScheduledStartTime, EndTime, IsActive);
                END
            """);

            jdbcTemplate.execute("""
                IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'StreamLiveChat')
                BEGIN
                    CREATE TABLE StreamLiveChat (
                        ID UNIQUEIDENTIFIER PRIMARY KEY DEFAULT NEWID(),
                        StreamLiveId UNIQUEIDENTIFIER NOT NULL,
                        UserId UNIQUEIDENTIFIER NOT NULL,
                        Nickname NVARCHAR(100) NOT NULL,
                        Message NVARCHAR(200) NOT NULL,
                        SentAt DATETIME NOT NULL DEFAULT GETDATE()
                    );
                    CREATE NONCLUSTERED INDEX idx_streamlivechat_streamid ON StreamLiveChat(StreamLiveId, SentAt ASC);
                END
            """);
        } catch (Exception e) {
            logger.error("Failed to ensure StreamLive schema: {}", e.getMessage());
        }
    }

    private final RowMapper<StreamLive> streamLiveRowMapper = (rs, rowNum) -> {
        StreamLive s = new StreamLive();
        s.setId(UUID.fromString(rs.getString("ID")));
        s.setTitle(rs.getString("Title"));
        s.setDescription(rs.getString("Description"));
        s.setContentType(rs.getString("ContentType"));
        s.setContentId(UUID.fromString(rs.getString("ContentId")));
        s.setContentName(rs.getString("ContentName"));
        s.setSlug(rs.getString("Slug"));
        s.setPosterUrl(rs.getString("PosterUrl"));
        s.setVideoUrl(rs.getString("VideoUrl"));
        s.setScheduledStartTime(rs.getTimestamp("ScheduledStartTime"));
        s.setDurationMinutes(rs.getInt("DurationMinutes"));
        s.setEndTime(rs.getTimestamp("EndTime"));
        s.setActive(rs.getBoolean("IsActive"));
        s.setCreatedAt(rs.getTimestamp("CreatedAt"));
        return s;
    };

    private final RowMapper<StreamLiveChat> streamLiveChatRowMapper = (rs, rowNum) -> {
        StreamLiveChat c = new StreamLiveChat();
        c.setId(UUID.fromString(rs.getString("ID")));
        c.setStreamLiveId(UUID.fromString(rs.getString("StreamLiveId")));
        c.setUserId(UUID.fromString(rs.getString("UserId")));
        c.setNickname(rs.getString("Nickname"));
        c.setMessage(rs.getString("Message"));
        c.setSentAt(rs.getTimestamp("SentAt"));
        try {
            c.setVerified(rs.getBoolean("isVerified"));
        } catch (Exception ignored) {
        }
        return c;
    };

    public void createStreamLive(StreamLive s) {
        String sql = """
            INSERT INTO StreamLive (ID, Title, Description, ContentType, ContentId, ContentName, Slug, PosterUrl, VideoUrl, ScheduledStartTime, DurationMinutes, EndTime, IsActive, CreatedAt)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        jdbcTemplate.update(sql,
                s.getId().toString(),
                s.getTitle(),
                s.getDescription(),
                s.getContentType(),
                s.getContentId().toString(),
                s.getContentName(),
                s.getSlug(),
                s.getPosterUrl(),
                s.getVideoUrl(),
                s.getScheduledStartTime(),
                s.getDurationMinutes(),
                s.getEndTime(),
                s.isActive() ? 1 : 0,
                s.getCreatedAt());
    }

    public void updateStreamLive(StreamLive s) {
        String sql = """
            UPDATE StreamLive
            SET Title = ?, Description = ?, ContentType = ?, ContentId = ?, ContentName = ?, Slug = ?, PosterUrl = ?, VideoUrl = ?, ScheduledStartTime = ?, DurationMinutes = ?, EndTime = ?, IsActive = ?
            WHERE ID = ?
        """;
        jdbcTemplate.update(sql,
                s.getTitle(),
                s.getDescription(),
                s.getContentType(),
                s.getContentId().toString(),
                s.getContentName(),
                s.getSlug(),
                s.getPosterUrl(),
                s.getVideoUrl(),
                s.getScheduledStartTime(),
                s.getDurationMinutes(),
                s.getEndTime(),
                s.isActive() ? 1 : 0,
                s.getId().toString());
    }

    public void deleteStreamLive(UUID id) {
        jdbcTemplate.update("DELETE FROM StreamLiveChat WHERE StreamLiveId = ?", id.toString());
        jdbcTemplate.update("DELETE FROM StreamLive WHERE ID = ?", id.toString());
    }

    public StreamLive findStreamById(UUID id) {
        try {
            return jdbcTemplate.queryForObject("SELECT * FROM StreamLive WHERE ID = ?", streamLiveRowMapper, id.toString());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Finds the primary active or next scheduled stream:
     * 1. Currently live stream (ScheduledStartTime <= NOW <= EndTime)
     * 2. If none live, nearest upcoming stream (ScheduledStartTime > NOW)
     * 3. If none upcoming, recently ended stream within 1 hour (EndTime <= NOW <= EndTime + 1 hour)
     */
    public StreamLive findActiveOrUpcomingStream() {
        Timestamp now = new Timestamp(System.currentTimeMillis());

        // 1. Check if there's an ongoing live stream
        try {
            String sqlLive = "SELECT TOP 1 * FROM StreamLive WHERE IsActive = 1 AND ScheduledStartTime <= ? AND EndTime >= ? ORDER BY ScheduledStartTime ASC";
            List<StreamLive> live = jdbcTemplate.query(sqlLive, streamLiveRowMapper, now, now);
            if (!live.isEmpty()) return live.get(0);
        } catch (Exception ignored) {}

        // 2. Check for next upcoming stream
        try {
            String sqlUpcoming = "SELECT TOP 1 * FROM StreamLive WHERE IsActive = 1 AND ScheduledStartTime > ? ORDER BY ScheduledStartTime ASC";
            List<StreamLive> upcoming = jdbcTemplate.query(sqlUpcoming, streamLiveRowMapper, now);
            if (!upcoming.isEmpty()) return upcoming.get(0);
        } catch (Exception ignored) {}

        // 3. Check for recently ended stream (within last 60 minutes)
        try {
            Timestamp sixtyMinutesAgo = new Timestamp(System.currentTimeMillis() - 60L * 60L * 1000L);
            String sqlRecentEnded = "SELECT TOP 1 * FROM StreamLive WHERE IsActive = 1 AND EndTime < ? AND EndTime >= ? ORDER BY EndTime DESC";
            List<StreamLive> ended = jdbcTemplate.query(sqlRecentEnded, streamLiveRowMapper, now, sixtyMinutesAgo);
            if (!ended.isEmpty()) return ended.get(0);
        } catch (Exception ignored) {}

        return null;
    }

    public List<StreamLive> findAllStreamsForAdmin() {
        return jdbcTemplate.query("SELECT * FROM StreamLive ORDER BY ScheduledStartTime DESC", streamLiveRowMapper);
    }

    public void saveChatMessage(StreamLiveChat chat) {
        String sql = """
            INSERT INTO StreamLiveChat (ID, StreamLiveId, UserId, Nickname, Message, SentAt)
            VALUES (?, ?, ?, ?, ?, ?)
        """;
        jdbcTemplate.update(sql,
                chat.getId().toString(),
                chat.getStreamLiveId().toString(),
                chat.getUserId().toString(),
                chat.getNickname(),
                chat.getMessage(),
                chat.getSentAt());
    }

    public List<StreamLiveChat> getRecentChatMessages(UUID streamLiveId, int limit) {
        String sql = """
            SELECT Sub.ID, Sub.StreamLiveId, Sub.UserId, Sub.Nickname, Sub.Message, Sub.SentAt, ISNULL(p.isVerified, 0) AS isVerified
            FROM (
                SELECT TOP (?) * FROM StreamLiveChat
                WHERE StreamLiveId = ?
                ORDER BY SentAt DESC
            ) Sub
            LEFT JOIN [WhoDatIdols].[dbo].[Person] p ON Sub.UserId = p.ID
            ORDER BY SentAt ASC
        """;
        return jdbcTemplate.query(sql, streamLiveChatRowMapper, limit, streamLiveId.toString());
    }

    /**
     * Delete chat messages for streams that ended before cutoffTime.
     */
    public int deleteChatMessagesForEndedStreams(Timestamp cutoffTime) {
        String sql = """
            DELETE FROM StreamLiveChat
            WHERE StreamLiveId IN (
                SELECT ID FROM StreamLive WHERE EndTime < ?
            )
        """;
        return jdbcTemplate.update(sql, cutoffTime);
    }
}
