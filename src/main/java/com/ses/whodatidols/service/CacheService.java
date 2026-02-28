package com.ses.whodatidols.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class CacheService {

    private static final Logger logger = LoggerFactory.getLogger(CacheService.class);
    private final CacheManager cacheManager;

    public CacheService(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    /**
     * Evicts all caches related to movies, series, episodes, and general content.
     * Call this whenever content is added, updated, or deleted.
     */
    public void evictContentCaches() {
        List<String> cachesToEvict = List.of(
                "recentMovies",
                "recentSeries",
                "featuredMovies",
                "featuredTv",
                "loadedEpisodes",
                "translatedEpisodes",
                "calendar",
                "weeklyBestMovies",
                "weeklyBestSeries",
                "videoDetails",
                "resolvedSlugs",
                "similarContent",
                "featuredContent");

        logger.info("Evicting content caches for instant update...");
        for (String cacheName : cachesToEvict) {
            org.springframework.cache.Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
                logger.debug("Cleared cache: {}", cacheName);
            }
        }
    }
}
