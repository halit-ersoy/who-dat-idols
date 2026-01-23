package com.ses.whodatidols.controller;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/hero")
public class HeroController {
    private final JdbcTemplate jdbcTemplate;

    public HeroController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Cacheable("heroVideos")
    @GetMapping("/videos")
    public ResponseEntity<List<Map<String, Object>>> getHeroVideos() {
        // Use a robust sorted query that joins with both Movie and SoapOperas tables
        String robustSql = "SELECT * FROM (" +
                           "  SELECT H.[ID], M.[name], M.[category], M.[_content], 'Movie' AS [type], H.sortOrder " +
                           "  FROM HeroVideo H INNER JOIN Movie M ON H.referanceID = M.ID " +
                           "  UNION ALL " +
                           "  SELECT H.[ID], S.[name], S.[category], S.[_content], 'SoapOpera' AS [type], H.sortOrder " +
                           "  FROM HeroVideo H INNER JOIN SoapOperas S ON H.referanceID = S.ID " +
                           ") AS Result ORDER BY sortOrder ASC";

        List<Map<String, Object>> heroVideos = jdbcTemplate.queryForList(robustSql);
        return ResponseEntity.ok(heroVideos);
    }
}