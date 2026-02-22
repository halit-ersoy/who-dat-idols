package com.ses.whodatidols.service;

import com.ses.whodatidols.model.Episode;
import com.ses.whodatidols.model.Movie;
import com.ses.whodatidols.model.Series;
import com.ses.whodatidols.repository.MovieRepository;
import com.ses.whodatidols.repository.SeriesRepository;
import com.ses.whodatidols.util.SlugUtil;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class SlugMigrationService {
    private static final Logger log = LoggerFactory.getLogger(SlugMigrationService.class);

    private final SeriesRepository seriesRepository;
    private final MovieRepository movieRepository;

    public SlugMigrationService(SeriesRepository seriesRepository, MovieRepository movieRepository) {
        this.seriesRepository = seriesRepository;
        this.movieRepository = movieRepository;
    }

    @PostConstruct
    public void migrateSlugs() {
        log.info("Starting slug migration check...");
        Set<String> existingSlugs = new HashSet<>();

        // 1. Migrate Movies
        List<Movie> allMovies = movieRepository.findAll();
        for (Movie m : allMovies) {
            if (m.getSlug() != null && !m.getSlug().isEmpty()) {
                existingSlugs.add(m.getSlug());
            }
        }

        int migratedMovies = 0;
        for (Movie m : allMovies) {
            if (m.getSlug() == null || m.getSlug().isEmpty()) {
                String baseSlug = SlugUtil.toSlug(m.getName());
                String slug = baseSlug;
                int counter = 1;
                while (existingSlugs.contains(slug)) {
                    slug = baseSlug + "-" + counter++;
                }
                m.setSlug(slug);
                movieRepository.update(m);
                existingSlugs.add(slug);
                migratedMovies++;
            }
        }

        // 2. Migrate Episodes
        // Since Episodes belong to Series, we need the series name for a good slug
        // Format: "SeriesName-SeasonX-EpisodeY"
        List<Series> allSeries = seriesRepository.findAllSeries();
        int migratedEpisodes = 0;

        for (Series s : allSeries) {
            List<Episode> episodes = seriesRepository.findEpisodesBySeriesId(s.getId());
            for (Episode e : episodes) {
                if (e.getSlug() == null || e.getSlug().isEmpty()) {
                    String baseName = s.getName() + " " + e.getSeasonNumber() + " Sezon " + e.getEpisodeNumber()
                            + " Bolum";
                    String baseSlug = SlugUtil.toSlug(baseName);
                    String slug = baseSlug;
                    int counter = 1;
                    while (existingSlugs.contains(slug)) {
                        slug = baseSlug + "-" + counter++;
                    }
                    e.setSlug(slug);
                    seriesRepository.updateEpisode(e);
                    existingSlugs.add(slug);
                    migratedEpisodes++;
                } else {
                    existingSlugs.add(e.getSlug());
                }
            }
        }

        log.info("Slug migration complete. Migrated {} movies and {} episodes.", migratedMovies, migratedEpisodes);
    }
}
