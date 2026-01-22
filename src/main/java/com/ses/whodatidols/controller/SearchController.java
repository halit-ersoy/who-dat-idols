package com.ses.whodatidols.controller;

import com.ses.whodatidols.repository.ContentRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final ContentRepository contentRepository;

    public SearchController(ContentRepository contentRepository) {
        this.contentRepository = contentRepository;
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String category) {
        
        List<Map<String, Object>> results = contentRepository.searchContent(q, year, type, category);
        return ResponseEntity.ok(results);
    }
}
