package com.ses.whodatidols.controller;

import com.ses.whodatidols.model.Episode;
import com.ses.whodatidols.model.Movie;
import com.ses.whodatidols.model.Series;
import com.ses.whodatidols.repository.MovieRepository;
import com.ses.whodatidols.repository.SeriesRepository;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class SitemapController {

    private final MovieRepository movieRepository;
    private final SeriesRepository seriesRepository;
    private final String BASE_URL = "https://whodatidols.com";

    public SitemapController(MovieRepository movieRepository, SeriesRepository seriesRepository) {
        this.movieRepository = movieRepository;
        this.seriesRepository = seriesRepository;
    }

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public String getSitemap() {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");

        // 1. Static Pages
        addUrl(xml, "/", "1.0", "daily");
        addUrl(xml, "/about", "0.5", "monthly");
        addUrl(xml, "/privacy_policy", "0.3", "monthly");
        addUrl(xml, "/terms_of_use", "0.3", "monthly");
        addUrl(xml, "/sss", "0.5", "weekly");
        addUrl(xml, "/programlar", "0.5", "weekly");
        addUrl(xml, "/diziler", "0.5", "weekly");
        addUrl(xml, "/bl-dizileri", "0.5", "weekly");

        // 2. Movies
        List<Movie> movies = movieRepository.findAll();
        for (Movie movie : movies) {
            String slug = movie.getSlug();
            if (slug != null && !slug.isEmpty()) {
                addUrl(xml, "/" + slug, "0.8", "weekly");
            }
        }

        // 3. Series & Episodes
        List<Series> allSeries = seriesRepository.findAllSeries();
        for (Series series : allSeries) {
            String seriesSlug = series.getSlug();
            if (seriesSlug != null && !seriesSlug.isEmpty()) {
                addUrl(xml, "/" + seriesSlug, "0.8", "weekly");
            }

            List<Episode> episodes = seriesRepository.findEpisodesBySeriesId(series.getId());
            for (Episode episode : episodes) {
                String epSlug = episode.getSlug();
                if (epSlug != null && !epSlug.isEmpty()) {
                    addUrl(xml, "/" + epSlug, "0.7", "weekly");
                }
            }
        }

        xml.append("</urlset>");
        return xml.toString();
    }

    private void addUrl(StringBuilder xml, String path, String priority, String changefreq) {
        String url = BASE_URL + path;
        // Escape XML special characters
        url = url.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");

        xml.append("  <url>\n");
        xml.append("    <loc>").append(url).append("</loc>\n");
        xml.append("    <changefreq>").append(changefreq).append("</changefreq>\n");
        xml.append("    <priority>").append(priority).append("</priority>\n");
        xml.append("  </url>\n");
    }
}
