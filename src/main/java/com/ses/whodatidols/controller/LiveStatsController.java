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
        // Real client IP resolution (supporting Reverse Proxy / Cloudflare)
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        } else if (ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        String userAgent = request.getHeader("User-Agent");
        String sessionId = ip + "_" + (userAgent != null ? userAgent : "anonymous");
        trackingService.recordHeartbeat(sessionId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/active-users")
    public ResponseEntity<Map<String, Integer>> getActiveUsers() {
        return ResponseEntity.ok(Map.of("activeUsers", trackingService.getActiveUserCount()));
    }
}
