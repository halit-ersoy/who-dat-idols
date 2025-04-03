package com.ses.whodatidols.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public class StaticImageRepository {
    private final JdbcTemplate jdbcTemplate;

    public StaticImageRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public String getImagePathById(UUID uuid) {
        String sql = "SELECT [path] FROM [WhoDatIdols].[dbo].[StaticImage] WHERE [ID] = ?";
        try {
            return jdbcTemplate.queryForObject(sql, String.class, uuid);
        } catch (Exception e) {
            return null;
        }
    }
}
