package com.ses.whodatidols.controller;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/hero")
public class HeroController {
    private final com.ses.whodatidols.repository.HeroRepository heroRepository;

    public HeroController(com.ses.whodatidols.repository.HeroRepository heroRepository) {
        this.heroRepository = heroRepository;
    }

    @Cacheable("heroVideos")
    @GetMapping("/videos")
    public ResponseEntity<List<Map<String, Object>>> getHeroVideos() {
        return ResponseEntity.ok(heroRepository.getHeroVideos());
    }
}