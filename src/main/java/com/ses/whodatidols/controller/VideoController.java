package com.ses.whodatidols.controller;

import com.ses.whodatidols.model.CastMemberDto;
import com.ses.whodatidols.repository.ActorRepository;
import com.ses.whodatidols.service.CastSyncService;
import com.ses.whodatidols.service.TmdbService;
import com.ses.whodatidols.service.TvMazeService;
import java.util.Collections;

import com.ses.whodatidols.service.TranscodingService;
import com.ses.whodatidols.service.VideoService;
import com.ses.whodatidols.model.Episode;
import com.ses.whodatidols.model.Movie;
import com.ses.whodatidols.model.Series;
import com.ses.whodatidols.repository.MovieRepository;
import com.ses.whodatidols.repository.SeriesRepository;
import com.ses.whodatidols.repository.ContentRepository;
import com.ses.whodatidols.repository.AdRepository;
import com.ses.whodatidols.model.Ad;
import com.ses.whodatidols.repository.SystemSettingRepository;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/video")
@SuppressWarnings({"SqlResolve", "SqlNoDataSourceInspection"})
public class VideoController {
    private final VideoService videoService;
    private final TranscodingService transcodingService;
    private final MovieRepository movieRepository;
    private final SeriesRepository seriesRepository;
    private final ContentRepository contentRepository;
    private final AdRepository adRepository;
    private final ActorRepository actorRepository;
    private final CastSyncService castSyncService;
    private final TmdbService tmdbService;
    private final TvMazeService tvMazeService;
    private final SystemSettingRepository systemSettingRepository;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();

    public VideoController(VideoService videoService, TranscodingService transcodingService,
            MovieRepository movieRepository, SeriesRepository seriesRepository,
            ContentRepository contentRepository, AdRepository adRepository,
            ActorRepository actorRepository, CastSyncService castSyncService,
            TmdbService tmdbService, TvMazeService tvMazeService,
            SystemSettingRepository systemSettingRepository,
            org.springframework.jdbc.core.JdbcTemplate jdbcTemplate) {
        this.videoService = videoService;
        this.transcodingService = transcodingService;
        this.movieRepository = movieRepository;
        this.seriesRepository = seriesRepository;
        this.contentRepository = contentRepository;
        this.adRepository = adRepository;
        this.actorRepository = actorRepository;
        this.castSyncService = castSyncService;
        this.tmdbService = tmdbService;
        this.tvMazeService = tvMazeService;
        this.systemSettingRepository = systemSettingRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    private UUID resolveToUuid(String idOrSlug) {
        if (idOrSlug == null || idOrSlug.isBlank()) return null;
        try {
            return UUID.fromString(idOrSlug.trim());
        } catch (IllegalArgumentException ignored) {}

        String slug = idOrSlug.trim();
        Episode ep = seriesRepository.findEpisodeBySlug(slug);
        if (ep != null) return ep.getId();

        Movie m = movieRepository.findMovieBySlug(slug);
        if (m != null) return m.getId();

        Series s = seriesRepository.findSeriesBySlug(slug);
        if (s != null) {
            UUID firstEpId = seriesRepository.findFirstEpisodeIdBySeriesId(s.getId());
            return firstEpId != null ? firstEpId : s.getId();
        }

        try {
            List<Map<String, Object>> upcomings = jdbcTemplate.queryForList(
                    "SELECT TOP 1 ID FROM Upcoming WHERE ID = TRY_CAST(? AS UNIQUEIDENTIFIER) OR Slug = ? OR Name = ? OR LOWER(REPLACE(REPLACE(Name, ' ', '-'), '''', '')) = ?",
                    slug, slug, slug, slug);
            if (!upcomings.isEmpty()) {
                Object uidObj = upcomings.get(0).get("ID");
                if (uidObj instanceof UUID) return (UUID) uidObj;
                if (uidObj != null) return UUID.fromString(uidObj.toString());
            }
        } catch (Exception ignored) {}

        return null;
    }

    @GetMapping("/details")
    @Cacheable("videoDetails")
    public ResponseEntity<Map<String, Object>> getDetails(@RequestParam("id") String idOrSlug) {
        Map<String, Object> response = new HashMap<>();
        UUID id = resolveToUuid(idOrSlug);

        // 1. Try Episode
        if (id != null) {
            Episode episode = seriesRepository.findEpisodeById(id);
            if (episode != null && !episode.isHidden()) {
                response.put("title", episode.getName());
                response.put("duration", episode.getDurationMinutes() > 0 ? episode.getDurationMinutes() + " dk" : "");
                response.put("year", episode.getReleaseYear());
                List<CastMemberDto> cast = Collections.emptyList();
                try {
                    var series = seriesRepository.findSeriesByEpisodeId(id);
                    if (series != null) {
                        if (series.isBl()) {
                            return ResponseEntity.notFound().build();
                        }
                        String formattedTitle = series.getName() + " - " + episode.getSeasonNumber() + ". Sezon "
                                + episode.getEpisodeNumber() + ". Bölüm";
                        response.put("title", formattedTitle);

                        response.put("plot", series.getSummary());
                        response.put("genres",
                                series.getCategory() != null ? List.of(series.getCategory().split(",")) : List.of());
                        response.put("language", series.getLanguage());
                        response.put("country", series.getCountry());
                        response.put("finalStatus", series.getFinalStatus());
                        response.put("adult", episode.isAdult());

                        cast = actorRepository.getCastForSeries(series.getId());
                        if (cast.isEmpty()) {
                            cast = castSyncService.syncCastForSeries(series);
                        }
                        response.put("seriesSlug", series.getSlug());
                        response.put("seriesName", series.getName());
                    } else {
                        response.put("title", episode.getName());
                        response.put("adult", episode.isAdult());
                    }
                } catch (Exception e) {
                    // Ignore if parent not found
                }

                response.put("cast", cast);
                response.put("season", episode.getSeasonNumber());
                response.put("episode", episode.getEpisodeNumber());
                response.put("type", "episode");
                response.put("seriesId", episode.getSeriesId());
                response.put("slug", episode.getSlug());
                return ResponseEntity.ok(response);
            }

            // 2. Try Movie
            Movie movie = movieRepository.findMovieById(id);
            if (movie != null && !movie.isHidden()) {
                if (movie.isBl()) {
                    return ResponseEntity.notFound().build();
                }
                response.put("title", movie.getName());
                response.put("duration", movie.getDurationMinutes() > 0 ? movie.getDurationMinutes() + " dk" : "");
                response.put("year", movie.getReleaseYear());
                response.put("plot", movie.getSummary());
                response.put("genres",
                        movie.getCategory() != null ? List.of(movie.getCategory().split(",")) : List.of());
                response.put("language", movie.getLanguage());
                response.put("country", movie.getCountry());
                response.put("type", "movie");
                response.put("slug", movie.getSlug());
                response.put("adult", movie.isAdult());

                List<CastMemberDto> movieCast = actorRepository.getCastForMovie(movie.getId());
                if (movieCast.isEmpty()) {
                    movieCast = castSyncService.syncCastForMovie(movie);
                }
                response.put("cast", movieCast);
                return ResponseEntity.ok(response);
            }

            // 3. Try Series
            Series series = seriesRepository.findSeriesById(id);
            if (series != null && !series.isHidden()) {
                if (series.isBl()) {
                    return ResponseEntity.notFound().build();
                }
                response.put("title", series.getName());
                response.put("plot", series.getSummary());
                response.put("genres",
                        series.getCategory() != null ? List.of(series.getCategory().split(",")) : List.of());
                response.put("language", series.getLanguage());
                response.put("country", series.getCountry());
                response.put("finalStatus", series.getFinalStatus());
                response.put("type", "series");
                response.put("seriesId", series.getId().toString());
                response.put("seriesSlug", series.getSlug());
                response.put("seriesName", series.getName());
                response.put("slug", series.getSlug());
                List<CastMemberDto> seriesCast = actorRepository.getCastForSeries(series.getId());
                if (seriesCast.isEmpty()) {
                    seriesCast = castSyncService.syncCastForSeries(series);
                }
                response.put("cast", seriesCast);
                return ResponseEntity.ok(response);
            }
        }

        // 3. Try Upcoming
        try {
            List<Map<String, Object>> upcomings = jdbcTemplate.queryForList(
                    "SELECT TOP 1 * FROM Upcoming WHERE ID = TRY_CAST(? AS UNIQUEIDENTIFIER) OR Slug = ? OR Name = ? OR LOWER(REPLACE(REPLACE(Name, ' ', '-'), '''', '')) = ?",
                    idOrSlug, idOrSlug, idOrSlug, idOrSlug);

            if (!upcomings.isEmpty()) {
                Map<String, Object> u = upcomings.get(0);
                boolean isBl = Boolean.TRUE.equals(u.get("isBl"));
                if (isBl) {
                    return ResponseEntity.notFound().build();
                }

                String title = u.containsKey("Name") && u.get("Name") != null ? (String) u.get("Name") : (String) u.get("name");
                String upcomingType = u.containsKey("Type") && u.get("Type") != null ? (String) u.get("Type") : (String) u.get("type");
                boolean isMovie = upcomingType != null && upcomingType.equalsIgnoreCase("Movie");
                Object refIdObj = u.get("ReferenceId");
                UUID refId = refIdObj != null ? UUID.fromString(refIdObj.toString()) : null;

                Object upcomingIdObj = u.get("ID");
                String upcomingIdStr = upcomingIdObj != null ? upcomingIdObj.toString() : "";

                response.put("id", upcomingIdStr);
                response.put("title", title);
                response.put("name", title);
                response.put("type", "upcoming");
                response.put("isUpcoming", true);
                response.put("category", upcomingType);

                Object dtObj = u.containsKey("Datetime") && u.get("Datetime") != null ? u.get("Datetime") : u.get("datetime");
                response.put("datetime", dtObj != null ? dtObj.toString() : "");

                Object stObj = u.containsKey("Status") && u.get("Status") != null ? u.get("Status") : u.get("status");
                response.put("status", stObj);
                response.put("poster", "/media/image/" + upcomingIdStr);
                response.put("adult", false);

                String plot = (String) u.get("Summary");
                Object year = u.get("ReleaseYear");
                String country = (String) u.get("Country");
                String language = (String) u.get("Language");
                String duration = (String) u.get("Duration");
                String slug = (String) u.get("Slug");
                List<String> genres = new ArrayList<>();
                List<CastMemberDto> cast = Collections.emptyList();

                // If CastData column exists in Upcoming table, try parsing it
                if (u.containsKey("CastData") && u.get("CastData") != null) {
                    try {
                        String cdStr = u.get("CastData").toString();
                        if (!cdStr.isBlank()) {
                            cast = objectMapper.readValue(cdStr, new com.fasterxml.jackson.core.type.TypeReference<List<CastMemberDto>>() {});
                        }
                    } catch (Exception ignored) {}
                }

                Series linkedSeries = null;
                Movie linkedMovie = null;

                if (refId != null) {
                    linkedSeries = seriesRepository.findSeriesById(refId);
                    if (linkedSeries == null) {
                        linkedMovie = movieRepository.findMovieById(refId);
                    }
                    if (linkedSeries == null && linkedMovie == null) {
                        Episode ep = seriesRepository.findEpisodeById(refId);
                        if (ep != null) {
                            linkedSeries = seriesRepository.findSeriesByEpisodeId(ep.getId());
                        }
                    }
                }

                if (linkedSeries == null && linkedMovie == null && title != null) {
                    linkedSeries = seriesRepository.findSeriesByName(title);
                    if (linkedSeries == null) {
                        linkedMovie = movieRepository.findMovieByName(title);
                    }
                }

                if (linkedSeries != null) {
                    if (plot == null || plot.isBlank()) plot = linkedSeries.getSummary();
                    if (year == null && linkedSeries.getUploadDate() != null) {
                        try {
                            year = linkedSeries.getUploadDate().atZone(java.time.ZoneId.systemDefault()).getYear();
                        } catch (Exception ignored) {}
                    }
                    if (country == null || country.isBlank()) country = linkedSeries.getCountry();
                    if (language == null || language.isBlank()) language = linkedSeries.getLanguage();
                    if (slug == null || slug.isBlank()) slug = linkedSeries.getSlug();
                    if (linkedSeries.getCategory() != null && !linkedSeries.getCategory().isBlank()) {
                        genres = List.of(linkedSeries.getCategory().split(","));
                    }
                    if (cast == null || cast.isEmpty()) {
                        cast = actorRepository.getCastForSeries(linkedSeries.getId());
                        if (cast.isEmpty()) {
                            cast = castSyncService.syncCastForSeries(linkedSeries);
                        }
                    }
                } else if (linkedMovie != null) {
                    if (plot == null || plot.isBlank()) plot = linkedMovie.getSummary();
                    if (year == null) year = linkedMovie.getReleaseYear();
                    if (country == null || country.isBlank()) country = linkedMovie.getCountry();
                    if (language == null || language.isBlank()) language = linkedMovie.getLanguage();
                    if (duration == null || duration.isBlank()) {
                        duration = linkedMovie.getDurationMinutes() > 0 ? linkedMovie.getDurationMinutes() + " dk" : "";
                    }
                    if (slug == null || slug.isBlank()) slug = linkedMovie.getSlug();
                    if (linkedMovie.getCategory() != null && !linkedMovie.getCategory().isBlank()) {
                        genres = List.of(linkedMovie.getCategory().split(","));
                    }
                    if (cast == null || cast.isEmpty()) {
                        cast = actorRepository.getCastForMovie(linkedMovie.getId());
                        if (cast.isEmpty()) {
                            cast = castSyncService.syncCastForMovie(linkedMovie);
                        }
                    }
                }

                // If plot or cast is still missing/placeholder, do on-the-fly fetch from TMDB or TVMaze!
                if ((plot == null || plot.isBlank() || plot.contains("Bu yapım çok yakında") || cast == null || cast.isEmpty()) && title != null && !title.isBlank()) {
                    try {
                        String searchType = isMovie ? "movie" : "tv";
                        List<Map<String, Object>> tmdbResults = tmdbService.search(title, searchType);
                        if (tmdbResults != null && !tmdbResults.isEmpty()) {
                            Map<String, Object> first = tmdbResults.get(0);
                            if (plot == null || plot.isBlank() || plot.contains("Bu yapım çok yakında")) {
                                String overview = (String) first.get("overview");
                                if (overview != null && !overview.isBlank()) {
                                    plot = overview;
                                }
                            }
                            if (year == null) {
                                String d = (String) first.get(isMovie ? "release_date" : "first_air_date");
                                if (d != null && d.length() >= 4) {
                                    year = d.substring(0, 4);
                                }
                            }
                            if (country == null || country.isBlank()) {
                                @SuppressWarnings("unchecked")
                                List<String> oc = (List<String>) first.get("origin_country");
                                if (oc != null && !oc.isEmpty()) {
                                    country = oc.get(0);
                                }
                            }
                            if (language == null || language.isBlank()) {
                                language = (String) first.get("original_language");
                            }
                            if (cast == null || cast.isEmpty()) {
                                Object tmdbIdObj = first.get("id");
                                if (tmdbIdObj instanceof Number) {
                                    int tmdbId = ((Number) tmdbIdObj).intValue();
                                    List<CastMemberDto> fetchedCast = tmdbService.getCast(tmdbId, searchType);
                                    if (fetchedCast != null && !fetchedCast.isEmpty()) {
                                        cast = fetchedCast;
                                    }
                                }
                            }
                        }

                        // Try TVMaze fallback if still missing
                        if ((plot == null || plot.isBlank() || plot.contains("Bu yapım çok yakında") || cast == null || cast.isEmpty()) && !isMovie) {
                            List<Map<String, Object>> tvmazeResults = tvMazeService.searchSeries(title);
                            if (tvmazeResults != null && !tvmazeResults.isEmpty()) {
                                Map<String, Object> first = tvmazeResults.get(0);
                                @SuppressWarnings("unchecked")
                                Map<String, Object> show = first.containsKey("show") ? (Map<String, Object>) first.get("show") : first;
                                if (plot == null || plot.isBlank() || plot.contains("Bu yapım çok yakında")) {
                                    String sSummary = (String) show.get("summary");
                                    if (sSummary != null && !sSummary.isBlank()) {
                                        plot = sSummary.replaceAll("<[^>]*>", "").trim();
                                    }
                                }
                                if (cast == null || cast.isEmpty()) {
                                    Object tvmazeIdObj = show.get("id");
                                    if (tvmazeIdObj instanceof Number) {
                                        int tvmazeId = ((Number) tvmazeIdObj).intValue();
                                        List<CastMemberDto> fetchedCast = tvMazeService.getShowCast(tvmazeId);
                                        if (fetchedCast != null && !fetchedCast.isEmpty()) {
                                            cast = fetchedCast;
                                        }
                                    }
                                }
                            }
                        }

                        // Background persist so future loads are instant
                        if (plot != null || (cast != null && !cast.isEmpty())) {
                            final String pToSave = (plot != null && !plot.contains("Bu yapım çok yakında")) ? plot : null;
                            final List<CastMemberDto> cToSave = cast;
                            final String idToSave = upcomingIdStr;
                            java.util.concurrent.CompletableFuture.runAsync(() -> {
                                try {
                                    String cJson = (cToSave != null && !cToSave.isEmpty()) ?
                                            objectMapper.writeValueAsString(cToSave) : null;
                                    if (pToSave != null && cJson != null) {
                                        jdbcTemplate.update("UPDATE Upcoming SET Summary = COALESCE(Summary, ?), CastData = COALESCE(CastData, ?) WHERE ID = ?",
                                                pToSave, cJson, UUID.fromString(idToSave));
                                    } else if (pToSave != null) {
                                        jdbcTemplate.update("UPDATE Upcoming SET Summary = COALESCE(Summary, ?) WHERE ID = ?",
                                                pToSave, UUID.fromString(idToSave));
                                    } else if (cJson != null) {
                                        jdbcTemplate.update("UPDATE Upcoming SET CastData = COALESCE(CastData, ?) WHERE ID = ?",
                                                cJson, UUID.fromString(idToSave));
                                    }
                                } catch (Exception ignored) {}
                            });
                        }
                    } catch (Exception e) {
                        System.err.println("Upcoming metadata on-the-fly fetch error: " + e.getMessage());
                    }
                }

                if (plot == null || plot.isBlank()) {
                    plot = "Bu yapım çok yakında Who Dat Idols platformunda yayınlanacaktır. Yayın tarihini ve geri sayımı takip edebilirsiniz.";
                }
                if (genres.isEmpty()) {
                    List<String> list = new ArrayList<>();
                    list.add(isMovie ? "Film" : "Dizi");
                    String g = (String) u.get("Genre");
                    if (g != null && !g.isBlank() && !g.equalsIgnoreCase("Normal")) {
                        list.add(g);
                    }
                    String cats = (String) u.get("Categories");
                    if (cats != null && !cats.isBlank()) {
                        for (String c : cats.split(",")) {
                            String trimmed = c.trim();
                            if (!trimmed.isEmpty() && !list.contains(trimmed)) {
                                list.add(trimmed);
                            }
                        }
                    }
                    genres = list;
                }
                if (year == null && dtObj != null) {
                    try {
                        java.sql.Timestamp ts = (java.sql.Timestamp) dtObj;
                        year = ts.toInstant().atZone(java.time.ZoneId.systemDefault()).getYear();
                    } catch (Exception ignored) {}
                }

                response.put("plot", plot);
                response.put("year", year != null ? year.toString() : "");
                response.put("country", country != null ? country : "");
                response.put("language", language != null ? language : "");
                response.put("duration", duration != null ? duration : "");
                response.put("slug", slug != null ? slug : upcomingIdStr);
                if (cast != null && !cast.isEmpty()) {
                    for (CastMemberDto member : cast) {
                        if (member.getActorId() == null && member.getName() != null && !member.getName().isBlank()) {
                            try {
                                UUID aId = actorRepository.findOrCreateActor(
                                        member.getName(),
                                        member.getPhotoUrl(),
                                        member.getTmdbId(),
                                        member.getTvmazeId());
                                member.setActorId(aId);
                            } catch (Exception ignored) {}
                        }
                    }
                }

                response.put("genres", genres);
                response.put("cast", cast != null ? cast : Collections.emptyList());

                return ResponseEntity.ok(response);
            }
        } catch (Exception ignored) {
        }

        return ResponseEntity.notFound().build();
    }

    @GetMapping("/resolve-slug")
    @Cacheable("resolvedSlugs")
    public ResponseEntity<Map<String, String>> resolveSlug(@RequestParam("slug") String slug) {
        boolean isUuid = false;
        try {
            UUID.fromString(slug);
            isUuid = true;
        } catch (IllegalArgumentException e) {
            // Not a UUID
        }

        // If it's a UUID, do ID-based lookup
        if (isUuid) {
            UUID id = UUID.fromString(slug);

            // Check episode by ID
            Episode ep = seriesRepository.findEpisodeById(id);
            if (ep != null && !ep.isHidden()) {
                return ResponseEntity.ok(Map.of("id", ep.getId().toString(), "type", "episode"));
            }

            // Check series by ID → return first episode
            Series s = seriesRepository.findSeriesById(id);
            if (s != null && !s.isHidden()) {
                UUID firstEpId = seriesRepository.findFirstEpisodeIdBySeriesId(s.getId());
                if (firstEpId != null) {
                    Episode firstEp = seriesRepository.findEpisodeById(firstEpId);
                    if (firstEp != null && !firstEp.isHidden()) {
                        return ResponseEntity.ok(Map.of("id", firstEpId.toString(), "type", "episode"));
                    }
                }
            }

            // Check movie by ID
            Movie m = movieRepository.findMovieById(id);
            if (m != null && !m.isHidden()) {
                return ResponseEntity.ok(Map.of("id", m.getId().toString(), "type", "movie"));
            }

            // Check Upcoming by ID
            try {
                List<Map<String, Object>> upcomings = jdbcTemplate.queryForList(
                        "SELECT TOP 1 ID, isBl FROM Upcoming WHERE ID = TRY_CAST(? AS UNIQUEIDENTIFIER)", slug);
                if (!upcomings.isEmpty()) {
                    boolean isBl = Boolean.TRUE.equals(upcomings.get(0).get("isBl"));
                    if (!isBl) {
                        return ResponseEntity.ok(Map.of("id", upcomings.get(0).get("ID").toString(), "type", "upcoming"));
                    }
                }
            } catch (Exception ignored) {
            }

            return ResponseEntity.notFound().build();
        }

        // --- Slug-based lookup (exact match, no iteration) ---

        // 1. Check if it's an episode slug (most specific — includes season/episode info)
        Episode episode = seriesRepository.findEpisodeBySlug(slug);
        if (episode != null) {
            Series parentSeries = seriesRepository.findSeriesByEpisodeId(episode.getId());
            if (parentSeries != null && parentSeries.isBl()) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(Map.of("id", episode.getId().toString(), "type", "episode"));
        }

        // 2. Check if it's a movie slug (movies before series to avoid slug collision)
        Movie movie = movieRepository.findMovieBySlug(slug);
        if (movie != null) {
            if (movie.isBl()) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(Map.of("id", movie.getId().toString(), "type", "movie"));
        }

        // 3. Check if it's a series slug → return first episode
        Series series = seriesRepository.findSeriesBySlug(slug);
        if (series != null) {
            if (series.isBl()) {
                return ResponseEntity.notFound().build();
            }
            UUID firstEpId = seriesRepository.findFirstEpisodeIdBySeriesId(series.getId());
            if (firstEpId != null) {
                Map<String, String> res = new HashMap<>();
                res.put("id", firstEpId.toString());
                res.put("type", "episode");
                res.put("seriesSlug", series.getSlug());
                res.put("seriesName", series.getName());
                return ResponseEntity.ok(res);
            }
        }

        // 4. Check Upcoming by slug or name
        try {
            List<Map<String, Object>> upcomings = jdbcTemplate.queryForList(
                    "SELECT TOP 1 ID, isBl FROM Upcoming WHERE Slug = ? OR Name = ? OR LOWER(REPLACE(REPLACE(Name, ' ', '-'), '''', '')) = ?",
                    slug, slug, slug);
            if (!upcomings.isEmpty()) {
                boolean isBl = Boolean.TRUE.equals(upcomings.get(0).get("isBl"));
                if (!isBl) {
                    return ResponseEntity.ok(Map.of("id", upcomings.get(0).get("ID").toString(), "type", "upcoming"));
                }
            }
        } catch (Exception ignored) {
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

    @CacheEvict(value = { "weeklyBestMovies", "weeklyBestSeries" }, allEntries = true)
    @PostMapping("/increment-view")
    public ResponseEntity<?> incrementViewCount(@RequestParam("id") String idOrSlug) {
        try {
            UUID id = resolveToUuid(idOrSlug);
            if (id == null) {
                return ResponseEntity.ok().build();
            }
            videoService.incrementViewCount(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.ok().build();
        }
    }

    @GetMapping("/similar")
    @Cacheable("similarContent")
    public ResponseEntity<List<Map<String, Object>>> getSimilarContent(@RequestParam("id") String idOrSlug) {
        UUID id = resolveToUuid(idOrSlug);
        UUID contentIdForSearch = id;

        if (id != null) {
            Episode episode = seriesRepository.findEpisodeById(id);
            if (episode != null && episode.getSeriesId() != null) {
                contentIdForSearch = episode.getSeriesId();
            }
        }

        // Check if Upcoming
        try {
            List<Map<String, Object>> upcomings = jdbcTemplate.queryForList(
                    "SELECT TOP 1 * FROM Upcoming WHERE ID = TRY_CAST(? AS UNIQUEIDENTIFIER) OR Slug = ? OR Name = ? OR LOWER(REPLACE(REPLACE(Name, ' ', '-'), '''', '')) = ?",
                    idOrSlug, idOrSlug, idOrSlug, idOrSlug);
            if (!upcomings.isEmpty()) {
                Map<String, Object> u = upcomings.get(0);
                Object refIdObj = u.get("ReferenceId");
                if (refIdObj != null) {
                    contentIdForSearch = UUID.fromString(refIdObj.toString());
                } else {
                    String name = u.containsKey("Name") && u.get("Name") != null ? (String) u.get("Name") : (String) u.get("name");
                    Series s = seriesRepository.findSeriesByName(name);
                    if (s != null) {
                        contentIdForSearch = s.getId();
                    } else {
                        Movie m = movieRepository.findMovieByName(name);
                        if (m != null) {
                            contentIdForSearch = m.getId();
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }

        if (contentIdForSearch == null) {
            return ResponseEntity.ok(contentRepository.findTopPopularContent(12));
        }

        List<Map<String, Object>> similar = contentRepository.findSimilarContent(contentIdForSearch, 12);
        return ResponseEntity.ok(similar);
    }

    @GetMapping({"/ad/random", "/commercial/random"})
    public ResponseEntity<Map<String, Object>> getRandomAd() {
        if (!systemSettingRepository.isAdVideoEnabled()) {
            return ResponseEntity.noContent().build();
        }
        int probability = systemSettingRepository.getAdProbability();
        if (probability <= 0) {
            return ResponseEntity.noContent().build();
        }
        int roll = new java.util.Random().nextInt(100);
        if (roll >= probability) {
            return ResponseEntity.noContent().build();
        }

        List<com.ses.whodatidols.model.Ad> ads = adRepository.findVisible();
        if (ads.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        int randomIndex = new java.util.Random().nextInt(ads.size());
        com.ses.whodatidols.model.Ad selectedAd = ads.get(randomIndex);
        Map<String, Object> response = new HashMap<>();
        response.put("id", selectedAd.getId());
        response.put("name", selectedAd.getName());
        response.put("linkButtons", selectedAd.getLinkButtons());
        return ResponseEntity.ok(response);
    }

    @GetMapping({"/ad/probability", "/commercial/probability"})
    public ResponseEntity<Map<String, Object>> getAdProbabilityPublic() {
        return ResponseEntity.ok(Map.of("probability", systemSettingRepository.getAdProbability()));
    }
}
