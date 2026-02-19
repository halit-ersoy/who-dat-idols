package com.ses.whodatidols.repository;

import com.ses.whodatidols.viewmodel.CommentViewModel;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

@Repository
public class CommentRepository {
    private final JdbcTemplate jdbcTemplate;

    public CommentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        ensureSchema();
    }

    private void ensureSchema() {
        try {
            // Check if IsApproved column exists, if not add it
            String checkSql = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'Comments' AND COLUMN_NAME = 'IsApproved'";
            Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class);
            if (count != null && count == 0) {
                jdbcTemplate.execute("ALTER TABLE Comments ADD IsApproved BIT DEFAULT 0 NOT NULL");
                // Optional: Update existing comments to be approved
                jdbcTemplate.execute("UPDATE Comments SET IsApproved = 1");
            }
        } catch (Exception e) {
            System.err.println("Schema update failed: " + e.getMessage());
        }
    }

    public List<CommentViewModel> getComments(UUID contentId, String cookie) {
        String sql = """
                    SELECT
                        c.ID, c.Text as comment, c.CreatedAt as date, c.Spoiler,
                        COALESCE(p.Nickname, c.Nickname) as nickname,
                        p.profilePhoto,
                        c.LikeCount,
                        c.ParentId,
                        c.ContentId,
                        CASE WHEN cl.UserId IS NOT NULL THEN 1 ELSE 0 END as isLiked,
                        CASE WHEN c.UserId = p_me.ID THEN 1 ELSE 0 END as isAuthor,
                        c.IsApproved
                    FROM Comments c
                    LEFT JOIN Person p ON c.UserId = p.ID
                    LEFT JOIN Person p_me ON p_me.cookie = ?
                    LEFT JOIN CommentLikes cl ON c.ID = cl.CommentId AND cl.UserId = p_me.ID
                    WHERE c.ContentId = ? AND (c.IsApproved = 1 OR c.UserId = p_me.ID)
                    ORDER BY c.CreatedAt DESC
                """;

        List<CommentViewModel> allComments = jdbcTemplate.query(sql, new CommentRowMapper(),
                cookie,
                contentId.toString());

        return buildCommentTree(allComments);
    }

    private List<CommentViewModel> buildCommentTree(List<CommentViewModel> flatComments) {
        java.util.Map<UUID, CommentViewModel> map = new java.util.HashMap<>();
        List<CommentViewModel> roots = new java.util.ArrayList<>();

        // Initialize maps and reply lists
        for (CommentViewModel c : flatComments) {
            map.put(c.getId(), c);
            if (c.getReplies() == null) {
                c.setReplies(new java.util.ArrayList<>());
            }
        }

        // Build tree
        for (CommentViewModel c : flatComments) {
            if (c.getParentId() != null && map.containsKey(c.getParentId())) {
                map.get(c.getParentId()).getReplies().add(c);
            } else {
                roots.add(c);
            }
        }

        return roots;
    }

    public void addComment(UUID contentId, String cookie, String text, boolean spoiler, UUID parentId) {
        String sql = """
                    INSERT INTO Comments (ContentId, UserId, Text, Spoiler, Nickname, ParentId, IsApproved)
                    SELECT ?, ID, ?, ?, Nickname, ?, 0
                    FROM Person WHERE cookie = ?
                """;
        jdbcTemplate.update(sql,
                contentId.toString(),
                text,
                spoiler,
                parentId != null ? parentId.toString() : null,
                cookie);
    }

    public List<CommentViewModel> getPendingComments() {
        String sql = """
                    SELECT
                        c.ID, c.Text as comment, c.CreatedAt as date, c.Spoiler,
                        COALESCE(p.Nickname, c.Nickname) as nickname,
                        p.profilePhoto,
                        c.LikeCount,
                        c.ParentId,
                        c.ContentId,
                        0 as isLiked,
                        0 as isAuthor,
                        c.IsApproved,
                        COALESCE(m.Name, s.Name, (e.name + ' S' + CAST(e.SeasonNumber AS VARCHAR) + 'E' + CAST(e.EpisodeNumber AS VARCHAR))) as ContentName
                    FROM Comments c
                    LEFT JOIN Person p ON c.UserId = p.ID
                    LEFT JOIN Movie m ON c.ContentId = m.ID
                    LEFT JOIN Series s ON c.ContentId = s.ID
                    LEFT JOIN Episode e ON c.ContentId = e.ID
                    WHERE c.IsApproved = 0
                    ORDER BY c.CreatedAt ASC
                """;
        return jdbcTemplate.query(sql, new CommentRowMapper());
    }

    public void approveComment(UUID commentId) {
        jdbcTemplate.update("UPDATE Comments SET IsApproved = 1 WHERE ID = ?", commentId.toString());
    }

    public void rejectComment(UUID commentId) {
        String deleteLikesSql = """
                    WITH CommentTree AS (
                        SELECT ID FROM Comments WHERE ID = ?
                        UNION ALL
                        SELECT c.ID FROM Comments c
                        INNER JOIN CommentTree ct ON c.ParentId = ct.ID
                    )
                    DELETE FROM CommentLikes WHERE CommentId IN (SELECT ID FROM CommentTree)
                """;

        String deleteCommentsSql = """
                    WITH CommentTree AS (
                        SELECT ID FROM Comments WHERE ID = ?
                        UNION ALL
                        SELECT c.ID FROM Comments c
                        INNER JOIN CommentTree ct ON c.ParentId = ct.ID
                    )
                    DELETE FROM Comments WHERE ID IN (SELECT ID FROM CommentTree)
                """;

        jdbcTemplate.update(deleteLikesSql, commentId.toString());
        jdbcTemplate.update(deleteCommentsSql, commentId.toString());
    }

    public void likeComment(UUID commentId, String cookie) {
        String userIdSql = "SELECT ID FROM Person WHERE cookie = ?";
        try {
            UUID userId = jdbcTemplate.queryForObject(userIdSql, UUID.class, cookie);

            if (userId == null)
                return;

            String checkSql = "SELECT COUNT(*) FROM CommentLikes WHERE CommentId = ? AND UserId = ?";
            Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, commentId.toString(),
                    userId.toString());

            if (count != null && count > 0) {
                jdbcTemplate.update("DELETE FROM CommentLikes WHERE CommentId = ? AND UserId = ?", commentId.toString(),
                        userId.toString());
                jdbcTemplate.update("UPDATE Comments SET LikeCount = LikeCount - 1 WHERE ID = ?", commentId.toString());
            } else {
                jdbcTemplate.update("INSERT INTO CommentLikes (CommentId, UserId) VALUES (?, ?)", commentId.toString(),
                        userId.toString());
                jdbcTemplate.update("UPDATE Comments SET LikeCount = LikeCount + 1 WHERE ID = ?", commentId.toString());
            }
        } catch (Exception e) {
        }
    }

    @org.springframework.transaction.annotation.Transactional
    public void deleteComment(UUID commentId, String cookie) {
        String deleteLikesSql = """
                    WITH CommentTree AS (
                        SELECT ID FROM Comments WHERE ID = ? AND UserId = (SELECT ID FROM Person WHERE cookie = ?)
                        UNION ALL
                        SELECT c.ID FROM Comments c
                        INNER JOIN CommentTree ct ON c.ParentId = ct.ID
                    )
                    DELETE FROM CommentLikes WHERE CommentId IN (SELECT ID FROM CommentTree)
                """;

        String deleteCommentsSql = """
                    WITH CommentTree AS (
                        SELECT ID FROM Comments WHERE ID = ? AND UserId = (SELECT ID FROM Person WHERE cookie = ?)
                        UNION ALL
                        SELECT c.ID FROM Comments c
                        INNER JOIN CommentTree ct ON c.ParentId = ct.ID
                    )
                    DELETE FROM Comments WHERE ID IN (SELECT ID FROM CommentTree)
                """;

        jdbcTemplate.update(deleteLikesSql, commentId.toString(), cookie);
        jdbcTemplate.update(deleteCommentsSql, commentId.toString(), cookie);
    }

    private static class CommentRowMapper implements RowMapper<CommentViewModel> {
        @Override
        public CommentViewModel mapRow(@org.springframework.lang.NonNull ResultSet rs, int rowNum) throws SQLException {
            CommentViewModel vm = new CommentViewModel();
            vm.setId(UUID.fromString(rs.getString("ID")));
            vm.setNickname(rs.getString("nickname"));
            vm.setComment(rs.getString("comment"));
            vm.setSpoiler(rs.getBoolean("Spoiler"));
            vm.setLikeCount(rs.getInt("LikeCount"));
            vm.setLikedByCurrentUser(rs.getInt("isLiked") > 0);
            vm.setAuthor(rs.getInt("isAuthor") > 0);
            vm.setProfilePhoto(rs.getString("profilePhoto"));

            try {
                vm.setApproved(rs.getBoolean("IsApproved"));
            } catch (SQLException e) {
                vm.setApproved(true);
            }

            String parentIdStr = rs.getString("ParentId");
            if (parentIdStr != null) {
                vm.setParentId(UUID.fromString(parentIdStr));
            }

            try {
                vm.setContentName(rs.getString("ContentName"));
            } catch (SQLException e) {
                vm.setContentName(null);
            }

            String contentIdStr = rs.getString("ContentId");
            if (contentIdStr != null) {
                vm.setContentId(UUID.fromString(contentIdStr));
            }

            java.sql.Timestamp ts = rs.getTimestamp("date");
            if (ts != null) {
                vm.setDate(ts.toLocalDateTime());
            }
            return vm;
        }
    }
}
