package com.ses.whodatidols.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
public class CacheConfig {

    private static final Logger logger = LoggerFactory.getLogger(CacheConfig.class);

    /**
     * Evict dynamic content caches every 15 minutes.
     * This ensures the homepage shows recent updates reasonably fast
     * while drastically reducing DB load.
     */
    @CacheEvict(value = {
            "weeklyBestMovies",
            "weeklyBestSeries",
            "recentSeries",
            "recentMovies",
            "translatedEpisodes",
            "loadedEpisodes"
    }, allEntries = true)
    @Scheduled(fixedRateString = "${cache.dynamic.ttl:900000}") // 900000 ms = 15 minutes
    public void evictDynamicCaches() {
        logger.debug("Evicting dynamic content caches (weekly best, recent content, etc.)");
    }

    /**
     * Evict mostly static content caches every 12 hours.
     * Things like update notes, announcements, and calendar don't change often.
     */
    @CacheEvict(value = {
            "updateNotes",
            "calendar",
            "announcements"
    }, allEntries = true)
    @Scheduled(fixedRateString = "${cache.static.ttl:43200000}") // 43200000 ms = 12 hours
    public void evictStaticCaches() {
        logger.debug("Evicting static content caches (update notes, calendar, announcements)");
    }
}
