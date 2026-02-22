package com.ses.whodatidols.controller;

import com.ses.whodatidols.model.Episode;
import com.ses.whodatidols.model.Movie;
import com.ses.whodatidols.model.Person;
import com.ses.whodatidols.model.Series;
import com.ses.whodatidols.repository.PersonRepository;
import com.ses.whodatidols.service.MovieService;
import com.ses.whodatidols.service.SeriesService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Controller
public class HomeController {
    private final PersonRepository personRepository;
    private final MovieService movieService;
    private final SeriesService seriesService;

    public HomeController(PersonRepository personRepository, MovieService movieService,
            SeriesService seriesService) {
        this.personRepository = personRepository;
        this.movieService = movieService;
        this.seriesService = seriesService;
    }

    @GetMapping("/")
    public ResponseEntity<Resource> getIndex() {
        try {
            Resource htmlPage = new ClassPathResource("static/homepage/html/index.html");
            if (!htmlPage.exists()) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML_VALUE)
                    .body(htmlPage);
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    @Cacheable("featuredContent")
    @GetMapping("/api/featured-content")
    @ResponseBody
    public ResponseEntity<FeaturedContentResponse> getFeaturedContent() {
        FeaturedContentResponse response = new FeaturedContentResponse();

        // 1. Movies (20 adet - Recently Added)
        List<Movie> movies = movieService.getRecentMovies(20);
        List<FeaturedItem> movieItems = new java.util.ArrayList<>();
        for (Movie m : movies) {
            FeaturedItem item = new FeaturedItem();
            item.id = m.getSlug() != null ? m.getSlug() : m.getId().toString();
            item.title = m.getName();
            item.image = "/media/image/" + m.getId();

            // Use explicit country if available, otherwise fallback to language mapping
            if (m.getCountry() != null && !m.getCountry().isEmpty()) {
                item.country = m.getCountry();
            } else {
                item.country = mapLanguageToCode(m.getLanguage());
            }

            item.isNew = isRecent(m.getUploadDate());
            item.isFinal = false; // Filmler için genelde false
            item.season = null;
            item.episode = null;

            movieItems.add(item);
        }
        response.movies = movieItems;

        // 2. Series (Bölüm bazlı - Condensing applied)
        // Fetch more to ensure diversity after condensing
        List<Episode> rawEpisodes = seriesService.getRecentEpisodesWithMetadata(1000);
        List<Episode> condensedEpisodes = condenseConsecutiveEpisodes(rawEpisodes);

        List<FeaturedItem> tvItems = new java.util.ArrayList<>();
        int tvCount = 0;
        for (Episode ep : condensedEpisodes) {
            if (tvCount >= 20)
                break;

            FeaturedItem item = new FeaturedItem();
            item.id = ep.getSlug() != null ? ep.getSlug() : ep.getId().toString(); // Item ID is slug or Episode ID

            String title = ep.getName();
            String language = "kr";
            String country = null;
            boolean isFinal = false;
            String imageId = ep.getId().toString();

            // Fetch Series Metadata (Language, Final Status, Name)
            if (ep.getSeriesId() != null) {
                Series series = seriesService.getSeriesById(ep.getSeriesId());
                if (series != null) {
                    title = series.getName();
                    language = series.getLanguage();
                    country = series.getCountry();
                    isFinal = series.getFinalStatus() == 1;
                    imageId = series.getId().toString();
                }
            }

            item.title = title;
            item.image = "/media/image/" + imageId;

            // Use explicit country if available, otherwise fallback to language mapping
            if (country != null && !country.isEmpty()) {
                item.country = country;
            } else {
                item.country = mapLanguageToCode(language);
            }

            item.isNew = true; // Zaten son eklenenler listesi
            item.isFinal = isFinal;
            item.season = ep.getSeasonNumber();
            item.episode = ep.getEpisodeNumber();

            tvItems.add(item);
            tvCount++;
        }
        response.tv = tvItems;

        return ResponseEntity.ok(response);
    }

    /**
     * Condenses contiguous blocks of episodes from the same series.
     * Only the first (most recent) and last (oldest in the run) are kept.
     */
    private List<Episode> condenseConsecutiveEpisodes(List<Episode> episodes) {
        if (episodes == null || episodes.isEmpty())
            return episodes;

        List<Episode> result = new java.util.ArrayList<>();
        int i = 0;
        int n = episodes.size();

        while (i < n) {
            Episode current = episodes.get(i);
            UUID seriesId = current.getSeriesId();

            if (seriesId == null) {
                result.add(current);
                i++;
                continue;
            }

            // Find contiguous block end
            int j = i + 1;
            while (j < n) {
                Episode next = episodes.get(j);
                if (seriesId.equals(next.getSeriesId())) {
                    j++;
                } else {
                    break;
                }
            }

            // Kepp first of run
            result.add(episodes.get(i));

            // If block has more than 1, keep last of run too
            if (j - i > 1) {
                result.add(episodes.get(j - 1));
            }

            i = j;
        }
        return result;
    }

    private String mapLanguageToCode(String lang) {
        if (lang == null)
            return "kr"; // Varsayılan
        lang = lang.toLowerCase();
        if (lang.contains("kore") || lang.contains("korea"))
            return "kr";
        if (lang.contains("japon") || lang.contains("japan"))
            return "jp";
        if (lang.contains("çin") || lang.contains("china"))
            return "cn";
        if (lang.contains("tayland") || lang.contains("thai"))
            return "th";
        if (lang.contains("tayvan") || lang.contains("taiwan"))
            return "tw";
        if (lang.contains("filipin") || lang.contains("phili"))
            return "ph";
        if (lang.contains("endonez") || lang.contains("indon"))
            return "id";
        if (lang.contains("malez") || lang.contains("malay"))
            return "my";
        if (lang.contains("singa"))
            return "sg";
        if (lang.contains("lehçe") || lang.contains("polon") || lang.contains("poland"))
            return "pl";
        return "kr"; // Bilinmiyorsa Kore varsay
    }

    private boolean isRecent(java.time.LocalDateTime date) {
        if (date == null)
            return false;
        // 7 günden yeni mi?
        return date.isAfter(java.time.LocalDateTime.now().minusDays(7));
    }

    // DTO Classes
    public static class FeaturedContentResponse {
        public List<FeaturedItem> movies;
        public List<FeaturedItem> tv;
    }

    public static class FeaturedItem {
        public String id;
        public String title;
        public Integer season;
        public Integer episode;
        public boolean isNew;
        public boolean isFinal;
        public String image;
        public String country;
    }

    @GetMapping("/about")
    public ResponseEntity<Resource> getAboutPage() {
        try {
            Resource htmlPage = new ClassPathResource("static/about/html/about.html");
            if (!htmlPage.exists()) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML_VALUE)
                    .body(htmlPage);
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping("/privacy_policy")
    public ResponseEntity<Resource> getPrivacyPolicyPage() {
        try {
            Resource htmlPage = new ClassPathResource("static/privacy_policy/html/privacy_policy.html");
            if (!htmlPage.exists()) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML_VALUE)
                    .body(htmlPage);
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping("/terms_of_use")
    public ResponseEntity<Resource> getTermsOfUsePage() {
        try {
            Resource htmlPage = new ClassPathResource("static/terms_of_use/html/terms_of_use.html");
            if (!htmlPage.exists()) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML_VALUE)
                    .body(htmlPage);
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping("/sss")
    public ResponseEntity<Resource> getFAQPage() {
        try {
            Resource htmlPage = new ClassPathResource("static/sss/html/sss.html");
            if (!htmlPage.exists()) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML_VALUE)
                    .body(htmlPage);
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping("/{slug:[a-zA-Z0-9-]+}")
    public ResponseEntity<Resource> watchPageBySlug(@PathVariable("slug") String slug) {
        // Exclude static paths from matching here, though Spring usually handles static
        // prior to @PathVariable
        if (slug.equals("about") || slug.equals("privacy_policy") || slug.equals("terms_of_use") || slug.equals("sss")
                || slug.equals("favorites") || slug.equals("profile") || slug.equals("login-admin")
                || slug.equals("settings") || slug.equals("coming-soon") || slug.equals("programlar")
                || slug.equals("diziler") || slug.equals("bl-dizileri") || slug.equals("watch")) {
            return ResponseEntity.notFound().build();
        }

        try {
            ClassPathResource htmlPage = new ClassPathResource("static/watch/html/watch.html");
            if (!htmlPage.exists()) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML_VALUE)
                    .body(htmlPage);
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    @PostMapping("/register")
    @ResponseBody
    public ResponseEntity<?> registerUser(@RequestBody Person person) {
        try {
            // 1. Mandatory Fields Validation
            if (person.getName() == null || person.getName().trim().isEmpty() ||
                    person.getSurname() == null || person.getSurname().trim().isEmpty() ||
                    person.getNickname() == null || person.getNickname().trim().isEmpty() ||
                    person.getEmail() == null || person.getEmail().trim().isEmpty() ||
                    person.getPassword() == null || person.getPassword().trim().isEmpty()) {

                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "Lütfen tüm zorunlu alanları doldurunuz.");
                return ResponseEntity.badRequest().body(error);
            }

            // 2. Duplicate Check (Nickname & Email)
            Optional<Person> existingUser = personRepository.findByNicknameOrEmail(person.getNickname());
            if (existingUser.isPresent()) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "Bu kullanıcı adı zaten kullanımda.");
                return ResponseEntity.badRequest().body(error);
            }

            existingUser = personRepository.findByNicknameOrEmail(person.getEmail());
            if (existingUser.isPresent()) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "Bu e-posta adresi zaten kullanımda.");
                return ResponseEntity.badRequest().body(error);
            }

            // 3. Proceed to Register
            Map<String, Object> result = personRepository.registerUser(person);

            if ((Boolean) result.get("success")) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            errorResponse.put("success", "false");
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @PostMapping("/forgot-password")
    @ResponseBody
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> request) {
        try {
            String usernameOrEmail = request.get("usernameOrEmail");
            if (usernameOrEmail == null || usernameOrEmail.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Kullanıcı adı veya e-posta adresi gerekli"));
            }

            Map<String, Object> result = personRepository.generateResetCode(usernameOrEmail);
            boolean isSuccess = (boolean) result.get("Result");

            if (isSuccess) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Doğrulama kodu e-posta adresinize gönderildi"));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", result.get("Message")));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()));
        }
    }

    @PostMapping("/verify-code")
    @ResponseBody
    public ResponseEntity<?> verifyCode(@RequestBody Map<String, String> request) {
        try {
            String usernameOrEmail = request.get("usernameOrEmail");
            String code = request.get("code");

            Map<String, Object> result = personRepository.verifyResetCode(usernameOrEmail, code);
            boolean isSuccess = (boolean) result.get("Result");

            return ResponseEntity.ok(Map.of(
                    "success", isSuccess,
                    "message", result.get("Message")));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()));
        }
    }

    @PostMapping("/reset-password")
    @ResponseBody
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> request) {
        try {
            String usernameOrEmail = request.get("usernameOrEmail");
            String code = request.get("code");
            String newPassword = request.get("newPassword");

            Map<String, Object> result = personRepository.resetPassword(usernameOrEmail, code, newPassword);
            boolean isSuccess = (boolean) result.get("Result");

            return ResponseEntity.ok(Map.of(
                    "success", isSuccess,
                    "message", result.get("Message")));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()));
        }
    }

    @PostMapping("/login")
    @ResponseBody
    public ResponseEntity<?> loginUser(@RequestBody Map<String, String> loginRequest, HttpServletResponse response) {
        try {
            String usernameOrEmail = loginRequest.get("usernameOrEmail");
            if (usernameOrEmail == null) {
                usernameOrEmail = loginRequest.get("email");
            }
            String password = loginRequest.get("password");

            Map<String, Object> result = personRepository.loginUser(usernameOrEmail, password);

            Object successObj = result.get("success");
            boolean isSuccess = false;
            if (successObj instanceof Boolean) {
                isSuccess = (Boolean) successObj;
            } else if (successObj instanceof Number) {
                isSuccess = ((Number) successObj).intValue() == 1;
            }

            if (isSuccess) {
                Object cookieObj = result.get("cookie");
                if (cookieObj != null) {
                    Cookie authCookie = new Cookie("wdiAuth", cookieObj.toString());
                    authCookie.setHttpOnly(true);
                    authCookie.setPath("/");
                    authCookie.setMaxAge(7 * 24 * 60 * 60);
                    authCookie.setSecure(true);
                    response.addCookie(authCookie);
                }
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.status(401).body(result);
            }
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @GetMapping("/login-admin")
    public ResponseEntity<Resource> getAdminLoginPage() {
        try {
            Resource htmlPage = new ClassPathResource("static/panel/html/login-admin.html");
            if (!htmlPage.exists()) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML_VALUE)
                    .body(htmlPage);
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping("/settings")
    public ResponseEntity<Resource> getSettingsPage() {
        try {
            Resource htmlPage = new ClassPathResource("static/settings/html/settings.html");
            if (!htmlPage.exists()) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML_VALUE)
                    .body(htmlPage);
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping("/api/user/profile")
    @ResponseBody
    public ResponseEntity<?> getUserProfile(@CookieValue(name = "wdiAuth", required = false) String cookie) {
        if (cookie == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Unauthorized"));
        }

        try {
            Map<String, Object> result = personRepository.getUserInfoByCookie(cookie);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/api/user/update-password")
    @ResponseBody
    public ResponseEntity<?> updatePassword(
            @CookieValue(name = "wdiAuth", required = false) String cookie,
            @RequestBody Map<String, String> request) {
        if (cookie == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Unauthorized"));
        }

        String newPassword = request.get("newPassword");
        if (newPassword == null || newPassword.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Password is required"));
        }

        try {
            Map<String, Object> result = personRepository.updatePasswordByCookie(cookie, newPassword);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/api/user/update-profile")
    @ResponseBody
    public ResponseEntity<?> updateProfile(
            @CookieValue(name = "wdiAuth", required = false) String cookie,
            @RequestBody Map<String, String> request) {
        if (cookie == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Unauthorized"));
        }

        try {
            String newNickname = request.get("nickname");
            String newName = request.get("name");
            String newSurname = request.get("surname");
            String newEmail = request.get("email");

            // 1. Mandatory Fields Validation
            if (newNickname == null || newNickname.trim().isEmpty() ||
                    newName == null || newName.trim().isEmpty() ||
                    newSurname == null || newSurname.trim().isEmpty() ||
                    newEmail == null || newEmail.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Lütfen tüm zorunlu alanları doldurunuz."));
            }

            newNickname = newNickname.trim();
            newName = newName.trim();
            newSurname = newSurname.trim();
            newEmail = newEmail.trim();

            // 2. Fetch current user info to check for changes
            Map<String, Object> currentInfo = personRepository.getUserInfoByCookie(cookie);
            String currentNickname = (String) currentInfo.get("nickname");
            String currentEmail = (String) currentInfo.get("email");

            // 3. Duplicate Checks (only if changed)
            if (currentNickname != null && !newNickname.equalsIgnoreCase(currentNickname)) {
                Optional<Person> existing = personRepository.findByNicknameOrEmail(newNickname);
                if (existing.isPresent()) {
                    return ResponseEntity.badRequest()
                            .body(Map.of("success", false, "message", "Bu kullanıcı adı zaten kullanımda."));
                }
            }

            if (currentEmail != null && !newEmail.equalsIgnoreCase(currentEmail)) {
                Optional<Person> existing = personRepository.findByNicknameOrEmail(newEmail);
                if (existing.isPresent()) {
                    return ResponseEntity.badRequest()
                            .body(Map.of("success", false, "message", "Bu e-posta adresi zaten kullanımda."));
                }
            }

            Map<String, Object> result = personRepository.updateProfileByCookie(
                    cookie, newNickname, newName, newSurname, newEmail);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/favorites")
    public ResponseEntity<Resource> getFavoritesPage() {
        try {
            Resource htmlPage = new ClassPathResource("static/favorites/html/favorites.html");
            if (!htmlPage.exists()) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML_VALUE)
                    .body(htmlPage);
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping("/profile")
    public ResponseEntity<Resource> getProfilePage() {
        try {
            Resource htmlPage = new ClassPathResource("static/profile/html/profile.html");
            if (!htmlPage.exists()) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML_VALUE)
                    .body(htmlPage);
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping({ "/coming-soon", "/programlar", "/diziler", "/bl-dizileri" })
    public ResponseEntity<Resource> getComingSoonPage() {
        try {
            Resource htmlPage = new ClassPathResource("static/coming-soon/html/coming-soon.html");
            if (!htmlPage.exists()) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML_VALUE)
                    .body(htmlPage);
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

}
