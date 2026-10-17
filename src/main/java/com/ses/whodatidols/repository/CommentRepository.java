package com.ses.whodatidols.repository;

import com.ses.whodatidols.viewmodel.CommentViewModel;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
@SuppressWarnings({"SqlResolve", "SqlNoDataSourceInspection"})
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

    public List<CommentViewModel> getComments(String idOrSlug, String cookie) {
        String sql = """
                    WITH TargetContent AS (
                        SELECT TRY_CAST(? AS UNIQUEIDENTIFIER) AS ID
                        UNION
                        SELECT ID FROM Upcoming WHERE Slug = ? OR Name = ?
                        UNION
                        SELECT ID FROM Series WHERE Slug = ? OR Name = ?
                        UNION
                        SELECT ID FROM Movie WHERE Slug = ? OR Name = ?
                        UNION
                        SELECT ID FROM Episode WHERE Slug = ? OR Name = ?
                    ),
                    RelatedIDs AS (
                        SELECT ID FROM TargetContent WHERE ID IS NOT NULL
                        UNION
                        SELECT e.SeriesId FROM Episode e JOIN TargetContent t ON e.ID = t.ID WHERE e.SeriesId IS NOT NULL
                        UNION
                        SELECT e.ID FROM Episode e JOIN TargetContent t ON e.SeriesId = t.ID WHERE e.SeasonNumber = 1 AND e.EpisodeNumber = 1
                        UNION
                        SELECT e1.ID FROM Episode ep JOIN TargetContent t ON ep.ID = t.ID JOIN Episode e1 ON e1.SeriesId = ep.SeriesId WHERE e1.SeasonNumber = 1 AND e1.EpisodeNumber = 1
                        UNION
                        SELECT u.ID FROM Upcoming u
                        JOIN Episode e ON e.ID IN (SELECT ID FROM TargetContent)
                        JOIN Series s ON e.SeriesId = s.ID
                        WHERE u.Name COLLATE Latin1_General_CI_AI = s.Name COLLATE Latin1_General_CI_AI
                           OR u.ReferenceId = s.ID
                           OR u.ReferenceId = e.ID
                        UNION
                        SELECT u.ID FROM Upcoming u
                        JOIN Series s ON s.ID IN (SELECT ID FROM TargetContent)
                        WHERE u.Name COLLATE Latin1_General_CI_AI = s.Name COLLATE Latin1_General_CI_AI
                           OR u.ReferenceId = s.ID
                        UNION
                        SELECT u.ID FROM Upcoming u
                        JOIN Movie m ON m.ID IN (SELECT ID FROM TargetContent)
                        WHERE u.Name COLLATE Latin1_General_CI_AI = m.Name COLLATE Latin1_General_CI_AI
                           OR u.ReferenceId = m.ID
                        UNION
                        SELECT u.ReferenceId FROM Upcoming u JOIN TargetContent t ON u.ID = t.ID WHERE u.ReferenceId IS NOT NULL
                        UNION
                        SELECT s.ID FROM Series s JOIN Upcoming u ON s.Name COLLATE Latin1_General_CI_AI = u.Name COLLATE Latin1_General_CI_AI JOIN TargetContent t ON u.ID = t.ID
                        UNION
                        SELECT e.ID FROM Episode e JOIN Series s ON e.SeriesId = s.ID JOIN Upcoming u ON s.Name COLLATE Latin1_General_CI_AI = u.Name COLLATE Latin1_General_CI_AI JOIN TargetContent t ON u.ID = t.ID WHERE e.SeasonNumber = 1 AND e.EpisodeNumber = 1
                        UNION
                        SELECT e.ID FROM Episode e JOIN Upcoming u ON e.SeriesId = u.ReferenceId JOIN TargetContent t ON u.ID = t.ID WHERE e.SeasonNumber = 1 AND e.EpisodeNumber = 1
                        UNION
                        SELECT m.ID FROM Movie m JOIN Upcoming u ON m.Name COLLATE Latin1_General_CI_AI = u.Name COLLATE Latin1_General_CI_AI JOIN TargetContent t ON u.ID = t.ID
                    )
                    SELECT
                        c.ID, c.Text as comment, c.CreatedAt as date, c.Spoiler,
                        COALESCE(p.Nickname, c.Nickname) as nickname,
                        c.UserId as authorId,
                        c.LikeCount,
                        c.ParentId,
                        c.ContentId,
                        CASE WHEN cl.UserId IS NOT NULL THEN 1 ELSE 0 END as isLiked,
                        CASE WHEN c.UserId = p_me.ID THEN 1 ELSE 0 END as isAuthor,
                        c.IsApproved,
                        p.Role,
                        p.isVerified
                    FROM Comments c
                    LEFT JOIN Person p ON c.UserId = p.ID
                    LEFT JOIN Person p_me ON p_me.ID = [dbo].[ResolveUserIdByCookie](TRY_CAST(? AS UNIQUEIDENTIFIER))
                    LEFT JOIN CommentLikes cl ON c.ID = cl.CommentId AND cl.UserId = p_me.ID
                    WHERE c.ContentId IN (SELECT ID FROM RelatedIDs) AND (c.IsApproved = 1 OR c.UserId = p_me.ID)
                    ORDER BY c.CreatedAt DESC
                """;

        List<CommentViewModel> allComments = jdbcTemplate.query(sql, new CommentRowMapper(),
                idOrSlug, idOrSlug, idOrSlug, idOrSlug, idOrSlug, idOrSlug, idOrSlug, idOrSlug, idOrSlug,
                cookie);

        return buildCommentTree(allComments);
    }

    public List<CommentViewModel> getComments(UUID contentId, String cookie) {
        return getComments(contentId != null ? contentId.toString() : null, cookie);
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

    public void addComment(String idOrSlug, String cookie, String text, boolean spoiler, UUID parentId) {
        String sql = """
                    INSERT INTO Comments (ContentId, UserId, Text, Spoiler, Nickname, ParentId, IsApproved, CreatedAt)
                    SELECT COALESCE(
                        TRY_CAST(? AS UNIQUEIDENTIFIER),
                        (SELECT TOP 1 ID FROM Upcoming WHERE Slug = ? OR Name = ?),
                        (SELECT TOP 1 ID FROM Episode WHERE Slug = ? OR Name = ?),
                        (SELECT TOP 1 ID FROM Series WHERE Slug = ? OR Name = ?),
                        (SELECT TOP 1 ID FROM Movie WHERE Slug = ? OR Name = ?)
                    ), ID, ?, ?, Nickname, TRY_CAST(? AS UNIQUEIDENTIFIER), CASE WHEN Role IN ('ADMIN', 'SUPER_ADMIN', 'KURUCU', 'GELISTIRICI') THEN 1 ELSE 0 END, ?
                    FROM Person WHERE ID = [dbo].[ResolveUserIdByCookie](TRY_CAST(? AS UNIQUEIDENTIFIER)) AND isBanned = 0
                """;
        int affected = jdbcTemplate.update(sql,
                idOrSlug, idOrSlug, idOrSlug, idOrSlug, idOrSlug, idOrSlug, idOrSlug, idOrSlug, idOrSlug,
                text,
                spoiler,
                parentId != null ? parentId.toString() : null,
                java.sql.Timestamp.from(Instant.now()),
                cookie);

        if (affected == 0) {
            throw new RuntimeException("Unauthorized or Banned");
        }
    }

    public void addComment(UUID contentId, String cookie, String text, boolean spoiler, UUID parentId) {
        addComment(contentId != null ? contentId.toString() : null, cookie, text, spoiler, parentId);
    }

    public List<CommentViewModel> getPendingComments() {
        String sql = """
                    SELECT
                        c.ID, c.Text as comment, c.CreatedAt as date, c.Spoiler,
                        COALESCE(p.Nickname, c.Nickname) as nickname,
                        c.UserId as authorId,
                        c.LikeCount,
                        c.ParentId,
                        c.ContentId,
                        0 as isLiked,
                        0 as isAuthor,
                        c.IsApproved,
                        COALESCE(m.Name, s.Name, (e.name + ' S' + CAST(e.SeasonNumber AS VARCHAR) + 'E' + CAST(e.EpisodeNumber AS VARCHAR)), (u.Name + ' (Beklenen)')) as ContentName,
                        COALESCE(m.Slug, e.Slug, u.Slug, CAST(c.ContentId AS VARCHAR(36))) as ContentSlug,
                        p.Role,
                        p.isVerified
                    FROM Comments c
                    LEFT JOIN Person p ON c.UserId = p.ID
                    LEFT JOIN Movie m ON c.ContentId = m.ID
                    LEFT JOIN Series s ON c.ContentId = s.ID
                    LEFT JOIN Episode e ON c.ContentId = e.ID
                    LEFT JOIN Upcoming u ON c.ContentId = u.ID
                    WHERE c.IsApproved = 0
                    ORDER BY c.CreatedAt ASC
                """;
        return jdbcTemplate.query(sql, new CommentRowMapper());
    }

    public List<CommentViewModel> getApprovedComments() {
        String sql = """
                    SELECT
                        c.ID, c.Text as comment, c.CreatedAt as date, c.Spoiler,
                        COALESCE(p.Nickname, c.Nickname) as nickname,
                        c.UserId as authorId,
                        c.LikeCount,
                        c.ParentId,
                        c.ContentId,
                        0 as isLiked,
                        0 as isAuthor,
                        c.IsApproved,
                        COALESCE(m.Name, s.Name, (e.name + ' S' + CAST(e.SeasonNumber AS VARCHAR) + 'E' + CAST(e.EpisodeNumber AS VARCHAR)), (u.Name + ' (Beklenen)')) as ContentName,
                        COALESCE(m.Slug, e.Slug, u.Slug, CAST(c.ContentId AS VARCHAR(36))) as ContentSlug,
                        p.Role,
                        p.isVerified
                    FROM Comments c
                    LEFT JOIN Person p ON c.UserId = p.ID
                    LEFT JOIN Movie m ON c.ContentId = m.ID
                    LEFT JOIN Series s ON c.ContentId = s.ID
                    LEFT JOIN Episode e ON c.ContentId = e.ID
                    LEFT JOIN Upcoming u ON c.ContentId = u.ID
                    WHERE c.IsApproved = 1
                    ORDER BY c.CreatedAt DESC
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
        String userSql = "SELECT ID FROM Person WHERE ID = [dbo].[ResolveUserIdByCookie](TRY_CAST(? AS UNIQUEIDENTIFIER)) AND isBanned = 0";
        try {
            UUID userId = jdbcTemplate.queryForObject(userSql, UUID.class, cookie);

            if (userId == null)
                throw new RuntimeException("Unauthorized or Banned");

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

    public String getUserRoleByCookie(String cookie) {
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT Role FROM Person WHERE ID = [dbo].[ResolveUserIdByCookie](TRY_CAST(? AS UNIQUEIDENTIFIER))",
                    String.class, cookie);
        } catch (Exception e) {
            return "USER";
        }
    }

    @org.springframework.transaction.annotation.Transactional
    public boolean deleteComment(UUID commentId, String cookie) {
        String role = getUserRoleByCookie(cookie);
        boolean isAdmin = "ADMIN".equalsIgnoreCase(role) || "SUPER_ADMIN".equalsIgnoreCase(role);

        // Find comment owner
        String ownerSql = "SELECT UserId FROM Comments WHERE ID = ?";
        UUID ownerId;
        try {
            ownerId = UUID.fromString(jdbcTemplate.queryForObject(ownerSql, String.class, commentId.toString()));
        } catch (Exception e) {
            return false; // Comment not found
        }

        // Check ownership if not admin
        if (!isAdmin) {
            String requesterIdSql = "SELECT ID FROM Person WHERE ID = [dbo].[ResolveUserIdByCookie](TRY_CAST(? AS UNIQUEIDENTIFIER))";
            UUID requesterId;
            try {
                requesterId = UUID.fromString(jdbcTemplate.queryForObject(requesterIdSql, String.class, cookie));
            } catch (Exception e) {
                return false; // Requester not found or invalid cookie
            }

            if (!ownerId.equals(requesterId)) {
                return false; // Not the owner
            }
        }

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
        int affectedRows = jdbcTemplate.update(deleteCommentsSql, commentId.toString());
        return affectedRows > 0;
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

            try {
                vm.setApproved(rs.getBoolean("IsApproved"));
            } catch (SQLException ignored) {
            }

            try {
                String authorIdStr = rs.getString("authorId");
                if (authorIdStr != null) {
                    vm.setAuthorId(UUID.fromString(authorIdStr));
                }
            } catch (SQLException e) {
            }

            try {
                String contentIdStr = rs.getString("ContentId");
                if (contentIdStr != null) {
                    vm.setContentId(UUID.fromString(contentIdStr));
                }
            } catch (SQLException e) {
            }

            try {
                String parentIdStr = rs.getString("ParentId");
                if (parentIdStr != null) {
                    vm.setParentId(UUID.fromString(parentIdStr));
                }
            } catch (SQLException e) {
            }

            try {
                String contentName = rs.getString("ContentName");
                if (contentName != null) {
                    vm.setContentName(contentName);
                }
            } catch (SQLException ignored) {
            }

            try {
                String contentSlug = rs.getString("ContentSlug");
                if (contentSlug != null) {
                    vm.setContentSlug(contentSlug);
                }
            } catch (SQLException ignored) {
            }

            try {
                String role = rs.getString("Role");
                if (role != null) {
                    vm.setRole(role);
                }
            } catch (SQLException ignored) {
            }

            try {
                vm.setVerified(rs.getBoolean("isVerified"));
            } catch (SQLException ignored) {
            }

            try {
                java.sql.Timestamp ts = rs.getTimestamp("date");
                if (ts != null) {
                    vm.setDate(ts.toInstant());
                }
            } catch (SQLException ignored) {
            }

            return vm;
        }
    }
}
