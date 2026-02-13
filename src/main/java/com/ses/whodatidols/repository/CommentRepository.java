package com.ses.whodatidols.repository;

import com.ses.whodatidols.viewmodel.CommentViewModel;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public class CommentRepository {
    private final JdbcTemplate jdbcTemplate;

    public CommentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<CommentViewModel> getComments(UUID contentId, String cookie) {
        String sql = """
                    SELECT
                        c.ID, c.Text as comment, c.CreatedAt as date, c.Spoiler,
                        COALESCE(p.Nickname, c.Nickname) as nickname,
                        p.profilePhoto,
                        c.LikeCount,
                        c.ParentId,
                        CASE WHEN cl.UserId IS NOT NULL THEN 1 ELSE 0 END as isLiked,
                        CASE WHEN c.UserId = p_me.ID THEN 1 ELSE 0 END as isAuthor
                    FROM Comments c
                    LEFT JOIN Person p ON c.UserId = p.ID
                    LEFT JOIN Person p_me ON p_me.cookie = ?
                    LEFT JOIN CommentLikes cl ON c.ID = cl.CommentId AND cl.UserId = p_me.ID
                    WHERE c.ContentId = ?
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
                    INSERT INTO Comments (ContentId, UserId, Text, Spoiler, Nickname, ParentId)
                    SELECT ?, ID, ?, ?, Nickname, ?
                    FROM Person WHERE cookie = ?
                """;
        jdbcTemplate.update(sql,
                contentId.toString(),
                text,
                spoiler,
                parentId != null ? parentId.toString() : null,
                cookie);
    }

    public void likeComment(UUID commentId, String cookie) {
        String userIdSql = "SELECT ID FROM Person WHERE cookie = ?";
        try {
            UUID userId = jdbcTemplate.queryForObject(userIdSql, UUID.class, cookie);

            // Check if already liked
            String checkSql = "SELECT COUNT(*) FROM CommentLikes WHERE CommentId = ? AND UserId = ?";
            Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, commentId.toString(),
                    userId.toString());

            if (count != null && count > 0) {
                // Unlike
                jdbcTemplate.update("DELETE FROM CommentLikes WHERE CommentId = ? AND UserId = ?", commentId.toString(),
                        userId.toString());
                jdbcTemplate.update("UPDATE Comments SET LikeCount = LikeCount - 1 WHERE ID = ?", commentId.toString());
            } else {
                // Like
                jdbcTemplate.update("INSERT INTO CommentLikes (CommentId, UserId) VALUES (?, ?)", commentId.toString(),
                        userId.toString());
                jdbcTemplate.update("UPDATE Comments SET LikeCount = LikeCount + 1 WHERE ID = ?", commentId.toString());
            }
        } catch (Exception e) {
            // Handle error or ignore
        }
    }

    @org.springframework.transaction.annotation.Transactional
    public void deleteComment(UUID commentId, String cookie) {
        // First delete likes for the entire tree
        String deleteLikesSql = """
                    WITH CommentTree AS (
                        SELECT ID FROM Comments WHERE ID = ? AND UserId = (SELECT ID FROM Person WHERE cookie = ?)
                        UNION ALL
                        SELECT c.ID FROM Comments c
                        INNER JOIN CommentTree ct ON c.ParentId = ct.ID
                    )
                    DELETE FROM CommentLikes WHERE CommentId IN (SELECT ID FROM CommentTree)
                """;

        // Then delete the comments themselves
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

            String parentIdStr = rs.getString("ParentId");
            if (parentIdStr != null) {
                vm.setParentId(UUID.fromString(parentIdStr));
            }

            java.sql.Timestamp ts = rs.getTimestamp("date");
            if (ts != null) {
                vm.setDate(ts.toLocalDateTime());
            }
            return vm;
        }
    }
}
