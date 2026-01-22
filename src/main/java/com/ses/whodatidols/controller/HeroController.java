package com.ses.whodatidols.controller;

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

    @GetMapping("/videos")
    public ResponseEntity<List<Map<String, Object>>> getHeroVideos() {
        String sql = "SELECT [ID], [name], [category], [_content], [type] FROM [WhoDatIdols].[dbo].[HeroVideo] ORDER BY [ID] DESC";

        List<Map<String, Object>> heroVideos = jdbcTemplate.queryForList(sql);
        return ResponseEntity.ok(heroVideos);
    }
}