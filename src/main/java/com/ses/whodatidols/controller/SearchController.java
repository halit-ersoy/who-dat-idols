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

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(SearchController.class);
    private final ContentRepository contentRepository;

    public SearchController(ContentRepository contentRepository) {
        this.contentRepository = contentRepository;
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> search(
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "year", required = false) Integer year,
            @RequestParam(name = "type", required = false) String type,
            @RequestParam(name = "category", required = false) String category) {

        logger.info("Search requested: query={}, year={}, type={}, category={}", q, year, type, category);

        try {
            List<Map<String, Object>> results = contentRepository.searchContent(q, year, type, category);
            logger.info("Search successful, found {} results", results.size());
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            logger.error("Search failed for query {}: {}", q, e.getMessage(), e);
            return ResponseEntity.status(500).build();
        }
    }
}
