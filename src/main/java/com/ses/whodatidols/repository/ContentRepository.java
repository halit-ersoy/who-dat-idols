package com.ses.whodatidols.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public class ContentRepository {
    private final JdbcTemplate jdbcTemplate;

    public ContentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public String findVideoUrlById(UUID id) {
        // Change this to query the Movie table instead of the non-existent contents table
        String sql = "SELECT [sourcePath] FROM [WhoDatIdols].[dbo].[Movie] WHERE [ID] = ?";
        try {
            return jdbcTemplate.queryForObject(sql, String.class, id.toString());
        } catch (Exception e) {
            return null;
        }
    }
}