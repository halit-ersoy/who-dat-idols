package com.ses.whodatidols.controller;

import com.ses.whodatidols.model.Series;
import com.ses.whodatidols.repository.SeriesRepository;
import com.ses.whodatidols.service.SeriesService;
import com.ses.whodatidols.viewmodel.EpisodeViewModel;
import com.ses.whodatidols.viewmodel.VideoViewModel;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.ses.whodatidols.viewmodel.PageResponse;

@RestController
@RequestMapping({ "/api/series", "/api/soapoperas" }) // Added alias for compatibility
public class SeriesController {
    private final SeriesRepository seriesRepository;
    private final SeriesService seriesService;

    public SeriesController(SeriesRepository seriesRepository, SeriesService seriesService) {
        this.seriesRepository = seriesRepository;
        this.seriesService = seriesService;
    }

    @Cacheable(value = "recentSeries", key = "#page + '-' + #size")
    @GetMapping("/recent")
    public ResponseEntity<PageResponse<VideoViewModel>> getRecentSeriesPaged(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "18") int size) {

        int offset = (page - 1) * size;
        List<Series> recentSeries = seriesRepository.findRecentSeriesPaged(offset, size);
        int totalElements = seriesRepository.countAllSeries();
        int totalPages = (int) Math.ceil((double) totalElements / size);

        List<VideoViewModel> viewModels = recentSeries.stream()
                .map(series -> {
                    VideoViewModel vm = new VideoViewModel();
                    String category = series.getCategory() != null ? series.getCategory() : "";
                    String mainCategory = category.contains(",") ? category.split(",")[0] : category;

                    vm.setId(series.getId().toString());
                    vm.setTitle(series.getName());
                    vm.setInfo(mainCategory);

                    vm.setThumbnailUrl("/media/image/" + series.getId());
                    List<EpisodeViewModel> episodes = seriesService.getEpisodesForSeries(series.getId());
                    if (!episodes.isEmpty()) {
                        EpisodeViewModel lastEp = episodes.get(episodes.size() - 1);
                        if (lastEp.getSlug() != null && !lastEp.getSlug().isEmpty()) {
                            vm.setVideoUrl("/" + lastEp.getSlug());
                        } else {
                            vm.setVideoUrl("/" + lastEp.getId());
                        }
                    } else {
                        vm.setVideoUrl("/" + series.getId());
                    }

                    return vm;
                })
                .collect(Collectors.toList());

        PageResponse<VideoViewModel> response = new PageResponse<>(viewModels, totalPages, page, totalElements);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/search")
    public ResponseEntity<PageResponse<VideoViewModel>> searchSeriesPaged(
            @RequestParam(value = "query", defaultValue = "") String query,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "18") int size) {

        if (query.trim().isEmpty()) {
            return getRecentSeriesPaged(page, size);
        }

        int offset = (page - 1) * size;
        List<Series> recentSeries = seriesRepository.searchSeriesPaged(query, offset, size);
        int totalElements = seriesRepository.countSeriesBySearch(query);
        int totalPages = (int) Math.ceil((double) totalElements / size);

        List<VideoViewModel> viewModels = recentSeries.stream()
                .map(series -> {
                    VideoViewModel vm = new VideoViewModel();
                    String category = series.getCategory() != null ? series.getCategory() : "";
                    String mainCategory = category.contains(",") ? category.split(",")[0] : category;

                    vm.setId(series.getId().toString());
                    vm.setTitle(series.getName());
                    vm.setInfo(mainCategory);

                    vm.setThumbnailUrl("/media/image/" + series.getId());
                    List<EpisodeViewModel> episodes = seriesService.getEpisodesForSeries(series.getId());
                    if (!episodes.isEmpty()) {
                        EpisodeViewModel lastEp = episodes.get(episodes.size() - 1);
                        if (lastEp.getSlug() != null && !lastEp.getSlug().isEmpty()) {
                            vm.setVideoUrl("/" + lastEp.getSlug());
                        } else {
                            vm.setVideoUrl("/" + lastEp.getId());
                        }
                    } else {
                        vm.setVideoUrl("/" + series.getId());
                    }

                    return vm;
                })
                .collect(Collectors.toList());

        PageResponse<VideoViewModel> response = new PageResponse<>(viewModels, totalPages, page, totalElements);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/filter")
    public ResponseEntity<PageResponse<VideoViewModel>> filterSeriesPaged(
            @RequestParam(value = "seriesType", required = false) String seriesType,
            @RequestParam(value = "categoryId", required = false) Integer categoryId,
            @RequestParam(value = "finalStatus", required = false) Integer finalStatus,
            @RequestParam(value = "year", required = false) Integer year,
            @RequestParam(value = "country", required = false) String country,
            @RequestParam(value = "sort", defaultValue = "newest") String sort,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "18") int size) {

        int offset = (page - 1) * size;

        List<Series> filteredSeries = seriesRepository.findSeriesWithFilters(seriesType, categoryId, finalStatus, year,
                country, sort, offset, size);
        int totalElements = seriesRepository.countSeriesWithFilters(seriesType, categoryId, finalStatus, year, country);
        int totalPages = (int) Math.ceil((double) totalElements / size);

        List<VideoViewModel> viewModels = filteredSeries.stream()
                .map(series -> {
                    VideoViewModel vm = new VideoViewModel();
                    String category = series.getCategory() != null ? series.getCategory() : "";
                    String mainCategory = category.contains(",") ? category.split(",")[0] : category;

                    vm.setId(series.getId().toString());
                    vm.setTitle(series.getName());
                    vm.setInfo(mainCategory);

                    vm.setThumbnailUrl("/media/image/" + series.getId());
                    List<EpisodeViewModel> episodes = seriesService.getEpisodesForSeries(series.getId());
                    if (!episodes.isEmpty()) {
                        EpisodeViewModel lastEp = episodes.get(episodes.size() - 1);
                        if (lastEp.getSlug() != null && !lastEp.getSlug().isEmpty()) {
                            vm.setVideoUrl("/" + lastEp.getSlug());
                        } else {
                            vm.setVideoUrl("/" + lastEp.getId());
                        }
                    } else {
                        vm.setVideoUrl("/" + series.getId());
                    }

                    return vm;
                })
                .collect(Collectors.toList());

        PageResponse<VideoViewModel> response = new PageResponse<>(viewModels, totalPages, page, totalElements);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/episodes")
    public ResponseEntity<List<EpisodeViewModel>> getEpisodes(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(seriesService.getEpisodesForSeries(id));
    }

    @GetMapping("/episode/{id}/parent")
    public ResponseEntity<Series> getParentSeries(@PathVariable("id") UUID episodeId) {
        // Find parent via FK or XML fallback
        Series series = seriesRepository.findSeriesByEpisodeId(episodeId);
        if (series == null)
            return ResponseEntity.notFound().build();
        return ResponseEntity.ok(series);
    }
}
