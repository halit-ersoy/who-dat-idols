package com.ses.whodatidols.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ActiveUserTrackingService {

    // Map of sessionID/identifier -> Last seen timestamp
    private final Map<String, Instant> activeUsers = new ConcurrentHashMap<>();
    private final int INACTIVITY_TIMEOUT_MINUTES = 5;

    public void recordHeartbeat(String sessionId) {
        if (sessionId == null || sessionId.isEmpty())
            return;
        activeUsers.put(sessionId, Instant.now());
    }

    public int getActiveUserCount() {
        return activeUsers.size();
    }

    // Cleanup inactive users every 1 minute
    @Scheduled(fixedRate = 60000)
    public void cleanupInactiveUsers() {
        Instant threshold = Instant.now().minusSeconds(INACTIVITY_TIMEOUT_MINUTES * 60);
        activeUsers.entrySet().removeIf(entry -> entry.getValue().isBefore(threshold));
    }
}
