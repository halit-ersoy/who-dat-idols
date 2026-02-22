package com.ses.whodatidols.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.HashMap;

@Repository
public class ContentRepository {
    private final JdbcTemplate jdbcTemplate;

    public ContentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void incrementViewCount(UUID id) {
        String sql = "EXEC IncrementContentCountById @id = ?";
        jdbcTemplate.update(sql, id.toString());
    }

    /**
     * Returns the content type (movie, soap_opera, trailer) for the given ID.
     * The path resolution is now handled in the Service layer.
     */
    public String findVideoUrlById(UUID id) {
        try {
            String sql = "SELECT [dbo].[GetContentTypeById](?)";
            return jdbcTemplate.queryForObject(sql, String.class, id.toString());
        } catch (Exception e) {
            return "unknown";
        }
    }

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ContentRepository.class);

    public List<Map<String, Object>> searchContent(String nameTerm, Integer yearTerm, String typeTerm,
            String categoryTerm) {
        String sql = "EXEC [dbo].[SearchContentSmart] @NameTerm = ?, @YearTerm = ?, @TypeTerm = ?, @CategoryTerm = ?";

        try {
            return jdbcTemplate.query(sql, (rs, rowNum) -> {
                Map<String, Object> map = new HashMap<>();
                map.put("ID", rs.getString("ID"));
                map.put("Name", rs.getString("Name"));
                map.put("Category", rs.getString("Category"));
                map.put("Year", rs.getObject("Year"));
                map.put("Type", rs.getString("Type"));
                map.put("Description", rs.getString("Description"));
                return map;
            }, nameTerm, yearTerm, typeTerm, categoryTerm);
        } catch (Exception e) {
            logger.error("Error executing search stored procedure: {}", e.getMessage(), e);
            throw e;
        }
    }

    public List<Map<String, Object>> findSimilarContent(UUID contentId, int limit) {
        String sql = """
                WITH TargetCategories AS (
                    SELECT CategoryID FROM SeriesCategories WHERE SeriesID = ?
                    UNION ALL
                    SELECT CategoryID FROM MovieCategories WHERE MovieID = ?
                ),
                SimilarSeries AS (
                    SELECT S.ID, S.Name, 'soap_opera' as Type, S.viewCount,
                           (SELECT STRING_AGG(C.Name, ', ') FROM Categories C JOIN SeriesCategories SC2 ON SC2.CategoryID = C.ID WHERE SC2.SeriesID = S.ID) as Category,
                           COUNT(SC.CategoryID) as MatchCount,
                           (SELECT TOP 1 E.slug FROM Episode E WHERE E.SeriesId = S.ID ORDER BY SeasonNumber ASC, EpisodeNumber ASC) as slug
                    FROM Series S
                    JOIN SeriesCategories SC ON S.ID = SC.SeriesID
                    WHERE SC.CategoryID IN (SELECT CategoryID FROM TargetCategories)
                      AND S.ID <> ?
                    GROUP BY S.ID, S.Name, S.viewCount
                ),
                SimilarMovies AS (
                    SELECT M.ID, M.Name, 'movie' as Type, M.viewCount,
                           (SELECT STRING_AGG(C.Name, ', ') FROM Categories C JOIN MovieCategories MC2 ON MC2.CategoryID = C.ID WHERE MC2.MovieID = M.ID) as Category,
                           COUNT(MC.CategoryID) as MatchCount,
                           M.slug as slug
                    FROM Movie M
                    JOIN MovieCategories MC ON M.ID = MC.MovieID
                    WHERE MC.CategoryID IN (SELECT CategoryID FROM TargetCategories)
                      AND M.ID <> ?
                    GROUP BY M.ID, M.Name, M.viewCount, M.slug
                )
                SELECT TOP (?) ID, Name, Type, Category, viewCount, slug FROM (
                    SELECT * FROM SimilarSeries
                    UNION ALL
                    SELECT * FROM SimilarMovies
                ) Final
                ORDER BY MatchCount DESC, viewCount DESC
                """;

        try {
            return jdbcTemplate.query(sql, (rs, rowNum) -> {
                Map<String, Object> map = new HashMap<>();
                map.put("ID", rs.getString("ID"));
                map.put("Name", rs.getString("Name"));
                map.put("Category", rs.getString("Category"));
                map.put("Type", rs.getString("Type"));
                map.put("viewCount", rs.getInt("viewCount"));
                try {
                    map.put("slug", rs.getString("slug"));
                } catch (Exception e) {
                }
                return map;
            }, contentId.toString(), contentId.toString(), contentId.toString(), contentId.toString(), limit);
        } catch (Exception e) {
            logger.error("Error finding similar content for {}: {}", contentId, e.getMessage());
            return List.of();
        }
    }
}