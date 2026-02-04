package com.ses.whodatidols.controller;

import com.ses.whodatidols.model.Movie;
import com.ses.whodatidols.model.Person;
import com.ses.whodatidols.model.SoapOpera;
import com.ses.whodatidols.repository.PersonRepository;
import com.ses.whodatidols.service.MovieService;
import com.ses.whodatidols.service.SoapOperaService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
public class HomeController {
    private final PersonRepository personRepository;
    private final MovieService movieService;
    private final SoapOperaService soapOperaService;

    public HomeController(PersonRepository personRepository, MovieService movieService,
            SoapOperaService soapOperaService) {
        this.personRepository = personRepository;
        this.movieService = movieService;
        this.soapOperaService = soapOperaService;
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

    // ... (Existing mappings can stay, skipping showing them all for efficiency if
    // not modified) ...

    @GetMapping("/api/featured-content")
    @ResponseBody
    public ResponseEntity<FeaturedContentResponse> getFeaturedContent() {
        FeaturedContentResponse response = new FeaturedContentResponse();

        // 1. Movies (20 adet)
        List<Movie> movies = movieService.getAllMovies(); // Veritabanından zaten sıralı geliyor
        List<FeaturedItem> movieItems = new java.util.ArrayList<>();
        int count = 0;
        for (Movie m : movies) {
            if (count >= 20)
                break;

            FeaturedItem item = new FeaturedItem();
            item.id = m.getId().toString();
            item.title = m.getName();
            item.image = "/media/image/" + m.getId(); // Resim endpointi
            item.country = mapLanguageToCode(m.getLanguage());
            item.isNew = isRecent(m.getUploadDate());
            item.isFinal = false; // Filmler için genelde false
            item.season = null;
            item.episode = null;

            movieItems.add(item);
            count++;
        }
        response.movies = movieItems;

        // 2. Series (20 adet - Bölüm bazlı)
        List<SoapOpera> episodes = soapOperaService.getRecentEpisodesWithMetadata(20);
        List<FeaturedItem> tvItems = new java.util.ArrayList<>();
        for (SoapOpera ep : episodes) {
            FeaturedItem item = new FeaturedItem();
            item.id = ep.getId().toString();
            item.title = ep.getName();
            // Resim Parent ID'den gelmeli. Service'de content alanına Parent ID'yi
            // koymuştuk.
            // Eğer content null ise kendi ID'sini dener ama genelde poster parent'tadır.
            String imgId = (ep.getContent() != null && !ep.getContent().isEmpty()) ? ep.getContent()
                    : ep.getId().toString();
            item.image = "/media/image/" + imgId;

            item.country = mapLanguageToCode(ep.getLanguage());
            item.isNew = true; // Zaten son eklenenler listesi
            item.isFinal = ep.getFinalStatus() == 1;
            item.season = ep.getSeasonNumber();
            item.episode = ep.getEpisodeNumber();

            tvItems.add(item);
        }
        response.tv = tvItems;

        return ResponseEntity.ok(response);
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

    @GetMapping("/watch")
    public ResponseEntity<Resource> watchPage(@RequestParam("id") UUID id) {
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
            // Call the new method that uses the stored procedure
            Map<String, Object> result = personRepository.registerUser(person);

            // Check if registration was successful
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
                usernameOrEmail = loginRequest.get("email"); // Fallback for some frontend versions
            }
            String password = loginRequest.get("password");

            Map<String, Object> result = personRepository.loginUser(usernameOrEmail, password);

            // SQL prosedürü Result sütununda bit (Boolean) döndürüyor, Repository bunu
            // 'success' anahtarına atadı.
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
                    authCookie.setMaxAge(7 * 24 * 60 * 60); // 7 gün
                    authCookie.setSecure(true); // HTTPS kullanıldığı için güvenli
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

}
