package com.ses.whodatidols.controller;

import com.ses.whodatidols.model.CastMemberDto;
import com.ses.whodatidols.model.Series;
import com.ses.whodatidols.repository.ActorRepository;
import com.ses.whodatidols.repository.SeriesRepository;
import com.ses.whodatidols.service.CastSyncService;
import com.ses.whodatidols.service.SeriesService;
import com.ses.whodatidols.viewmodel.EpisodeViewModel;
import com.ses.whodatidols.viewmodel.PageResponse;
import com.ses.whodatidols.viewmodel.VideoViewModel;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping({ "/api/series", "/api/soapoperas" }) // Added alias for compatibility
public class SeriesController {
    private final SeriesRepository seriesRepository;
    private final SeriesService seriesService;
    private final ActorRepository actorRepository;
    private final CastSyncService castSyncService;

    public SeriesController(SeriesRepository seriesRepository, SeriesService seriesService,
                            ActorRepository actorRepository, CastSyncService castSyncService) {
        this.seriesRepository = seriesRepository;
        this.seriesService = seriesService;
        this.actorRepository = actorRepository;
        this.castSyncService = castSyncService;
    }

    @Cacheable(value = "recentSeries", key = "#type + '-' + #page + '-' + #size")
    @GetMapping("/recent")
    public ResponseEntity<PageResponse<VideoViewModel>> getRecentSeriesPaged(
            @RequestParam(value = "type", defaultValue = "Dizi") String type,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "18") int size) {

        int offset = (page - 1) * size;
        List<Series> recentSeries = seriesRepository.findRecentSeriesPaged(type, offset, size);
        int totalElements = seriesRepository.countAllSeries(type);
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
                    if (series.getSlug() != null && !series.getSlug().isBlank()) {
                        vm.setVideoUrl("/" + series.getSlug());
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
            @RequestParam(value = "type", defaultValue = "Dizi") String type,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "18") int size) {

        if (query.trim().isEmpty()) {
            return getRecentSeriesPaged(type, page, size);
        }

        int offset = (page - 1) * size;
        List<Series> recentSeries = seriesRepository.searchSeriesPaged(query, type, offset, size);
        int totalElements = seriesRepository.countSeriesBySearch(query, type);
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
                    if (series.getSlug() != null && !series.getSlug().isBlank()) {
                        vm.setVideoUrl("/" + series.getSlug());
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
                    if (series.getSlug() != null && !series.getSlug().isBlank()) {
                        vm.setVideoUrl("/" + series.getSlug());
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

    @GetMapping({ "/{idOrSlug}/details", "/details" })
    public ResponseEntity<Map<String, Object>> getSeriesDetails(
            @PathVariable(value = "idOrSlug", required = false) String idOrSlugPath,
            @RequestParam(value = "id", required = false) String idParam,
            @RequestParam(value = "slug", required = false) String slugParam) {

        String identifier = (idOrSlugPath != null && !idOrSlugPath.isBlank()) ? idOrSlugPath.trim() : null;
        if (identifier == null) {
            identifier = (slugParam != null && !slugParam.isBlank()) ? slugParam.trim() : (idParam != null ? idParam.trim() : null);
        }
        if (identifier == null || identifier.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        Series series = null;
        try {
            UUID uid = UUID.fromString(identifier);
            series = seriesRepository.findSeriesById(uid);
        } catch (IllegalArgumentException ignored) {
        }

        if (series == null) {
            series = seriesRepository.findSeriesBySlug(identifier);
        }

        if (series == null || series.isHidden()) {
            return ResponseEntity.notFound().build();
        }

        List<EpisodeViewModel> episodes = seriesService.getEpisodesForSeries(series.getId());
        List<CastMemberDto> cast = actorRepository.getCastForSeries(series.getId());
        if (cast == null || cast.isEmpty()) {
            try {
                cast = castSyncService.syncCastForSeries(series);
            } catch (Exception ignored) {
                cast = Collections.emptyList();
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("id", series.getId().toString());
        response.put("name", series.getName());
        response.put("title", series.getName());
        response.put("summary", series.getSummary());
        response.put("category", series.getCategory());
        response.put("language", series.getLanguage());
        response.put("country", series.getCountry());
        response.put("seriesType", series.getSeriesType() != null ? series.getSeriesType() : "Dizi");
        response.put("finalStatus", series.getFinalStatus());
        response.put("slug", series.getSlug());
        response.put("uploadDate", series.getUploadDate());
        response.put("totalEpisodes", episodes != null ? episodes.size() : 0);
        response.put("posterUrl", "/media/image/" + series.getId());
        response.put("backdropUrl", "/media/image/" + series.getId());
        response.put("episodes", episodes != null ? episodes : Collections.emptyList());
        response.put("cast", cast != null ? cast : Collections.emptyList());

        if (episodes != null && !episodes.isEmpty()) {
            EpisodeViewModel firstEp = episodes.get(0);
            response.put("firstEpisodeUrl", (firstEp.getSlug() != null && !firstEp.getSlug().isBlank()) ? "/" + firstEp.getSlug() : "/" + firstEp.getId());
            EpisodeViewModel lastEp = episodes.get(episodes.size() - 1);
            response.put("lastEpisodeUrl", (lastEp.getSlug() != null && !lastEp.getSlug().isBlank()) ? "/" + lastEp.getSlug() : "/" + lastEp.getId());
        }

        return ResponseEntity.ok(response);
    }
}
