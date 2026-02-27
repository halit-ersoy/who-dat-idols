package com.ses.whodatidols.repository;

import com.ses.whodatidols.model.Episode;
import com.ses.whodatidols.model.Series;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.lang.NonNull;

import java.util.List;
import java.util.UUID;

@Repository
public class SeriesRepository {

    private final JdbcTemplate jdbcTemplate;

    public SeriesRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        ensureSchema();
    }

    private void ensureSchema() {
        try {
            jdbcTemplate.execute(
                    "IF NOT EXISTS (SELECT * FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'Series' AND COLUMN_NAME = 'Country') "
                            +
                            "BEGIN " +
                            "    ALTER TABLE Series ADD Country NVARCHAR(50); " +
                            "END");
            jdbcTemplate.execute(
                    "IF NOT EXISTS (SELECT * FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'Series' AND COLUMN_NAME = 'SeriesType') "
                            +
                            "BEGIN " +
                            "    ALTER TABLE Series ADD SeriesType NVARCHAR(50); " +
                            "END");
            jdbcTemplate.execute(
                    "IF NOT EXISTS (SELECT * FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'Episode' AND COLUMN_NAME = 'slug') "
                            +
                            "BEGIN " +
                            "    ALTER TABLE Episode ADD slug NVARCHAR(255); " +
                            "END");

            jdbcTemplate.execute(
                    "IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_episode_slug' AND object_id = OBJECT_ID('Episode')) "
                            +
                            "BEGIN " +
                            "    CREATE NONCLUSTERED INDEX idx_episode_slug ON Episode(slug); " +
                            "END");

            // One-time populate missing slugs for episodes
            jdbcTemplate.execute(
                    "UPDATE Episode SET slug = LOWER(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(name, ' ', '-'), 'ı', 'i'), 'ğ', 'g'), 'ü', 'u'), 'ş', 's')) WHERE slug IS NULL OR slug = ''");

            jdbcTemplate.execute(
                    "IF NOT EXISTS (SELECT * FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'Series' AND COLUMN_NAME = 'slug') "
                            +
                            "BEGIN " +
                            "    ALTER TABLE Series ADD slug NVARCHAR(255); " +
                            "END");

            jdbcTemplate.execute(
                    "IF NOT EXISTS (SELECT * FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'Series' AND COLUMN_NAME = 'isAdult') "
                            +
                            "BEGIN " +
                            "    ALTER TABLE Series ADD isAdult BIT DEFAULT 0 NOT NULL; " +
                            "END");

            jdbcTemplate.execute(
                    "IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_series_slug' AND object_id = OBJECT_ID('Series')) "
                            +
                            "BEGIN " +
                            "    CREATE NONCLUSTERED INDEX idx_series_slug ON Series(slug); " +
                            "END");

            // Performance Indexes for Series
            jdbcTemplate.execute(
                    "IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_series_uploaddate' AND object_id = OBJECT_ID('Series')) "
                            +
                            "BEGIN " +
                            "    CREATE NONCLUSTERED INDEX idx_series_uploaddate ON Series(uploadDate DESC, name ASC); "
                            +
                            "END");

            jdbcTemplate.execute(
                    "IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_series_viewcount' AND object_id = OBJECT_ID('Series')) "
                            +
                            "BEGIN " +
                            "    CREATE NONCLUSTERED INDEX idx_series_viewcount ON Series(viewCount DESC, name ASC); " +
                            "END");

            jdbcTemplate.execute(
                    "IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_series_country' AND object_id = OBJECT_ID('Series')) "
                            +
                            "BEGIN " +
                            "    CREATE NONCLUSTERED INDEX idx_series_country ON Series(Country); " +
                            "END");

            // Performance Indexes for Episode
            jdbcTemplate.execute(
                    "IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_episode_uploaddate' AND object_id = OBJECT_ID('Episode')) "
                            +
                            "BEGIN " +
                            "    CREATE NONCLUSTERED INDEX idx_episode_uploaddate ON Episode(uploadDate DESC, name ASC); "
                            +
                            "END");

            jdbcTemplate.execute(
                    "IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_episode_viewcount' AND object_id = OBJECT_ID('Episode')) "
                            +
                            "BEGIN " +
                            "    CREATE NONCLUSTERED INDEX idx_episode_viewcount ON Episode(viewCount DESC); " +
                            "END");

            // Ensure finalStatus is INT (In case it was previously BIT/BOOLEAN)
            jdbcTemplate.execute(
                    "IF EXISTS (SELECT * FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'Series' AND COLUMN_NAME = 'finalStatus' AND DATA_TYPE = 'bit') "
                            +
                            "BEGIN " +
                            "    ALTER TABLE Series ALTER COLUMN finalStatus INT; " +
                            "END");

            jdbcTemplate.execute(
                    "IF NOT EXISTS (SELECT * FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'Series' AND COLUMN_NAME = 'finalStatus') "
                            +
                            "BEGIN " +
                            "    ALTER TABLE Series ADD finalStatus INT DEFAULT 0; " +
                            "END");

            jdbcTemplate.execute(
                    "UPDATE Series SET slug = LOWER(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(name, ' ', '-'), 'ı', 'i'), 'ğ', 'g'), 'ü', 'u'), 'ş', 's')) WHERE slug IS NULL OR slug = ''");
        } catch (Exception e) {
            System.err.println("Schema update failed: " + e.getMessage());
        }
    }

    private final @NonNull RowMapper<Series> seriesRowMapper = (rs, rowNum) -> {
        Series s = new Series();
        s.setId(UUID.fromString(rs.getString("ID")));
        s.setName(rs.getString("name"));
        s.setCategory(rs.getString("category"));
        s.setSummary(rs.getString("Summary"));
        s.setLanguage(rs.getString("Language"));

        try {
            s.setAdult(rs.getBoolean("isAdult"));
        } catch (Exception e) {
        }

        try {
            s.setCountry(rs.getString("Country"));
        } catch (Exception e) {
        }

        try {
            s.setSeriesType(rs.getString("SeriesType"));
        } catch (Exception e) {
        }

        try {
            s.setSlug(rs.getString("slug"));
        } catch (Exception e) {
        }

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
        }

        try {
            s.setEpisodeCount(rs.getInt("episodeCount"));
        } catch (Exception e) {
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

        String seriesId = rs.getString("SeriesId");
        if (seriesId != null)
            e.setSeriesId(UUID.fromString(seriesId));

        e.setSeasonNumber(rs.getInt("SeasonNumber"));
        e.setEpisodeNumber(rs.getInt("EpisodeNumber"));

        try {
            e.setSlug(rs.getString("slug"));
        } catch (Exception ex) {
        }

        return e;
    };

    public List<Series> findAllSeries() {
        String sql = """
                SELECT S.ID, S.name, S.Summary, S.Language, S.Country, S.SeriesType, S.finalStatus, S.EpisodeMetadataXml, S.uploadDate, S.slug, S.isAdult,
                       (SELECT COUNT(*) FROM Episode E WHERE E.SeriesId = S.ID) as episodeCount,
                       (SELECT STRING_AGG(C.Name, ', ') FROM Categories C
                        JOIN SeriesCategories SC ON SC.CategoryID = C.ID
                        WHERE SC.SeriesID = S.ID) as category
                FROM Series S
                ORDER BY S.name ASC
                """;
        return jdbcTemplate.query(sql, seriesRowMapper);
    }

    public Series findSeriesByName(String name) {
        try {
            String sql = """
                    SELECT S.ID, S.name, S.Summary, S.Language, S.Country, S.SeriesType, S.finalStatus, S.EpisodeMetadataXml, S.uploadDate, S.slug, S.isAdult,
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
                    SELECT S.ID, S.name, S.Summary, S.Language, S.Country, S.SeriesType, S.finalStatus, S.EpisodeMetadataXml, S.uploadDate, S.slug, S.isAdult,
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
                "INSERT INTO Series (ID, name, Summary, Language, Country, SeriesType, finalStatus, EpisodeMetadataXml, uploadDate, slug, isAdult) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                series.getId().toString(),
                series.getName(),
                series.getSummary(),
                series.getLanguage(),
                series.getCountry(),
                series.getSeriesType(),
                series.getFinalStatus(),
                series.getEpisodeMetadataXml(),
                series.getUploadDate(),
                series.getSlug(),
                series.isAdult());

        updateSeriesCategories(series.getId(), series.getCategory());
    }

    public void updateSeriesSlug(UUID id, String slug) {
        jdbcTemplate.update("UPDATE Series SET slug = ? WHERE ID = ?", slug, id.toString());
    }

    public void updateSeriesMetadata(Series series) {
        jdbcTemplate.update(
                "UPDATE Series SET name=?, Summary=?, Language=?, Country=?, SeriesType=?, finalStatus=?, isAdult=? WHERE ID=?",
                series.getName(),
                series.getSummary(),
                series.getLanguage(),
                series.getCountry(),
                series.getSeriesType(),
                series.getFinalStatus(),
                series.isAdult(),
                series.getId().toString());

        updateSeriesCategories(series.getId(), series.getCategory());
    }

    public UUID findFirstEpisodeIdBySeriesId(UUID seriesId) {
        try {
            String sql = "SELECT TOP 1 ID FROM Episode WHERE SeriesId = ? ORDER BY SeasonNumber ASC, EpisodeNumber ASC";
            String idStr = jdbcTemplate.queryForObject(sql, String.class, seriesId.toString());
            return idStr != null ? UUID.fromString(idStr) : null;
        } catch (Exception e) {
            return null;
        }
    }

    public UUID findLatestEpisodeIdBySeriesId(UUID seriesId) {
        try {
            String sql = "SELECT TOP 1 ID FROM Episode WHERE SeriesId = ? ORDER BY SeasonNumber DESC, EpisodeNumber DESC";
            String idStr = jdbcTemplate.queryForObject(sql, String.class, seriesId.toString());
            return idStr != null ? UUID.fromString(idStr) : null;
        } catch (Exception e) {
            return null;
        }
    }

    private void updateSeriesCategories(UUID seriesId, String categoriesStr) {
        jdbcTemplate.update("DELETE FROM SeriesCategories WHERE SeriesID = ?", seriesId.toString());

        if (categoriesStr != null && !categoriesStr.isBlank()) {
            for (String cat : categoriesStr.split(",")) {
                String trimmed = cat.trim();
                if (trimmed.isEmpty())
                    continue;

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
                "INSERT INTO Episode (ID, name, DurationMinutes, ReleaseYear, uploadDate, SeriesId, SeasonNumber, EpisodeNumber, slug) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                episode.getId().toString(),
                episode.getName(),
                episode.getDurationMinutes(),
                episode.getReleaseYear(),
                episode.getUploadDate(),
                episode.getSeriesId(),
                episode.getSeasonNumber(),
                episode.getEpisodeNumber(),
                episode.getSlug());
    }

    public void updateEpisode(Episode episode) {
        jdbcTemplate.update(
                "UPDATE Episode SET DurationMinutes = ?, ReleaseYear = ?, SeasonNumber = ?, EpisodeNumber = ?, slug = ? WHERE ID = ?",
                episode.getDurationMinutes(),
                episode.getReleaseYear(),
                episode.getSeasonNumber(),
                episode.getEpisodeNumber(),
                episode.getSlug(),
                episode.getId().toString());
    }

    public void deleteEpisodeById(UUID id) {
        jdbcTemplate.update("DELETE FROM Episode WHERE ID = ?", id.toString());
    }

    public void deleteSeriesById(UUID id) {
        jdbcTemplate.update("DELETE FROM Series WHERE ID = ?", id.toString());
    }

    public Series findSeriesByEpisodeId(UUID episodeId) {
        try {
            String sql = """
                    SELECT S.ID, S.name, S.Summary, S.Language, S.Country, S.SeriesType, S.finalStatus, S.EpisodeMetadataXml, S.uploadDate, S.slug, S.isAdult,
                           (SELECT STRING_AGG(C.Name, ', ') FROM Categories C
                            JOIN SeriesCategories SC ON SC.CategoryID = C.ID
                            WHERE SC.SeriesID = S.ID) as category
                    FROM Series S
                    JOIN Episode E ON E.SeriesId = S.ID
                    WHERE E.ID = ?
                    """;
            return jdbcTemplate.queryForObject(sql, seriesRowMapper, episodeId.toString());
        } catch (Exception e) {
            return findSeriesByEpisodeIdInsideXML(episodeId.toString());
        }
    }

    public Series findSeriesByEpisodeIdInsideXML(String episodeId) {
        try {
            String sql = """
                    SELECT S.ID, S.name, S.Summary, S.Language, S.Country, S.SeriesType, S.finalStatus, S.EpisodeMetadataXml, S.uploadDate, S.slug, S.isAdult,
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

    public List<Episode> findEpisodesBySeriesId(UUID seriesId) {
        return jdbcTemplate.query("SELECT * FROM Episode WHERE SeriesId = ? ORDER BY SeasonNumber, EpisodeNumber",
                episodeRowMapper, seriesId.toString());
    }

    public Episode findEpisodeBySlug(String slug) {
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT TOP 1 * FROM Episode WHERE slug = ?",
                    episodeRowMapper, slug);
        } catch (Exception e) {
            return null;
        }
    }

    public Series findSeriesBySlug(String slug) {
        try {
            String sql = """
                    SELECT S.ID, S.name, S.Summary, S.Language, S.Country, S.SeriesType, S.finalStatus, S.EpisodeMetadataXml, S.uploadDate, S.slug, S.isAdult,
                           (SELECT STRING_AGG(C.Name, ', ') FROM Categories C
                            JOIN SeriesCategories SC ON SC.CategoryID = C.ID
                            WHERE SC.SeriesID = S.ID) as category
                    FROM Series S
                    WHERE S.slug = ?
                    """;
            return jdbcTemplate.queryForObject(sql, seriesRowMapper, slug);
        } catch (Exception e) {
            return null;
        }
    }

    public List<Episode> findRecentEpisodes(int limit) {
        return jdbcTemplate.query("SELECT TOP (?) * FROM Episode ORDER BY uploadDate DESC, name ASC", episodeRowMapper,
                limit);
    }

    public List<Episode> findEpisodesBySeriesIdAndSeasonAndEpisodeNumber(UUID seriesId, int season, int episodeNumber) {
        return jdbcTemplate.query(
                "SELECT * FROM Episode WHERE SeriesId = ? AND SeasonNumber = ? AND EpisodeNumber = ?",
                episodeRowMapper, seriesId.toString(), season, episodeNumber);
    }

    public List<Series> findRecentSeries(int limit) {
        String sql;
        if (limit > 0) {
            sql = """
                    SELECT TOP (?) S.ID, S.name, S.Summary, S.Language, S.Country, S.SeriesType, S.finalStatus, S.EpisodeMetadataXml, S.uploadDate, S.slug, S.isAdult,
                           (SELECT STRING_AGG(C.Name, ', ') FROM Categories C
                            JOIN SeriesCategories SC ON SC.CategoryID = C.ID
                            WHERE SC.SeriesID = S.ID) as category
                    FROM Series S
                    ORDER BY S.uploadDate DESC, S.name ASC
                    """;
            return jdbcTemplate.query(sql, seriesRowMapper, limit);
        } else {
            sql = """
                    SELECT S.ID, S.name, S.Summary, S.Language, S.Country, S.SeriesType, S.finalStatus, S.EpisodeMetadataXml, S.uploadDate, S.slug, S.isAdult,
                           (SELECT STRING_AGG(C.Name, ', ') FROM Categories C
                            JOIN SeriesCategories SC ON SC.CategoryID = C.ID
                            WHERE SC.SeriesID = S.ID) as category
                    FROM Series S
                    ORDER BY S.uploadDate DESC, S.name ASC
                    """;
            return jdbcTemplate.query(sql, seriesRowMapper);
        }
    }

    public int countAllSeries() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM Series", Integer.class);
        return count != null ? count : 0;
    }

    public int countSeriesBySearch(String query) {
        String likeQuery = "%" + query.trim().toLowerCase() + "%";
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM Series WHERE LOWER(name) LIKE ?",
                Integer.class, likeQuery);
        return count != null ? count : 0;
    }

    public List<Series> findRecentSeriesPaged(int offset, int limit) {
        String sql = """
                SELECT S.ID, S.name, S.Summary, S.Language, S.Country, S.SeriesType, S.finalStatus, S.EpisodeMetadataXml, S.uploadDate, S.slug, S.isAdult,
                       (SELECT STRING_AGG(C.Name, ', ') FROM Categories C
                        JOIN SeriesCategories SC ON SC.CategoryID = C.ID
                        WHERE SC.SeriesID = S.ID) as category
                FROM Series S
                ORDER BY S.uploadDate DESC, S.name ASC
                OFFSET ? ROWS FETCH NEXT ? ROWS ONLY
                """;
        return jdbcTemplate.query(sql, seriesRowMapper, offset, limit);
    }

    public List<Series> searchSeriesPaged(String query, int offset, int limit) {
        String likeQuery = "%" + query.trim().toLowerCase() + "%";
        String sql = """
                SELECT S.ID, S.name, S.Summary, S.Language, S.Country, S.SeriesType, S.finalStatus, S.EpisodeMetadataXml, S.uploadDate, S.slug, S.isAdult,
                       (SELECT STRING_AGG(C.Name, ', ') FROM Categories C
                        JOIN SeriesCategories SC ON SC.CategoryID = C.ID
                        WHERE SC.SeriesID = S.ID) as category
                FROM Series S
                WHERE LOWER(S.name) LIKE ?
                ORDER BY S.uploadDate DESC, S.name ASC
                OFFSET ? ROWS FETCH NEXT ? ROWS ONLY
                """;
        return jdbcTemplate.query(sql, seriesRowMapper, likeQuery, offset, limit);
    }

    public List<Episode> findTop6EpisodesByCount() {
        String sql = "SELECT TOP 6 * FROM Episode ORDER BY viewCount DESC";
        return jdbcTemplate.query(sql, episodeRowMapper);
    }

    public List<Series> findTop6SeriesByCount() {
        String sql = """
                SELECT TOP 6 S.ID, S.name, S.Summary, S.Language, S.Country, S.SeriesType, S.finalStatus, S.EpisodeMetadataXml, S.uploadDate, S.slug, S.isAdult,
                       (SELECT COUNT(*) FROM Episode E WHERE E.SeriesId = S.ID) as episodeCount,
                       (SELECT STRING_AGG(C.Name, ', ') FROM Categories C
                        JOIN SeriesCategories SC ON SC.CategoryID = C.ID
                        WHERE SC.SeriesID = S.ID) as category
                FROM Series S
                ORDER BY S.viewCount DESC, S.name ASC
                """;
        return jdbcTemplate.query(sql, seriesRowMapper);
    }

    @SuppressWarnings("null")
    public List<Series> findSeriesWithFilters(String seriesType, Integer categoryId, Integer finalStatus, Integer year,
            String country, String sort, int offset, int limit) {
        StringBuilder sql = new StringBuilder(
                """
                            SELECT S.ID, S.name, S.Summary, S.Language, S.Country, S.SeriesType, S.finalStatus, S.EpisodeMetadataXml, S.uploadDate, S.slug, S.isAdult,
                                   (SELECT COUNT(*) FROM Episode E WHERE E.SeriesId = S.ID) as episodeCount,
                                   (SELECT STRING_AGG(C.Name, ', ') FROM Categories C
                                    JOIN SeriesCategories SC ON SC.CategoryID = C.ID
                                    WHERE SC.SeriesID = S.ID) as category
                            FROM Series S
                            WHERE 1=1
                        """);

        List<Object> params = new java.util.ArrayList<>();

        // Default to 'Dizi' if not provided
        if (seriesType != null && !seriesType.isEmpty()) {
            sql.append(" AND S.SeriesType = ?");
            params.add(seriesType);
        } else {
            sql.append(" AND (S.SeriesType = 'Dizi' OR S.SeriesType IS NULL)");
        }

        if (categoryId != null && categoryId > 0) {
            sql.append(
                    " AND EXISTS (SELECT 1 FROM SeriesCategories SC2 WHERE SC2.SeriesID = S.ID AND SC2.CategoryID = ?)");
            params.add(categoryId);
        }

        if (finalStatus != null && finalStatus >= 0) {
            sql.append(" AND S.finalStatus = ?");
            params.add(finalStatus);
        }

        if (year != null && year > 0) {
            sql.append(" AND EXISTS (SELECT 1 FROM Episode E WHERE E.SeriesId = S.ID AND E.ReleaseYear = ?)");
            params.add(year);
        }

        if (country != null && !country.isEmpty() && !country.equals("all")) {
            sql.append(" AND S.Country = ?");
            params.add(country);
        }

        // Sorting
        if ("oldest".equalsIgnoreCase(sort)) {
            sql.append(" ORDER BY S.uploadDate ASC, S.name ASC");
        } else if ("a-z".equalsIgnoreCase(sort)) {
            sql.append(" ORDER BY S.name ASC");
        } else if ("z-a".equalsIgnoreCase(sort)) {
            sql.append(" ORDER BY S.name DESC");
        } else {
            // Default newest
            sql.append(" ORDER BY S.uploadDate DESC, S.name ASC");
        }

        sql.append(" OFFSET ? ROWS FETCH NEXT ? ROWS ONLY");
        params.add(offset);
        params.add(limit);

        String finalSql = sql.toString();
        return jdbcTemplate.query(finalSql, seriesRowMapper, params.toArray());
    }

    @SuppressWarnings("null")
    public int countSeriesWithFilters(String seriesType, Integer categoryId, Integer finalStatus, Integer year,
            String country) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM Series S WHERE 1=1");
        List<Object> params = new java.util.ArrayList<>();

        if (seriesType != null && !seriesType.isEmpty()) {
            sql.append(" AND S.SeriesType = ?");
            params.add(seriesType);
        } else {
            sql.append(" AND (S.SeriesType = 'Dizi' OR S.SeriesType IS NULL)");
        }

        if (categoryId != null && categoryId > 0) {
            sql.append(
                    " AND EXISTS (SELECT 1 FROM SeriesCategories SC2 WHERE SC2.SeriesID = S.ID AND SC2.CategoryID = ?)");
            params.add(categoryId);
        }

        if (finalStatus != null && finalStatus >= 0) {
            sql.append(" AND S.finalStatus = ?");
            params.add(finalStatus);
        }

        if (year != null && year > 0) {
            sql.append(" AND EXISTS (SELECT 1 FROM Episode E WHERE E.SeriesId = S.ID AND E.ReleaseYear = ?)");
            params.add(year);
        }

        if (country != null && !country.isEmpty() && !country.equals("all")) {
            sql.append(" AND S.Country = ?");
            params.add(country);
        }

        String finalSql = sql.toString();
        Integer count = jdbcTemplate.queryForObject(finalSql, Integer.class, params.toArray());
        return count != null ? count : 0;
    }

}
