package com.ses.whodatidols.controller;

import com.ses.whodatidols.model.Episode;
import com.ses.whodatidols.model.Movie;
import com.ses.whodatidols.model.Person;
import com.ses.whodatidols.model.Series;
import com.ses.whodatidols.repository.MovieRepository;
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

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

import com.ses.whodatidols.viewmodel.PageResponse;

@Controller
public class HomeController {
    private final PersonRepository personRepository;
    private final MovieService movieService;
    private final SeriesService seriesService;
    private final MovieRepository movieRepository;

    @org.springframework.beans.factory.annotation.Value("${media.profile.images.path}")
    private String profileImagesPath;

    public HomeController(PersonRepository personRepository, MovieService movieService,
            SeriesService seriesService, MovieRepository movieRepository) {
        this.personRepository = personRepository;
        this.movieService = movieService;
        this.seriesService = seriesService;
        this.movieRepository = movieRepository;
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

    @Cacheable(value = "featuredMovies", key = "#page + '-' + #size")
    @GetMapping("/api/featured-content/movies")
    @ResponseBody
    public ResponseEntity<PageResponse<FeaturedItem>> getFeaturedMoviesPaged(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {

        int offset = (page - 1) * size;
        List<Movie> movies = movieRepository.findRecentMoviesPaged(offset, size);
        int totalElements = movieRepository.countAllMovies();
        int totalPages = (int) Math.ceil((double) totalElements / size);

        List<FeaturedItem> movieItems = new java.util.ArrayList<>();
        for (Movie m : movies) {
            FeaturedItem item = new FeaturedItem();
            item.id = m.getSlug() != null ? m.getSlug() : m.getId().toString();
            item.title = m.getName();
            item.image = "/media/image/" + m.getId();

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

        PageResponse<FeaturedItem> response = new PageResponse<>(movieItems, totalPages, page, totalElements);
        return ResponseEntity.ok(response);
    }

    @Cacheable(value = "featuredTv", key = "#page + '-' + #size")
    @GetMapping("/api/featured-content/tv")
    @ResponseBody
    public ResponseEntity<PageResponse<FeaturedItem>> getFeaturedTvPaged(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {

        // Fetch more to ensure diversity after condensing
        List<Episode> rawEpisodes = seriesService.getRecentEpisodesWithMetadata(1000);
        List<Episode> condensedEpisodes = condenseConsecutiveEpisodes(rawEpisodes);

        int totalElements = condensedEpisodes.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);

        int offset = (page - 1) * size;
        List<Episode> paginatedEpisodes = condensedEpisodes.stream()
                .skip(offset)
                .limit(size)
                .collect(java.util.stream.Collectors.toList());

        List<FeaturedItem> tvItems = new java.util.ArrayList<>();
        for (Episode ep : paginatedEpisodes) {
            FeaturedItem item = new FeaturedItem();
            item.id = ep.getSlug() != null ? ep.getSlug() : ep.getId().toString();

            String title = ep.getName();
            String language = "kr";
            String country = null;
            boolean isFinal = false;
            String imageId = ep.getId().toString();

            if (ep.getSeriesId() != null) {
                Series series = seriesService.getSeriesById(ep.getSeriesId());
                if (series != null) {
                    title = series.getName();
                    language = series.getLanguage();
                    country = series.getCountry();
                    imageId = series.getId().toString();
                    item.seriesType = series.getSeriesType();

                    UUID latestEpisodeId = seriesService.getLatestEpisodeIdBySeriesId(ep.getSeriesId());
                    if (latestEpisodeId != null && latestEpisodeId.equals(ep.getId())) {
                        isFinal = series.getFinalStatus() > 0;
                        item.finalStatus = series.getFinalStatus();
                    } else {
                        isFinal = false;
                        item.finalStatus = 0;
                    }
                }
            }

            item.title = title;
            item.image = "/media/image/" + imageId;

            if (country != null && !country.isEmpty()) {
                item.country = country;
            } else {
                item.country = mapLanguageToCode(language);
            }

            item.isNew = isRecent(ep.getUploadDate());
            item.isFinal = isFinal;
            item.season = ep.getSeasonNumber();
            item.episode = ep.getEpisodeNumber();

            tvItems.add(item);
        }

        PageResponse<FeaturedItem> response = new PageResponse<>(tvItems, totalPages, page, totalElements);
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

    private boolean isRecent(Instant uploadDate) {
        if (uploadDate == null)
            return false;
        return uploadDate.isAfter(Instant.now().minus(7, ChronoUnit.DAYS));
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
        public int finalStatus;
        public String seriesType;
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
        // Validate slug existence in Series, Movies, or Episodes
        boolean isValid = seriesService.isValidSlug(slug) || movieService.isValidSlug(slug);
        if (!isValid) {
            return ResponseEntity.status(302)
                    .header("Location", "/")
                    .build();
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

            // 1.2 Password Length Validation
            if (person.getPassword().length() < 6) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "Şifre en az 6 karakter olmalıdır.");
                return ResponseEntity.badRequest().body(error);
            }

            // 1.3 Email Format Validation
            if (!person.getEmail().matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "Geçersiz e-posta formatı!");
                return ResponseEntity.badRequest().body(error);
            }

            // 1.5 Username Validation (Regex: Letters, Numbers, Underscore, Dot)
            if (!person.getNickname().matches("^[a-zA-Z0-9_.]+$")) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "Kullanıcı adı sadece harf, rakam, alt çizgi (_) ve nokta (.) içerebilir.");
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

            if (newPassword == null || newPassword.length() < 6) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Şifre çok kısa! En az 6 karakter gereklidir.");
                return ResponseEntity.badRequest().body(response);
            }

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

            // 1.5 Username Validation
            if (!newNickname.matches("^[a-zA-Z0-9_.]+$")) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message",
                                "Kullanıcı adı sadece harf, rakam, alt çizgi (_) ve nokta (.) içerebilir."));
            }

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

    @PostMapping("/api/user/request-verification")
    @ResponseBody
    public ResponseEntity<?> requestVerification(@CookieValue(name = "wdiAuth", required = false) String cookie) {
        if (cookie == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Unauthorized"));
        }
        try {
            Map<String, Object> userInfo = personRepository.getUserInfoByCookie(cookie);
            String email = (String) userInfo.get("email");

            if (email == null || email.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "E-posta adresi bulunamadı."));
            }

            Map<String, Object> result = personRepository
                    .generateEmailVerificationCode((String) userInfo.get("nickname"));
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/api/user/verify-email")
    @ResponseBody
    public ResponseEntity<?> verifyEmail(@CookieValue(name = "wdiAuth", required = false) String cookie,
            @RequestBody Map<String, String> request) {
        if (cookie == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Unauthorized"));
        }
        try {
            String code = request.get("code");
            if (code == null || code.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Kod gerekli."));
            }

            Map<String, Object> userInfo = personRepository.getUserInfoByCookie(cookie);
            String nickname = (String) userInfo.get("nickname");

            Map<String, Object> result = personRepository.verifyEmailCode(nickname, code.trim());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/api/user/upload-profile-photo")
    @ResponseBody
    public ResponseEntity<?> uploadProfilePhoto(
            @CookieValue(name = "wdiAuth", required = false) String cookie,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {

        if (cookie == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Oturum süresi dolmuş."));
        }

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Dosya seçilmedi."));
        }

        try {
            Map<String, Object> userInfo = personRepository.getUserInfoByCookie(cookie);
            if (userInfo == null || !userInfo.containsKey("ID")) {
                return ResponseEntity.status(401).body(Map.of("success", false, "message", "Kullanıcı bulunamadı."));
            }

            String userId = userInfo.get("ID").toString();
            BufferedImage originalImage = ImageIO.read(file.getInputStream());

            if (originalImage == null) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Geçersiz resim formatı."));
            }

            int width = originalImage.getWidth();
            int height = originalImage.getHeight();

            if (width < 300 || height < 300) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Resim boyutu en az 300x300 olmalıdır."));
            }

            BufferedImage finalImage = originalImage;
            if (width > 500 || height > 500) {
                int newWidth = 500;
                int newHeight = 500;
                Image resultingImage = originalImage.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);
                finalImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
                Graphics2D g2d = finalImage.createGraphics();
                g2d.drawImage(resultingImage, 0, 0, null);
                g2d.dispose();
            }

            Path uploadDir = Paths.get(profileImagesPath).toAbsolutePath().normalize();
            Files.createDirectories(uploadDir);

            Path targetLocation = uploadDir.resolve(userId + ".jpg");
            ImageIO.write(finalImage, "jpg", targetLocation.toFile());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Profil fotoğrafı güncellendi.",
                    "imageUrl", "/media/profile/" + userId + "?t=" + System.currentTimeMillis()));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500)
                    .body(Map.of("success", false, "message", "Yükleme sırasında bir hata oluştu: " + e.getMessage()));
        }
    }

    @DeleteMapping("/api/user/remove-profile-photo")
    @ResponseBody
    public ResponseEntity<?> removeProfilePhoto(@CookieValue(name = "wdiAuth", required = false) String cookie) {
        if (cookie == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Unauthorized"));
        }

        try {
            Map<String, Object> userInfo = personRepository.getUserInfoByCookie(cookie);
            String userId = userInfo.get("ID").toString();

            Path uploadDir = Paths.get(profileImagesPath).toAbsolutePath().normalize();
            Path targetLocation = uploadDir.resolve(userId + ".jpg");

            if (Files.exists(targetLocation)) {
                Files.delete(targetLocation);
            }

            return ResponseEntity.ok(Map.of("success", true, "message", "Fotoğraf kaldırıldı."));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500)
                    .body(Map.of("success", false, "message", "Silme sırasında bir hata oluştu: " + e.getMessage()));
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

    @GetMapping({ "/coming-soon", "/bl-dizileri" })
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

    @GetMapping({ "/programlar", "/diziler", "/filmler" })
    public ResponseEntity<Resource> getCategoryPage() {
        try {
            Resource htmlPage = new ClassPathResource("static/category/html/category.html");
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
