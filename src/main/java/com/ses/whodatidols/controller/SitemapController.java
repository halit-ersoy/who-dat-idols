package com.ses.whodatidols.controller;

import com.ses.whodatidols.model.Episode;
import com.ses.whodatidols.model.Movie;
import com.ses.whodatidols.model.Series;
import com.ses.whodatidols.repository.MovieRepository;
import com.ses.whodatidols.repository.SeriesRepository;
import com.ses.whodatidols.util.SlugUtil;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
public class SitemapController {

    private final MovieRepository movieRepository;
    private final SeriesRepository seriesRepository;
    private final JdbcTemplate jdbcTemplate;

    private static final String BASE_URL = "https://whodatidols.com";

    private static final Set<String> BLACKLISTED_SLUGS = Set.of(
            "delete", "edit", "admin", "login", "register", "panel", "api", "logout", "error", "404", "watch",
            "messages", "settings", "profile", "favorites", "history", "login-admin", "banned"
    );

    public SitemapController(MovieRepository movieRepository, SeriesRepository seriesRepository, JdbcTemplate jdbcTemplate) {
        this.movieRepository = movieRepository;
        this.seriesRepository = seriesRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Main Sitemap Index (Dizin Dosyası)
     * Google and search engines read this to discover all segmented sub-sitemaps.
     * lastmod is intentionally omitted because the database does not track update timestamps.
     */
    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    @Cacheable(value = "sitemaps", key = "'index'")
    public String getSitemapIndex() {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<sitemapindex xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");

        addIndexEntry(xml, "/sitemap-main.xml");
        addIndexEntry(xml, "/sitemap-movies.xml");
        addIndexEntry(xml, "/sitemap-series.xml");
        addIndexEntry(xml, "/sitemap-episodes.xml");
        addIndexEntry(xml, "/sitemap-upcoming.xml");

        xml.append("</sitemapindex>");
        return xml.toString();
    }

    /**
     * Static & Navigation Pages
     * lastmod is omitted because static routes have no reliable modification date.
     */
    @GetMapping(value = "/sitemap-main.xml", produces = MediaType.APPLICATION_XML_VALUE)
    @Cacheable(value = "sitemaps", key = "'main'")
    public String getMainSitemap() {
        StringBuilder xml = startUrlSet(false);
        Set<String> added = new HashSet<>();

        addUrl(xml, added, "/", "1.0", "daily", null, null, null, null);
        addUrl(xml, added, "/filmler", "0.9", "daily", null, null, null, null);
        addUrl(xml, added, "/diziler", "0.9", "daily", null, null, null, null);
        addUrl(xml, added, "/programlar", "0.6", "weekly", null, null, null, null);
        addUrl(xml, added, "/coming-soon", "0.7", "weekly", null, null, null, null);
        addUrl(xml, added, "/about", "0.5", "monthly", null, null, null, null);
        addUrl(xml, added, "/sss", "0.5", "monthly", null, null, null, null);
        addUrl(xml, added, "/terms_of_use", "0.3", "monthly", null, null, null, null);
        addUrl(xml, added, "/privacy_policy", "0.3", "monthly", null, null, null, null);

        xml.append("</urlset>");
        return xml.toString();
    }

    /**
     * Movies Sitemap (with Image and Video extensions)
     * publication_date in video:video reflects the initial release/upload timestamp.
     * url lastmod is omitted to prevent misleading timestamps.
     */
    @GetMapping(value = "/sitemap-movies.xml", produces = MediaType.APPLICATION_XML_VALUE)
    @Cacheable(value = "sitemaps", key = "'movies'")
    public String getMoviesSitemap() {
        StringBuilder xml = startUrlSet(true);
        Set<String> added = new HashSet<>();

        List<Movie> movies = movieRepository.findAll();
        for (Movie movie : movies) {
            if (movie.isHidden()) {
                continue;
            }
            if (movie.isBl() || "BL".equalsIgnoreCase(movie.getMovieType()) || "GL".equalsIgnoreCase(movie.getMovieType())) {
                continue;
            }
            String slug = movie.getSlug();
            if (slug != null && !slug.isBlank() && !BLACKLISTED_SLUGS.contains(slug.toLowerCase(Locale.ROOT))) {
                String imgUrl = BASE_URL + "/media/image/" + movie.getId();
                String movieTitle = movie.getName();
                String movieSummary = (movie.getSummary() != null && !movie.getSummary().isBlank())
                        ? movie.getSummary()
                        : movieTitle + " Türkçe Altyazılı Full HD İzle";
                addUrl(xml, added, "/" + slug, "0.8", "weekly", imgUrl, movieTitle, movieTitle, movieSummary, movie.getUploadDate());
            }
        }

        xml.append("</urlset>");
        return xml.toString();
    }

    /**
     * Series Sitemap (Main Series landing pages with Image extension)
     * NOTE: Series pages represent collections, not single video players.
     * Therefore, video:video extension is omitted here per Google Video Guidelines.
     */
    @GetMapping(value = "/sitemap-series.xml", produces = MediaType.APPLICATION_XML_VALUE)
    @Cacheable(value = "sitemaps", key = "'series'")
    public String getSeriesSitemap() {
        StringBuilder xml = startUrlSet(false);
        Set<String> added = new HashSet<>();

        List<Series> allSeries = seriesRepository.findAllSeries();
        for (Series series : allSeries) {
            if (series.isHidden()) {
                continue;
            }
            if (series.isBl() || "BL".equalsIgnoreCase(series.getSeriesType()) || "GL".equalsIgnoreCase(series.getSeriesType())) {
                continue;
            }
            String slug = series.getSlug();
            if (slug != null && !slug.isBlank() && !BLACKLISTED_SLUGS.contains(slug.toLowerCase(Locale.ROOT))) {
                String imgUrl = BASE_URL + "/media/image/" + series.getId();
                addUrl(xml, added, "/" + slug, "0.8", "weekly", imgUrl, series.getName(), null, null, null);
            }
        }

        xml.append("</urlset>");
        return xml.toString();
    }

    /**
     * Episodes Sitemap (Individual playable episodes with Image and Video extensions)
     */
    @GetMapping(value = "/sitemap-episodes.xml", produces = MediaType.APPLICATION_XML_VALUE)
    @Cacheable(value = "sitemaps", key = "'episodes'")
    public String getEpisodesSitemap() {
        StringBuilder xml = startUrlSet(true);
        Set<String> added = new HashSet<>();

        List<Series> allSeries = seriesRepository.findAllSeries();
        for (Series series : allSeries) {
            if (series.isHidden()) {
                continue;
            }
            if (series.isBl() || "BL".equalsIgnoreCase(series.getSeriesType()) || "GL".equalsIgnoreCase(series.getSeriesType())) {
                continue;
            }
            List<Episode> episodes = seriesRepository.findEpisodesBySeriesId(series.getId());
            for (Episode episode : episodes) {
                if (episode.isHidden()) {
                    continue;
                }
                String slug = episode.getSlug();
                if (slug != null && !slug.isBlank() && !BLACKLISTED_SLUGS.contains(slug.toLowerCase(Locale.ROOT))) {
                    String imgUrl = BASE_URL + "/media/image/" + series.getId();
                    String epTitle = series.getName() + " " + episode.getSeasonNumber() + ". Sezon " + episode.getEpisodeNumber() + ". Bölüm";
                    String epDesc = epTitle + " Türkçe Altyazılı Full HD İzle - Who Dat Idols?";
                    addUrl(xml, added, "/" + slug, "0.7", "weekly", imgUrl, epTitle, epTitle, epDesc, episode.getUploadDate());
                }
            }
        }

        xml.append("</urlset>");
        return xml.toString();
    }

    /**
     * Upcoming Shows / Movies Sitemap
     */
    @GetMapping(value = "/sitemap-upcoming.xml", produces = MediaType.APPLICATION_XML_VALUE)
    @Cacheable(value = "sitemaps", key = "'upcoming'")
    public String getUpcomingSitemap() {
        StringBuilder xml = startUrlSet(false);
        Set<String> added = new HashSet<>();

        try {
            String sql = """
                    SELECT u.ID as upcomingId, u.Name, COALESCE(u.Slug, s.slug, m.slug) as slug
                    FROM Upcoming u
                    LEFT JOIN Series s ON u.ReferenceId = s.ID
                    LEFT JOIN Movie m ON u.ReferenceId = m.ID
                    WHERE (u.isBl = 0 OR u.isBl IS NULL)
                      AND (u.Genre IS NULL OR u.Genre NOT IN ('BL', 'GL'))
                    ORDER BY u.[datetime] ASC
                    """;
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
            for (Map<String, Object> row : rows) {
                String name = (String) row.get("Name");
                String slug = (String) row.get("slug");
                if (slug == null || slug.isBlank()) {
                    slug = SlugUtil.toSlug(name);
                }
                if (slug != null && !slug.isBlank() && !BLACKLISTED_SLUGS.contains(slug.toLowerCase(Locale.ROOT))) {
                    String imgUrl = BASE_URL + "/media/image/" + row.get("upcomingId");
                    addUrl(xml, added, "/" + slug, "0.8", "daily", imgUrl, name, null, null, null);
                }
            }
        } catch (Exception e) {
            System.err.println("Upcoming sitemap error: " + e.getMessage());
        }

        xml.append("</urlset>");
        return xml.toString();
    }

    private StringBuilder startUrlSet(boolean includeVideo) {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\"\n");
        xml.append("        xmlns:image=\"http://www.google.com/schemas/sitemap-image/1.1\"");
        if (includeVideo) {
            xml.append("\n        xmlns:video=\"http://www.google.com/schemas/sitemap-video/1.1\"");
        }
        xml.append(">\n");
        return xml;
    }

    private void addIndexEntry(StringBuilder xml, String path) {
        xml.append("  <sitemap>\n");
        xml.append("    <loc>").append(BASE_URL).append(path).append("</loc>\n");
        xml.append("  </sitemap>\n");
    }

    private void addUrl(StringBuilder xml, Set<String> addedUrls, String path, String priority,
                        String changefreq, String imageUrl, String imageTitle,
                        String videoTitle, String videoDesc) {
        addUrl(xml, addedUrls, path, priority, changefreq, imageUrl, imageTitle, videoTitle, videoDesc, null);
    }

    private void addUrl(StringBuilder xml, Set<String> addedUrls, String path, String priority,
                        String changefreq, String imageUrl, String imageTitle,
                        String videoTitle, String videoDesc, Instant videoPublicationDate) {
        if (addedUrls.contains(path)) return;
        addedUrls.add(path);

        String fullUrl = escapeXml(BASE_URL + path);

        xml.append("  <url>\n");
        xml.append("    <loc>").append(fullUrl).append("</loc>\n");
        // Omit <lastmod> since database does not record reliable update timestamps
        xml.append("    <changefreq>").append(changefreq).append("</changefreq>\n");
        xml.append("    <priority>").append(priority).append("</priority>\n");

        if (imageUrl != null && !imageUrl.isBlank()) {
            xml.append("    <image:image>\n");
            xml.append("      <image:loc>").append(escapeXml(imageUrl)).append("</image:loc>\n");
            if (imageTitle != null && !imageTitle.isBlank()) {
                xml.append("      <image:title>").append(escapeXml(imageTitle)).append("</image:title>\n");
            }
            xml.append("    </image:image>\n");
        }

        if (videoTitle != null && !videoTitle.isBlank()) {
            xml.append("    <video:video>\n");
            if (imageUrl != null && !imageUrl.isBlank()) {
                xml.append("      <video:thumbnail_loc>").append(escapeXml(imageUrl)).append("</video:thumbnail_loc>\n");
            }
            xml.append("      <video:title>").append(escapeXml(videoTitle)).append("</video:title>\n");
            String desc = (videoDesc != null && !videoDesc.isBlank()) ? videoDesc : videoTitle + " izle - Who Dat Idols?";
            xml.append("      <video:description>").append(escapeXml(desc)).append("</video:description>\n");
            xml.append("      <video:player_loc>").append(fullUrl).append("</video:player_loc>\n");
            if (videoPublicationDate != null) {
                xml.append("      <video:publication_date>").append(DateTimeFormatter.ISO_INSTANT.format(videoPublicationDate)).append("</video:publication_date>\n");
            }
            xml.append("    </video:video>\n");
        }

        xml.append("  </url>\n");
    }

    private String escapeXml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
