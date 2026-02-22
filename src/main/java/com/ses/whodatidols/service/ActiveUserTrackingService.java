package com.ses.whodatidols.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ActiveUserTrackingService {

    // Map of sessionID/identifier -> Last seen timestamp
    private final Map<String, LocalDateTime> activeUsers = new ConcurrentHashMap<>();
    private final int INACTIVITY_TIMEOUT_MINUTES = 5;

    public void recordHeartbeat(String sessionId) {
        if (sessionId == null || sessionId.isEmpty())
            return;
        activeUsers.put(sessionId, LocalDateTime.now());
    }

    public int getActiveUserCount() {
        return activeUsers.size();
    }

    // Cleanup inactive users every 1 minute
    @Scheduled(fixedRate = 60000)
    public void cleanupInactiveUsers() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(INACTIVITY_TIMEOUT_MINUTES);
        activeUsers.entrySet().removeIf(entry -> entry.getValue().isBefore(threshold));
    }
}
