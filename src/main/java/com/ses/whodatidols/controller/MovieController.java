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

    @GetMapping("/recent")
    public ResponseEntity<List<VideoViewModel>> getRecentMovies(
            @RequestParam(value = "day", defaultValue = "10") int day) {
        List<Movie> recentMovies = movieRepository.findRecentMovies(day);

        List<VideoViewModel> viewModels = recentMovies.stream()
                .map(movie -> {
                    VideoViewModel vm = new VideoViewModel();
                    vm.setId(movie.getId().toString());
                    vm.setTitle(movie.getName());
                    vm.setInfo(movie.getYear() + " • " + movie.getCategory() + " • " + movie.getTime() + " dk");

                    String thumbnailUrl = "/api/images/movie?id=" + movie.getId();
                    vm.setThumbnailUrl(thumbnailUrl);

                    // Set video URL based on the ID
                    vm.setVideoUrl("/watch?id=" + movie.getId());

                    return vm;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(viewModels);
    }
}