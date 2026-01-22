package com.ses.whodatidols.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/saved")
public class SavedController {
    private final JdbcTemplate jdbcTemplate;

    public SavedController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/translated")
    public ResponseEntity<?> getTranslatedEpisodes() {
        try {
            List<Map<String, Object>> result = jdbcTemplate.queryForList("EXEC GetTranslated");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Error fetching translated episodes: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    @GetMapping("/loaded")
    public ResponseEntity<?> getLoadedEpisodes() {
        try {
            List<Map<String, Object>> result = jdbcTemplate.queryForList("EXEC GetLoaded");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Error fetching loaded episodes: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    @GetMapping("/lists")
    public ResponseEntity<?> getLists(
            @CookieValue(value = "wdiAuth", required = false) String cookie) {

        if (cookie == null || cookie.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of(
                    "Result", 0,
                    "Message", "Authentication required"
            ));
        }

        try {
            UUID cookieUuid;
            try {
                cookieUuid = UUID.fromString(cookie);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.status(401).body(Map.of("Result", 0, "Message", "Invalid session"));
            }

            String sql = "{call ToggleVideoSave(?, ?, ?, ?, ?)}";
            List<Map<String, Object>> lists = jdbcTemplate.queryForList(
                    sql,
                    "get",          // @mode
                    null,           // @title
                    null,           // @new_title
                    null,           // @video_id
                    cookieUuid      // @cookie
            );
            return ResponseEntity.ok(lists);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "Result", 0,
                    "Message", "Error retrieving lists: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/create")
    public ResponseEntity<?> createList(
            @RequestParam("title") String title,
            @CookieValue(value = "wdiAuth", required = false) String cookie) {

        if (cookie == null || cookie.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of(
                    "Result", 0,
                    "Message", "Authentication required"
            ));
        }

        try {
            UUID cookieUuid;
            try {
                cookieUuid = UUID.fromString(cookie);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.status(401).body(Map.of("Result", 0, "Message", "Invalid session"));
            }

            String sql = "{call ToggleVideoSave(?, ?, ?, ?, ?)}";
            Map<String, Object> result = jdbcTemplate.queryForMap(
                    sql,
                    "create",       // @mode
                    title,          // @title
                    null,           // @new_title
                    null,           // @video_id
                    cookieUuid      // @cookie
            );
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("Result", 0);
            error.put("Message", "Error creating list: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    @PostMapping("/add")
    public ResponseEntity<?> addToList(
            @RequestParam("title") String title,
            @RequestParam("videoId") UUID videoId,
            @CookieValue(value = "wdiAuth", required = false) String cookie) {

        if (cookie == null || cookie.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of(
                    "Result", 0,
                    "Message", "Authentication required"
            ));
        }

        try {
            UUID cookieUuid;
            try {
                cookieUuid = UUID.fromString(cookie);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.status(401).body(Map.of("Result", 0, "Message", "Invalid session"));
            }

            String sql = "{call ToggleVideoSave(?, ?, ?, ?, ?)}";
            Map<String, Object> result = jdbcTemplate.queryForMap(
                    sql,
                    "add",          // @mode
                    title,          // @title
                    null,           // @new_title
                    videoId,        // @video_id
                    cookieUuid      // @cookie
            );
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("Result", 0);
            error.put("Message", "Error adding to list: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    @PostMapping("/remove")
    public ResponseEntity<?> removeFromList(
            @RequestParam("videoId") UUID videoId,
            @CookieValue(value = "wdiAuth", required = false) String cookie) {

        if (cookie == null || cookie.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of(
                    "Result", 0,
                    "Message", "Authentication required"
            ));
        }

        try {
            UUID cookieUuid;
            try {
                cookieUuid = UUID.fromString(cookie);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.status(401).body(Map.of("Result", 0, "Message", "Invalid session"));
            }

            String sql = "{call ToggleVideoSave(?, ?, ?, ?, ?)}";
            Map<String, Object> result = jdbcTemplate.queryForMap(
                    sql,
                    "remove",       // @mode
                    null,           // @title
                    null,           // @new_title
                    videoId,        // @video_id
                    cookieUuid      // @cookie
            );
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("Result", 0);
            error.put("Message", "Error removing from list: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    @PostMapping("/delete-list")
    public ResponseEntity<?> deleteList(
            @RequestParam("title") String title,
            @CookieValue(value = "wdiAuth", required = false) String cookie) {

        if (cookie == null || cookie.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of(
                    "Result", 0,
                    "Message", "Authentication required"
            ));
        }

        try {
            UUID cookieUuid;
            try {
                cookieUuid = UUID.fromString(cookie);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.status(401).body(Map.of("Result", 0, "Message", "Invalid session"));
            }

            String sql = "{call ToggleVideoSave(?, ?, ?, ?, ?)}";
            Map<String, Object> result = jdbcTemplate.queryForMap(
                    sql,
                    "deleteList",   // @mode
                    title,          // @title
                    null,           // @new_title
                    null,           // @video_id
                    cookieUuid      // @cookie
            );
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("Result", 0);
            error.put("Message", "Error deleting list: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    @PostMapping("/rename-list")
    public ResponseEntity<?> renameList(
            @RequestParam("title") String title,
            @RequestParam("newTitle") String newTitle,
            @CookieValue(value = "wdiAuth", required = false) String cookie) {

        if (cookie == null || cookie.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of(
                    "Result", 0,
                    "Message", "Authentication required"
            ));
        }

        try {
            UUID cookieUuid;
            try {
                cookieUuid = UUID.fromString(cookie);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.status(401).body(Map.of("Result", 0, "Message", "Invalid session"));
            }

            String sql = "{call ToggleVideoSave(?, ?, ?, ?, ?)}";
            Map<String, Object> result = jdbcTemplate.queryForMap(
                    sql,
                    "renameList",   // @mode
                    title,          // @title
                    newTitle,       // @new_title
                    null,           // @video_id
                    cookieUuid      // @cookie
            );
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("Result", 0);
            error.put("Message", "Error renaming list: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
}