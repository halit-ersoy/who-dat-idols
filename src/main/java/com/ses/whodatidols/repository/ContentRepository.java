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
        // First, try to find in the Movie table
        String sqlMovie = "SELECT [sourcePath] FROM [WhoDatIdols].[dbo].[Movie] WHERE [ID] = ?";
        try {
            String path = jdbcTemplate.queryForObject(sqlMovie, String.class, id.toString());
            if (path != null && !path.isEmpty()) {
                return path;
            }
        } catch (Exception e) {
            // Not found in Movie table, continue to SoapOpera
        }

        // If not found or exception occurred, try the SoapOpera table
        String sqlSoapOpera = "SELECT [sourcePath] FROM [WhoDatIdols].[dbo].[SoapOpera] WHERE [ID] = ?";
        try {
            return jdbcTemplate.queryForObject(sqlSoapOpera, String.class, id.toString());
        } catch (Exception e) {
            return null;
        }
    }
}