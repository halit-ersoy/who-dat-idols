package com.ses.whodatidols.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class LoginAttemptService {

    private static final int MAX_ATTEMPT = 5;
    private static final int LOCK_TIME_MINUTES = 5;

    private final Cache<String, Integer> attemptsCache;

    public LoginAttemptService() {
        attemptsCache = Caffeine.newBuilder()
                .expireAfterWrite(LOCK_TIME_MINUTES, TimeUnit.MINUTES)
                .build();
    }

    public void loginSucceeded(String key) {
        attemptsCache.invalidate(key);
    }

    public void loginFailed(String key) {
        int attempts = getAttempts(key);
        attempts++;
        attemptsCache.put(key, attempts);
    }

    public boolean isBlocked(String key) {
        return getAttempts(key) >= MAX_ATTEMPT;
    }

    private int getAttempts(String key) {
        Integer attempts = attemptsCache.getIfPresent(key);
        return attempts == null ? 0 : attempts;
    }
}
