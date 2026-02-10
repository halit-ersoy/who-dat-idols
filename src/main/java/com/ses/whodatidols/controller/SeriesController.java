package com.ses.whodatidols.controller;

import com.ses.whodatidols.model.Series;
import com.ses.whodatidols.repository.SeriesRepository;
import com.ses.whodatidols.service.SeriesService;
import com.ses.whodatidols.viewmodel.EpisodeViewModel;
import com.ses.whodatidols.viewmodel.VideoViewModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/series") // Renamed endpoint to /api/series from /api/soapoperas
public class SeriesController {
    private final SeriesRepository seriesRepository;
    private final SeriesService seriesService;

    public SeriesController(SeriesRepository seriesRepository, SeriesService seriesService) {
        this.seriesRepository = seriesRepository;
        this.seriesService = seriesService;
    }

    @GetMapping("/recent")
    public ResponseEntity<List<VideoViewModel>> getRecentSeries(
            @RequestParam(value = "day", defaultValue = "20") int day) {
        // limit logic reusing 'day' param as count/limit
        List<Series> recentSeries = seriesRepository.findRecentSeries(day);

        List<VideoViewModel> viewModels = recentSeries.stream()
                .map(series -> {
                    VideoViewModel vm = new VideoViewModel();
                    String category = series.getCategory() != null ? series.getCategory() : "";
                    String mainCategory = category.contains(",") ? category.split(",")[0] : category;

                    vm.setId(series.getId().toString());
                    vm.setTitle(series.getName());
                    vm.setInfo(mainCategory);

                    // Thumbnail URL for the parent series
                    vm.setThumbnailUrl("/media/image/" + series.getId());
                    // Video URL points to series ID, MediaController play logic handles it
                    vm.setVideoUrl("/watch?id=" + series.getId());

                    return vm;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(viewModels);
    }

    @GetMapping("/{id}/episodes")
    public ResponseEntity<List<EpisodeViewModel>> getEpisodes(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(seriesService.getEpisodesForSeries(id));
    }

    @GetMapping("/episode/{id}/parent")
    public ResponseEntity<Series> getParentSeries(@PathVariable("id") UUID episodeId) {
        // This is legacy support, or we can find via SeriesId FK
        // But for now, if we have just episodeId, we might not know seriesId easily
        // without query
        // SeriesRepository has logic for this
        Series series = seriesRepository.findSeriesByEpisodeIdInsideXML(episodeId.toString());
        if (series == null)
            return ResponseEntity.notFound().build();
        return ResponseEntity.ok(series);
    }
}
