package com.ses.whodatidols.controller;

import com.ses.whodatidols.model.CalendarEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

import java.util.*;

@SuppressWarnings({"SqlResolve", "SqlRedundantOrderingDirection"})
@RestController
public class CalendarController {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public CalendarController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initTable() {
        try {
            // Create table if not exists
            String sql = "IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='CalendarEvent' AND xtype='U') " +
                    "CREATE TABLE CalendarEvent (" +
                    "ID UNIQUEIDENTIFIER PRIMARY KEY DEFAULT NEWID(), " +
                    "dayOfWeek NVARCHAR(20) NOT NULL, " +
                    "title NVARCHAR(255) NOT NULL, " +
                    "episode NVARCHAR(100), " +
                    "showTime NVARCHAR(10), " +
                    "sortOrder INT DEFAULT 0, " +
                    "posterUrl NVARCHAR(1000), " +
                    "platform NVARCHAR(50) DEFAULT 'all', " +
                    "isBl BIT DEFAULT 0, " +
                    "channel NVARCHAR(100), " +
                    "category NVARCHAR(255), " +
                    "summary NVARCHAR(MAX), " +
                    "slug NVARCHAR(255), " +
                    "seriesId UNIQUEIDENTIFIER, " +
                    "tmdbId INT, " +
                    "tvmazeId INT" +
                    ")";
            jdbcTemplate.execute(sql);

            // Safe column additions for existing tables
            String[] alterCols = {
                    "IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('CalendarEvent') AND name = 'posterUrl') ALTER TABLE CalendarEvent ADD posterUrl NVARCHAR(1000)",
                    "IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('CalendarEvent') AND name = 'platform') ALTER TABLE CalendarEvent ADD platform NVARCHAR(50) DEFAULT 'all'",
                    "IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('CalendarEvent') AND name = 'isBl') ALTER TABLE CalendarEvent ADD isBl BIT DEFAULT 0",
                    "IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('CalendarEvent') AND name = 'channel') ALTER TABLE CalendarEvent ADD channel NVARCHAR(100)",
                    "IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('CalendarEvent') AND name = 'category') ALTER TABLE CalendarEvent ADD category NVARCHAR(255)",
                    "IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('CalendarEvent') AND name = 'summary') ALTER TABLE CalendarEvent ADD summary NVARCHAR(MAX)",
                    "IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('CalendarEvent') AND name = 'slug') ALTER TABLE CalendarEvent ADD slug NVARCHAR(255)",
                    "IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('CalendarEvent') AND name = 'seriesId') ALTER TABLE CalendarEvent ADD seriesId UNIQUEIDENTIFIER",
                    "IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('CalendarEvent') AND name = 'tmdbId') ALTER TABLE CalendarEvent ADD tmdbId INT",
                    "IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('CalendarEvent') AND name = 'tvmazeId') ALTER TABLE CalendarEvent ADD tvmazeId INT"
            };

            for (String alterSql : alterCols) {
                try {
                    jdbcTemplate.execute(alterSql);
                } catch (Exception ignored) {
                }
            }
        } catch (Exception e) {
            System.err.println("Error initializing CalendarEvent table: " + e.getMessage());
        }
    }

    // Public API to get calendar data (defaults to Who Dat Idols view)
    @Cacheable(value = "calendar", key = "#platform != null ? #platform : 'whodatidols'")
    @GetMapping("/api/calendar")
    public ResponseEntity<Map<String, List<Map<String, Object>>>> getCalendar(
            @RequestParam(value = "platform", required = false) String platform) {

        String sql;
        if ("bldizi".equalsIgnoreCase(platform)) {
            sql = "SELECT * FROM CalendarEvent WHERE isBl = 1 OR platform IN ('bldizi', 'all', 'both') ORDER BY sortOrder, showTime";
        } else if ("all".equalsIgnoreCase(platform)) {
            sql = "SELECT * FROM CalendarEvent ORDER BY sortOrder, showTime";
        } else {
            // Default to Who Dat Idols
            sql = "SELECT * FROM CalendarEvent WHERE (isBl = 0 OR isBl IS NULL) AND (platform IN ('whodatidols', 'all', 'both') OR platform IS NULL) ORDER BY sortOrder, showTime";
        }

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);

        Map<String, List<Map<String, Object>>> calendarData = new LinkedHashMap<>();
        String[] days = { "monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday" };
        for (String day : days) {
            calendarData.put(day, new ArrayList<>());
        }

        for (Map<String, Object> row : rows) {
            Object dayObj = row.get("dayOfWeek");
            if (dayObj != null) {
                String day = dayObj.toString().toLowerCase().trim();
                if (calendarData.containsKey(day)) {
                    Map<String, Object> event = mapRowToEvent(row);
                    calendarData.get(day).add(event);
                }
            }
        }

        return ResponseEntity.ok(calendarData);
    }

    // Admin API to list events with platform filtering
    @GetMapping("/admin/calendar/list")
    public ResponseEntity<List<Map<String, Object>>> getAdminCalendarList(
            @RequestParam(value = "platform", defaultValue = "all") String platform) {
        String sql;
        if ("bldizi".equalsIgnoreCase(platform)) {
            sql = "SELECT * FROM CalendarEvent WHERE isBl = 1 OR platform IN ('bldizi', 'all', 'both') ORDER BY dayOfWeek, sortOrder, showTime";
        } else if ("whodatidols".equalsIgnoreCase(platform)) {
            sql = "SELECT * FROM CalendarEvent WHERE (isBl = 0 OR isBl IS NULL) AND (platform IN ('whodatidols', 'all', 'both') OR platform IS NULL) ORDER BY dayOfWeek, sortOrder, showTime";
        } else {
            sql = "SELECT * FROM CalendarEvent ORDER BY dayOfWeek, sortOrder, showTime";
        }

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            result.add(mapRowToEvent(row));
        }
        return ResponseEntity.ok(result);
    }

    // Admin API to add event
    @CacheEvict(value = "calendar", allEntries = true)
    @PostMapping("/admin/calendar/add")
    public ResponseEntity<String> addEvent(@RequestBody CalendarEvent event) {
        try {
            String sql = "INSERT INTO CalendarEvent (" +
                    "dayOfWeek, title, episode, showTime, sortOrder, " +
                    "posterUrl, platform, isBl, channel, category, summary, slug, seriesId, tmdbId, tvmazeId" +
                    ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            int sortOrder = event.getSortOrder();
            String plat = event.getPlatform() != null && !event.getPlatform().isBlank() ? event.getPlatform() : "whodatidols";
            boolean isBl = "bldizi".equalsIgnoreCase(plat) || Boolean.TRUE.equals(event.getIsBl());

            jdbcTemplate.update(sql,
                    event.getDayOfWeek() != null ? event.getDayOfWeek().toLowerCase() : "monday",
                    event.getTitle(),
                    event.getEpisode(),
                    event.getShowTime(),
                    sortOrder,
                    event.getPosterUrl(),
                    plat,
                    isBl,
                    event.getChannel(),
                    event.getCategory(),
                    event.getSummary(),
                    event.getSlug(),
                    event.getSeriesId(),
                    event.getTmdbId(),
                    event.getTvmazeId());

            return ResponseEntity.ok("Event added successfully");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error adding event: " + e.getMessage());
        }
    }

    // Admin API to update event
    @CacheEvict(value = "calendar", allEntries = true)
    @PostMapping("/admin/calendar/update")
    public ResponseEntity<String> updateEvent(@RequestBody CalendarEvent event) {
        try {
            if (event.getId() == null) {
                return ResponseEntity.badRequest().body("Event ID is required");
            }

            String plat = event.getPlatform() != null && !event.getPlatform().isBlank() ? event.getPlatform() : "whodatidols";
            boolean isBl = "bldizi".equalsIgnoreCase(plat) || Boolean.TRUE.equals(event.getIsBl());

            String sql = "UPDATE CalendarEvent SET " +
                    "dayOfWeek = ?, title = ?, episode = ?, showTime = ?, sortOrder = ?, " +
                    "posterUrl = ?, platform = ?, isBl = ?, channel = ?, category = ?, summary = ?, " +
                    "slug = ?, seriesId = ?, tmdbId = ?, tvmazeId = ? " +
                    "WHERE ID = ?";

            jdbcTemplate.update(sql,
                    event.getDayOfWeek() != null ? event.getDayOfWeek().toLowerCase() : "monday",
                    event.getTitle(),
                    event.getEpisode(),
                    event.getShowTime(),
                    event.getSortOrder(),
                    event.getPosterUrl(),
                    plat,
                    isBl,
                    event.getChannel(),
                    event.getCategory(),
                    event.getSummary(),
                    event.getSlug(),
                    event.getSeriesId(),
                    event.getTmdbId(),
                    event.getTvmazeId(),
                    event.getId());

            return ResponseEntity.ok("Event updated successfully");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error updating event: " + e.getMessage());
        }
    }

    // Admin API to delete event
    @CacheEvict(value = "calendar", allEntries = true)
    @DeleteMapping("/admin/calendar/delete")
    public ResponseEntity<String> deleteEvent(@RequestParam("id") String id) {
        try {
            String sql = "DELETE FROM CalendarEvent WHERE ID = ?";
            jdbcTemplate.update(sql, UUID.fromString(id));
            return ResponseEntity.ok("Event deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error deleting event: " + e.getMessage());
        }
    }

    private Map<String, Object> mapRowToEvent(Map<String, Object> row) {
        Map<String, Object> event = new HashMap<>();
        event.put("id", row.get("ID"));
        event.put("dayOfWeek", row.get("dayOfWeek"));
        event.put("title", row.get("title"));
        event.put("episode", row.get("episode"));
        event.put("time", row.get("showTime"));
        event.put("sortOrder", row.get("sortOrder"));
        event.put("posterUrl", row.get("posterUrl"));
        
        String platform = (String) row.get("platform");
        Boolean isBl = (Boolean) row.get("isBl");
        if (platform == null || platform.isBlank()) {
            platform = Boolean.TRUE.equals(isBl) ? "bldizi" : "whodatidols";
        }
        event.put("platform", platform);
        event.put("isBl", Boolean.TRUE.equals(isBl));
        event.put("channel", row.get("channel"));
        event.put("category", row.get("category"));
        event.put("summary", row.get("summary"));
        event.put("slug", row.get("slug"));
        event.put("seriesId", row.get("seriesId"));
        event.put("tmdbId", row.get("tmdbId"));
        event.put("tvmazeId", row.get("tvmazeId"));
        return event;
    }
}
