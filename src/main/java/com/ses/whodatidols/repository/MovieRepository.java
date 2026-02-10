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

    // Stored Procedure çağrısı (Okuma işlemleri için mevcut yapıyı koruyoruz)
    private static final String GET_RECENT_MOVIES = "EXEC GetMoviesByUploadDateOffset @dayOffset = ?";

    public MovieRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // --- KAYIT (INSERT) İŞLEMİ ---
    public void save(Movie movie) {
        String sql = "INSERT INTO [WhoDatIdols].[dbo].[Movie] " +
                "(ID, name, category, Summary, DurationMinutes, language, ReleaseYear, uploadDate) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        jdbcTemplate.update(sql,
                movie.getId().toString(),
                movie.getName(),
                movie.getCategory(),
                movie.getSummary(),
                movie.getDurationMinutes(),
                movie.getLanguage(),
                movie.getReleaseYear(),
                java.sql.Timestamp.valueOf(movie.getUploadDate()));
    }

    // --- LİSTELEME (TÜM FİLMLER) ---
    public List<Movie> findAll() {
        String sql = "SELECT * FROM [WhoDatIdols].[dbo].[Movie] ORDER BY uploadDate DESC";
        return jdbcTemplate.query(sql, new MovieRowMapper());
    }

    // --- GÜNCELLEME (UPDATE) ---
    public void update(Movie movie) {
        // Not: Dosya değişirse ID sabit kaldığı için dosya üzerine yazılır.
        // Burada sadece metin verilerini güncelliyoruz.
        String sql = "UPDATE [WhoDatIdols].[dbo].[Movie] SET " +
                "name = ?, category = ?, Summary = ?, ReleaseYear = ?, language = ? " +
                "WHERE ID = ?";

        jdbcTemplate.update(sql,
                movie.getName(),
                movie.getCategory(),
                movie.getSummary(),
                movie.getReleaseYear(),
                movie.getLanguage(),
                movie.getId().toString());
    }

    // --- SİLME (DELETE) ---
    public void deleteById(UUID id) {
        String sql = "DELETE FROM [WhoDatIdols].[dbo].[Movie] WHERE ID = ?";
        jdbcTemplate.update(sql, id.toString());
    }

    // --- OKUMA İŞLEMLERİ ---
    public List<Movie> findRecentMovies(int day) {
        return jdbcTemplate.query(GET_RECENT_MOVIES, new MovieRowMapper(), day);
    }

    public List<Movie> findTop6MoviesByCount() {
        return jdbcTemplate.query("EXEC GetTop6MovieIdsByCount", new MovieRowMapper());
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
            movie.setReleaseYear(rs.getInt("ReleaseYear")); // UPDATED

            // uploadDate null gelebilir, kontrol ediyoruz
            if (rs.getTimestamp("uploadDate") != null) {
                movie.setUploadDate(rs.getTimestamp("uploadDate").toLocalDateTime());
            }
            return movie;
        }
    }
}