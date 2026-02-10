package com.ses.whodatidols.controller;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

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
        // Updated for Hero table, Series table, ReferenceId columns
        String robustSql = "SELECT * FROM (" +
                "  SELECT H.[ID], M.[name], M.[category], H.[CustomSummary] as _content, 'Movie' AS [type], H.sortOrder "
                +
                "  FROM Hero H INNER JOIN Movie M ON H.ReferenceId = M.ID " +
                "  UNION ALL " +
                "  SELECT H.[ID], S.[name], S.[category], H.[CustomSummary] as _content, 'SoapOpera' AS [type], H.sortOrder "
                +
                "  FROM Hero H INNER JOIN Series S ON H.ReferenceId = S.ID " +
                ") AS Result ORDER BY sortOrder ASC";

        List<Map<String, Object>> heroVideos = jdbcTemplate.queryForList(robustSql);
        return ResponseEntity.ok(heroVideos);
    }
}