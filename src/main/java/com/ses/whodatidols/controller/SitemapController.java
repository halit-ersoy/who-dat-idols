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
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
public class SitemapController {

    private final MovieRepository movieRepository;
    private final SeriesRepository seriesRepository;
    private final JdbcTemplate jdbcTemplate;

    private static final String BASE_URL = "https://whodatidols.com";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            .withZone(ZoneId.of("UTC"));

    private static final Set<String> BLACKLISTED_SLUGS = Set.of(
            "delete", "edit", "admin", "login", "register", "panel", "api", "logout", "error", "404", "watch"
    );

    public SitemapController(MovieRepository movieRepository, SeriesRepository seriesRepository, JdbcTemplate jdbcTemplate) {
        this.movieRepository = movieRepository;
        this.seriesRepository = seriesRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Main Sitemap Index (Dizin Dosyası)
     * Google and search engines read this to discover all segmented sub-sitemaps.
     */
    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    @Cacheable(value = "sitemaps", key = "'index'")
    public String getSitemapIndex() {
        String today = LocalDate.now(ZoneId.of("UTC")).toString();
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<sitemapindex xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");

        addIndexEntry(xml, "/sitemap-main.xml", today);
        addIndexEntry(xml, "/sitemap-movies.xml", today);
        addIndexEntry(xml, "/sitemap-series.xml", today);
        addIndexEntry(xml, "/sitemap-episodes.xml", today);
        addIndexEntry(xml, "/sitemap-upcoming.xml", today);

        xml.append("</sitemapindex>");
        return xml.toString();
    }

    /**
     * Static & Navigation Pages
     */
    @GetMapping(value = "/sitemap-main.xml", produces = MediaType.APPLICATION_XML_VALUE)
    @Cacheable(value = "sitemaps", key = "'main'")
    public String getMainSitemap() {
        StringBuilder xml = startUrlSet();
        Set<String> added = new HashSet<>();
        Instant now = Instant.now();

        addUrl(xml, added, "/", "1.0", "daily", now, null, null);
        addUrl(xml, added, "/filmler", "0.9", "daily", now, null, null);
        addUrl(xml, added, "/diziler", "0.9", "daily", now, null, null);
        addUrl(xml, added, "/programlar", "0.6", "weekly", now, null, null);
        addUrl(xml, added, "/coming-soon", "0.7", "weekly", now, null, null);
        addUrl(xml, added, "/about", "0.5", "monthly", null, null, null);
        addUrl(xml, added, "/sss", "0.5", "monthly", null, null, null);
        addUrl(xml, added, "/terms_of_use", "0.3", "monthly", null, null, null);
        addUrl(xml, added, "/privacy_policy", "0.3", "monthly", null, null, null);

        xml.append("</urlset>");
        return xml.toString();
    }

    /**
     * Movies Sitemap (with Image extension)
     */
    @GetMapping(value = "/sitemap-movies.xml", produces = MediaType.APPLICATION_XML_VALUE)
    @Cacheable(value = "sitemaps", key = "'movies'")
    public String getMoviesSitemap() {
        StringBuilder xml = startUrlSet();
        Set<String> added = new HashSet<>();

        List<Movie> movies = movieRepository.findAll();
        for (Movie movie : movies) {
            if (movie.isBl() || "BL".equalsIgnoreCase(movie.getMovieType()) || "GL".equalsIgnoreCase(movie.getMovieType())) {
                continue;
            }
            String slug = movie.getSlug();
            if (slug != null && !slug.isBlank() && !BLACKLISTED_SLUGS.contains(slug.toLowerCase(Locale.ROOT))) {
                String imgUrl = BASE_URL + "/media/image/" + movie.getId();
                addUrl(xml, added, "/" + slug, "0.8", "weekly", movie.getUploadDate(), imgUrl, movie.getName());
            }
        }

        xml.append("</urlset>");
        return xml.toString();
    }

    /**
     * Series Sitemap (Main Series landing pages with Image extension)
     */
    @GetMapping(value = "/sitemap-series.xml", produces = MediaType.APPLICATION_XML_VALUE)
    @Cacheable(value = "sitemaps", key = "'series'")
    public String getSeriesSitemap() {
        StringBuilder xml = startUrlSet();
        Set<String> added = new HashSet<>();

        List<Series> allSeries = seriesRepository.findAllSeries();
        for (Series series : allSeries) {
            if (series.isBl() || "BL".equalsIgnoreCase(series.getSeriesType()) || "GL".equalsIgnoreCase(series.getSeriesType())) {
                continue;
            }
            String slug = series.getSlug();
            if (slug != null && !slug.isBlank() && !BLACKLISTED_SLUGS.contains(slug.toLowerCase(Locale.ROOT))) {
                String imgUrl = BASE_URL + "/media/image/" + series.getId();
                addUrl(xml, added, "/" + slug, "0.8", "weekly", series.getUploadDate(), imgUrl, series.getName());
            }
        }

        xml.append("</urlset>");
        return xml.toString();
    }

    /**
     * Episodes Sitemap (Individual episodes with Image extension)
     */
    @GetMapping(value = "/sitemap-episodes.xml", produces = MediaType.APPLICATION_XML_VALUE)
    @Cacheable(value = "sitemaps", key = "'episodes'")
    public String getEpisodesSitemap() {
        StringBuilder xml = startUrlSet();
        Set<String> added = new HashSet<>();

        List<Series> allSeries = seriesRepository.findAllSeries();
        for (Series series : allSeries) {
            if (series.isBl() || "BL".equalsIgnoreCase(series.getSeriesType()) || "GL".equalsIgnoreCase(series.getSeriesType())) {
                continue;
            }
            List<Episode> episodes = seriesRepository.findEpisodesBySeriesId(series.getId());
            for (Episode episode : episodes) {
                String slug = episode.getSlug();
                if (slug != null && !slug.isBlank() && !BLACKLISTED_SLUGS.contains(slug.toLowerCase(Locale.ROOT))) {
                    String imgUrl = BASE_URL + "/media/image/" + series.getId();
                    String epTitle = series.getName() + " " + episode.getSeasonNumber() + ". Sezon " + episode.getEpisodeNumber() + ". Bölüm";
                    addUrl(xml, added, "/" + slug, "0.7", "weekly", episode.getUploadDate(), imgUrl, epTitle);
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
        StringBuilder xml = startUrlSet();
        Set<String> added = new HashSet<>();

        try {
            String sql = """
                    SELECT u.ID as upcomingId, u.Name, COALESCE(u.Slug, s.slug, m.slug) as slug,
                           u.CreatedAt, u.[datetime]
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
                    Object dt = row.get("CreatedAt");
                    Instant lastmod = null;
                    if (dt instanceof java.sql.Timestamp ts) {
                        lastmod = ts.toInstant();
                    }
                    String imgUrl = BASE_URL + "/media/image/" + row.get("upcomingId");
                    addUrl(xml, added, "/" + slug, "0.8", "daily", lastmod != null ? lastmod : Instant.now(), imgUrl, name);
                }
            }
        } catch (Exception e) {
            System.err.println("Upcoming sitemap error: " + e.getMessage());
        }

        xml.append("</urlset>");
        return xml.toString();
    }

    private StringBuilder startUrlSet() {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\"\n");
        xml.append("        xmlns:image=\"http://www.google.com/schemas/sitemap-image/1.1\">\n");
        return xml;
    }

    private void addIndexEntry(StringBuilder xml, String path, String lastmod) {
        xml.append("  <sitemap>\n");
        xml.append("    <loc>").append(BASE_URL).append(path).append("</loc>\n");
        if (lastmod != null) {
            xml.append("    <lastmod>").append(lastmod).append("</lastmod>\n");
        }
        xml.append("  </sitemap>\n");
    }

    private void addUrl(StringBuilder xml, Set<String> addedUrls, String path, String priority,
                        String changefreq, Instant lastmod, String imageUrl, String imageTitle) {
        if (addedUrls.contains(path)) return;
        addedUrls.add(path);

        String fullUrl = escapeXml(BASE_URL + path);

        xml.append("  <url>\n");
        xml.append("    <loc>").append(fullUrl).append("</loc>\n");
        if (lastmod != null) {
            xml.append("    <lastmod>").append(FORMATTER.format(lastmod)).append("</lastmod>\n");
        }
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
