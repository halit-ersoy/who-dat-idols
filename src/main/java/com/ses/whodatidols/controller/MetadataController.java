package com.ses.whodatidols.controller;

import com.ses.whodatidols.service.TmdbService;
import com.ses.whodatidols.service.TvMazeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/public/api/metadata")
public class MetadataController {

    private final TvMazeService tvMazeService;
    private final TmdbService tmdbService;

    public MetadataController(TvMazeService tvMazeService, TmdbService tmdbService) {
        this.tvMazeService = tvMazeService;
        this.tmdbService = tmdbService;
    }

    @GetMapping("/search")
    public ResponseEntity<List<Map<String, Object>>> search(
            @RequestParam("query") String query,
            @RequestParam(value = "source", defaultValue = "tvmaze") String source,
            @RequestParam(value = "type", defaultValue = "series") String type) {
        try {
            if ("tmdb".equalsIgnoreCase(source)) {
                return ResponseEntity.ok(tmdbService.search(query, type));
            } else {
                return ResponseEntity.ok(tvMazeService.searchSeries(query));
            }
        } catch (Exception e) {
            System.err.println("Metadata Search Error: " + e.getMessage());
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping("/details")
    public ResponseEntity<Map<String, Object>> getDetails(
            @RequestParam("id") String id,
            @RequestParam(value = "source", defaultValue = "tvmaze") String source,
            @RequestParam(value = "type", defaultValue = "series") String type) {
        try {
            if ("tmdb".equalsIgnoreCase(source)) {
                return ResponseEntity.ok(tmdbService.getDetails(Integer.parseInt(id), type));
            } else {
                return ResponseEntity.ok(tvMazeService.getSeriesDetails(Integer.parseInt(id)));
            }
        } catch (Exception e) {
            System.err.println("Metadata Details Error: " + e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
}
