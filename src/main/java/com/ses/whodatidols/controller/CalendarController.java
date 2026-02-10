package com.ses.whodatidols.controller;

import com.ses.whodatidols.model.CalendarEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

import java.util.*;

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
            // Create table if not exists - Renamed to CalendarEvent (Singular)
            String sql = "IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='CalendarEvent' AND xtype='U') " +
                    "CREATE TABLE CalendarEvent (" +
                    "ID UNIQUEIDENTIFIER PRIMARY KEY DEFAULT NEWID(), " +
                    "dayOfWeek NVARCHAR(20) NOT NULL, " +
                    "title NVARCHAR(255) NOT NULL, " +
                    "episode NVARCHAR(100), " +
                    "showTime NVARCHAR(10), " +
                    "sortOrder INT DEFAULT 0" +
                    ")";
            jdbcTemplate.execute(sql);
        } catch (Exception e) {
            System.err.println("Error initializing CalendarEvent table: " + e.getMessage());
        }
    }

    // Public API to get calendar data
    @GetMapping("/api/calendar")
    public ResponseEntity<Map<String, List<Map<String, Object>>>> getCalendar() {
        String sql = "SELECT * FROM CalendarEvent ORDER BY sortOrder ASC, showTime ASC";
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);

        Map<String, List<Map<String, Object>>> calendarData = new HashMap<>();
        // Initialize days
        String[] days = { "monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday" };
        for (String day : days) {
            calendarData.put(day, new ArrayList<>());
        }

        for (Map<String, Object> row : rows) {
            String day = ((String) row.get("dayOfWeek")).toLowerCase();
            if (calendarData.containsKey(day)) {
                Map<String, Object> event = new HashMap<>();
                event.put("id", row.get("ID"));
                event.put("title", row.get("title"));
                event.put("episode", row.get("episode"));
                event.put("time", row.get("showTime"));
                calendarData.get(day).add(event);
            }
        }

        return ResponseEntity.ok(calendarData);
    }

    // Admin API to add event
    @PostMapping("/admin/calendar/add")
    public ResponseEntity<String> addEvent(@RequestBody CalendarEvent event) {
        try {
            String sql = "INSERT INTO CalendarEvent (dayOfWeek, title, episode, showTime, sortOrder) VALUES (?, ?, ?, ?, ?)";
            // Simple sort order logic: just put at the end or 0
            int sortOrder = 0;
            // We could query MAX(sortOrder) + 1 if needed

            jdbcTemplate.update(sql,
                    event.getDayOfWeek(),
                    event.getTitle(),
                    event.getEpisode(),
                    event.getShowTime(),
                    sortOrder);
            return ResponseEntity.ok("Event added successfully");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error adding event: " + e.getMessage());
        }
    }

    // Admin API to delete event
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
}
