package com.ses.whodatidols.controller;

import com.ses.whodatidols.model.Episode;
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
                    vm.setInfo(
                            movie.getReleaseYear() + " • " + mainCategory + " • " + movie.getDurationMinutes() + " dk");

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
        // Use logic for Episodes now, assuming SoapOperaCount tracks episodes
        List<Episode> topEpisodes = seriesService.getTop6EpisodesByCount();

        List<VideoViewModel> viewModels = topEpisodes.stream()
                .map(ep -> {
                    VideoViewModel vm = new VideoViewModel();

                    // Haftanın en iyilerinde dönen ID bölüm (child) ID'sidir.
                    // Metadata için parent seriyi bulalım.
                    Series parent = null;
                    if (ep.getSeriesId() != null) {
                        parent = seriesService.getSeriesById(ep.getSeriesId());
                    }

                    String category = "";
                    String imageId = ep.getId().toString();
                    String title = ep.getName();

                    if (parent != null) {
                        category = parent.getCategory() != null ? parent.getCategory() : "";
                        // Resim için parent ID kullanmak daha garantidir
                        imageId = parent.getId().toString();
                        // If episode name is generic, maybe append series name?
                        // But usually episode name is set to series name if file upload logic didn't
                        // specify distinct name.
                        // Or logic: "Series Name - Episode Name" if different?
                        // Legacy logic used soapOpera.getName() which was Series Name mostly.
                        // Episode.name is initialized to SeriesName in saveEpisodeWithFile.
                        title = parent.getName(); // Use series name for display list?
                        // Or if episode has specific name?
                        // Let's stick to parent name as "Weekly Top Series" usually means Series.
                    }

                    String mainCategory = category.contains(",") ? category.split(",")[0] : category;

                    vm.setId(ep.getId().toString()); // Video ID is Episode ID
                    vm.setTitle(title);
                    vm.setInfo(mainCategory);

                    // Thumbnail URL supports the resolved imageId (parent series)
                    vm.setThumbnailUrl("/media/image/" + imageId);
                    vm.setVideoUrl("/watch?id=" + ep.getId());

                    return vm;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(viewModels);
    }
}