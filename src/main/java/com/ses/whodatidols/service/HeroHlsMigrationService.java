package com.ses.whodatidols.service;

import com.ses.whodatidols.util.FFmpegUtils;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@Service
public class HeroHlsMigrationService {
    private static final Logger log = LoggerFactory.getLogger(HeroHlsMigrationService.class);

    private final JdbcTemplate jdbcTemplate;
    private final FFmpegUtils ffmpegUtils;

    @Value("${media.source.trailers.path}")
    private String trailersPath;

    public HeroHlsMigrationService(JdbcTemplate jdbcTemplate, FFmpegUtils ffmpegUtils) {
        this.jdbcTemplate = jdbcTemplate;
        this.ffmpegUtils = ffmpegUtils;
    }

    @PostConstruct
    public void checkAndMigrateHeroVideos() {
        try {
            // Get all video heroes
            List<Map<String, Object>> videoHeroes = jdbcTemplate.queryForList("SELECT ID FROM Hero WHERE isImage = 0");
            if (videoHeroes.isEmpty()) {
                return;
            }

            log.info("Starting Hero HLS migration check. Found {} video heroes.", videoHeroes.size());
            Path uploadPath = Paths.get(trailersPath).toAbsolutePath().normalize();

            for (Map<String, Object> hero : videoHeroes) {
                Object idObj = hero.get("ID");
                if (idObj == null) continue;
                String idStr = idObj.toString();
                
                Path mp4Path = uploadPath.resolve(idStr + ".mp4");
                Path hlsDir = uploadPath.resolve("hls").resolve(idStr);
                Path playlistPath = hlsDir.resolve("playlist.m3u8");

                if (Files.exists(mp4Path) && !Files.exists(playlistPath)) {
                    log.info("Converting video hero {} to HLS asynchronously...", idStr);
                    java.util.concurrent.CompletableFuture.runAsync(() -> {
                        try {
                            ffmpegUtils.convertToHls(mp4Path.toString(), hlsDir.toString());
                            log.info("HLS conversion complete for video hero: {}", idStr);
                        } catch (Exception e) {
                            log.error("Failed to convert video hero {} to HLS on startup: {}", idStr, e.getMessage());
                        }
                    });
                }
            }
        } catch (Exception e) {
            log.error("Hero HLS migration failed: {}", e.getMessage());
        }
    }
}
