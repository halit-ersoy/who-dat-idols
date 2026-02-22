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

import com.ses.whodatidols.viewmodel.PageResponse;

@RestController
@RequestMapping("/api/movies")
public class MovieController {
        private final MovieRepository movieRepository;

        public MovieController(MovieRepository movieRepository) {
                this.movieRepository = movieRepository;
        }

        @GetMapping("/recent")
        public ResponseEntity<PageResponse<VideoViewModel>> getRecentMoviesPaged(
                        @RequestParam(value = "page", defaultValue = "1") int page,
                        @RequestParam(value = "size", defaultValue = "18") int size) {

                int offset = (page - 1) * size;
                List<Movie> recentMovies = movieRepository.findRecentMoviesPaged(offset, size);
                int totalElements = movieRepository.countAllMovies();
                int totalPages = (int) Math.ceil((double) totalElements / size);

                List<VideoViewModel> viewModels = recentMovies.stream()
                                .map(movie -> {
                                        VideoViewModel vm = new VideoViewModel();
                                        String mainCategory = movie.getCategory() != null
                                                        && !movie.getCategory().isEmpty()
                                                                        ? movie.getCategory().split(",")[0]
                                                                        : "";

                                        vm.setId(movie.getId().toString());
                                        vm.setTitle(movie.getName());
                                        String durationText = movie.getDurationMinutes() > 0
                                                        ? " • " + movie.getDurationMinutes() + " dk"
                                                        : "";
                                        vm.setInfo(
                                                        movie.getReleaseYear() + " • " + mainCategory + durationText);

                                        vm.setThumbnailUrl("/media/image/" + movie.getId());
                                        vm.setVideoUrl(movie.getSlug() != null && !movie.getSlug().isEmpty()
                                                        ? "/" + movie.getSlug()
                                                        : "/" + movie.getId());

                                        return vm;
                                })
                                .collect(Collectors.toList());

                PageResponse<VideoViewModel> response = new PageResponse<>(viewModels, totalPages, page, totalElements);
                return ResponseEntity.ok(response);
        }

        @GetMapping("/search")
        public ResponseEntity<PageResponse<VideoViewModel>> searchMoviesPaged(
                        @RequestParam(value = "query", defaultValue = "") String query,
                        @RequestParam(value = "page", defaultValue = "1") int page,
                        @RequestParam(value = "size", defaultValue = "18") int size) {

                if (query.trim().isEmpty()) {
                        return getRecentMoviesPaged(page, size);
                }

                int offset = (page - 1) * size;
                List<Movie> recentMovies = movieRepository.searchMoviesPaged(query, offset, size);
                int totalElements = movieRepository.countMoviesBySearch(query);
                int totalPages = (int) Math.ceil((double) totalElements / size);

                List<VideoViewModel> viewModels = recentMovies.stream()
                                .map(movie -> {
                                        VideoViewModel vm = new VideoViewModel();
                                        String mainCategory = movie.getCategory() != null
                                                        && !movie.getCategory().isEmpty()
                                                                        ? movie.getCategory().split(",")[0]
                                                                        : "";

                                        vm.setId(movie.getId().toString());
                                        vm.setTitle(movie.getName());
                                        String durationText = movie.getDurationMinutes() > 0
                                                        ? " • " + movie.getDurationMinutes() + " dk"
                                                        : "";
                                        vm.setInfo(
                                                        movie.getReleaseYear() + " • " + mainCategory + durationText);

                                        vm.setThumbnailUrl("/media/image/" + movie.getId());
                                        vm.setVideoUrl(movie.getSlug() != null && !movie.getSlug().isEmpty()
                                                        ? "/" + movie.getSlug()
                                                        : "/" + movie.getId());

                                        return vm;
                                })
                                .collect(Collectors.toList());

                PageResponse<VideoViewModel> response = new PageResponse<>(viewModels, totalPages, page, totalElements);
                return ResponseEntity.ok(response);
        }
}