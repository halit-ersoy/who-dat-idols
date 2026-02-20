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
        } catch (Exception e) {
            System.err.println("Schema update failed: " + e.getMessage());
        }
    }

    // --- KAYIT (INSERT) İŞLEMİ ---
    public void save(Movie movie) {
        String sql = "INSERT INTO [WhoDatIdols].[dbo].[Movie] " +
                "(ID, name, Summary, DurationMinutes, language, Country, ReleaseYear, uploadDate) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        jdbcTemplate.update(sql,
                movie.getId().toString(),
                movie.getName(),
                movie.getSummary(),
                movie.getDurationMinutes(),
                movie.getLanguage(),
                movie.getCountry(),
                movie.getReleaseYear(),
                java.sql.Timestamp.valueOf(movie.getUploadDate()));

        // Update categories in junction table
        updateMovieCategories(movie.getId(), movie.getCategory());
    }

    // --- LİSTELEME (TÜM FİLMLER) ---
    public List<Movie> findAll() {
        String sql = """
                SELECT M.ID, M.name, M.Summary, M.DurationMinutes, M.language, M.Country, M.ReleaseYear, M.uploadDate,
                       (SELECT STRING_AGG(C.Name, ', ') FROM Categories C
                        JOIN MovieCategories MC ON MC.CategoryID = C.ID
                        WHERE MC.MovieID = M.ID) as category
                FROM [WhoDatIdols].[dbo].[Movie] M
                ORDER BY M.name ASC
                """;
        return jdbcTemplate.query(sql, new MovieRowMapper());
    }

    // --- GÜNCELLEME (UPDATE) ---
    public void update(Movie movie) {
        String sql = "UPDATE [WhoDatIdols].[dbo].[Movie] SET " +
                "name = ?, Summary = ?, ReleaseYear = ?, language = ?, Country = ? " +
                "WHERE ID = ?";

        jdbcTemplate.update(sql,
                movie.getName(),
                movie.getSummary(),
                movie.getReleaseYear(),
                movie.getLanguage(),
                movie.getCountry(),
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
    public List<Movie> findRecentMovies(int limit) {
        String sql = """
                SELECT TOP (?) M.ID, M.name, M.Summary, M.DurationMinutes, M.language, M.Country, M.ReleaseYear, M.uploadDate,
                       (SELECT STRING_AGG(C.Name, ', ') FROM Categories C
                        JOIN MovieCategories MC ON MC.CategoryID = C.ID
                        WHERE MC.MovieID = M.ID) as category
                FROM [WhoDatIdols].[dbo].[Movie] M
                ORDER BY M.uploadDate DESC, M.name ASC
                """;
        return jdbcTemplate.query(sql, new MovieRowMapper(), limit);
    }

    public List<Movie> findTop6MoviesByCount() {
        return jdbcTemplate.query("EXEC GetTop6MovieIdsByCount", new MovieRowMapper());
    }

    public Movie findMovieById(UUID id) {
        try {
            String sql = """
                    SELECT M.ID, M.name, M.Summary, M.DurationMinutes, M.language, M.Country, M.ReleaseYear, M.uploadDate,
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
            return movie;
        }
    }
}