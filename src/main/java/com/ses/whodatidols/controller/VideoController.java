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
                } else {
                    response.put("title", episode.getName());
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
            return ResponseEntity.ok(response);
        }

        return ResponseEntity.notFound().build();
    }

    @GetMapping("/resolve-slug")
    public ResponseEntity<Map<String, String>> resolveSlug(@RequestParam("slug") String slug) {
        // Try finding an episode with this slug or ID
        boolean isUuid = false;
        try {
            UUID.fromString(slug);
            isUuid = true;
        } catch (IllegalArgumentException e) {
            // Not a UUID
        }

        List<Series> allSeries = seriesRepository.findAllSeries();
        for (Series s : allSeries) {
            List<Episode> episodes = seriesRepository.findEpisodesBySeriesId(s.getId());
            for (Episode e : episodes) {
                if (slug.equalsIgnoreCase(e.getSlug()) || (isUuid && e.getId().toString().equalsIgnoreCase(slug))) {
                    return ResponseEntity.ok(Map.of("id", e.getId().toString(), "type", "episode"));
                }
            }
        }

        // Try finding a movie with this slug or ID
        List<Movie> allMovies = movieRepository.findAll();
        for (Movie m : allMovies) {
            if (slug.equalsIgnoreCase(m.getSlug()) || (isUuid && m.getId().toString().equalsIgnoreCase(slug))) {
                return ResponseEntity.ok(Map.of("id", m.getId().toString(), "type", "movie"));
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