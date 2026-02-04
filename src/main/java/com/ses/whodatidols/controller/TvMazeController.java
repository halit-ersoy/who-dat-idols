package com.ses.whodatidols.controller;

import com.ses.whodatidols.service.TvMazeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/public/api/tvmaze")
public class TvMazeController {

    private final TvMazeService tvMazeService;

    public TvMazeController(TvMazeService tvMazeService) {
        this.tvMazeService = tvMazeService;
    }

    @GetMapping("/search")
    public ResponseEntity<List<Map<String, Object>>> search(@RequestParam("query") String query) {
        try {
            List<Map<String, Object>> result = tvMazeService.searchSeries(query);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            System.err.println("TVMaze Search Service Error: " + e.getMessage());
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping("/details")
    public ResponseEntity<Map<String, Object>> getDetails(@RequestParam("id") Integer id) {
        try {
            return ResponseEntity.ok(tvMazeService.getSeriesDetails(id));
        } catch (Exception e) {
            System.err.println("TVMaze Details Error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }
}
