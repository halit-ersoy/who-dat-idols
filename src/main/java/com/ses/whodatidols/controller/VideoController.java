package com.ses.whodatidols.controller;

import com.ses.whodatidols.service.TranscodingService;
import com.ses.whodatidols.service.VideoService;
import com.ses.whodatidols.model.Episode;
import com.ses.whodatidols.model.Movie;
import com.ses.whodatidols.repository.MovieRepository;
import com.ses.whodatidols.repository.SeriesRepository;
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

    public VideoController(VideoService videoService, TranscodingService transcodingService,
            MovieRepository movieRepository, SeriesRepository seriesRepository) {
        this.videoService = videoService;
        this.transcodingService = transcodingService;
        this.movieRepository = movieRepository;
        this.seriesRepository = seriesRepository;
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
                } else {
                    response.put("title", episode.getName());
                }
            } catch (Exception e) {
                // Ignore if parent not found
            }

            response.put("season", episode.getSeasonNumber());
            response.put("episode", episode.getEpisodeNumber());
            response.put("type", "episode");
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
            response.put("type", "movie");
            return ResponseEntity.ok(response);
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
}