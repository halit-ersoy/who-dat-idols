package com.ses.whodatidols.controller;

import com.ses.whodatidols.service.TranscodingService;
import com.ses.whodatidols.service.VideoService;
import com.ses.whodatidols.model.Episode;
import com.ses.whodatidols.model.Movie;
import com.ses.whodatidols.model.Series;
import com.ses.whodatidols.repository.MovieRepository;
import com.ses.whodatidols.repository.SeriesRepository;
import com.ses.whodatidols.repository.ContentRepository;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.cache.annotation.Cacheable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/video")
public class VideoController {
    private final VideoService videoService;
    private final TranscodingService transcodingService;
    private final MovieRepository movieRepository;
    private final SeriesRepository seriesRepository;
    private final ContentRepository contentRepository;

    public VideoController(VideoService videoService, TranscodingService transcodingService,
            MovieRepository movieRepository, SeriesRepository seriesRepository,
            ContentRepository contentRepository) {
        this.videoService = videoService;
        this.transcodingService = transcodingService;
        this.movieRepository = movieRepository;
        this.seriesRepository = seriesRepository;
        this.contentRepository = contentRepository;
    }

    @GetMapping("/details")
    @Cacheable("videoDetails")
    public ResponseEntity<Map<String, Object>> getDetails(@RequestParam("id") UUID id) {
        Map<String, Object> response = new HashMap<>();

        // 1. Try Episode
        Episode episode = seriesRepository.findEpisodeById(id);
        if (episode != null) {
            response.put("title", episode.getName());
            response.put("duration", episode.getDurationMinutes() > 0 ? episode.getDurationMinutes() + " dk" : "");
            response.put("year", episode.getReleaseYear());
            // Descriptions are usually on the Series parent, but maybe Episode has it?
            // Model doesn't have description for Episode.
            // Let's fetch parent series for category and description
            try {
                var series = seriesRepository.findSeriesByEpisodeId(id);
                if (series != null) {
                    // Update title to be "Series Name - X. Sezon Y. Bölüm"
                    String formattedTitle = series.getName() + " - " + episode.getSeasonNumber() + ". Sezon "
                            + episode.getEpisodeNumber() + ". Bölüm";
                    response.put("title", formattedTitle);

                    response.put("plot", series.getSummary());
                    response.put("genres",
                            series.getCategory() != null ? List.of(series.getCategory().split(",")) : List.of());
                    response.put("language", series.getLanguage());
                    response.put("country", series.getCountry());
                    response.put("finalStatus", series.getFinalStatus());
                    response.put("adult", series.isAdult());
                } else {
                    response.put("title", episode.getName());
                    response.put("adult", false); // Default if no parent series found
                }
            } catch (Exception e) {
                // Ignore if parent not found
            }

            response.put("season", episode.getSeasonNumber());
            response.put("episode", episode.getEpisodeNumber());
            response.put("type", "episode");
            response.put("seriesId", episode.getSeriesId()); // Added for frontend list check
            response.put("slug", episode.getSlug());
            return ResponseEntity.ok(response);
        }

        // 2. Try Movie
        Movie movie = movieRepository.findMovieById(id);
        if (movie != null) {
            response.put("title", movie.getName());
            response.put("duration", movie.getDurationMinutes() > 0 ? movie.getDurationMinutes() + " dk" : "");
            response.put("year", movie.getReleaseYear());
            response.put("plot", movie.getSummary());
            response.put("genres",
                    movie.getCategory() != null ? List.of(movie.getCategory().split(",")) : List.of());
            response.put("language", movie.getLanguage());
            response.put("country", movie.getCountry());
            response.put("type", "movie");
            response.put("slug", movie.getSlug());
            response.put("adult", movie.isAdult());
            return ResponseEntity.ok(response);
        }

        return ResponseEntity.notFound().build();
    }

    @GetMapping("/resolve-slug")
    @Cacheable("resolvedSlugs")
    public ResponseEntity<Map<String, String>> resolveSlug(@RequestParam("slug") String slug) {
        boolean isUuid = false;
        try {
            UUID.fromString(slug);
            isUuid = true;
        } catch (IllegalArgumentException e) {
            // Not a UUID
        }

        // If it's a UUID, do ID-based lookup
        if (isUuid) {
            UUID id = UUID.fromString(slug);

            // Check episode by ID
            Episode ep = seriesRepository.findEpisodeById(id);
            if (ep != null) {
                return ResponseEntity.ok(Map.of("id", ep.getId().toString(), "type", "episode"));
            }

            // Check series by ID → return first episode
            Series s = seriesRepository.findSeriesById(id);
            if (s != null) {
                UUID firstEpId = seriesRepository.findFirstEpisodeIdBySeriesId(s.getId());
                if (firstEpId != null) {
                    return ResponseEntity.ok(Map.of("id", firstEpId.toString(), "type", "episode"));
                }
            }

            // Check movie by ID
            Movie m = movieRepository.findMovieById(id);
            if (m != null) {
                return ResponseEntity.ok(Map.of("id", m.getId().toString(), "type", "movie"));
            }

            return ResponseEntity.notFound().build();
        }

        // --- Slug-based lookup (exact match, no iteration) ---

        // 1. Check if it's an episode slug (most specific — includes season/episode
        // info)
        Episode episode = seriesRepository.findEpisodeBySlug(slug);
        if (episode != null) {
            return ResponseEntity.ok(Map.of("id", episode.getId().toString(), "type", "episode"));
        }

        // 2. Check if it's a movie slug (movies before series to avoid slug collision)
        Movie movie = movieRepository.findMovieBySlug(slug);
        if (movie != null) {
            return ResponseEntity.ok(Map.of("id", movie.getId().toString(), "type", "movie"));
        }

        // 3. Check if it's a series slug → return first episode
        Series series = seriesRepository.findSeriesBySlug(slug);
        if (series != null) {
            UUID firstEpId = seriesRepository.findFirstEpisodeIdBySeriesId(series.getId());
            if (firstEpId != null) {
                return ResponseEntity.ok(Map.of("id", firstEpId.toString(), "type", "episode"));
            }
        }

        return ResponseEntity.notFound().build();
    }

    @GetMapping("/transcode")
    public ResponseEntity<ResourceRegion> getTranscodedVideo(
            @RequestParam("id") UUID id,
            @RequestParam(value = "res", defaultValue = "720") int resolution,
            @RequestHeader(value = "Range", required = false) String rangeHeader) throws Exception {
        String originalPath = videoService.getVideoPath(id);
        return transcodingService.getTranscodedVideo(originalPath, String.valueOf(id), resolution, rangeHeader);
    }

    @PostMapping("/increment-view")
    public ResponseEntity<?> incrementViewCount(@RequestParam("id") UUID id) {
        try {
            videoService.incrementViewCount(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failed to increment view count");
        }
    }

    @GetMapping("/similar")
    @Cacheable("similarContent")
    public ResponseEntity<List<Map<String, Object>>> getSimilarContent(@RequestParam("id") UUID id) {
        // If the ID is an episode, we should find recommendations based on the series
        UUID contentIdForSearch = id;
        Episode episode = seriesRepository.findEpisodeById(id);
        if (episode != null && episode.getSeriesId() != null) {
            contentIdForSearch = episode.getSeriesId();
        }

        List<Map<String, Object>> similar = contentRepository.findSimilarContent(contentIdForSearch, 12);
        return ResponseEntity.ok(similar);
    }
}