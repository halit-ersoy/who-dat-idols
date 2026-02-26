package com.ses.whodatidols.controller;

import com.ses.whodatidols.repository.SystemSettingRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class SystemSettingController {

    private final SystemSettingRepository repository;

    public SystemSettingController(SystemSettingRepository repository) {
        this.repository = repository;
    }

    /**
     * Public endpoint to get the current announcement.
     * Used by the Homepage.
     */
    @Cacheable("announcements")
    @GetMapping("/api/settings/announcement")
    public ResponseEntity<Map<String, Object>> getAnnouncement() {
        return ResponseEntity.ok(repository.getAnnouncement());
    }

    /**
     * Admin endpoint to update the announcement.
     * Used by the Admin Panel.
     * Note: In a production environment, ensure this is protected by Security
     * Configuration.
     */
    @PostMapping("/admin/settings/announcement")
    public ResponseEntity<?> updateAnnouncement(@RequestBody Map<String, Object> payload) {
        String text = (String) payload.get("text");
        Object activeObj = payload.get("active");
        boolean active = false;

        if (activeObj instanceof Boolean) {
            active = (Boolean) activeObj;
        } else if (activeObj instanceof String) {
            active = Boolean.parseBoolean((String) activeObj);
        }

        repository.updateAnnouncement(text, active);
        return ResponseEntity.ok(Map.of("success", true, "message", "Duyuru güncellendi"));
    }
}
