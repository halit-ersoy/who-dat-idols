package com.ses.whodatidols.controller;

import com.ses.whodatidols.service.ActiveUserTrackingService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/stats")
public class LiveStatsController {

    private final ActiveUserTrackingService trackingService;

    public LiveStatsController(ActiveUserTrackingService trackingService) {
        this.trackingService = trackingService;
    }

    @PostMapping("/heartbeat")
    public ResponseEntity<?> recordHeartbeat(HttpServletRequest request) {
        // Use IP + User-Agent as a simple session identifier if no auth is present
        String sessionId = request.getRemoteAddr() + "_" + request.getHeader("User-Agent");
        trackingService.recordHeartbeat(sessionId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/active-users")
    public ResponseEntity<Map<String, Integer>> getActiveUsers() {
        return ResponseEntity.ok(Map.of("activeUsers", trackingService.getActiveUserCount()));
    }
}
