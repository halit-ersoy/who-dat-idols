package com.ses.whodatidols.controller;

import com.ses.whodatidols.service.MovieService;
import com.ses.whodatidols.service.SeoService;
import com.ses.whodatidols.service.SeriesService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Controller
public class LegacyUrlRedirectController {

    private static final Logger logger = LoggerFactory.getLogger(LegacyUrlRedirectController.class);

    private final SeoService seoService;
    private final SeriesService seriesService;
    private final MovieService movieService;
    private final JdbcTemplate jdbcTemplate;

    public LegacyUrlRedirectController(SeoService seoService, SeriesService seriesService,
                                       MovieService movieService, JdbcTemplate jdbcTemplate) {
        this.seoService = seoService;
        this.seriesService = seriesService;
        this.movieService = movieService;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Handles legacy series/movie/episode URL structures:
     * /dizi/..., /diziler/..., /film/..., /filmler/..., /izle/..., /bolum/...
     */
    @GetMapping({
            "/dizi/{slug}", "/dizi/{slug}/**",
            "/diziler/{slug}", "/diziler/{slug}/**",
            "/film/{slug}", "/film/{slug}/**",
            "/filmler/{slug}", "/filmler/{slug}/**",
            "/izle/{slug}", "/izle/{slug}/**",
            "/bolum/{slug}", "/bolum/{slug}/**"
    })
    public ResponseEntity<?> handleLegacyContentUrl(@PathVariable("slug") String slug, HttpServletRequest request) {
        String canonicalSlug = seoService.resolveCanonicalSlug(slug);

        if (canonicalSlug != null) {
            // Check if content belongs to BL/GL site
            if (seriesService.isBlSlug(canonicalSlug) || movieService.isBlSlug(canonicalSlug)) {
                return ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY)
                        .header(HttpHeaders.LOCATION, "https://bldizi.com/" + canonicalSlug)
                        .build();
            }

            logger.info("301 Redirecting legacy URL {} to /{}", request.getRequestURI(), canonicalSlug);
            return ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY)
                    .header(HttpHeaders.LOCATION, "/" + canonicalSlug)
                    .build();
        }

        // No match found -> Return 404 (never 500, never redirect to home)
        return notFoundResponse();
    }

    /**
     * Handles legacy category / genre archive URLs dynamically from live database:
     * e.g. /tur/aksiyon-filmleri -> 301 to /filmler?categoryId={dynamicId}
     * e.g. /tur/korku-dizileri -> 301 to /diziler?categoryId={dynamicId}
     */
    @GetMapping({"/tur/{cat}", "/tur/{cat}/**"})
    public ResponseEntity<?> handleLegacyTurUrl(@PathVariable("cat") String cat, HttpServletRequest request) {
        String lower = cat.toLowerCase(Locale.ROOT);
        boolean isFilm = lower.contains("film");
        boolean isDizi = lower.contains("dizi");

        String cleaned = lower.replaceAll("-filmleri|-filmler|-filmi|-dizileri|-diziler|-dizisi", "");

        Integer catId = findCategoryIdDynamically(cleaned);

        String basePath = isFilm ? "/filmler" : (isDizi ? "/diziler" : "/filmler");
        String targetUrl = catId != null ? (basePath + "?categoryId=" + catId) : basePath;

        logger.info("301 Redirecting legacy category {} to {}", request.getRequestURI(), targetUrl);
        return ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY)
                .header(HttpHeaders.LOCATION, targetUrl)
                .build();
    }

    private Integer findCategoryIdDynamically(String cleanedGenre) {
        if (cleanedGenre == null || cleanedGenre.isBlank()) return null;
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT ID, Name FROM Categories");
            String normalizedTarget = normalize(cleanedGenre);
            for (Map<String, Object> row : rows) {
                String name = (String) row.get("Name");
                if (name == null) continue;
                String normalizedCat = normalize(name);
                if (normalizedCat.equals(normalizedTarget) ||
                    normalizedTarget.contains(normalizedCat) ||
                    normalizedCat.contains(normalizedTarget)) {
                    Object idObj = row.get("ID");
                    if (idObj instanceof Number num) return num.intValue();
                }
            }
        } catch (Exception e) {
            logger.warn("Dynamic category lookup error: {}", e.getMessage());
        }
        return null;
    }

    private String normalize(String str) {
        return str.toLowerCase(Locale.ROOT)
                .replace("ı", "i")
                .replace("ğ", "g")
                .replace("ü", "u")
                .replace("ş", "s")
                .replace("ö", "o")
                .replace("ç", "c")
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
    }

    /**
     * Handles legacy topic / tag archive URLs:
     * /konu/dog?orderby=rating -> 404 Not Found (no tag system in current platform)
     */
    @GetMapping({"/konu", "/konu/**"})
    public ResponseEntity<?> handleLegacyKonuUrl() {
        return notFoundResponse();
    }

    private ResponseEntity<?> notFoundResponse() {
        try {
            Resource htmlPage = new ClassPathResource("static/error/html/error.html");
            if (htmlPage.exists()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML_VALUE)
                        .body(htmlPage);
            }
        } catch (Exception ignored) {}
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }
}
