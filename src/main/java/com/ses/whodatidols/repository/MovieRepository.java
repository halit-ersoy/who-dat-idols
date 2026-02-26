package com.ses.whodatidols.repository;

import com.ses.whodatidols.model.Movie;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

@Repository
public class MovieRepository {
    private final JdbcTemplate jdbcTemplate;

    public MovieRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        ensureSchema();
    }

    private void ensureSchema() {
        try {
            jdbcTemplate.execute(
                    "IF NOT EXISTS (SELECT * FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'Movie' AND COLUMN_NAME = 'Country') "
                            +
                            "BEGIN " +
                            "    ALTER TABLE Movie ADD Country NVARCHAR(50); " +
                            "END");
            jdbcTemplate.execute(
                    "IF NOT EXISTS (SELECT * FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'Movie' AND COLUMN_NAME = 'slug') "
                            +
                            "BEGIN " +
                            "    ALTER TABLE Movie ADD slug NVARCHAR(255); " +
                            "END");

            jdbcTemplate.execute(
                    "IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_movie_slug' AND object_id = OBJECT_ID('Movie')) "
                            +
                            "BEGIN " +
                            "    CREATE NONCLUSTERED INDEX idx_movie_slug ON Movie(slug); " +
                            "END");

            // Performance Indexes
            jdbcTemplate.execute(
                    "IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_movie_uploaddate' AND object_id = OBJECT_ID('Movie')) "
                            +
                            "BEGIN " +
                            "    CREATE NONCLUSTERED INDEX idx_movie_uploaddate ON Movie(uploadDate DESC, name ASC); " +
                            "END");

            jdbcTemplate.execute(
                    "IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_movie_viewcount' AND object_id = OBJECT_ID('Movie')) "
                            +
                            "BEGIN " +
                            "    CREATE NONCLUSTERED INDEX idx_movie_viewcount ON Movie(viewCount DESC, name ASC); " +
                            "END");

            jdbcTemplate.execute(
                    "IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_movie_country_year' AND object_id = OBJECT_ID('Movie')) "
                            +
                            "BEGIN " +
                            "    CREATE NONCLUSTERED INDEX idx_movie_country_year ON Movie(Country, ReleaseYear); " +
                            "END");

            // One-time populate missing slugs
            jdbcTemplate.execute(
                    "UPDATE Movie SET slug = LOWER(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(name, ' ', '-'), 'ı', 'i'), 'ğ', 'g'), 'ü', 'u'), 'ş', 's')) WHERE slug IS NULL OR slug = ''");
        } catch (Exception e) {
            System.err.println("Schema update failed: " + e.getMessage());
        }
    }

    // --- KAYIT (INSERT) İŞLEMİ ---
    public void save(Movie movie) {
        String sql = "INSERT INTO [WhoDatIdols].[dbo].[Movie] " +
                "(ID, name, Summary, DurationMinutes, language, Country, ReleaseYear, uploadDate, slug) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        jdbcTemplate.update(sql,
                movie.getId().toString(),
                movie.getName(),
                movie.getSummary(),
                movie.getDurationMinutes(),
                movie.getLanguage(),
                movie.getCountry(),
                movie.getReleaseYear(),
                java.sql.Timestamp.valueOf(movie.getUploadDate()),
                movie.getSlug());

        // Update categories in junction table
        updateMovieCategories(movie.getId(), movie.getCategory());
    }

    // --- LİSTELEME (TÜM FİLMLER) ---
    public List<Movie> findAll() {
        String sql = """
                SELECT M.ID, M.name, M.Summary, M.DurationMinutes, M.language, M.Country, M.ReleaseYear, M.uploadDate, M.slug,
                       (SELECT STRING_AGG(C.Name, ', ') FROM Categories C
                        JOIN MovieCategories MC ON MC.CategoryID = C.ID
                        WHERE MC.MovieID = M.ID) as category
                FROM [WhoDatIdols].[dbo].[Movie] M
                ORDER BY M.name ASC
                """;
        return jdbcTemplate.query(sql, new MovieRowMapper());
    }

    public Movie findMovieBySlug(String slug) {
        try {
            String sql = """
                    SELECT TOP 1 M.ID, M.name, M.Summary, M.DurationMinutes, M.language, M.Country, M.ReleaseYear, M.uploadDate, M.slug,
                           (SELECT STRING_AGG(C.Name, ', ') FROM Categories C
                            JOIN MovieCategories MC ON MC.CategoryID = C.ID
                            WHERE MC.MovieID = M.ID) as category
                    FROM [WhoDatIdols].[dbo].[Movie] M
                    WHERE M.slug = ?
                    """;
            return jdbcTemplate.queryForObject(sql, new MovieRowMapper(), slug);
        } catch (Exception e) {
            return null;
        }
    }

    // --- GÜNCELLEME (UPDATE) ---
    public void update(Movie movie) {
        String sql = "UPDATE [WhoDatIdols].[dbo].[Movie] SET " +
                "name = ?, Summary = ?, ReleaseYear = ?, language = ?, Country = ?, slug = ? " +
                "WHERE ID = ?";

        jdbcTemplate.update(sql,
                movie.getName(),
                movie.getSummary(),
                movie.getReleaseYear(),
                movie.getLanguage(),
                movie.getCountry(),
                movie.getSlug(),
                movie.getId().toString());

        // Update categories in junction table
        updateMovieCategories(movie.getId(), movie.getCategory());
    }

    private void updateMovieCategories(UUID movieId, String categoriesStr) {
        // Clear existing
        jdbcTemplate.update("DELETE FROM MovieCategories WHERE MovieID = ?", movieId.toString());

        if (categoriesStr != null && !categoriesStr.isBlank()) {
            for (String cat : categoriesStr.split(",")) {
                String trimmed = cat.trim();
                if (trimmed.isEmpty())
                    continue;

                // Ensure category exists in Categories table or just join if we assume they
                // exist
                // For robustness:
                jdbcTemplate.update(
                        "IF NOT EXISTS (SELECT 1 FROM Categories WHERE Name = ?) INSERT INTO Categories (Name) VALUES (?)",
                        trimmed, trimmed);
                jdbcTemplate.update(
                        "INSERT INTO MovieCategories (MovieID, CategoryID) SELECT ?, ID FROM Categories WHERE Name = ?",
                        movieId.toString(), trimmed);
            }
        }
    }

    // --- SİLME (DELETE) ---
    public void deleteById(UUID id) {
        String sql = "DELETE FROM [WhoDatIdols].[dbo].[Movie] WHERE ID = ?";
        jdbcTemplate.update(sql, id.toString());
    }

    // --- OKUMA İŞLEMLERİ ---
    public Movie findMovieByName(String name) {
        String sql = """
                SELECT TOP 1 M.ID, M.name, M.Summary, M.DurationMinutes, M.language, M.Country, M.ReleaseYear, M.uploadDate, M.slug,
                       (SELECT STRING_AGG(C.Name, ', ') FROM Categories C
                        JOIN MovieCategories MC ON MC.CategoryID = C.ID
                        WHERE MC.MovieID = M.ID) as category
                FROM [WhoDatIdols].[dbo].[Movie] M
                WHERE M.name = ?
                """;
        try {
            return jdbcTemplate.queryForObject(sql, new MovieRowMapper(), name);
        } catch (Exception e) {
            return null;
        }
    }

    public List<Movie> findRecentMovies(int limit) {
        String sql;
        if (limit > 0) {
            sql = """
                    SELECT TOP (?) M.ID, M.name, M.Summary, M.DurationMinutes, M.language, M.Country, M.ReleaseYear, M.uploadDate, M.slug,
                           (SELECT STRING_AGG(C.Name, ', ') FROM Categories C
                            JOIN MovieCategories MC ON MC.CategoryID = C.ID
                            WHERE MC.MovieID = M.ID) as category
                    FROM [WhoDatIdols].[dbo].[Movie] M
                    ORDER BY M.uploadDate DESC, M.name ASC
                    """;
            return jdbcTemplate.query(sql, new MovieRowMapper(), limit);
        } else {
            sql = """
                    SELECT M.ID, M.name, M.Summary, M.DurationMinutes, M.language, M.Country, M.ReleaseYear, M.uploadDate, M.slug,
                           (SELECT STRING_AGG(C.Name, ', ') FROM Categories C
                            JOIN MovieCategories MC ON MC.CategoryID = C.ID
                            WHERE MC.MovieID = M.ID) as category
                    FROM [WhoDatIdols].[dbo].[Movie] M
                    ORDER BY M.uploadDate DESC, M.name ASC
                    """;
            return jdbcTemplate.query(sql, new MovieRowMapper());
        }
    }

    // --- PAGINATION OKUMA İŞLEMLERİ ---
    public int countAllMovies() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM [WhoDatIdols].[dbo].[Movie]", Integer.class);
        return count != null ? count : 0;
    }

    public int countMoviesBySearch(String query) {
        String likeQuery = "%" + query.trim().toLowerCase() + "%";
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM [WhoDatIdols].[dbo].[Movie] WHERE LOWER(name) LIKE ?", Integer.class, likeQuery);
        return count != null ? count : 0;
    }

    public List<Movie> findRecentMoviesPaged(int offset, int limit) {
        String sql = """
                SELECT M.ID, M.name, M.Summary, M.DurationMinutes, M.language, M.Country, M.ReleaseYear, M.uploadDate, M.slug,
                       (SELECT STRING_AGG(C.Name, ', ') FROM Categories C
                        JOIN MovieCategories MC ON MC.CategoryID = C.ID
                        WHERE MC.MovieID = M.ID) as category
                FROM [WhoDatIdols].[dbo].[Movie] M
                ORDER BY M.uploadDate DESC, M.name ASC
                OFFSET ? ROWS FETCH NEXT ? ROWS ONLY
                """;
        return jdbcTemplate.query(sql, new MovieRowMapper(), offset, limit);
    }

    public List<Movie> searchMoviesPaged(String query, int offset, int limit) {
        String likeQuery = "%" + query.trim().toLowerCase() + "%";
        String sql = """
                SELECT M.ID, M.name, M.Summary, M.DurationMinutes, M.language, M.Country, M.ReleaseYear, M.uploadDate, M.slug,
                       (SELECT STRING_AGG(C.Name, ', ') FROM Categories C
                        JOIN MovieCategories MC ON MC.CategoryID = C.ID
                        WHERE MC.MovieID = M.ID) as category
                FROM [WhoDatIdols].[dbo].[Movie] M
                WHERE LOWER(M.name) LIKE ?
                ORDER BY M.uploadDate DESC, M.name ASC
                OFFSET ? ROWS FETCH NEXT ? ROWS ONLY
                """;
        return jdbcTemplate.query(sql, new MovieRowMapper(), likeQuery, offset, limit);
    }

    public List<Movie> findTop6MoviesByCount() {
        return jdbcTemplate.query("EXEC GetTop6MovieIdsByCount", new MovieRowMapper());
    }

    public Movie findMovieById(UUID id) {
        try {
            String sql = """
                    SELECT M.ID, M.name, M.Summary, M.DurationMinutes, M.language, M.Country, M.ReleaseYear, M.uploadDate, M.slug,
                           (SELECT STRING_AGG(C.Name, ', ') FROM Categories C
                            JOIN MovieCategories MC ON MC.CategoryID = C.ID
                            WHERE MC.MovieID = M.ID) as category
                    FROM [WhoDatIdols].[dbo].[Movie] M
                    WHERE M.ID = ?
                    """;
            return jdbcTemplate.queryForObject(sql, new MovieRowMapper(), id.toString());
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    @SuppressWarnings("null")
    public List<Movie> findMoviesWithFilters(Integer categoryId, Integer year, String country, String sort, int offset,
            int limit) {
        StringBuilder sql = new StringBuilder(
                """
                            SELECT M.ID, M.name, M.Summary, M.DurationMinutes, M.language, M.Country, M.ReleaseYear, M.uploadDate, M.slug,
                                   (SELECT STRING_AGG(C.Name, ', ') FROM Categories C
                                    JOIN MovieCategories MC ON MC.CategoryID = C.ID
                                    WHERE MC.MovieID = M.ID) as category
                            FROM [WhoDatIdols].[dbo].[Movie] M
                            WHERE 1=1
                        """);

        List<Object> params = new java.util.ArrayList<>();

        if (categoryId != null && categoryId > 0) {
            sql.append(
                    " AND EXISTS (SELECT 1 FROM MovieCategories MC2 WHERE MC2.MovieID = M.ID AND MC2.CategoryID = ?)");
            params.add(categoryId);
        }

        if (year != null && year > 0) {
            sql.append(" AND M.ReleaseYear = ?");
            params.add(year);
        }

        if (country != null && !country.isEmpty() && !country.equals("all")) {
            sql.append(" AND M.Country = ?");
            params.add(country);
        }

        // Sorting
        if ("oldest".equalsIgnoreCase(sort)) {
            sql.append(" ORDER BY M.uploadDate ASC, M.name ASC");
        } else if ("a-z".equalsIgnoreCase(sort)) {
            sql.append(" ORDER BY M.name ASC");
        } else if ("z-a".equalsIgnoreCase(sort)) {
            sql.append(" ORDER BY M.name DESC");
        } else {
            // Default newest
            sql.append(" ORDER BY M.uploadDate DESC, M.name ASC");
        }
        sql.append(" OFFSET ? ROWS FETCH NEXT ? ROWS ONLY");
        params.add(offset);
        params.add(limit);

        String finalSql = sql.toString();
        return jdbcTemplate.query(finalSql, new MovieRowMapper(), params.toArray());
    }

    @SuppressWarnings("null")
    public int countMoviesWithFilters(Integer categoryId, Integer year, String country) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM [WhoDatIdols].[dbo].[Movie] M WHERE 1=1");
        List<Object> params = new java.util.ArrayList<>();

        if (categoryId != null && categoryId > 0) {
            sql.append(
                    " AND EXISTS (SELECT 1 FROM MovieCategories MC2 WHERE MC2.MovieID = M.ID AND MC2.CategoryID = ?)");
            params.add(categoryId);
        }

        if (year != null && year > 0) {
            sql.append(" AND M.ReleaseYear = ?");
            params.add(year);
        }

        if (country != null && !country.isEmpty() && !country.equals("all")) {
            sql.append(" AND M.Country = ?");
            params.add(country);
        }

        String finalSql = sql.toString();
        Integer count = jdbcTemplate.queryForObject(finalSql, Integer.class, params.toArray());
        return count != null ? count : 0;
    }

    public String getImagePathById(UUID movieId) {
        // SQL Injection riskine karşı parametreli sorgu
        String sql = "SELECT [Summary] FROM [WhoDatIdols].[dbo].[Movie] WHERE [ID] = ?";
        try {
            return jdbcTemplate.queryForObject(sql, String.class, movieId.toString());
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    // --- ROW MAPPER (Sütun Eşleştirici) ---
    private static class MovieRowMapper implements RowMapper<Movie> {
        @Override
        public Movie mapRow(@NonNull ResultSet rs, int rowNum) throws SQLException {
            Movie movie = new Movie();
            movie.setId(UUID.fromString(rs.getString("ID")));
            movie.setName(rs.getString("name"));
            movie.setCategory(rs.getString("category"));
            movie.setSummary(rs.getString("Summary")); // UPDATED
            movie.setDurationMinutes(rs.getInt("DurationMinutes")); // UPDATED
            movie.setLanguage(rs.getString("language"));

            // Handle nullable Country
            try {
                movie.setCountry(rs.getString("Country"));
            } catch (SQLException e) {
                // Column might not exist in some projections or failures
            }

            movie.setReleaseYear(rs.getInt("ReleaseYear")); // UPDATED

            // uploadDate null gelebilir, kontrol ediyoruz
            if (rs.getTimestamp("uploadDate") != null) {
                movie.setUploadDate(rs.getTimestamp("uploadDate").toLocalDateTime());
            }

            try {
                movie.setSlug(rs.getString("slug"));
            } catch (SQLException e) {
                // Column might not exist
            }

            return movie;
        }
    }
}