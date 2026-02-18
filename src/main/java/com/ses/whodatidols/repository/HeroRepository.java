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
            // Check if sortOrder column exists, if not add it
            String checkSql = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'Hero' AND COLUMN_NAME = 'sortOrder'";
            Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class);
            if (count != null && count == 0) {
                jdbcTemplate.execute("ALTER TABLE Hero ADD sortOrder INT DEFAULT 0 NOT NULL");
                // Initialize sortOrder for existing records
                jdbcTemplate.execute(
                        "WITH Ordered AS (SELECT ID, ROW_NUMBER() OVER (ORDER BY (SELECT NULL)) - 1 as newOrder FROM Hero) UPDATE H SET sortOrder = O.newOrder FROM Hero H JOIN Ordered O ON H.ID = O.ID");
            }
        } catch (Exception e) {
            System.err.println("Hero schema update failed: " + e.getMessage());
        }
    }

    public List<Map<String, Object>> getHeroVideos() {
        String robustSql = """
                SELECT * FROM (
                  SELECT H.[ID], H.[ReferenceId], M.[name],
                         (SELECT STRING_AGG(C.Name, ', ') FROM Categories C JOIN MovieCategories MC ON MC.CategoryID = C.ID WHERE MC.MovieID = M.ID) as [category],
                         H.[CustomSummary] as _content, 'Movie' AS [type], H.sortOrder
                  FROM Hero H INNER JOIN Movie M ON H.ReferenceId = M.ID
                  UNION ALL
                  SELECT H.[ID], H.[ReferenceId], S.[name],
                         (SELECT STRING_AGG(C.Name, ', ') FROM Categories C JOIN SeriesCategories SC ON SC.CategoryID = C.ID WHERE SC.SeriesID = S.ID) as [category],
                         H.[CustomSummary] as _content, 'SoapOpera' AS [type], H.sortOrder
                  FROM Hero H INNER JOIN Series S ON H.ReferenceId = S.ID
                ) AS Result ORDER BY sortOrder ASC
                """;
        return jdbcTemplate.queryForList(robustSql);
    }

    public UUID addHero(UUID contentId, String customSummary, String type) {
        // Ensure type consistency
        if ("Film".equalsIgnoreCase(type) || "Movie".equalsIgnoreCase(type)) {
            type = "Movie";
        } else {
            type = "SoapOpera";
        }

        UUID newId = UUID.randomUUID();
        String sql = "INSERT INTO Hero (ID, ReferenceId, CustomSummary, sortOrder) VALUES (?, ?, ?, (SELECT ISNULL(MAX(sortOrder), 0) + 1 FROM Hero))";

        jdbcTemplate.update(sql, newId, contentId, customSummary);

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
