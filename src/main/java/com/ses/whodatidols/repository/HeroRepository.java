package com.ses.whodatidols.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
public class HeroRepository {
    private final JdbcTemplate jdbcTemplate;

    public HeroRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        ensureSchema();
    }

    private void ensureSchema() {
        try {
            // 1. Check & add sortOrder
            String checkSortOrder = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'Hero' AND COLUMN_NAME = 'sortOrder'";
            Integer countSort = jdbcTemplate.queryForObject(checkSortOrder, Integer.class);
            if (countSort != null && countSort == 0) {
                jdbcTemplate.execute("ALTER TABLE Hero ADD sortOrder INT DEFAULT 0 NOT NULL");
                // Initialize sortOrder for existing records
                jdbcTemplate.execute(
                        "WITH Ordered AS (SELECT ID, ROW_NUMBER() OVER (ORDER BY (SELECT NULL)) - 1 as newOrder FROM Hero) UPDATE H SET sortOrder = O.newOrder FROM Hero H JOIN Ordered O ON H.ID = O.ID");
            }

            // 2. Check & add isImage if missing
            String checkIsImage = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'Hero' AND COLUMN_NAME = 'isImage'";
            Integer countImg = jdbcTemplate.queryForObject(checkIsImage, Integer.class);
            if (countImg != null && countImg == 0) {
                jdbcTemplate.execute("ALTER TABLE Hero ADD isImage BIT DEFAULT 0 NOT NULL");
            }

            // 3. Sync existing data using mediaType if it exists, then drop it
            String checkMediaType = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'Hero' AND COLUMN_NAME = 'mediaType'";
            Integer countMedia = jdbcTemplate.queryForObject(checkMediaType, Integer.class);
            if (countMedia != null && countMedia > 0) {
                jdbcTemplate.execute("UPDATE Hero SET isImage = 1 WHERE mediaType = 'image'");
                jdbcTemplate.execute("UPDATE Hero SET isImage = 0 WHERE mediaType = 'video' OR mediaType IS NULL OR isImage IS NULL");
                jdbcTemplate.execute("ALTER TABLE Hero DROP COLUMN mediaType");
            }

            // 4. Drop heroType if it exists
            String checkHeroType = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'Hero' AND COLUMN_NAME = 'heroType'";
            Integer countHero = jdbcTemplate.queryForObject(checkHeroType, Integer.class);
            if (countHero != null && countHero > 0) {
                jdbcTemplate.execute("ALTER TABLE Hero DROP COLUMN heroType");
            }

            // 5. Check & add platform
            String checkPlatform = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'Hero' AND COLUMN_NAME = 'platform'";
            Integer countPlat = jdbcTemplate.queryForObject(checkPlatform, Integer.class);
            if (countPlat != null && countPlat == 0) {
                jdbcTemplate.execute("ALTER TABLE Hero ADD platform NVARCHAR(50) DEFAULT 'whodatidols' NOT NULL");
                try {
                    jdbcTemplate.execute("CREATE INDEX IX_Hero_Platform_SortOrder ON Hero (platform, sortOrder)");
                } catch (Exception ignored) {}
            }

            // 6. Migrate any BL/GL hero references to platform = 'bldizi'
            try {
                jdbcTemplate.execute("""
                    UPDATE H
                    SET platform = 'bldizi'
                    FROM Hero H
                    WHERE H.platform = 'whodatidols'
                      AND (
                        EXISTS (SELECT 1 FROM Series S WHERE S.ID = H.ReferenceId AND (S.isBl = 1 OR S.SeriesType IN ('BL', 'GL')))
                        OR EXISTS (SELECT 1 FROM Movie M WHERE M.ID = H.ReferenceId AND (M.isBl = 1 OR M.MovieType IN ('BL', 'GL')))
                      )
                """);
            } catch (Exception ignored) {}
        } catch (Exception e) {
            System.err.println("Hero schema update failed: " + e.getMessage());
        }
    }

    public List<Map<String, Object>> getHeroVideos() {
        return getHeroVideos("whodatidols");
    }

    public List<Map<String, Object>> getHeroVideos(String platform) {
        if (platform == null || platform.isBlank()) {
            platform = "whodatidols";
        }
        platform = platform.trim().toLowerCase();

        String sql = """
                SELECT * FROM (
                  SELECT H.[ID], H.[ReferenceId], M.[name], M.[slug] as [slug],
                         (SELECT STRING_AGG(C.Name, ', ') FROM Categories C JOIN MovieCategories MC ON MC.CategoryID = C.ID WHERE MC.MovieID = M.ID) as [category],
                         H.[CustomSummary] as _content, 'Movie' AS [type], H.sortOrder, H.isImage, H.platform
                  FROM Hero H INNER JOIN Movie M ON H.ReferenceId = M.ID
                  WHERE H.platform = ? AND M.IsHidden = 0
                  UNION ALL
                  SELECT H.[ID], H.[ReferenceId], S.[name],
                         (SELECT TOP 1 E.[slug] FROM Episode E WHERE E.SeriesId = S.ID ORDER BY SeasonNumber ASC, EpisodeNumber ASC) as [slug],
                         (SELECT STRING_AGG(C.Name, ', ') FROM Categories C JOIN SeriesCategories SC ON SC.CategoryID = C.ID WHERE SC.SeriesID = S.ID) as [category],
                         H.[CustomSummary] as _content, 'SoapOpera' AS [type], H.sortOrder, H.isImage, H.platform
                  FROM Hero H INNER JOIN Series S ON H.ReferenceId = S.ID
                  WHERE H.platform = ? AND S.IsHidden = 0
                ) AS Result ORDER BY sortOrder ASC
                """;
        return jdbcTemplate.queryForList(sql, platform, platform);
    }

    public List<Map<String, Object>> getHeroVideosForAdmin(String platform) {
        boolean filterByPlatform = platform != null && !platform.isBlank() && !"all".equalsIgnoreCase(platform);
        String normPlat = filterByPlatform ? platform.trim().toLowerCase() : null;

        String movieWhere = filterByPlatform ? " WHERE H.platform = ? " : "";
        String seriesWhere = filterByPlatform ? " WHERE H.platform = ? " : "";

        String sql = """
                SELECT * FROM (
                  SELECT H.[ID], H.[ReferenceId], M.[name], M.[slug] as [slug],
                         (SELECT STRING_AGG(C.Name, ', ') FROM Categories C JOIN MovieCategories MC ON MC.CategoryID = C.ID WHERE MC.MovieID = M.ID) as [category],
                         H.[CustomSummary] as _content, 'Movie' AS [type], H.sortOrder, H.isImage, H.platform
                  FROM Hero H INNER JOIN Movie M ON H.ReferenceId = M.ID
                  """ + movieWhere + """
                  UNION ALL
                  SELECT H.[ID], H.[ReferenceId], S.[name],
                         (SELECT TOP 1 E.[slug] FROM Episode E WHERE E.SeriesId = S.ID ORDER BY SeasonNumber ASC, EpisodeNumber ASC) as [slug],
                         (SELECT STRING_AGG(C.Name, ', ') FROM Categories C JOIN SeriesCategories SC ON SC.CategoryID = C.ID WHERE SC.SeriesID = S.ID) as [category],
                         H.[CustomSummary] as _content, 'SoapOpera' AS [type], H.sortOrder, H.isImage, H.platform
                  FROM Hero H INNER JOIN Series S ON H.ReferenceId = S.ID
                  """ + seriesWhere + """
                ) AS Result ORDER BY platform ASC, sortOrder ASC
                """;

        if (filterByPlatform) {
            return jdbcTemplate.queryForList(sql, normPlat, normPlat);
        } else {
            return jdbcTemplate.queryForList(sql);
        }
    }

    public List<Map<String, Object>> searchContentForHero(String q, String platform) {
        String queryTerm = (q != null && !q.trim().isEmpty()) ? "%" + q.trim() + "%" : "%";

        String sql = """
            SELECT TOP 20 ID, Name, Type, isBl, Category FROM (
                SELECT CAST(M.ID AS NVARCHAR(36)) AS ID, M.Name, 'Movie' AS Type, M.isBl,
                       (SELECT STRING_AGG(C.Name, ', ') FROM Categories C JOIN MovieCategories MC ON MC.CategoryID = C.ID WHERE MC.MovieID = M.ID) AS Category
                FROM Movie M
                WHERE M.Name LIKE ?
                UNION ALL
                SELECT CAST(S.ID AS NVARCHAR(36)) AS ID, S.Name, 'SoapOpera' AS Type, S.isBl,
                       (SELECT STRING_AGG(C.Name, ', ') FROM Categories C JOIN SeriesCategories SC ON SC.CategoryID = C.ID WHERE SC.SeriesID = S.ID) AS Category
                FROM Series S
                WHERE S.Name LIKE ?
            ) AS Combined
            ORDER BY Name ASC
        """;
        return jdbcTemplate.queryForList(sql, queryTerm, queryTerm);
    }

    public UUID addHero(UUID contentId, String customSummary, String type, boolean isImage) {
        return addHero(contentId, customSummary, type, isImage, "whodatidols");
    }

    public UUID addHero(UUID contentId, String customSummary, String type, boolean isImage, String platform) {
        // Ensure type consistency
        if ("Film".equalsIgnoreCase(type) || "Movie".equalsIgnoreCase(type)) {
            type = "Movie";
        } else {
            type = "SoapOpera";
        }

        if (platform == null || platform.isBlank()) {
            platform = "whodatidols";
        }
        platform = platform.trim().toLowerCase();

        UUID newId = UUID.randomUUID();
        String sql = """
            INSERT INTO Hero (ID, ReferenceId, CustomSummary, sortOrder, isImage, platform)
            VALUES (?, ?, ?, (SELECT ISNULL(MAX(sortOrder), -1) + 1 FROM Hero WHERE platform = ?), ?, ?)
        """;

        jdbcTemplate.update(sql, newId, contentId, customSummary, platform, isImage ? 1 : 0, platform);

        return newId;
    }

    public void updateHeroOrder(List<String> heroIds) {
        String sql = "UPDATE Hero SET sortOrder = ? WHERE ID = ?";
        for (int i = 0; i < heroIds.size(); i++) {
            jdbcTemplate.update(sql, i, heroIds.get(i));
        }
    }

    public void deleteHero(UUID id) {
        jdbcTemplate.update("DELETE FROM Hero WHERE ID = ?", id.toString());
    }
}
