package com.ses.whodatidols;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ses.whodatidols.model.Movie;
import com.ses.whodatidols.model.Episode;
import com.ses.whodatidols.repository.SeriesRepository;
import com.ses.whodatidols.repository.MovieRepository;
import com.ses.whodatidols.service.MovieService;
import com.ses.whodatidols.service.SeoService;
import com.ses.whodatidols.service.SeriesService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import javax.net.ssl.SSLContext;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class SeoAndRedirectTests {

    @LocalServerPort
    private int port;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SeoService seoService;

    @Autowired
    private SeriesService seriesService;

    @Autowired
    private MovieService movieService;

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private SeriesRepository seriesRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("1. GSC 5xx Issue Fix: /konu/* must return 404 (Not 500, no GLOBAL_HANDLER_CAUGHT)")
    void testKonuLegacyUrlReturns404() throws Exception {
        mockMvc.perform(get("/konu/dog").param("orderby", "rating"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(not(containsString("GLOBAL_HANDLER_CAUGHT"))));

        mockMvc.perform(get("/konu/disturbed").param("orderby", "rating"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(not(containsString("GLOBAL_HANDLER_CAUGHT"))));
    }

    @Test
    @DisplayName("2. Missing static resources must return 404 (Not 500)")
    void testMissingStaticResourceReturns404() throws Exception {
        mockMvc.perform(get("/static/missing-file-test-xyz.css"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(not(containsString("GLOBAL_HANDLER_CAUGHT"))))
                .andExpect(content().string(not(containsString("NullPointerException"))));
    }

    @Test
    @DisplayName("3. Legacy Category /tur/* must redirect 301 to dynamic categoryId on /filmler or /diziler")
    void testLegacyTurRedirectsWithCategoryId() throws Exception {
        // Dynamic category lookup from live Categories table
        Integer aksiyonId = jdbcTemplate.queryForObject(
                "SELECT TOP 1 ID FROM Categories WHERE Name LIKE '%Aksiyon%'", Integer.class);
        assertNotNull(aksiyonId);

        mockMvc.perform(get("/tur/aksiyon-filmleri").param("orderby", "title").param("order", "DESC"))
                .andExpect(status().isMovedPermanently())
                .andExpect(header().string("Location", "/filmler?categoryId=" + aksiyonId));

        Integer korkuId = jdbcTemplate.queryForObject(
                "SELECT TOP 1 ID FROM Categories WHERE Name LIKE '%Korku%'", Integer.class);
        assertNotNull(korkuId);

        mockMvc.perform(get("/tur/korku-dizileri"))
                .andExpect(status().isMovedPermanently())
                .andExpect(header().string("Location", "/diziler?categoryId=" + korkuId));

        // General fallback without matching genre
        mockMvc.perform(get("/tur/yabanci-diziler"))
                .andExpect(status().isMovedPermanently())
                .andExpect(header().string("Location", "/diziler"));
    }

    @Test
    @DisplayName("4. GSC Sample Legacy Content URLs redirect 301 to existing canonical items")
    void testGscLegacyContentRedirects() throws Exception {
        // /film/the-match-2025-izle -> 301 to /the-match
        mockMvc.perform(get("/film/the-match-2025-izle"))
                .andExpect(status().isMovedPermanently())
                .andExpect(header().string("Location", "/the-match"));

        // /diziler/boys-ii-planet-izle -> 301 to /boys-ii-planet
        mockMvc.perform(get("/diziler/boys-ii-planet-izle"))
                .andExpect(status().isMovedPermanently())
                .andExpect(header().string("Location", "/boys-ii-planet"));

        // /dizi/the-witch-store/?orderby=title&order=DESC -> 301 to /the-witch-store
        mockMvc.perform(get("/dizi/the-witch-store/").param("orderby", "title").param("order", "DESC"))
                .andExpect(status().isMovedPermanently())
                .andExpect(header().string("Location", "/the-witch-store"));

        // /dizi/alice-in-borderland?orderby=comment_count&order=DESC -> 301 to /alice-in-borderland
        mockMvc.perform(get("/dizi/alice-in-borderland").param("orderby", "comment_count").param("order", "DESC"))
                .andExpect(status().isMovedPermanently())
                .andExpect(header().string("Location", "/alice-in-borderland"));

        // /dizi/sixth-sense/page/2?orderby=title&order=DESC -> 301 to /sixth-sense
        mockMvc.perform(get("/dizi/sixth-sense/page/2").param("orderby", "title").param("order", "DESC"))
                .andExpect(status().isMovedPermanently())
                .andExpect(header().string("Location", "/sixth-sense"));

        // /dizi/the-sweetest-secret/1-sezon-the-sweetest-secret/page/2 -> 301 to /the-sweetest-secret
        mockMvc.perform(get("/dizi/the-sweetest-secret/1-sezon-the-sweetest-secret/page/2"))
                .andExpect(status().isMovedPermanently())
                .andExpect(header().string("Location", "/the-sweetest-secret"));

        // Unicode Arrow: /5→9-from-five-to-nine-1-sezon-1-bolum-izle -> 301 to /59-from-five-to-nine-1-sezon-1-bolum
        mockMvc.perform(get("/5→9-from-five-to-nine-1-sezon-1-bolum-izle"))
                .andExpect(status().isMovedPermanently())
                .andExpect(header().string("Location", "/59-from-five-to-nine-1-sezon-1-bolum"));
    }

    @Test
    @DisplayName("5. Watch URL with query parameters redirects 301 to canonical clean URL")
    void testWatchQueryParamRedirects() throws Exception {
        mockMvc.perform(get("/watch").param("slug", "alice-in-borderland"))
                .andExpect(status().isMovedPermanently())
                .andExpect(header().string("Location", "/alice-in-borderland"));

        mockMvc.perform(get("/watch"))
                .andExpect(status().isMovedPermanently())
                .andExpect(header().string("Location", "/"));
    }

    @Test
    @DisplayName("6. SSR Watch HTML generates valid Canonical and Type-Specific JSON-LD")
    void testWatchHtmlSSRMetadata() {
        // Episode: should have TVEpisode with embedded VideoObject
        String epSlug = "alice-in-borderland-1-sezon-1-bolum";
        String epHtml = seoService.buildWatchPageHtml(epSlug);
        assertNotNull(epHtml);
        assertTrue(epHtml.contains("<link rel=\"canonical\" id=\"canonicalLink\" href=\"https://whodatidols.com/" + epSlug + "\">"));
        assertTrue(epHtml.contains("\"@type\":\"TVEpisode\""));
        assertTrue(epHtml.contains("\"@type\":\"VideoObject\""));
        assertTrue(epHtml.contains("\"@type\":\"BreadcrumbList\""));

        int start = epHtml.indexOf("<script id=\"structuredData\" type=\"application/ld+json\">") +
                "<script id=\"structuredData\" type=\"application/ld+json\">".length();
        int end = epHtml.indexOf("</script>", start);
        String jsonLdStr = epHtml.substring(start, end).trim();
        assertNotEquals("null", jsonLdStr);
        assertNotEquals("{}", jsonLdStr);
        assertDoesNotThrow(() -> {
            JsonNode root = objectMapper.readTree(jsonLdStr);
            assertTrue(root.isArray());
            assertEquals("https://schema.org", root.get(0).get("@context").asText());
        });

        // Series: should have TVSeries, Breadcrumbs, but explicitly NO VideoObject
        String seriesSlug = "alice-in-borderland";
        String seriesHtml = seoService.buildWatchPageHtml(seriesSlug);
        assertNotNull(seriesHtml);
        assertTrue(seriesHtml.contains("<link rel=\"canonical\" id=\"canonicalLink\" href=\"https://whodatidols.com/" + seriesSlug + "\">"));
        assertTrue(seriesHtml.contains("\"@type\":\"TVSeries\""));
        assertFalse(seriesHtml.contains("\"@type\":\"VideoObject\""), "Series overview must NOT have VideoObject");
    }

    @Test
    @DisplayName("7. Sitemaps structure, omission of unreliable lastmod, and video sitemap tags")
    void testSitemaps() throws Exception {
        // Sitemap index: must omit unreliable lastmod
        mockMvc.perform(get("/sitemap.xml"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML))
                .andExpect(content().string(containsString("<sitemapindex")))
                .andExpect(content().string(containsString("/sitemap-main.xml")))
                .andExpect(content().string(not(containsString("<lastmod>"))));

        // Static sitemap must omit lastmod
        mockMvc.perform(get("/sitemap-main.xml"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML))
                .andExpect(content().string(not(containsString("<lastmod>"))));

        // Series sitemap must NOT contain video tags
        mockMvc.perform(get("/sitemap-series.xml"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML))
                .andExpect(content().string(not(containsString("<video:video>"))));

        // Episodes sitemap must contain video tags with publication_date
        mockMvc.perform(get("/sitemap-episodes.xml"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML))
                .andExpect(content().string(containsString("xmlns:video=\"http://www.google.com/schemas/sitemap-video/1.1\"")))
                .andExpect(content().string(containsString("<video:video>")))
                .andExpect(content().string(containsString("<video:player_loc>")))
                .andExpect(content().string(containsString("<video:publication_date>")));
    }

    @Test
    @DisplayName("8. Robots.txt permits Googlebot Web Rendering Service for Video APIs")
    void testRobotsTxtContent() throws Exception {
        mockMvc.perform(get("/robots.txt"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Allow: /api/video/")))
                .andExpect(content().string(containsString("Allow: /api/featured-content/")))
                .andExpect(content().string(containsString("Disallow: /api/")))
                .andExpect(content().string(containsString("Sitemap: https://whodatidols.com/sitemap.xml")));
    }

    @Test
    @DisplayName("9. Hidden content is never exposed via SSR, resolveCanonicalSlug, or isValidSlug")
    void testHiddenContentProtection() throws Exception {
        String fakeHiddenSlug = "test-hidden-content-seo-check-" + UUID.randomUUID();
        UUID testId = UUID.randomUUID();

        Movie testMovie = new Movie();
        testMovie.setId(testId);
        testMovie.setName("Test Hidden Movie");
        testMovie.setSlug(fakeHiddenSlug);
        testMovie.setSummary("Hidden test movie summary");
        testMovie.setLanguage("Türkçe");
        testMovie.setCountry("KR");
        testMovie.setReleaseYear(2024);
        testMovie.setDurationMinutes(90);
        testMovie.setUploadDate(Instant.now());
        testMovie.setHidden(true);
        testMovie.setBl(false);
        testMovie.setMovieType("Film");

        try {
            movieRepository.save(testMovie);

            // 1. MovieService.isValidSlug should return false for hidden content
            assertFalse(movieService.isValidSlug(fakeHiddenSlug));

            // 2. SeoService.resolveCanonicalSlug should return null for hidden content
            assertNull(seoService.resolveCanonicalSlug(fakeHiddenSlug));
            assertNull(seoService.resolveCanonicalSlug(fakeHiddenSlug + "-izle"));

            // 3. SeoService.buildWatchPageHtml should return null or strip structuredData
            String html = seoService.buildWatchPageHtml(fakeHiddenSlug);
            if (html != null) {
                assertFalse(html.contains(fakeHiddenSlug));
                assertFalse(html.contains("<script id=\"structuredData\""));
            }

            // 4. Requesting the hidden page must return 404 Not Found
            mockMvc.perform(get("/" + fakeHiddenSlug))
                    .andExpect(status().isNotFound());

            // 5. Sitemap must NOT contain the hidden movie
            mockMvc.perform(get("/sitemap-movies.xml"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(not(containsString(fakeHiddenSlug))));
        } finally {
            movieRepository.deleteById(testId);
        }
    }

    @Test
    @DisplayName("10. True internal server errors return 500 without masking as 404")
    void testTrueServerErrorReturns500() throws Exception {
        // Calling an endpoint with invalid query or triggering server exception
        // /api/featured-content/movies with negative values causes a server error
        // We verify that an unhandled controller error status is 500 and NOT masked as 404
        mockMvc.perform(get("/api/featured-content/movies").param("page", "-999").param("size", "-999"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assertNotEquals(404, status, "Internal or bad request error should never be converted to 404");
                });
    }

    @Test
    @DisplayName("11. Ambiguous slug resolution with multiple matching productions rejects redirect (returns null/404)")
    void testAmbiguousResolutionRejectsRedirect() {
        // Step 1: Exact match priority:
        // A production with slug 'the-match' should resolve directly to 'the-match'
        assertEquals("the-match", seoService.resolveCanonicalSlug("the-match"));

        // Step 2: Unambiguous candidate (year/suffix stripped) resolves when only 1 candidate exists
        assertEquals("the-match", seoService.resolveCanonicalSlug("the-match-2025-izle"));

        // Step 3: Completely non-existent slug returns null (leads to 404)
        String nonExistent = "totally-non-existent-production-slug-xyz-12345";
        assertNull(seoService.resolveCanonicalSlug(nonExistent));

        // Step 4: Multi-candidate ambiguity test
        // Create two distinct movies: 'test-ambig-film' and 'test-ambig-film-2024'
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        String baseSlug = "test-ambig-prod-" + UUID.randomUUID().toString().substring(0, 6);
        String yearSlug = baseSlug + "-2024";

        Movie m1 = new Movie();
        m1.setId(id1);
        m1.setName("Test Ambig Movie 1");
        m1.setSlug(baseSlug);
        m1.setSummary("Summary 1");
        m1.setLanguage("Türkçe");
        m1.setCountry("KR");
        m1.setReleaseYear(2020);
        m1.setDurationMinutes(90);
        m1.setUploadDate(Instant.now());
        m1.setHidden(false);
        m1.setBl(false);
        m1.setMovieType("Film");

        Movie m2 = new Movie();
        m2.setId(id2);
        m2.setName("Test Ambig Movie 2");
        m2.setSlug(yearSlug);
        m2.setSummary("Summary 2");
        m2.setLanguage("Türkçe");
        m2.setCountry("KR");
        m2.setReleaseYear(2024);
        m2.setDurationMinutes(100);
        m2.setUploadDate(Instant.now());
        m2.setHidden(false);
        m2.setBl(false);
        m2.setMovieType("Film");

        try {
            movieRepository.save(m1);
            movieRepository.save(m2);

            // If user requests exact baseSlug -> exact match returns m1
            assertEquals(baseSlug, seoService.resolveCanonicalSlug(baseSlug));

            // If user requests exact yearSlug -> exact match returns m2 (does NOT strip 2024!)
            assertEquals(yearSlug, seoService.resolveCanonicalSlug(yearSlug));

            // If incoming legacy URL is ambiguous, e.g. baseSlug + "-2024-izle":
            // Candidate variations will produce:
            // 1. baseSlug + "-2024" (matches m2)
            // 2. baseSlug (matches m1)
            // Since 2 distinct productions matched: AMBIGUITY! Must return NULL to prevent guessing!
            String ambiguousLegacySlug = yearSlug + "-izle";
            String resolved = seoService.resolveCanonicalSlug(ambiguousLegacySlug);
            assertNull(resolved, "Ambiguous slug matching multiple productions must return null (404), never guess!");
        } finally {
            movieRepository.deleteById(id1);
            movieRepository.deleteById(id2);
        }
    }

    @Test
    @DisplayName("12. Category Pages SSR Canonical, Title, Robots Meta, Pagination, and BreadcrumbList")
    void testCategoryPagesSsrMetadata() throws Exception {
        // 1. Pure Base (Page 1): /filmler
        mockMvc.perform(get("/filmler"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<title>Filmler - Who Dat Idols?</title>")))
                .andExpect(content().string(containsString("<link rel=\"canonical\" id=\"canonicalLink\" href=\"https://whodatidols.com/filmler\">")))
                .andExpect(content().string(containsString("<meta name=\"robots\" content=\"index, follow\">")))
                .andExpect(content().string(containsString("<nav class=\"pagination-nav\" id=\"seo-pagination\"")))
                .andExpect(content().string(containsString("<a href=\"/filmler?page=2\"")))
                .andExpect(content().string(containsString("\"@type\":\"BreadcrumbList\"")));

        // 2. Base Paginated Page (Page 2): /filmler?page=2
        // Must carry its OWN self-referential canonical (NOT canonicalized to page 1!)
        mockMvc.perform(get("/filmler").param("page", "2"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<title>Filmler - Sayfa 2 - Who Dat Idols?</title>")))
                .andExpect(content().string(containsString("<link rel=\"canonical\" id=\"canonicalLink\" href=\"https://whodatidols.com/filmler?page=2\">")))
                .andExpect(content().string(containsString("<meta name=\"robots\" content=\"index, follow\">")))
                .andExpect(content().string(containsString("<link rel=\"prev\" href=\"https://whodatidols.com/filmler\">")))
                .andExpect(content().string(containsString("<a href=\"/filmler\" class=\"page-link prev-link\" rel=\"prev\">")));

        // 3. Clean Category Filter (Page 1): /filmler?categoryId={aksiyonId}
        Integer aksiyonId = jdbcTemplate.queryForObject(
                "SELECT TOP 1 ID FROM Categories WHERE Name LIKE '%Aksiyon%'", Integer.class);
        assertNotNull(aksiyonId);

        mockMvc.perform(get("/filmler").param("categoryId", aksiyonId.toString()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<title>Aksiyon Filmleri - Who Dat Idols?</title>")))
                .andExpect(content().string(containsString("<link rel=\"canonical\" id=\"canonicalLink\" href=\"https://whodatidols.com/filmler?categoryId=" + aksiyonId + "\">")))
                .andExpect(content().string(containsString("<meta name=\"robots\" content=\"index, follow\">")))
                .andExpect(content().string(containsString("Aksiyon Filmleri")));

        // 4. Clean Category Filter (Page 2): /filmler?categoryId={aksiyonId}&page=2
        // Must carry its OWN self-referential canonical preserving categoryId!
        mockMvc.perform(get("/filmler").param("categoryId", aksiyonId.toString()).param("page", "2"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<title>Aksiyon Filmleri - Sayfa 2 - Who Dat Idols?</title>")))
                .andExpect(content().string(containsString("<link rel=\"canonical\" id=\"canonicalLink\" href=\"https://whodatidols.com/filmler?categoryId=" + aksiyonId + "&page=2\">")))
                .andExpect(content().string(containsString("<meta name=\"robots\" content=\"index, follow\">")))
                .andExpect(content().string(containsString("<link rel=\"prev\" href=\"https://whodatidols.com/filmler?categoryId=" + aksiyonId + "\">")))
                .andExpect(content().string(containsString("<a href=\"/filmler?categoryId=" + aksiyonId + "\" class=\"page-link prev-link\" rel=\"prev\">")));

        // 5. Faceted Filter with Sorting: /filmler?categoryId={aksiyonId}&sort=oldest
        // Should keep canonical to clean category, but apply noindex, follow
        mockMvc.perform(get("/filmler").param("categoryId", aksiyonId.toString()).param("sort", "oldest"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<link rel=\"canonical\" id=\"canonicalLink\" href=\"https://whodatidols.com/filmler?categoryId=" + aksiyonId + "\">")))
                .andExpect(content().string(containsString("<meta name=\"robots\" content=\"noindex, follow\">")));

        // 6. Category Filter with Page and Sorting: /filmler?categoryId={aksiyonId}&page=2&sort=oldest
        // Should point canonical to clean paginated category (without sort), with noindex, follow
        mockMvc.perform(get("/filmler").param("categoryId", aksiyonId.toString()).param("page", "2").param("sort", "oldest"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<link rel=\"canonical\" id=\"canonicalLink\" href=\"https://whodatidols.com/filmler?categoryId=" + aksiyonId + "&page=2\">")))
                .andExpect(content().string(containsString("<meta name=\"robots\" content=\"noindex, follow\">")));

        // 7. Non-existent / Out-of-bounds page number: /filmler?page=99999
        // Must return 404 to stop search engines from generating infinite empty URLs
        mockMvc.perform(get("/filmler").param("page", "99999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("13. Series landing vs Episode watch: Canonical, Schema, and VideoObject differentiation")
    void testSeriesVsEpisodeMetadataIntegrity() {
        // Series Overview: TVSeries, no VideoObject, canonical points to series slug
        String seriesSlug = "alice-in-borderland";
        String seriesHtml = seoService.buildWatchPageHtml(seriesSlug);
        assertNotNull(seriesHtml);
        assertTrue(seriesHtml.contains("<link rel=\"canonical\" id=\"canonicalLink\" href=\"https://whodatidols.com/alice-in-borderland\">"));
        assertTrue(seriesHtml.contains("\"@type\":\"TVSeries\""));
        assertFalse(seriesHtml.contains("\"@type\":\"VideoObject\""), "Series landing must NOT contain VideoObject");

        // Episode Watch: TVEpisode, has VideoObject, canonical points to episode slug (NOT series!)
        String epSlug = "alice-in-borderland-1-sezon-1-bolum";
        String epHtml = seoService.buildWatchPageHtml(epSlug);
        assertNotNull(epHtml);
        assertTrue(epHtml.contains("<link rel=\"canonical\" id=\"canonicalLink\" href=\"https://whodatidols.com/alice-in-borderland-1-sezon-1-bolum\">"));
        assertTrue(epHtml.contains("\"@type\":\"TVEpisode\""));
        assertTrue(epHtml.contains("\"@type\":\"VideoObject\""), "Episode page MUST contain VideoObject");
        assertTrue(epHtml.contains("\"thumbnailUrl\":"));
        assertTrue(epHtml.contains("\"potentialAction\":"));

        // Movie: Movie, has VideoObject, canonical points to movie slug
        String movieSlug = "the-match";
        String movieHtml = seoService.buildWatchPageHtml(movieSlug);
        assertNotNull(movieHtml);
        assertTrue(movieHtml.contains("<link rel=\"canonical\" id=\"canonicalLink\" href=\"https://whodatidols.com/the-match\">"));
        assertTrue(movieHtml.contains("\"@type\":\"Movie\""));
        assertTrue(movieHtml.contains("\"@type\":\"VideoObject\""), "Movie page MUST contain VideoObject");
    }

    @Test
    @DisplayName("14. Video API returns seriesSlug for episodes so client does not corrupt series canonical")
    void testVideoApiReturnsSeriesContext() throws Exception {
        // When resolving series slug, resolve-slug must return first episode ID along with seriesSlug
        mockMvc.perform(get("/api/video/resolve-slug").param("slug", "alice-in-borderland"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("episode"))
                .andExpect(jsonPath("$.seriesSlug").value("alice-in-borderland"))
                .andExpect(jsonPath("$.seriesName").value("Alice in Borderland"));

        // When requesting episode details, response must include seriesSlug and seriesName
        Episode ep = seriesRepository.findEpisodeBySlug("alice-in-borderland-1-sezon-1-bolum");
        assertNotNull(ep);
        mockMvc.perform(get("/api/video/details").param("id", ep.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("episode"))
                .andExpect(jsonPath("$.slug").value("alice-in-borderland-1-sezon-1-bolum"))
                .andExpect(jsonPath("$.seriesSlug").value("alice-in-borderland"))
                .andExpect(jsonPath("$.seriesName").value("Alice in Borderland"));
    }

    @Test
    @DisplayName("15. Real Browser Headless Chrome DOM Verification: Initial HTML vs Post-JS DOM")
    void testRealBrowserDomVerification() throws Exception {
        File chromeExe = new File("C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe");
        if (!chromeExe.exists()) {
            chromeExe = new File("C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe");
        }
        assertTrue(chromeExe.exists(), "Headless Chrome or Edge must be available for real browser verification");

        // 1. Movie Test: /the-match
        verifyUrlInBrowser(chromeExe, "/the-match", "Movie", true, "https://whodatidols.com/the-match");

        // 2. Series Landing Test: /alice-in-borderland
        verifyUrlInBrowser(chromeExe, "/alice-in-borderland", "TVSeries", false, "https://whodatidols.com/alice-in-borderland");

        // 3. Episode Test: /alice-in-borderland-1-sezon-1-bolum
        verifyUrlInBrowser(chromeExe, "/alice-in-borderland-1-sezon-1-bolum", "TVEpisode", true, "https://whodatidols.com/alice-in-borderland-1-sezon-1-bolum");
    }

    private SSLContext createTrustAllSslContext() throws Exception {
        TrustManager[] trustAll = new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() { return null; }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                }
        };
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustAll, new SecureRandom());
        return sslContext;
    }

    private void verifyUrlInBrowser(File browserExe, String path,
                                    String expectedType, boolean expectVideoObject, String expectedCanonical) throws Exception {
        String baseUrl = "https://localhost:" + port + path;

        // Step A: Fetch Initial SSR HTML via HTTPS connection
        HttpsURLConnection conn = (HttpsURLConnection) new URI(baseUrl).toURL().openConnection();
        conn.setHostnameVerifier((hostname, session) -> true);
        conn.setSSLSocketFactory(createTrustAllSslContext().getSocketFactory());
        conn.connect();
        assertEquals(200, conn.getResponseCode());
        String initialHtml;
        try (InputStream in = conn.getInputStream()) {
            initialHtml = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }

        // Extract Initial HTML metadata
        String initialCanonical = extractTagAttribute(initialHtml, "link", "rel=\"canonical\"", "href");
        String initialJsonLd = extractScriptContent(initialHtml, "structuredData");
        assertNotNull(initialCanonical, "Initial HTML must have canonical");
        assertEquals(expectedCanonical, initialCanonical, "Initial HTML canonical must match expected");
        assertNotNull(initialJsonLd, "Initial HTML must have structuredData");

        JsonNode initialRoot = objectMapper.readTree(initialJsonLd);
        assertTrue(initialRoot.isArray());
        JsonNode initialMain = initialRoot.get(0);
        assertEquals(expectedType, initialMain.get("@type").asText(), "Initial HTML schema type must match");
        assertEquals(expectVideoObject, initialMain.has("video") && "VideoObject".equals(initialMain.get("video").get("@type").asText()),
                "Initial HTML VideoObject presence mismatch");

        // Step B: Run Real Browser (Chrome/Edge headless) and capture final DOM after JS execution
        ProcessBuilder pb = new ProcessBuilder(
                browserExe.getAbsolutePath(),
                "--headless=new",
                "--ignore-certificate-errors",
                "--allow-insecure-localhost",
                "--dump-dom",
                "--virtual-time-budget=8000",
                baseUrl
        );
        pb.redirectErrorStream(true);
        Process process = pb.start();
        String finalDom;
        try (InputStream in = process.getInputStream()) {
            finalDom = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        process.waitFor();

        // Step C: Verify Final DOM after JS execution
        // Verify that async API calls completed: "Yükleniyor..." placeholder replaced and cast members loaded
        assertFalse(finalDom.contains("id=\"title\">Yükleniyor...</div>"), "API must have completed and replaced 'Yükleniyor...' with actual title");
        assertTrue(finalDom.contains("class=\"cast-member\""), "Cast members from API payload must be rendered in DOM");

        String finalCanonical = extractTagAttribute(finalDom, "link", "rel=\"canonical\"", "href");
        assertNotNull(finalCanonical, "Final DOM must contain canonical link");
        assertEquals(expectedCanonical, finalCanonical, "Final DOM canonical must NOT be altered or corrupted by client JS!");

        String finalJsonLd = extractScriptContent(finalDom, "structuredData");
        assertNotNull(finalJsonLd, "Final DOM must contain structuredData");

        JsonNode finalRoot = objectMapper.readTree(finalJsonLd);
        assertTrue(finalRoot.isArray(), "Final DOM structuredData must be an array");
        JsonNode finalMain = finalRoot.get(0);
        assertEquals(expectedType, finalMain.get("@type").asText(), "Final DOM schema type must remain " + expectedType);
        assertEquals(expectVideoObject, finalMain.has("video") && "VideoObject".equals(finalMain.get("video").get("@type").asText()),
                "Final DOM VideoObject presence must match requirement");

        // Step D: Verify BreadcrumbList in Final DOM
        boolean hasBreadcrumb = false;
        for (JsonNode node : finalRoot) {
            if ("BreadcrumbList".equals(node.path("@type").asText())) {
                hasBreadcrumb = true;
                break;
            }
        }
        assertTrue(hasBreadcrumb, "Final DOM must contain BreadcrumbList");
    }

    private String extractTagAttribute(String html, String tag, String identifier, String attribute) {
        Pattern pattern = Pattern.compile("<" + tag + "[^>]*" + Pattern.quote(identifier) + "[^>]*" + Pattern.quote(attribute) + "=\"([^\"]+)\"", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(html);
        if (matcher.find()) return matcher.group(1);

        Pattern patternAlt = Pattern.compile("<" + tag + "[^>]*" + Pattern.quote(attribute) + "=\"([^\"]+)\"[^>]*" + Pattern.quote(identifier), Pattern.CASE_INSENSITIVE);
        Matcher matcherAlt = patternAlt.matcher(html);
        if (matcherAlt.find()) return matcherAlt.group(1);

        return null;
    }

    private String extractScriptContent(String html, String scriptId) {
        Pattern pattern = Pattern.compile("<script[^>]*id=\"" + Pattern.quote(scriptId) + "\"[^>]*>(.*?)</script>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(html);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }
}
