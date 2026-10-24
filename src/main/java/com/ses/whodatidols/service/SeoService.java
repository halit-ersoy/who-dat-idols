package com.ses.whodatidols.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ses.whodatidols.model.Episode;
import com.ses.whodatidols.model.Movie;
import com.ses.whodatidols.model.Series;
import com.ses.whodatidols.repository.MovieRepository;
import com.ses.whodatidols.repository.SeriesRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class SeoService {

    private static final Logger logger = LoggerFactory.getLogger(SeoService.class);
    private static final String BASE_URL = "https://whodatidols.com";
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_INSTANT;

    private final SeriesRepository seriesRepository;
    private final MovieRepository movieRepository;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Cache watch.html template in memory for performance
    private volatile String watchHtmlTemplate = null;

    private static final List<String> COMMON_SUFFIXES = List.of(
            "-turkce-altyazili-izle",
            "-turkce-dublaj-izle",
            "-altyazili-izle",
            "-dublaj-izle",
            "-izle"
    );

    private static final Pattern YEAR_SUFFIX_PATTERN = Pattern.compile("^(.*?)-(19\\d\\d|20\\d\\d)$");

    public SeoService(SeriesRepository seriesRepository, MovieRepository movieRepository, JdbcTemplate jdbcTemplate) {
        this.seriesRepository = seriesRepository;
        this.movieRepository = movieRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Resolves an incoming raw slug or legacy path to its current canonical slug.
     * Enforces exact-match-first to prevent unintended year or suffix stripping collisions.
     * Returns null if no content match is found.
     */
    public String resolveCanonicalSlug(String raw) {
        if (raw == null || raw.isBlank()) return null;

        String decoded;
        try {
            decoded = URLDecoder.decode(raw.trim(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            decoded = raw.trim();
        }

        decoded = decoded.replaceAll("/+$", "").replaceAll("^/+", "").trim();
        if (decoded.isEmpty()) return null;

        // Strip subpaths like /page/2 or /1-sezon-the-sweetest-secret/page/2
        String basePart = decoded;
        int firstSlash = decoded.indexOf('/');
        if (firstSlash != -1) {
            basePart = decoded.substring(0, firstSlash);
        }

        // STEP 1: EXACT MATCH FIRST (Do NOT strip year or suffix if exact match exists in DB)
        String directMatch = findDirectSlug(basePart);
        if (directMatch != null) {
            return directMatch;
        }

        // STEP 2: Only if exact match fails, generate stripped variations
        Set<String> candidates = generateCandidates(basePart);
        Set<String> matchedSlugs = new LinkedHashSet<>();

        for (String candidate : candidates) {
            if (candidate.isBlank() || candidate.equalsIgnoreCase(basePart)) continue;
            String matched = findDirectSlug(candidate);
            if (matched != null) {
                matchedSlugs.add(matched);
            }
        }

        // If EXACTLY ONE distinct canonical slug matched: unambiguous redirect
        if (matchedSlugs.size() == 1) {
            return matchedSlugs.iterator().next();
        }

        // If multiple distinct productions match: AMBIGUOUS, DO NOT GUESS! Return null (leads to 404)
        if (matchedSlugs.size() > 1) {
            logger.warn("Ambiguous slug candidates for '{}': matched multiple productions {}. Rejecting redirect.",
                    raw, matchedSlugs);
        }

        return null;
    }

    private String findDirectSlug(String target) {
        if (target == null || target.isBlank()) return null;

        // 1. Check Episode
        try {
            Episode ep = seriesRepository.findEpisodeBySlug(target);
            if (ep != null && !ep.isHidden() && ep.getSlug() != null && !ep.getSlug().isBlank()) {
                Series series = seriesRepository.findSeriesById(ep.getSeriesId());
                if (series != null && !series.isHidden()) {
                    return ep.getSlug();
                }
            }
        } catch (Exception ignored) {}

        // 2. Check Series
        try {
            Series s = seriesRepository.findSeriesBySlug(target);
            if (s != null && !s.isHidden() && s.getSlug() != null && !s.getSlug().isBlank()) {
                return s.getSlug();
            }
        } catch (Exception ignored) {}

        // 3. Check Movie
        try {
            Movie m = movieRepository.findMovieBySlug(target);
            if (m != null && !m.isHidden() && m.getSlug() != null && !m.getSlug().isBlank()) {
                return m.getSlug();
            }
        } catch (Exception ignored) {}

        // 4. Check Upcoming
        try {
            List<Map<String, Object>> upcomings = jdbcTemplate.queryForList(
                    "SELECT TOP 1 Slug FROM Upcoming WHERE Slug = ? OR Name = ? OR LOWER(REPLACE(REPLACE(Name, ' ', '-'), '''', '')) = ?",
                    target, target, target);
            if (!upcomings.isEmpty()) {
                String upSlug = (String) upcomings.get(0).get("Slug");
                if (upSlug != null && !upSlug.isBlank()) return upSlug;
                return target;
            }
        } catch (Exception ignored) {}

        return null;
    }

    private Set<String> generateCandidates(String slug) {
        Set<String> list = new LinkedHashSet<>();
        list.add(slug);

        List<String> seeds = new ArrayList<>(list);
        for (String seed : seeds) {
            addVariations(list, seed);
        }

        return list;
    }

    private void addVariations(Set<String> list, String s) {
        // Strip common Turkish streaming legacy suffixes (-izle, -turkce-altyazili-izle, etc.)
        for (String suffix : COMMON_SUFFIXES) {
            if (s.endsWith(suffix)) {
                String stripped = s.substring(0, s.length() - suffix.length());
                list.add(stripped);
                // Also check if stripped ends with a year (e.g. -2025)
                Matcher ym = YEAR_SUFFIX_PATTERN.matcher(stripped);
                if (ym.matches()) {
                    list.add(ym.group(1));
                }
            }
        }

        // Check if string itself ends with a year without suffix
        Matcher ym = YEAR_SUFFIX_PATTERN.matcher(s);
        if (ym.matches()) {
            list.add(ym.group(1));
        }

        // Unicode Arrow normalization (e.g. 5→9 -> 59 and 5-9)
        if (s.contains("→") || s.contains("\u2192")) {
            String withDash = s.replace("→", "-").replace("\u2192", "-").replaceAll("-+", "-");
            String withoutArrow = s.replace("→", "").replace("\u2192", "").replaceAll("-+", "-");
            list.add(withDash);
            list.add(withoutArrow);

            for (String suffix : COMMON_SUFFIXES) {
                if (withDash.endsWith(suffix)) {
                    list.add(withDash.substring(0, withDash.length() - suffix.length()));
                }
                if (withoutArrow.endsWith(suffix)) {
                    list.add(withoutArrow.substring(0, withoutArrow.length() - suffix.length()));
                }
            }
        }
    }

    public String resolveIdToSlug(String idStr) {
        if (idStr == null || idStr.isBlank()) return null;
        UUID uid;
        try {
            uid = UUID.fromString(idStr.trim());
        } catch (Exception e) {
            return null;
        }

        try {
            Episode ep = seriesRepository.findEpisodeById(uid);
            if (ep != null && ep.getSlug() != null && !ep.getSlug().isBlank()) return ep.getSlug();
        } catch (Exception ignored) {}

        try {
            Series s = seriesRepository.findSeriesById(uid);
            if (s != null && s.getSlug() != null && !s.getSlug().isBlank()) return s.getSlug();
        } catch (Exception ignored) {}

        try {
            Movie m = movieRepository.findMovieById(uid);
            if (m != null && m.getSlug() != null && !m.getSlug().isBlank()) return m.getSlug();
        } catch (Exception ignored) {}

        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT TOP 1 Slug FROM Upcoming WHERE ID = ?", uid);
            if (!rows.isEmpty()) {
                String slug = (String) rows.get(0).get("Slug");
                if (slug != null && !slug.isBlank()) return slug;
            }
        } catch (Exception ignored) {}

        return null;
    }

    /**
     * Builds watch.html with SSR title, description, canonical link, OpenGraph tags,
     * and valid type-specific JSON-LD schema (never null, never empty {}).
     */
    public String buildWatchPageHtml(String slug) {
        String template = getWatchHtmlTemplate();
        if (template == null) return null;

        // Retrieve metadata for this slug
        ContentMeta meta = resolveContentMeta(slug);
        if (meta == null) {
            // If no metadata, strip any structuredData script tag and return template
            return template.replaceAll("<script id=\"structuredData\"[^\"]*?>.*?</script>", "");
        }

        String canonicalUrl = BASE_URL + "/" + escapeHtml(meta.slug);
        String title = escapeHtml(meta.title);
        String description = escapeHtml(meta.description);

        // 1. Replace Canonical Link
        String updated = template.replaceAll(
                "<link rel=\"canonical\" id=\"canonicalLink\" href=\"[^\"]*\">",
                "<link rel=\"canonical\" id=\"canonicalLink\" href=\"" + canonicalUrl + "\">"
        );

        // 2. Replace Title
        updated = updated.replaceAll(
                "<title>[^<]*</title>",
                "<title>" + title + "</title>"
        );

        // 3. Replace Meta Description
        updated = updated.replaceAll(
                "<meta name=\"description\" content=\"[^\"]*\">",
                "<meta name=\"description\" content=\"" + description + "\">"
        );

        // 4. Build JSON-LD structured data (Type-specific: TVEpisode, Movie, TVSeries, CreativeWork)
        String jsonLd = buildJsonLd(meta);
        if (jsonLd != null && !jsonLd.isBlank() && !jsonLd.equals("{}")) {
            String scriptTag = "\n    <script id=\"structuredData\" type=\"application/ld+json\">" + jsonLd + "</script>";
            if (updated.contains("<script id=\"structuredData\"")) {
                updated = updated.replaceAll(
                        "<script id=\"structuredData\" type=\"application/ld\\+json\">.*?</script>",
                        Matcher.quoteReplacement(scriptTag)
                );
            } else {
                updated = updated.replace("</head>", scriptTag + "\n</head>");
            }
        } else {
            updated = updated.replaceAll("<script id=\"structuredData\"[^\"]*?>.*?</script>", "");
        }

        // 5. Inject OpenGraph tags if not already present
        String ogTags = "\n    <meta property=\"og:title\" content=\"" + title + "\">" +
                "\n    <meta property=\"og:description\" content=\"" + description + "\">" +
                "\n    <meta property=\"og:url\" content=\"" + canonicalUrl + "\">" +
                (meta.imageUrl != null ? "\n    <meta property=\"og:image\" content=\"" + escapeHtml(meta.imageUrl) + "\">" : "") +
                "\n    <meta property=\"og:type\" content=\"" + meta.ogType + "\">";

        updated = updated.replace("</head>", ogTags + "\n</head>");

        return updated;
    }

    private String buildJsonLd(ContentMeta meta) {
        try {
            List<Map<String, Object>> schemas = new ArrayList<>();

            // 1. Main Entity Schema
            Map<String, Object> mainEntity = new LinkedHashMap<>();
            mainEntity.put("@context", "https://schema.org");
            mainEntity.put("@type", meta.schemaType);
            mainEntity.put("name", meta.title);
            mainEntity.put("description", meta.description);
            mainEntity.put("url", BASE_URL + "/" + meta.slug);
            if (meta.imageUrl != null) {
                mainEntity.put("image", meta.imageUrl);
            }

            // VideoObject is strictly added ONLY to Episode and Movie pages (actual playable video)
            // It is explicitly NOT added to Series (overview) or Upcoming (countdown) pages
            if ("TVEpisode".equals(meta.schemaType) || "Movie".equals(meta.schemaType)) {
                Map<String, Object> videoObject = new LinkedHashMap<>();
                videoObject.put("@type", "VideoObject");
                videoObject.put("name", meta.title);
                videoObject.put("description", meta.description);
                if (meta.imageUrl != null) {
                    videoObject.put("thumbnailUrl", meta.imageUrl);
                }
                if (meta.uploadDate != null) {
                    videoObject.put("uploadDate", ISO_FORMATTER.format(meta.uploadDate));
                }
                Map<String, Object> watchAction = new LinkedHashMap<>();
                watchAction.put("@type", "WatchAction");
                watchAction.put("target", BASE_URL + "/" + meta.slug);
                videoObject.put("potentialAction", watchAction);

                mainEntity.put("video", videoObject);
            }

            schemas.add(mainEntity);

            // 2. BreadcrumbList Schema
            Map<String, Object> breadcrumbs = new LinkedHashMap<>();
            breadcrumbs.put("@context", "https://schema.org");
            breadcrumbs.put("@type", "BreadcrumbList");

            List<Map<String, Object>> items = new ArrayList<>();
            items.add(Map.of("@type", "ListItem", "position", 1, "name", "Ana Sayfa", "item", BASE_URL));

            String catName = "Diziler";
            String catUrl = BASE_URL + "/diziler";
            if ("Movie".equalsIgnoreCase(meta.schemaType)) {
                catName = "Filmler";
                catUrl = BASE_URL + "/filmler";
            }
            items.add(Map.of("@type", "ListItem", "position", 2, "name", catName, "item", catUrl));
            items.add(Map.of("@type", "ListItem", "position", 3, "name", meta.title, "item", BASE_URL + "/" + meta.slug));

            breadcrumbs.put("itemListElement", items);
            schemas.add(breadcrumbs);

            return objectMapper.writeValueAsString(schemas);
        } catch (Exception e) {
            logger.warn("Error building JSON-LD for slug {}: {}", meta.slug, e.getMessage());
            return null;
        }
    }

    private ContentMeta resolveContentMeta(String slug) {
        // Episode?
        try {
            Episode ep = seriesRepository.findEpisodeBySlug(slug);
            if (ep != null && !ep.isHidden()) {
                Series series = seriesRepository.findSeriesById(ep.getSeriesId());
                if (series == null || series.isHidden()) return null;
                String seriesName = series.getName() != null ? series.getName() : "Dizi";
                String title = seriesName + " " + ep.getSeasonNumber() + ". Sezon " + ep.getEpisodeNumber() + ". Bölüm İzle - Who Dat Idols?";
                String desc = series.getSummary() != null && !series.getSummary().isBlank() ?
                        series.getSummary() : seriesName + " dizisinin " + ep.getSeasonNumber() + ". sezon " + ep.getEpisodeNumber() + ". bölümünü Full HD izle.";
                String img = BASE_URL + "/media/image/" + series.getId();
                Instant upload = ep.getUploadDate() != null ? ep.getUploadDate() : series.getUploadDate();
                return new ContentMeta(slug, title, desc, img, upload, "TVEpisode", "video.episode");
            }
        } catch (Exception ignored) {}

        // Series?
        try {
            Series s = seriesRepository.findSeriesBySlug(slug);
            if (s != null && !s.isHidden()) {
                String title = s.getName() + " İzle - Who Dat Idols?";
                String desc = s.getSummary() != null && !s.getSummary().isBlank() ?
                        s.getSummary() : s.getName() + " tüm bölümlerini Full HD Türkçe altyazılı izle.";
                String img = BASE_URL + "/media/image/" + s.getId();
                return new ContentMeta(slug, title, desc, img, s.getUploadDate(), "TVSeries", "video.tv_show");
            }
        } catch (Exception ignored) {}

        // Movie?
        try {
            Movie m = movieRepository.findMovieBySlug(slug);
            if (m != null && !m.isHidden()) {
                String title = m.getName() + " İzle - Who Dat Idols?";
                String desc = m.getSummary() != null && !m.getSummary().isBlank() ?
                        m.getSummary() : m.getName() + " filmini Full HD Türkçe altyazılı izle.";
                String img = BASE_URL + "/media/image/" + m.getId();
                return new ContentMeta(slug, title, desc, img, m.getUploadDate(), "Movie", "video.movie");
            }
        } catch (Exception ignored) {}

        // Upcoming?
        try {
            List<Map<String, Object>> upcomings = jdbcTemplate.queryForList(
                    "SELECT TOP 1 * FROM Upcoming WHERE Slug = ? OR Name = ? OR LOWER(REPLACE(REPLACE(Name, ' ', '-'), '''', '')) = ?",
                    slug, slug, slug);
            if (!upcomings.isEmpty()) {
                Map<String, Object> u = upcomings.get(0);
                String name = (String) u.get("Name");
                String title = name + " Çok Yakında - Who Dat Idols?";
                String desc = u.get("Summary") != null ? (String) u.get("Summary") : name + " yakında Who Dat Idols ekranlarında.";
                String img = BASE_URL + "/media/image/" + u.get("ID");
                Object cr = u.get("CreatedAt");
                Instant up = cr instanceof java.sql.Timestamp ts ? ts.toInstant() : Instant.now();
                return new ContentMeta(slug, title, desc, img, up, "CreativeWork", "video.other");
            }
        } catch (Exception ignored) {}

        return null;
    }

    // Cache category.html template in memory for performance
    private volatile String categoryHtmlTemplate = null;

    /**
     * Builds category.html with dynamic SSR title, description, canonical link,
     * robots meta tag (index/follow vs noindex/follow), and BreadcrumbList JSON-LD schema.
     */
    public String buildCategoryPageHtml(String requestUri, Integer categoryId, String country, Integer year, String sort, Integer page) {
        String template = getCategoryHtmlTemplate();
        if (template == null) return null;

        String lowerUri = requestUri != null ? requestUri.toLowerCase(Locale.ROOT) : "/filmler";
        String baseType = "movie";
        String baseName = "Filmler";
        String basePath = "/filmler";

        if (lowerUri.contains("/diziler")) {
            baseType = "series";
            baseName = "Diziler";
            basePath = "/diziler";
        } else if (lowerUri.contains("/programlar")) {
            baseType = "program";
            baseName = "Programlar";
            basePath = "/programlar";
        }

        // 1. Resolve Category Name if categoryId is provided
        String categoryName = null;
        if (categoryId != null && categoryId > 0) {
            try {
                List<String> names = jdbcTemplate.queryForList(
                        "SELECT TOP 1 Name FROM Categories WHERE ID = ?", String.class, categoryId);
                if (!names.isEmpty() && names.get(0) != null && !names.get(0).isBlank()) {
                    categoryName = names.get(0).trim();
                }
            } catch (Exception ignored) {}
        }

        // 2. Resolve Country Name if country is provided
        String countryName = null;
        if (country != null && !country.isBlank()) {
            countryName = switch (country.trim().toUpperCase(Locale.ROOT)) {
                case "KR" -> "Kore";
                case "JP" -> "Japon";
                case "CN" -> "Çin";
                case "TH" -> "Tayland";
                case "TW" -> "Tayvan";
                case "PH" -> "Filipin";
                default -> country.trim().toUpperCase(Locale.ROOT);
            };
        }

        // 3. Pagination bounds check
        int totalPages = getTotalCategoryPages(baseType, categoryId, country, year);
        int currentPage = (page != null) ? page : 1;
        if (currentPage < 1 || (page != null && page > totalPages)) {
            // Out of bounds / non-existent page -> return null (Controller returns 404)
            return null;
        }

        // 4. Determine display title and breadcrumb name
        String displayName;
        String breadcrumbItemName;

        if (categoryName != null) {
            String catSuffix = "program".equals(baseType) ? "Programları" : ("movie".equals(baseType) ? "Filmleri" : "Dizileri");
            displayName = categoryName + " " + catSuffix;
            breadcrumbItemName = displayName;
        } else if (countryName != null) {
            String countrySuffix = "program".equals(baseType) ? "Programları" : ("movie".equals(baseType) ? "Filmleri" : "Dizileri");
            displayName = countryName + " " + countrySuffix;
            breadcrumbItemName = displayName;
        } else {
            displayName = baseName;
            breadcrumbItemName = null;
        }

        String pageTitle = displayName + (currentPage > 1 ? " - Sayfa " + currentPage : "") + " - Who Dat Idols?";
        String description = (currentPage > 1)
                ? displayName + " içerik listesi - Sayfa " + currentPage + ". Full HD Türkçe altyazılı izleyin."
                : "En iyi " + displayName.toLowerCase(Locale.ROOT) + " Full HD Türkçe altyazılı Who Dat Idols farkıyla izleyin.";

        // 5. Canonical & Robots policy
        // - Legitimate paginated pages carry their own self-referential canonical URL preserving filters.
        // - Sorting permutations only re-order content -> canonical points to clean page, robots: noindex, follow.
        boolean hasSorting = (sort != null && !sort.isBlank() && !sort.equalsIgnoreCase("newest"));
        String canonicalRelative = buildPaginationUrl(basePath, categoryId, country, year, currentPage);
        String canonicalUrl = BASE_URL + canonicalRelative;
        String robots = hasSorting ? "noindex, follow" : "index, follow";

        String updated = template;

        // Replace Title
        updated = updated.replaceAll("<title>[^<]*</title>", "<title>" + escapeHtml(pageTitle) + "</title>");

        // Replace Description
        updated = updated.replaceAll("<meta name=\"description\" content=\"[^\"]*\">",
                "<meta name=\"description\" content=\"" + escapeHtml(description) + "\">");

        // Inject Canonical & Robots & OpenGraph
        StringBuilder seoTags = new StringBuilder();
        seoTags.append("\n    <link rel=\"canonical\" id=\"canonicalLink\" href=\"").append(canonicalUrl).append("\">");
        seoTags.append("\n    <meta name=\"robots\" content=\"").append(robots).append("\">");
        seoTags.append("\n    <meta property=\"og:title\" content=\"").append(escapeHtml(pageTitle)).append("\">");
        seoTags.append("\n    <meta property=\"og:description\" content=\"").append(escapeHtml(description)).append("\">");
        seoTags.append("\n    <meta property=\"og:url\" content=\"").append(canonicalUrl).append("\">");

        // Inject rel="prev" and rel="next" links
        if (currentPage > 1) {
            String prevUrl = BASE_URL + buildPaginationUrl(basePath, categoryId, country, year, currentPage - 1);
            seoTags.append("\n    <link rel=\"prev\" href=\"").append(prevUrl).append("\">");
        }
        if (currentPage < totalPages) {
            String nextUrl = BASE_URL + buildPaginationUrl(basePath, categoryId, country, year, currentPage + 1);
            seoTags.append("\n    <link rel=\"next\" href=\"").append(nextUrl).append("\">");
        }

        // Build Category BreadcrumbList JSON-LD
        List<Map<String, Object>> breadcrumbItems = new ArrayList<>();
        breadcrumbItems.add(Map.of("@type", "ListItem", "position", 1, "name", "Ana Sayfa", "item", BASE_URL));
        breadcrumbItems.add(Map.of("@type", "ListItem", "position", 2, "name", baseName, "item", BASE_URL + basePath));
        if (breadcrumbItemName != null) {
            breadcrumbItems.add(Map.of("@type", "ListItem", "position", 3, "name", breadcrumbItemName + (currentPage > 1 ? " (Sayfa " + currentPage + ")" : ""), "item", canonicalUrl));
        } else if (currentPage > 1) {
            breadcrumbItems.add(Map.of("@type", "ListItem", "position", 3, "name", baseName + " - Sayfa " + currentPage, "item", canonicalUrl));
        }

        Map<String, Object> breadcrumbSchema = new LinkedHashMap<>();
        breadcrumbSchema.put("@context", "https://schema.org");
        breadcrumbSchema.put("@type", "BreadcrumbList");
        breadcrumbSchema.put("itemListElement", breadcrumbItems);

        try {
            String jsonLd = objectMapper.writeValueAsString(List.of(breadcrumbSchema));
            seoTags.append("\n    <script id=\"structuredData\" type=\"application/ld+json\">").append(jsonLd).append("</script>");
        } catch (Exception ignored) {}

        updated = updated.replace("</head>", seoTags.toString() + "\n</head>");

        // 6. Build crawlable HTML pagination links (<a href="...">) for search engine discovery
        if (totalPages > 1) {
            StringBuilder paginationHtml = new StringBuilder();
            paginationHtml.append("<nav class=\"pagination-nav\" id=\"seo-pagination\" aria-label=\"Sayfalama\">\n");

            if (currentPage > 1) {
                String prevUrl = buildPaginationUrl(basePath, categoryId, country, year, currentPage - 1);
                paginationHtml.append("    <a href=\"").append(prevUrl).append("\" class=\"page-link prev-link\" rel=\"prev\">« Önceki</a>\n");
            }

            int startP = Math.max(1, currentPage - 3);
            int endP = Math.min(totalPages, startP + 6);
            if (endP - startP < 6) {
                startP = Math.max(1, endP - 6);
            }

            for (int p = startP; p <= endP; p++) {
                if (p == currentPage) {
                    paginationHtml.append("    <span class=\"page-number current-page\" aria-current=\"page\">").append(p).append("</span>\n");
                } else {
                    String pUrl = buildPaginationUrl(basePath, categoryId, country, year, p);
                    paginationHtml.append("    <a href=\"").append(pUrl).append("\" class=\"page-number\">").append(p).append("</a>\n");
                }
            }

            if (currentPage < totalPages) {
                String nextUrl = buildPaginationUrl(basePath, categoryId, country, year, currentPage + 1);
                paginationHtml.append("    <a href=\"").append(nextUrl).append("\" class=\"page-link next-link\" rel=\"next\">Sonraki »</a>\n");
            }

            paginationHtml.append("</nav>");

            String controlsReplacement = "<div class=\"pagination-controls\" id=\"pagination-controls\">\n" +
                    "                <button id=\"load-more-btn\" class=\"load-more-btn\">Daha Fazla Göster</button>\n" +
                    "                " + paginationHtml.toString() + "\n" +
                    "            </div>";

            updated = updated.replaceAll("(?s)<div[^>]*id=\"pagination-controls\"[^>]*>.*?</div>", Matcher.quoteReplacement(controlsReplacement));
        }

        return updated;
    }

    public int getTotalCategoryPages(String baseType, Integer categoryId, String country, Integer year) {
        int pageSize = 18;
        int totalElements = 0;
        try {
            if ("movie".equalsIgnoreCase(baseType)) {
                totalElements = movieRepository.countMoviesWithFilters(categoryId, year, country);
            } else if ("series".equalsIgnoreCase(baseType)) {
                totalElements = seriesRepository.countSeriesWithFilters("Dizi", categoryId, null, year, country);
            } else if ("program".equalsIgnoreCase(baseType)) {
                totalElements = seriesRepository.countSeriesWithFilters("Program", categoryId, null, year, country);
            } else {
                totalElements = movieRepository.countMoviesWithFilters(categoryId, year, country);
            }
        } catch (Exception e) {
            logger.warn("Error counting category content: {}", e.getMessage());
            totalElements = 50;
        }
        return Math.max(1, (int) Math.ceil((double) totalElements / pageSize));
    }

    public String buildPaginationUrl(String basePath, Integer categoryId, String country, Integer year, int p) {
        StringBuilder sb = new StringBuilder(basePath);
        boolean hasQuery = false;
        if (categoryId != null && categoryId > 0) {
            sb.append(hasQuery ? "&" : "?").append("categoryId=").append(categoryId);
            hasQuery = true;
        }
        if (country != null && !country.isBlank()) {
            sb.append(hasQuery ? "&" : "?").append("country=").append(country.trim());
            hasQuery = true;
        }
        if (year != null && year > 0) {
            sb.append(hasQuery ? "&" : "?").append("year=").append(year);
            hasQuery = true;
        }
        if (p > 1) {
            sb.append(hasQuery ? "&" : "?").append("page=").append(p);
        }
        return sb.toString();
    }

    private String getCategoryHtmlTemplate() {
        if (categoryHtmlTemplate != null) return categoryHtmlTemplate;
        try (InputStream in = new ClassPathResource("static/category/html/category.html").getInputStream()) {
            categoryHtmlTemplate = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            return categoryHtmlTemplate;
        } catch (Exception e) {
            logger.error("Failed to load static/category/html/category.html template", e);
            return null;
        }
    }

    private String getWatchHtmlTemplate() {
        if (watchHtmlTemplate != null) return watchHtmlTemplate;
        try (InputStream in = new ClassPathResource("static/watch/html/watch.html").getInputStream()) {
            watchHtmlTemplate = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            return watchHtmlTemplate;
        } catch (Exception e) {
            logger.error("Failed to load static/watch/html/watch.html template", e);
            return null;
        }
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    public static class ContentMeta {
        public String slug;
        public String title;
        public String description;
        public String imageUrl;
        public Instant uploadDate;
        public String schemaType; // TVEpisode, Movie, TVSeries, CreativeWork
        public String ogType; // video.episode, video.movie, video.tv_show, video.other

        public ContentMeta(String slug, String title, String description, String imageUrl,
                           Instant uploadDate, String schemaType, String ogType) {
            this.slug = slug;
            this.title = title;
            this.description = description;
            this.imageUrl = imageUrl;
            this.uploadDate = uploadDate;
            this.schemaType = schemaType;
            this.ogType = ogType;
        }
    }
}
