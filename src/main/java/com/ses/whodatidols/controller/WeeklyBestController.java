package com.ses.whodatidols.controller;

import com.ses.whodatidols.model.Movie;
import com.ses.whodatidols.model.SoapOpera;
import com.ses.whodatidols.repository.MovieRepository;
import com.ses.whodatidols.repository.SoapOperaRepository;
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
    private final SoapOperaRepository soapOperaRepository;

    public WeeklyBestController(MovieRepository movieRepository, SoapOperaRepository soapOperaRepository) {
        this.movieRepository = movieRepository;
        this.soapOperaRepository = soapOperaRepository;
    }

    @GetMapping("/movies")
    public ResponseEntity<List<VideoViewModel>> getTopMovies() {
        List<Movie> topMovies = movieRepository.findTop6MoviesByCount();

        List<VideoViewModel> viewModels = topMovies.stream()
                .map(movie -> {
                    VideoViewModel vm = new VideoViewModel();
                    String mainCategory = movie.getCategory().split(",")[0];

                    vm.setId(movie.getId().toString());
                    vm.setTitle(movie.getName());
                    vm.setInfo(movie.getYear() + " • " + mainCategory + " • " + movie.getTime() + " dk");

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
        List<SoapOpera> topSoapOperas = soapOperaRepository.findTop6SoapOperasByCount();

        List<VideoViewModel> viewModels = topSoapOperas.stream()
                .map(soapOpera -> {
                    VideoViewModel vm = new VideoViewModel();
                    String mainCategory = soapOpera.getCategory().split(",")[0];

                    vm.setId(soapOpera.getId().toString());
                    vm.setTitle(soapOpera.getName());
                    vm.setInfo(soapOpera.getYear() + " • " + mainCategory + " • " + soapOpera.getTime() + " dk");

                    // Set the thumbnail URL to use the image API endpoint
                    vm.setThumbnailUrl("/media/image/" + soapOpera.getId());
                    vm.setVideoUrl("/watch?id=" + soapOpera.getId());

                    return vm;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(viewModels);
    }
}