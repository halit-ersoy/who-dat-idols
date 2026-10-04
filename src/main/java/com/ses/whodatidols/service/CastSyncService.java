package com.ses.whodatidols.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ses.whodatidols.model.CastMemberDto;
import com.ses.whodatidols.model.Movie;
import com.ses.whodatidols.model.Series;
import com.ses.whodatidols.repository.ActorRepository;
import com.ses.whodatidols.repository.MovieRepository;
import com.ses.whodatidols.repository.SeriesRepository;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class CastSyncService {

    public static class SyncProgress {
        private boolean isSyncing = false;
        private int total = 0;
        private int current = 0;
        private int percent = 0;
        private String currentItem = "";
        private int moviesSynced = 0;
        private int seriesSynced = 0;
        private String status = "idle"; // "idle", "running", "completed", "error"
        private String message = "";
        private String phase = "init"; // "photos", "productions"

        @com.fasterxml.jackson.annotation.JsonProperty("isSyncing")
        public boolean isSyncing() { return isSyncing; }
        @com.fasterxml.jackson.annotation.JsonProperty("isSyncing")
        public void setSyncing(boolean syncing) { isSyncing = syncing; }


        public int getTotal() { return total; }
        public void setTotal(int total) { this.total = total; }
        public int getCurrent() { return current; }
        public void setCurrent(int current) { this.current = current; }
        public int getPercent() { return percent; }
        public void setPercent(int percent) { this.percent = percent; }
        public String getCurrentItem() { return currentItem; }
        public void setCurrentItem(String currentItem) { this.currentItem = currentItem; }
        public int getMoviesSynced() { return moviesSynced; }
        public void setMoviesSynced(int moviesSynced) { this.moviesSynced = moviesSynced; }
        public int getSeriesSynced() { return seriesSynced; }
        public void setSeriesSynced(int seriesSynced) { this.seriesSynced = seriesSynced; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public String getPhase() { return phase; }
        public void setPhase(String phase) { this.phase = phase; }
    }

    private final ActorRepository actorRepository;
    private final TmdbService tmdbService;
    private final TvMazeService tvMazeService;
    private final CacheService cacheService;
    private final ObjectMapper objectMapper;
    private final MovieRepository movieRepository;
    private final SeriesRepository seriesRepository;

    private final SyncProgress syncProgress = new SyncProgress();
    private volatile boolean stopRequested = false;

    public CastSyncService(ActorRepository actorRepository, TmdbService tmdbService,
                           TvMazeService tvMazeService, CacheService cacheService,
                           ObjectMapper objectMapper, MovieRepository movieRepository,
                           SeriesRepository seriesRepository) {
        this.actorRepository = actorRepository;
        this.tmdbService = tmdbService;
        this.tvMazeService = tvMazeService;
        this.cacheService = cacheService;
        this.objectMapper = objectMapper;
        this.movieRepository = movieRepository;
        this.seriesRepository = seriesRepository;
    }

    public synchronized SyncProgress getProgress() {
        return syncProgress;
    }

    public synchronized void stopSync() {
        if (syncProgress.isSyncing()) {
            stopRequested = true;
            syncProgress.setStatus("stopping");
            syncProgress.setCurrentItem("Durduruluyor...");
        }
    }

    private void handleStopped(int processed, int moviesSynced, int seriesSynced, int totalItems) {
        syncProgress.setSyncing(false);
        syncProgress.setStatus("stopped");
        syncProgress.setCurrentItem("Durduruldu");
        syncProgress.setMessage("Senkronizasyon kullanıcı tarafından durduruldu. (" + processed + "/" + totalItems + " yapım incelendi, " + moviesSynced + " film, " + seriesSynced + " dizi güncellendi)");
        cacheService.evictContentCaches();
    }

    public List<CastMemberDto> syncCastForMovie(Movie movie) {
        return syncCastForMovie(movie, true, false);
    }

    public List<CastMemberDto> syncCastForMovie(Movie movie, boolean evictCache) {
        return syncCastForMovie(movie, evictCache, false);
    }

    public List<CastMemberDto> syncCastForMovie(Movie movie, boolean evictCache, boolean forceRefresh) {
        if (movie == null || movie.getId() == null) {
            return Collections.emptyList();
        }

        if (!forceRefresh && actorRepository.hasCastForMovie(movie.getId()) && !actorRepository.isMovieMissingAnyPhoto(movie.getId())) {
            return actorRepository.getCastForMovie(movie.getId());
        }

        if (movie.getName() == null || movie.getName().trim().isEmpty()) {
            return Collections.emptyList();
        }

        try {
            List<Map<String, Object>> searchResults = tmdbService.search(movie.getName().trim(), "movie");
            if (searchResults != null && !searchResults.isEmpty()) {
                Map<String, Object> first = searchResults.get(0);
                Integer tmdbId = (Integer) first.get("id");
                if (tmdbId != null) {
                    List<CastMemberDto> cast = tmdbService.getCast(tmdbId, "movie");
                    if (cast != null && !cast.isEmpty()) {
                        actorRepository.saveMovieCast(movie.getId(), cast);
                        if (evictCache) {
                            cacheService.evictContentCaches();
                        }
                        return cast;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Movie cast sync failed for '" + movie.getName() + "': " + e.getMessage());
        }

        // Fallback: If DB already has cast, restore missing photos individually
        if (actorRepository.hasCastForMovie(movie.getId())) {
            List<CastMemberDto> existingCast = actorRepository.getCastForMovie(movie.getId());
            for (CastMemberDto dto : existingCast) {
                if (dto.getActorId() != null && !actorRepository.hasLocalPhoto(dto.getActorId())) {
                    actorRepository.restoreActorPhoto(dto.getActorId());
                }
            }
            return existingCast;
        }

        return Collections.emptyList();
    }

    public List<CastMemberDto> syncCastForSeries(Series series) {
        return syncCastForSeries(series, true, false);
    }

    public List<CastMemberDto> syncCastForSeries(Series series, boolean evictCache) {
        return syncCastForSeries(series, evictCache, false);
    }

    public List<CastMemberDto> syncCastForSeries(Series series, boolean evictCache, boolean forceRefresh) {
        if (series == null || series.getId() == null) {
            return Collections.emptyList();
        }

        if (!forceRefresh && actorRepository.hasCastForSeries(series.getId()) && !actorRepository.isSeriesMissingAnyPhoto(series.getId())) {
            return actorRepository.getCastForSeries(series.getId());
        }

        if (series.getName() == null || series.getName().trim().isEmpty()) {
            return Collections.emptyList();
        }

        String seriesName = series.getName().trim();

        // 1. Try TMDB
        try {
            List<Map<String, Object>> searchResults = tmdbService.search(seriesName, "tv");
            if (searchResults != null && !searchResults.isEmpty()) {
                Map<String, Object> first = searchResults.get(0);
                Integer tmdbId = (Integer) first.get("id");
                if (tmdbId != null) {
                    List<CastMemberDto> cast = tmdbService.getCast(tmdbId, "tv");
                    if (cast != null && !cast.isEmpty()) {
                        actorRepository.saveSeriesCast(series.getId(), cast);
                        if (evictCache) {
                            cacheService.evictContentCaches();
                        }
                        return cast;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("TMDB series cast sync failed for '" + seriesName + "': " + e.getMessage());
        }

        // 2. Try TVMaze
        try {
            List<Map<String, Object>> tvmazeResults = tvMazeService.searchSeries(seriesName);
            if (tvmazeResults != null && !tvmazeResults.isEmpty()) {
                Map<String, Object> first = tvmazeResults.get(0);
                @SuppressWarnings("unchecked")
                Map<String, Object> show = first.containsKey("show") ? (Map<String, Object>) first.get("show") : first;
                Integer tvmazeId = (Integer) show.get("id");
                if (tvmazeId != null) {
                    List<CastMemberDto> cast = tvMazeService.getShowCast(tvmazeId);
                    if (cast != null && !cast.isEmpty()) {
                        actorRepository.saveSeriesCast(series.getId(), cast);
                        if (evictCache) {
                            cacheService.evictContentCaches();
                        }
                        return cast;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("TVMaze series cast sync failed for '" + seriesName + "': " + e.getMessage());
        }

        // Fallback: If DB already has cast, restore missing photos individually
        if (actorRepository.hasCastForSeries(series.getId())) {
            List<CastMemberDto> existingCast = actorRepository.getCastForSeries(series.getId());
            for (CastMemberDto dto : existingCast) {
                if (dto.getActorId() != null && !actorRepository.hasLocalPhoto(dto.getActorId())) {
                    actorRepository.restoreActorPhoto(dto.getActorId());
                }
            }
            return existingCast;
        }

        return Collections.emptyList();
    }

    public synchronized boolean startSyncAllCastAsync() {
        if (syncProgress.isSyncing()) {
            return false;
        }

        stopRequested = false;
        syncProgress.setPhase("photos");
        syncProgress.setSyncing(true);
        syncProgress.setStatus("running");
        syncProgress.setCurrent(0);
        syncProgress.setTotal(0);
        syncProgress.setPercent(0);
        syncProgress.setMoviesSynced(0);
        syncProgress.setSeriesSynced(0);
        syncProgress.setCurrentItem("Eksik fotograflar taraniyor...");
        syncProgress.setMessage("");

        Thread.startVirtualThread(() -> {
            try {
                // Phase 1: Download any previously stored remote actor photos where local file is missing
                int photosDownloaded = actorRepository.downloadMissingLocalPhotos((curr, total, name) -> {
                    syncProgress.setCurrent(curr);
                    syncProgress.setTotal(total);
                    int pct = total > 0 ? (int) Math.round(((double) curr / total) * 100) : 0;
                    syncProgress.setPercent(pct);
                    syncProgress.setCurrentItem((name != null ? name : "Oyuncu") + " (" + curr + " / " + total + ")");
                }, () -> stopRequested);

                if (stopRequested) {
                    handleStopped(0, 0, 0, 0);
                    return;
                }

                // Phase 2: Production cast scan
                List<Movie> movies = movieRepository.findAll();
                List<Series> seriesList = seriesRepository.findAllSeries();
                int totalItems = movies.size() + seriesList.size();

                syncProgress.setPhase("productions");
                syncProgress.setTotal(totalItems);
                syncProgress.setCurrent(0);
                syncProgress.setPercent(0);
                syncProgress.setCurrentItem("Yapimlar taraniyor...");

                int processed = 0;
                int moviesSynced = 0;
                int seriesSynced = 0;

                for (Movie m : movies) {
                    if (stopRequested) {
                        handleStopped(processed, moviesSynced, seriesSynced, totalItems);
                        return;
                    }
                    processed++;
                    syncProgress.setCurrent(processed);
                    int pct = totalItems > 0 ? (int) Math.round(((double) processed / totalItems) * 100) : 0;
                    syncProgress.setPercent(pct);

                    try {
                        boolean needsSync = !actorRepository.hasCastForMovie(m.getId()) || actorRepository.isMovieMissingAnyPhoto(m.getId());
                        if (needsSync) {
                            syncProgress.setCurrentItem(m.getName() + " (Film - Senkronize Ediliyor)");
                            List<CastMemberDto> cast = syncCastForMovie(m, false, true);
                            if (!cast.isEmpty()) {
                                moviesSynced++;
                                syncProgress.setMoviesSynced(moviesSynced);
                            }
                            Thread.sleep(100);
                        } else {
                            syncProgress.setCurrentItem(m.getName() + " (Film - Güncel)");
                            Thread.sleep(15);
                        }
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        handleStopped(processed, moviesSynced, seriesSynced, totalItems);
                        return;
                    } catch (Exception e) {
                        System.err.println("Batch movie sync error for " + m.getName() + ": " + e.getMessage());
                    }
                }

                for (Series s : seriesList) {
                    if (stopRequested) {
                        handleStopped(processed, moviesSynced, seriesSynced, totalItems);
                        return;
                    }
                    processed++;
                    syncProgress.setCurrent(processed);
                    int pct = totalItems > 0 ? (int) Math.round(((double) processed / totalItems) * 100) : 0;
                    syncProgress.setPercent(pct);

                    try {
                        boolean needsSync = !actorRepository.hasCastForSeries(s.getId()) || actorRepository.isSeriesMissingAnyPhoto(s.getId());
                        if (needsSync) {
                            syncProgress.setCurrentItem(s.getName() + " (Dizi - Senkronize Ediliyor)");
                            List<CastMemberDto> cast = syncCastForSeries(s, false, true);
                            if (!cast.isEmpty()) {
                                seriesSynced++;
                                syncProgress.setSeriesSynced(seriesSynced);
                            }
                            Thread.sleep(100);
                        } else {
                            syncProgress.setCurrentItem(s.getName() + " (Dizi - Güncel)");
                            Thread.sleep(15);
                        }
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        handleStopped(processed, moviesSynced, seriesSynced, totalItems);
                        return;
                    } catch (Exception e) {
                        System.err.println("Batch series sync error for " + s.getName() + ": " + e.getMessage());
                    }
                }

                cacheService.evictContentCaches();

                syncProgress.setSyncing(false);
                syncProgress.setStatus("completed");
                syncProgress.setPercent(100);
                syncProgress.setCurrentItem("Tamamlandı");
                String summaryMsg = "Toplam " + totalItems + " yapım tarandı. " + moviesSynced + " film, " + seriesSynced + " dizi oyuncu kadrosu ve fotoğrafları güncellendi.";
                if (photosDownloaded > 0) {
                    summaryMsg += " (" + photosDownloaded + " eksik fotoğraf doğrudan indirildi)";
                }
                syncProgress.setMessage(summaryMsg);

            } catch (Exception e) {
                System.err.println("Batch cast sync error: " + e.getMessage());
                syncProgress.setSyncing(false);
                syncProgress.setStatus("error");
                syncProgress.setMessage(e.getMessage());
            }
        });

        return true;
    }

    public Map<String, Object> syncAllCast() {
        startSyncAllCastAsync();
        Map<String, Object> result = new HashMap<>();
        result.put("status", "started");
        return result;
    }

    public void saveCastFromJson(UUID contentId, String type, String castJson) {
        if (contentId == null || castJson == null || castJson.trim().isEmpty()) {
            return;
        }

        try {
            List<CastMemberDto> castList = objectMapper.readValue(castJson, new TypeReference<List<CastMemberDto>>() {});
            if (castList == null || castList.isEmpty()) {
                return;
            }

            if ("movie".equalsIgnoreCase(type)) {
                actorRepository.saveMovieCast(contentId, castList);
            } else {
                actorRepository.saveSeriesCast(contentId, castList);
            }
            cacheService.evictContentCaches();
        } catch (Exception e) {
            System.err.println("Failed to parse and save cast JSON for " + type + " (" + contentId + "): " + e.getMessage());
        }
    }
}
