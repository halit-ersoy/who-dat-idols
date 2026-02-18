package com.ses.whodatidols.controller;

import com.ses.whodatidols.model.Movie;
import com.ses.whodatidols.model.Series;
import com.ses.whodatidols.repository.MovieRepository;
import com.ses.whodatidols.service.SeriesService;
import com.ses.whodatidols.viewmodel.VideoViewModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/weekly-best")
public class WeeklyBestController {

    private final MovieRepository movieRepository;
    private final SeriesService seriesService;

    public WeeklyBestController(MovieRepository movieRepository, SeriesService seriesService) {
        this.movieRepository = movieRepository;
        this.seriesService = seriesService;
    }

    @GetMapping("/movies")
    public ResponseEntity<List<VideoViewModel>> getTopMovies() {
        List<Movie> topMovies = movieRepository.findTop6MoviesByCount();

        List<VideoViewModel> viewModels = topMovies.stream()
                .map(movie -> {
                    VideoViewModel vm = new VideoViewModel();
                    String category = movie.getCategory() != null ? movie.getCategory() : "";
                    String mainCategory = category.contains(",") ? category.split(",")[0] : category;

                    vm.setId(movie.getId().toString());
                    vm.setTitle(movie.getName());
                    String durationText = movie.getDurationMinutes() > 0 ? " • " + movie.getDurationMinutes() + " dk"
                            : "";
                    vm.setInfo(
                            movie.getReleaseYear() + " • " + mainCategory + durationText);

                    // Set the thumbnail URL to use the image API endpoint
                    vm.setThumbnailUrl("/media/image/" + movie.getId());
                    vm.setVideoUrl("/watch?id=" + movie.getId());

                    return vm;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(viewModels);
    }

    @GetMapping("/tv")
    public ResponseEntity<List<VideoViewModel>> getTopSoapOperas() {
        List<Series> topSeries = seriesService.getTop6SeriesByCount();

        List<VideoViewModel> viewModels = topSeries.stream()
                .map(series -> {
                    VideoViewModel vm = new VideoViewModel();
                    String category = series.getCategory() != null ? series.getCategory() : "";
                    String mainCategory = category.contains(",") ? category.split(",")[0] : category;

                    vm.setId(series.getId().toString());
                    vm.setTitle(series.getName());
                    vm.setInfo(mainCategory);
                    vm.setThumbnailUrl("/media/image/" + series.getId());

                    // For series, we need a way to watch it. Usually, we go to the watch page
                    // with the series ID, and it auto-opens the last episode.
                    vm.setVideoUrl("/watch?id=" + series.getId());

                    return vm;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(viewModels);
    }
}