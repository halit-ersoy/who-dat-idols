package com.ses.whodatidols.repository;

import com.ses.whodatidols.model.Content;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ContentRepository {
    private final JdbcTemplate jdbcTemplate;
    private static final String VIDEO_QUERY = "SELECT URL FROM [WhoDatIdols].[dbo].[contents] WHERE ID = ?";

    public ContentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public String findVideoUrlById(int id) {
        return jdbcTemplate.queryForObject(VIDEO_QUERY, String.class, id);
    }
}