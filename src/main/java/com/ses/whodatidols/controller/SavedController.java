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
        ensureSchema();
    }

    private void ensureSchema() {
        try {
            // UserLists Table
            jdbcTemplate.execute("""
                        IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='UserLists' AND xtype='U')
                        BEGIN
                            CREATE TABLE UserLists (
                                ID UNIQUEIDENTIFIER DEFAULT NEWID() PRIMARY KEY,
                                UserID UNIQUEIDENTIFIER NOT NULL,
                                Name NVARCHAR(255) NOT NULL,
                                CreatedAt DATETIME DEFAULT GETDATE()
                            );
                        END
                    """);

            // UserListItems Table
            jdbcTemplate.execute("""
                        IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='UserListItems' AND xtype='U')
                        BEGIN
                            CREATE TABLE UserListItems (
                                ID UNIQUEIDENTIFIER DEFAULT NEWID() PRIMARY KEY,
                                ListID UNIQUEIDENTIFIER NOT NULL,
                                VideoID UNIQUEIDENTIFIER NOT NULL,
                                VideoType NVARCHAR(50) DEFAULT 'movie',
                                AddedAt DATETIME DEFAULT GETDATE(),
                                FOREIGN KEY (ListID) REFERENCES UserLists(ID) ON DELETE CASCADE
                            );
                        END
                    """);
        } catch (Exception e) {
            System.err.println("Error initializing Favorites schema: " + e.getMessage());
        }
    }

    @GetMapping("/translated")
    public ResponseEntity<?> getTranslatedEpisodes() {
        try {
            List<Map<String, Object>> result = jdbcTemplate.queryForList("EXEC GetUpcoming 'Translated'");
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
            List<Map<String, Object>> result = jdbcTemplate.queryForList("EXEC GetUpcoming 'Upcoming'");
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
            return ResponseEntity.status(401).body(Map.of("Result", 0, "Message", "Authentication required"));
        }

        try {
            UUID userId = UUID.fromString(cookie);
            // Single query to fetch lists with items (Movie or Series)
            String sql = """
                    SELECT
                        L.Name as ListName,
                        L.ID as ListID,
                        I.VideoID,
                        COALESCE(M.name, S.name) as VideoName,
                        M.ReleaseYear as Year,
                        CASE
                            WHEN I.VideoType = 'movie' THEN (
                                SELECT STRING_AGG(C.Name, ', ')
                                FROM Categories C
                                JOIN MovieCategories MC ON MC.CategoryID = C.ID
                                WHERE MC.MovieID = M.ID
                            )
                            WHEN I.VideoType = 'series' THEN (
                                SELECT STRING_AGG(C.Name, ', ')
                                FROM Categories C
                                JOIN SeriesCategories SC ON SC.CategoryID = C.ID
                                WHERE SC.SeriesID = S.ID
                            )
                        END as Category,
                        I.VideoType as Type
                    FROM UserLists L
                    LEFT JOIN UserListItems I ON I.ListID = L.ID
                    LEFT JOIN Movie M ON I.VideoID = M.ID AND I.VideoType = 'movie'
                    LEFT JOIN Series S ON I.VideoID = S.ID AND I.VideoType = 'series'
                    WHERE L.UserID = ?
                    ORDER BY L.Name
                    """;

            List<Map<String, Object>> lists = jdbcTemplate.queryForList(sql, userId.toString());
            return ResponseEntity.ok(lists);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("Result", 0, "Message", "Error: " + e.getMessage()));
        }
    }

    @PostMapping("/create")
    public ResponseEntity<?> createList(
            @RequestParam("title") String title,
            @CookieValue(value = "wdiAuth", required = false) String cookie) {

        if (cookie == null || cookie.isEmpty())
            return ResponseEntity.status(401).build();
        try {
            UUID userId = UUID.fromString(cookie);
            if (title == null || title.isBlank())
                return ResponseEntity.badRequest().body(Map.of("Result", 0, "Message", "Title cannot be empty"));

            // Check if exists
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM UserLists WHERE UserID = ? AND Name = ?",
                    Integer.class, userId.toString(), title);

            if (count != null && count > 0) {
                return ResponseEntity.ok(Map.of("Result", 0, "Message", "List already exists"));
            }

            jdbcTemplate.update("INSERT INTO UserLists (UserID, Name) VALUES (?, ?)", userId.toString(), title);
            return ResponseEntity.ok(Map.of("Result", 1, "Message", "List created"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("Result", 0, "Message", "Error: " + e.getMessage()));
        }
    }

    @PostMapping("/add")
    public ResponseEntity<?> addToList(
            @RequestParam("title") String title,
            @RequestParam("videoId") UUID videoId,
            @CookieValue(value = "wdiAuth", required = false) String cookie) {

        if (cookie == null || cookie.isEmpty())
            return ResponseEntity.status(401).build();
        try {
            UUID userId = UUID.fromString(cookie);

            // Get List ID
            List<Map<String, Object>> listRows = jdbcTemplate.queryForList(
                    "SELECT ID FROM UserLists WHERE UserID = ? AND Name = ?",
                    userId.toString(), title);

            if (listRows.isEmpty()) {
                // Return error if list not found (UI should have created it)
                return ResponseEntity.ok(Map.of("Result", 0, "Message", "List not found"));
            }

            // Fix: Handle implicit String return type from JDBC for UNIQUEIDENTIFIER
            Object listIdObj = listRows.get(0).get("ID");
            UUID listId = UUID.fromString(listIdObj.toString());

            // Check implementation: Identify target VideoID and Type
            String videoType = "movie";
            UUID targetId = videoId;

            // 1. Check Movie
            Integer existMovie = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM Movie WHERE ID = ?", Integer.class,
                    videoId.toString());

            if (existMovie != null && existMovie > 0) {
                videoType = "movie";
            } else {
                // 2. Check Series
                Integer existSeries = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM Series WHERE ID = ?",
                        Integer.class, videoId.toString());
                if (existSeries != null && existSeries > 0) {
                    videoType = "series";
                } else {
                    // 3. Check Episode -> Resolve to Series
                    try {
                        String seriesIdStr = jdbcTemplate.queryForObject("SELECT SeriesID FROM Episode WHERE ID = ?",
                                String.class, videoId.toString());
                        if (seriesIdStr != null) {
                            targetId = UUID.fromString(seriesIdStr);
                            videoType = "series";
                        } else {
                            return ResponseEntity.ok(Map.of("Result", 0, "Message", "Video not found"));
                        }
                    } catch (Exception e) {
                        return ResponseEntity.ok(Map.of("Result", 0, "Message", "Video not found"));
                    }
                }
            }

            // Check if already contains
            Integer already = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM UserListItems WHERE ListID = ? AND VideoID = ?",
                    Integer.class, listId, targetId.toString());

            if (already != null && already > 0)
                return ResponseEntity.ok(Map.of("Result", 1, "Message", "Already in list"));

            jdbcTemplate.update(
                    "INSERT INTO UserListItems (ListID, VideoID, VideoType) VALUES (?, ?, ?)",
                    listId, targetId.toString(), videoType);

            return ResponseEntity.ok(Map.of("Result", 1, "Message", "Added to list"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("Result", 0, "Message", "Error: " + e.getMessage()));
        }
    }

    @PostMapping("/remove")
    public ResponseEntity<?> removeFromList(
            @RequestParam("videoId") UUID videoId,
            @CookieValue(value = "wdiAuth", required = false) String cookie) {

        if (cookie == null || cookie.isEmpty())
            return ResponseEntity.status(401).build();
        try {
            UUID userId = UUID.fromString(cookie);
            UUID targetId = videoId;

            // Check if it's an episode -> resolve to series
            // We can optimize by checking only if it's NOT in Movie/Series tables, OR just
            // try to resolve
            // Simplest robust way:
            // 1. Check if Movie or Series exists with this ID
            Integer existDirect = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM (SELECT ID FROM Movie UNION SELECT ID FROM Series) AS Combined WHERE ID = ?",
                    Integer.class, videoId.toString());

            if (existDirect != null && existDirect == 0) {
                // Try Episode
                try {
                    String seriesIdStr = jdbcTemplate.queryForObject("SELECT SeriesID FROM Episode WHERE ID = ?",
                            String.class, videoId.toString());
                    if (seriesIdStr != null) {
                        targetId = UUID.fromString(seriesIdStr);
                    }
                } catch (Exception e) {
                    // Not an episode either, or error. Proceed with original ID (will result in 0
                    // rows deleted)
                }
            }

            // Remove from ALL lists of this user
            jdbcTemplate.update("""
                    DELETE FROM UserListItems
                    WHERE VideoID = ? AND ListID IN (SELECT ID FROM UserLists WHERE UserID = ?)
                    """, targetId.toString(), userId.toString());

            return ResponseEntity.ok(Map.of("Result", 1, "Message", "Removed from lists"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("Result", 0, "Message", "Error: " + e.getMessage()));
        }
    }

    @PostMapping("/delete-list")
    public ResponseEntity<?> deleteList(
            @RequestParam("title") String title,
            @CookieValue(value = "wdiAuth", required = false) String cookie) {
        if (cookie == null || cookie.isEmpty())
            return ResponseEntity.status(401).build();
        try {
            UUID userId = UUID.fromString(cookie);
            jdbcTemplate.update("DELETE FROM UserLists WHERE UserID = ? AND Name = ?", userId.toString(), title);
            return ResponseEntity.ok(Map.of("Result", 1, "Message", "List deleted"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("Result", 0, "Message", "Error: " + e.getMessage()));
        }
    }

    @PostMapping("/rename-list")
    public ResponseEntity<?> renameList(
            @RequestParam("title") String title,
            @RequestParam("newTitle") String newTitle,
            @CookieValue(value = "wdiAuth", required = false) String cookie) {
        if (cookie == null || cookie.isEmpty())
            return ResponseEntity.status(401).build();
        try {
            UUID userId = UUID.fromString(cookie);
            jdbcTemplate.update("UPDATE UserLists SET Name = ? WHERE UserID = ? AND Name = ?", newTitle,
                    userId.toString(), title);
            return ResponseEntity.ok(Map.of("Result", 1, "Message", "List renamed"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("Result", 0, "Message", "Error: " + e.getMessage()));
        }
    }
}