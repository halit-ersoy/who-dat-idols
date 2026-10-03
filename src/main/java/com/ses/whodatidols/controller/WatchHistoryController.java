package com.ses.whodatidols.controller;

import com.ses.whodatidols.repository.WatchHistoryRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/history")
@SuppressWarnings({"SqlResolve", "SqlNoDataSourceInspection"})
public class WatchHistoryController {

    private final WatchHistoryRepository watchHistoryRepository;
    private final JdbcTemplate jdbcTemplate;

    public WatchHistoryController(WatchHistoryRepository watchHistoryRepository, JdbcTemplate jdbcTemplate) {
        this.watchHistoryRepository = watchHistoryRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    private UUID resolveUserId(String cookie) {
        if (cookie == null || cookie.isBlank()) {
            return null;
        }
        try {
            try {
                UUID possibleId = UUID.fromString(cookie);
                Integer count = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM Person WHERE ID = ?", Integer.class, possibleId.toString());
                if (count != null && count > 0) {
                    return possibleId;
                }
            } catch (Exception ignored) {
            }

            String sql = "SELECT ID FROM Person WHERE cookie = ?";
            String idStr = jdbcTemplate.queryForObject(sql, String.class, cookie);
            return idStr != null ? UUID.fromString(idStr) : null;
        } catch (Exception e) {
            return null;
        }
    }

    @GetMapping
    public ResponseEntity<?> getWatchHistory(
            @CookieValue(name = "wdiAuth", required = false) String cookie) {
        UUID userId = resolveUserId(cookie);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Oturum a\u00e7\u0131n\u0131z."));
        }

        List<Map<String, Object>> history = watchHistoryRepository.getUserWatchHistory(userId);
        return ResponseEntity.ok(history);
    }

    @PostMapping("/record")
    public ResponseEntity<?> recordWatch(
            @RequestParam("contentId") UUID contentId,
            @CookieValue(name = "wdiAuth", required = false) String cookie) {
        UUID userId = resolveUserId(cookie);
        if (userId == null) {
            // Unauthenticated user: not an error, just return ok without recording
            return ResponseEntity.ok(Map.of("success", false, "message", "Misafir kullan\u0131c\u0131"));
        }

        watchHistoryRepository.recordWatch(userId, contentId);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteHistoryItem(
            @PathVariable("id") UUID id,
            @CookieValue(name = "wdiAuth", required = false) String cookie) {
        UUID userId = resolveUserId(cookie);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Yetkisiz"));
        }

        boolean deleted = watchHistoryRepository.deleteHistoryItem(userId, id);
        return ResponseEntity.ok(Map.of("success", deleted));
    }

    @DeleteMapping("/clear")
    public ResponseEntity<?> clearWatchHistory(
            @CookieValue(name = "wdiAuth", required = false) String cookie) {
        UUID userId = resolveUserId(cookie);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Yetkisiz"));
        }

        boolean cleared = watchHistoryRepository.clearUserHistory(userId);
        return ResponseEntity.ok(Map.of("success", cleared));
    }
}
