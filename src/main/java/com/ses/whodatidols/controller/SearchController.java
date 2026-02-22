package com.ses.whodatidols.controller;

import com.ses.whodatidols.repository.ContentRepository;
import com.ses.whodatidols.repository.MovieRepository;
import com.ses.whodatidols.repository.SeriesRepository;
import com.ses.whodatidols.model.Movie;
import com.ses.whodatidols.model.Episode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(SearchController.class);
    private final ContentRepository contentRepository;
    private final MovieRepository movieRepository;
    private final SeriesRepository seriesRepository;

    public SearchController(ContentRepository contentRepository, MovieRepository movieRepository,
            SeriesRepository seriesRepository) {
        this.contentRepository = contentRepository;
        this.movieRepository = movieRepository;
        this.seriesRepository = seriesRepository;
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

            for (Map<String, Object> map : results) {
                String idStr = (String) map.get("ID");
                String itemType = (String) map.get("Type");
                if (idStr != null) {
                    try {
                        UUID uuid = UUID.fromString(idStr);
                        if ("Movie".equalsIgnoreCase(itemType)) {
                            Movie m = movieRepository.findMovieById(uuid);
                            if (m != null)
                                map.put("slug", m.getSlug());
                        } else if ("SoapOpera".equalsIgnoreCase(itemType) || "Series".equalsIgnoreCase(itemType)
                                || "soap_opera".equalsIgnoreCase(itemType)) {
                            List<Episode> eps = seriesRepository.findEpisodesBySeriesId(uuid);
                            if (!eps.isEmpty())
                                map.put("slug", eps.get(0).getSlug());
                        }
                    } catch (Exception ex) {
                        // ignore UUID parsing error
                    }
                }
            }

            logger.info("Search successful, found {} results", results.size());
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            logger.error("Search failed for query {}: {}", q, e.getMessage(), e);
            return ResponseEntity.status(500).build();
        }
    }
}
