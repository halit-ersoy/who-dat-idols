package com.ses.whodatidols.controller;

import com.ses.whodatidols.model.Movie;
import com.ses.whodatidols.repository.MovieRepository;
import com.ses.whodatidols.viewmodel.VideoViewModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/movies")
public class MovieController {
    private final MovieRepository movieRepository;

    public MovieController(MovieRepository movieRepository) {
        this.movieRepository = movieRepository;
    }

    // In MovieController, modify the getRecentMovies method:
    @GetMapping("/recent")
    public ResponseEntity<List<VideoViewModel>> getRecentMovies(
            @RequestParam(value = "limit", defaultValue = "0") int limit) {
        List<Movie> recentMovies = movieRepository.findRecentMovies(limit);

        List<VideoViewModel> viewModels = recentMovies.stream()
                .map(movie -> {
                    VideoViewModel vm = new VideoViewModel();
                    String mainCategory = movie.getCategory().split(",")[0];

                    vm.setId(movie.getId().toString());
                    vm.setTitle(movie.getName());
                    String durationText = movie.getDurationMinutes() > 0 ? " • " + movie.getDurationMinutes() + " dk"
                            : "";
                    vm.setInfo(
                            movie.getReleaseYear() + " • " + mainCategory + durationText);

                    // Updated to use the new media endpoint
                    vm.setThumbnailUrl("/media/image/" + movie.getId());
                    vm.setVideoUrl("/watch?id=" + movie.getId());

                    return vm;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(viewModels);
    }
}