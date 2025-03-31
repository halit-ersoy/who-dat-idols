package com.ses.whodatidols.repository;

import com.ses.whodatidols.model.Movie;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

@Repository
public class MovieRepository {
    private final JdbcTemplate jdbcTemplate;

    // SQL to fetch movies sorted by upload date
    private static final String GET_RECENT_MOVIES =
            "EXEC GetMoviesByUploadDateOffset @dayOffset = ?";

    public MovieRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Movie> findRecentMovies(int day) {
        return jdbcTemplate.query(GET_RECENT_MOVIES, new MovieRowMapper(), day);
    }

    public List<Movie> findTop6MoviesByCount() {
        return jdbcTemplate.query("EXEC GetTop6MovieIdsByCount", new MovieRowMapper());
    }

    public String getImagePathById(UUID movieId) {
        String sql = "SELECT [bannerPath] FROM [WhoDatIdols].[dbo].[Movie] WHERE [ID] = ?";
        try {
            return jdbcTemplate.queryForObject(sql, String.class, movieId.toString());
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    private static class MovieRowMapper implements RowMapper<Movie> {
        @Override
        public Movie mapRow(ResultSet rs, int rowNum) throws SQLException {
            Movie movie = new Movie();
            movie.setId(UUID.fromString(rs.getString("ID")));
            movie.setName(rs.getString("name"));
            movie.setCategory(rs.getString("category"));
            movie.setContent(rs.getString("_content"));
            movie.setTime(rs.getInt("time"));
            movie.setLanguage(rs.getString("language"));
            movie.setYear(rs.getInt("year"));
            movie.setSourcePath(rs.getString("sourcePath"));
            movie.setBannerPath(rs.getString("bannerPath"));
            movie.setUploadDate(rs.getTimestamp("uploadDate").toLocalDateTime());
            return movie;
        }
    }
}