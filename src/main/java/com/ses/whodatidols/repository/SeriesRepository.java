package com.ses.whodatidols.repository;

import com.ses.whodatidols.model.Episode;
import com.ses.whodatidols.model.Series;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.lang.NonNull;

import java.util.List;
import java.util.UUID;

@Repository
public class SeriesRepository {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public SeriesRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final @NonNull RowMapper<Series> seriesRowMapper = (rs, rowNum) -> {
        Series s = new Series();
        s.setId(UUID.fromString(rs.getString("ID")));
        s.setName(rs.getString("name"));
        s.setCategory(rs.getString("category"));
        s.setSummary(rs.getString("Summary"));
        s.setLanguage(rs.getString("Language"));
        // Handle nullable finalStatus
        Object fs = rs.getObject("finalStatus");
        if (fs instanceof Integer) {
            s.setFinalStatus((Integer) fs);
        } else if (fs instanceof Boolean) {
            s.setFinalStatus((Boolean) fs ? 1 : 0);
        } else {
            s.setFinalStatus(fs != null ? Integer.parseInt(fs.toString()) : 0);
        }
        s.setEpisodeMetadataXml(rs.getString("EpisodeMetadataXml"));
        try {
            java.sql.Timestamp ts = rs.getTimestamp("uploadDate");
            if (ts != null) {
                s.setUploadDate(ts.toLocalDateTime());
            }
        } catch (Exception e) {
            // Might not exist in all queries
        }

        // Check if episodeCount column exists in RS (it will when using the new query)
        try {
            s.setEpisodeCount(rs.getInt("episodeCount"));
        } catch (Exception e) {
            // Column might not exist in all queries using this mapper
        }
        return s;
    };

    private final @NonNull RowMapper<Episode> episodeRowMapper = (rs, rowNum) -> {
        Episode e = new Episode();
        e.setId(UUID.fromString(rs.getString("ID")));
        e.setName(rs.getString("name"));
        e.setDurationMinutes(rs.getInt("DurationMinutes"));
        e.setReleaseYear(rs.getInt("ReleaseYear"));
        e.setUploadDate(rs.getTimestamp("uploadDate").toLocalDateTime());

        // Handle new columns if they exist (they should after migration)
        String seriesId = rs.getString("SeriesId");
        if (seriesId != null)
            e.setSeriesId(UUID.fromString(seriesId));

        e.setSeasonNumber(rs.getInt("SeasonNumber"));
        e.setEpisodeNumber(rs.getInt("EpisodeNumber"));

        return e;
    };

    public List<Series> findAllSeries() {
        // Optimized query with subquery for count.
        // Also assuming Series table is just 'Series' now.
        // We use LEFT JOIN or Correlated Subquery. Subquery is safer for 1:N count.
        String sql = """
                SELECT S.ID, S.name, S.Summary, S.Language, S.finalStatus, S.EpisodeMetadataXml, S.uploadDate,
                       (SELECT COUNT(*) FROM Episode E WHERE E.SeriesId = S.ID) as episodeCount,
                       (SELECT STRING_AGG(C.Name, ', ') FROM Categories C
                        JOIN SeriesCategories SC ON SC.CategoryID = C.ID
                        WHERE SC.SeriesID = S.ID) as category
                FROM Series S
                ORDER BY S.viewCount DESC, S.name ASC
                """;
        return jdbcTemplate.query(sql, seriesRowMapper);
    }

    public Series findSeriesByName(String name) {
        try {
            String sql = """
                    SELECT S.ID, S.name, S.Summary, S.Language, S.finalStatus, S.EpisodeMetadataXml, S.uploadDate,
                           (SELECT STRING_AGG(C.Name, ', ') FROM Categories C
                            JOIN SeriesCategories SC ON SC.CategoryID = C.ID
                            WHERE SC.SeriesID = S.ID) as category
                    FROM Series S
                    WHERE S.name = ?
                    """;
            return jdbcTemplate.queryForObject(sql, seriesRowMapper, name);
        } catch (Exception e) {
            return null;
        }
    }

    public Series findSeriesById(UUID id) {
        try {
            String sql = """
                    SELECT S.ID, S.name, S.Summary, S.Language, S.finalStatus, S.EpisodeMetadataXml, S.uploadDate,
                           (SELECT STRING_AGG(C.Name, ', ') FROM Categories C
                            JOIN SeriesCategories SC ON SC.CategoryID = C.ID
                            WHERE SC.SeriesID = S.ID) as category
                    FROM Series S
                    WHERE S.ID = ?
                    """;
            return jdbcTemplate.queryForObject(sql, seriesRowMapper, id.toString());
        } catch (Exception e) {
            return null;
        }
    }

    public Episode findEpisodeById(UUID id) {
        try {
            return jdbcTemplate.queryForObject("SELECT * FROM Episode WHERE ID = ?", episodeRowMapper, id.toString());
        } catch (Exception e) {
            return null;
        }
    }

    public void createSeries(Series series) {
        jdbcTemplate.update(
                "INSERT INTO Series (ID, name, Summary, Language, finalStatus, EpisodeMetadataXml, uploadDate) VALUES (?, ?, ?, ?, ?, ?, ?)",
                series.getId().toString(),
                series.getName(),
                series.getSummary(),
                series.getLanguage(),
                series.getFinalStatus(),
                series.getEpisodeMetadataXml(),
                series.getUploadDate());

        updateSeriesCategories(series.getId(), series.getCategory());
    }

    public void updateSeriesMetadata(Series series) {
        jdbcTemplate.update(
                "UPDATE Series SET name=?, Summary=?, Language=?, finalStatus=? WHERE ID=?",
                series.getName(),
                series.getSummary(),
                series.getLanguage(),
                series.getFinalStatus(),
                series.getId().toString());

        updateSeriesCategories(series.getId(), series.getCategory());
    }

    private void updateSeriesCategories(UUID seriesId, String categoriesStr) {
        // Clear existing
        jdbcTemplate.update("DELETE FROM SeriesCategories WHERE SeriesID = ?", seriesId.toString());

        if (categoriesStr != null && !categoriesStr.isBlank()) {
            for (String cat : categoriesStr.split(",")) {
                String trimmed = cat.trim();
                if (trimmed.isEmpty())
                    continue;

                // Ensure category exists
                jdbcTemplate.update(
                        "IF NOT EXISTS (SELECT 1 FROM Categories WHERE Name = ?) INSERT INTO Categories (Name) VALUES (?)",
                        trimmed, trimmed);
                jdbcTemplate.update(
                        "INSERT INTO SeriesCategories (SeriesID, CategoryID) SELECT ?, ID FROM Categories WHERE Name = ?",
                        seriesId.toString(), trimmed);
            }
        }
    }

    public void updateSeriesXML(UUID id, String xml) {
        jdbcTemplate.update("UPDATE Series SET EpisodeMetadataXml = ? WHERE ID = ?", xml, id.toString());
    }

    public void saveEpisode(Episode episode) {
        jdbcTemplate.update(
                "INSERT INTO Episode (ID, name, DurationMinutes, ReleaseYear, uploadDate, SeriesId, SeasonNumber, EpisodeNumber) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                episode.getId().toString(),
                episode.getName(),
                episode.getDurationMinutes(),
                episode.getReleaseYear(),
                episode.getUploadDate(),
                episode.getSeriesId(),
                episode.getSeasonNumber(),
                episode.getEpisodeNumber());
    }

    public void updateEpisode(Episode episode) {
        jdbcTemplate.update(
                "UPDATE Episode SET DurationMinutes = ?, ReleaseYear = ?, SeasonNumber = ?, EpisodeNumber = ? WHERE ID = ?",
                episode.getDurationMinutes(),
                episode.getReleaseYear(),
                episode.getSeasonNumber(),
                episode.getEpisodeNumber(),
                episode.getId().toString());
    }

    public void deleteEpisodeById(UUID id) {
        jdbcTemplate.update("DELETE FROM Episode WHERE ID = ?", id.toString());
    }

    public void deleteSeriesById(UUID id) {
        jdbcTemplate.update("DELETE FROM Series WHERE ID = ?", id.toString());
    }

    // This method queried Series based on XML content.
    // New optimization: We can query Episode -> SeriesId directly if migrated!
    // But for now, to support legacy/transition, we keep looking at XML referencing
    // this episode ID relative to a Series?
    // Actually, findSeriesByEpisodeIdInsideXML was used to update parent XML when
    // deleting an episode.
    // If we migrate fully to FK, we don't need XML anymore!
    // But let's keep it compatible with the previous
    // 'findSeriesByEpisodeIdInsideXML' logic for now using the old column
    // (renamed).
    public Series findSeriesByEpisodeId(UUID episodeId) {
        try {
            // Priority 1: Use FK JOIN (New optimized schema)
            String sql = """
                    SELECT S.ID, S.name, S.Summary, S.Language, S.finalStatus, S.EpisodeMetadataXml, S.uploadDate,
                           (SELECT STRING_AGG(C.Name, ', ') FROM Categories C
                            JOIN SeriesCategories SC ON SC.CategoryID = C.ID
                            WHERE SC.SeriesID = S.ID) as category
                    FROM Series S
                    JOIN Episode E ON E.SeriesId = S.ID
                    WHERE E.ID = ?
                    """;
            return jdbcTemplate.queryForObject(sql, seriesRowMapper, episodeId.toString());
        } catch (Exception e) {
            // Priority 2: Fallback to XML (Legacy/Transition support)
            return findSeriesByEpisodeIdInsideXML(episodeId.toString());
        }
    }

    public Series findSeriesByEpisodeIdInsideXML(String episodeId) {
        try {
            String sql = """
                    SELECT S.ID, S.name, S.Summary, S.Language, S.finalStatus, S.EpisodeMetadataXml, S.uploadDate,
                           (SELECT STRING_AGG(C.Name, ', ') FROM Categories C
                            JOIN SeriesCategories SC ON SC.CategoryID = C.ID
                            WHERE SC.SeriesID = S.ID) as category
                    FROM Series S
                    WHERE S.EpisodeMetadataXml LIKE ?
                    """;
            return jdbcTemplate.queryForObject(sql, seriesRowMapper, "%" + episodeId + "%");
        } catch (Exception e) {
            return null;
        }
    }

    // New method for Optimized schema
    public List<Episode> findEpisodesBySeriesId(UUID seriesId) {
        return jdbcTemplate.query("SELECT * FROM Episode WHERE SeriesId = ? ORDER BY SeasonNumber, EpisodeNumber",
                episodeRowMapper, seriesId.toString());
    }

    public List<Episode> findRecentEpisodes(int limit) {
        return jdbcTemplate.query("SELECT TOP (?) * FROM Episode ORDER BY uploadDate DESC", episodeRowMapper, limit);
    }

    public List<Series> findRecentSeries(int limit) {
        // Assuming recently updated series? Or just all series limited?
        // original SoapOperaRepository probably had logic. Assuming 'uploadDate' or
        // just ID?
        // Wait, Series table doesn't have 'uploadDate', Episode does.
        // But maybe we want series that have recent episodes?
        // Or maybe Series table DOES have uploadDate? Let's check original SoapOpera
        // model.
        // Original SoapOpera had uploadDate.
        // My Series model DOES NOT have uploadDate visible in RowMapper?
        // RowMapper: "s.setEpisodeMetadataXml(rs.getString("EpisodeMetadataXml"));"
        // Let's check if I kept 'uploadDate' in Series table rename script.
        // Table 'SoapOperas' had 'uploadDate'. Rename script: "EXEC sp_rename
        // 'SoapOperas', 'Series';"
        // So 'Series' table HAS 'uploadDate'.
        // I should update Series RowMapper to include 'uploadDate' if needed for
        // sorting?
        // Or just trust the DB sort.
        // Let's check Series.java model again.
        // Series.java has no uploadDate field!
        // I need to add uploadDate to Series.java model if I want to map it, or just
        // return list.
        // But RowMapper doesn't map it, so it naturally won't be in the object.
        // This might be fine if we just want the list sorted by DB.

        String sql = """
                SELECT TOP (?) S.ID, S.name, S.Summary, S.Language, S.finalStatus, S.EpisodeMetadataXml, S.uploadDate,
                       (SELECT STRING_AGG(C.Name, ', ') FROM Categories C
                        JOIN SeriesCategories SC ON SC.CategoryID = C.ID
                        WHERE SC.SeriesID = S.ID) as category
                FROM Series S
                ORDER BY S.uploadDate DESC
                """;
        return jdbcTemplate.query(sql, seriesRowMapper, limit);
    }

    public List<Episode> findTop6EpisodesByCount() {
        String sql = "SELECT TOP 6 * FROM Episode ORDER BY viewCount DESC";
        return jdbcTemplate.query(sql, episodeRowMapper);
    }

    public List<Series> findTop6SeriesByCount() {
        String sql = """
                SELECT TOP 6 S.ID, S.name, S.Summary, S.Language, S.finalStatus, S.EpisodeMetadataXml, S.uploadDate,
                       (SELECT COUNT(*) FROM Episode E WHERE E.SeriesId = S.ID) as episodeCount,
                       (SELECT STRING_AGG(C.Name, ', ') FROM Categories C
                        JOIN SeriesCategories SC ON SC.CategoryID = C.ID
                        WHERE SC.SeriesID = S.ID) as category
                FROM Series S
                ORDER BY S.viewCount DESC, S.name ASC
                """;
        return jdbcTemplate.query(sql, seriesRowMapper);
    }
}
