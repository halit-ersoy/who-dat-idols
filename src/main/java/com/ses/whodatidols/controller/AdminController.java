package com.ses.whodatidols.controller;

import com.ses.whodatidols.model.Movie;
import com.ses.whodatidols.model.Series;
import com.ses.whodatidols.model.Person;
import com.ses.whodatidols.model.VideoSource;
import com.ses.whodatidols.repository.PersonRepository;
import com.ses.whodatidols.repository.VideoSourceRepository;
import com.ses.whodatidols.repository.CommentRepository;
import com.ses.whodatidols.viewmodel.CommentViewModel;
import com.ses.whodatidols.service.MovieService;
import com.ses.whodatidols.service.SeriesService;
import com.ses.whodatidols.service.TvMazeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.nio.file.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin")
public class AdminController {

    private final MovieService movieService;
    private final SeriesService seriesService;
    private final TvMazeService tvMazeService;
    private final JdbcTemplate jdbcTemplate;
    private final VideoSourceRepository videoSourceRepository;
    private final PersonRepository personRepository;
    private final CommentRepository commentRepository;
    private final com.ses.whodatidols.repository.HeroRepository heroRepository;

    @Value("${media.source.trailers.path}")
    private String trailersPath;

    @Value("${media.source.trailers.path}")
    private String heroVideosPath;

    @Value("${media.source.upcoming.path}")
    private String upcomingPath;

    @Value("${media.source.movies.path}")
    private String moviesPath;

    @Value("${media.source.soap_operas.path}")
    private String soapOperasPath;

    @Autowired
    public AdminController(MovieService movieService, SeriesService seriesService,
            TvMazeService tvMazeService, JdbcTemplate jdbcTemplate,
            VideoSourceRepository videoSourceRepository, PersonRepository personRepository,
            CommentRepository commentRepository, com.ses.whodatidols.repository.HeroRepository heroRepository) {
        this.movieService = movieService;
        this.seriesService = seriesService;
        this.tvMazeService = tvMazeService;
        this.jdbcTemplate = jdbcTemplate;
        this.videoSourceRepository = videoSourceRepository;
        this.personRepository = personRepository;
        this.commentRepository = commentRepository;
        this.heroRepository = heroRepository;
    }

    @GetMapping("/panel")
    public ResponseEntity<Resource> getAdminPanel() {
        try {
            Resource htmlPage = new ClassPathResource("static/panel/html/panel.html");
            if (!htmlPage.exists()) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML_VALUE)
                    .body(htmlPage);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping
    public ResponseEntity<Void> redirectToPanel() {
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, "/admin/panel")
                .build();
    }

    @GetMapping("/movies")
    public ResponseEntity<List<Movie>> getMovies() {
        return ResponseEntity.ok(movieService.getAllMovies());
    }

    @GetMapping("/series")
    public ResponseEntity<List<Series>> getSeriesList() {
        return ResponseEntity.ok(seriesService.getAllSeries());
    }

    @GetMapping("/series/check")
    public ResponseEntity<Map<String, Boolean>> checkSeriesExists(@RequestParam("name") String name) {
        boolean exists = seriesService.findSeriesByName(name) != null;
        return ResponseEntity.ok(Map.of("exists", exists));
    }

    @PostMapping("/add-movie")
    public ResponseEntity<String> addMovie(
            @ModelAttribute Movie movie,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "image", required = false) MultipartFile image,
            @RequestParam(value = "imageUrl", required = false) String imageUrl,
            @RequestParam("summary") String summary,
            @RequestParam(value = "country", required = false) String country) {
        try {
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body("Film dosyası seçilmelidir.");
            }
            movie.setCountry(country);
            movieService.saveMovieWithFile(movie, file, image, summary);

            if ((image == null || image.isEmpty()) && imageUrl != null && !imageUrl.isEmpty()) {
                movieService.saveImageFromUrl(movie.getId(), imageUrl);
            }

            return ResponseEntity.ok("{\"id\": \"" + movie.getId() + "\", \"message\": \"Film başarıyla işlendi.\"}");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Hata: " + e.getMessage());
        }
    }

    @PostMapping("/update-movie")
    public ResponseEntity<String> updateMovie(
            @ModelAttribute Movie movie,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "image", required = false) MultipartFile image,
            @RequestParam(value = "country", required = false) String country) {
        try {
            if (movie.getId() == null) {
                return ResponseEntity.badRequest().body("Film ID bulunamadı.");
            }
            movie.setCountry(country);
            movieService.updateMovie(movie, file, image);
            return ResponseEntity.ok("{\"id\": \"" + movie.getId() + "\", \"message\": \"Film güncellendi.\"}");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Güncelleme hatası: " + e.getMessage());
        }
    }

    @PostMapping("/update-series")
    public ResponseEntity<String> updateSeries(
            @ModelAttribute Series s,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "image", required = false) MultipartFile image,
            @RequestParam(value = "country", required = false) String country) {
        try {
            if (s.getId() == null)
                return ResponseEntity.badRequest().body("Dizi ID yok.");
            s.setCountry(country);
            seriesService.updateSeriesMetadata(s, file, image);
            return ResponseEntity.ok("{\"id\": \"" + s.getId() + "\", \"message\": \"Dizi güncellendi.\"}");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Hata: " + e.getMessage());
        }
    }

    @PostMapping("/add-series")
    public ResponseEntity<String> addSoapOpera(
            @ModelAttribute Series seriesInfo,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "image", required = false) MultipartFile image,
            @RequestParam(value = "imageUrl", required = false) String imageUrl,
            @RequestParam(value = "existingSeriesId", required = false) UUID existingSeriesId,
            @RequestParam(value = "summary", required = false) String summary,
            @RequestParam(value = "country", required = false) String country,
            @RequestParam("season") int season,
            @RequestParam("episode") int episode) {
        try {
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body("Bölüm dosyası seçilmelidir.");
            }

            if (existingSeriesId != null) {
                seriesInfo.setId(existingSeriesId);
            }

            seriesInfo.setSummary(summary);
            seriesInfo.setCountry(country);
            UUID episodeId = seriesService.saveEpisodeWithFile(seriesInfo, season, episode, file, image,
                    existingSeriesId);

            if (existingSeriesId == null && (image == null || image.isEmpty()) && imageUrl != null
                    && !imageUrl.isEmpty()) {
                Series parent = seriesService.findSeriesByName(seriesInfo.getName());
                if (parent != null) {
                    seriesService.saveImageFromUrl(parent.getId(), imageUrl);
                }
            }

            return ResponseEntity.ok("{\"id\": \"" + episodeId + "\", \"message\": \"Bölüm başarıyla işlendi (S"
                    + season + "E" + episode + ").\"}");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Hata: " + e.getMessage());
        }
    }

    @DeleteMapping("/delete-episode")
    public ResponseEntity<String> deleteEpisode(@RequestParam("id") UUID id) {
        try {
            seriesService.deleteEpisodeById(id);
            return ResponseEntity.ok("Bölüm silindi.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Silinemedi: " + e.getMessage());
        }
    }

    @PostMapping("/update-episode")
    public ResponseEntity<String> updateEpisode(
            @RequestParam("episodeId") UUID episodeId,
            @RequestParam("season") int season,
            @RequestParam("episodeNum") int episodeNum,
            @RequestParam(value = "file", required = false) MultipartFile file) {
        try {
            seriesService.updateEpisode(episodeId, season, episodeNum, file);
            return ResponseEntity.ok("{\"id\": \"" + episodeId + "\", \"message\": \"Bölüm güncellendi.\"}");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Bölüm güncelleme hatası: " + e.getMessage());
        }
    }

    @DeleteMapping("/delete-series-by-name")
    public ResponseEntity<String> deleteSeriesByName(@RequestParam("name") String name) {
        try {
            seriesService.deleteSeriesByName(name);
            return ResponseEntity.ok("Dizi komple silindi.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Silinemedi: " + e.getMessage());
        }
    }

    @DeleteMapping("/delete-movie")
    public ResponseEntity<String> deleteMovie(@RequestParam("id") UUID id) {
        try {
            movieService.deleteMovieById(id);
            return ResponseEntity.ok("Film başarıyla silindi.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Film silinirken hata oluştu: " + e.getMessage());
        }
    }

    @PostMapping("/delete-movies-bulk")
    public ResponseEntity<String> deleteMoviesBulk(@RequestBody List<UUID> ids) {
        try {
            for (UUID id : ids) {
                movieService.deleteMovieById(id);
            }
            return ResponseEntity.ok(ids.size() + " film başarıyla silindi.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Toplu silme hatası: " + e.getMessage());
        }
    }

    @PostMapping("/delete-series-bulk")
    public ResponseEntity<String> deleteSeriesBulk(@RequestBody List<UUID> ids) {
        try {
            for (UUID id : ids) {
                seriesService.deleteSeriesById(id);
            }
            return ResponseEntity.ok(ids.size() + " dizi başarıyla silindi.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Toplu silme hatası: " + e.getMessage());
        }
    }

    // HERO MANAGEMENT
    @GetMapping("/hero-videos")
    public ResponseEntity<List<Map<String, Object>>> getAdminHeroVideos() {
        return getHeroVideos();
    }

    private ResponseEntity<List<Map<String, Object>>> getHeroVideos() {
        return ResponseEntity.ok(heroRepository.getHeroVideos());
    }

    @Caching(evict = {
            @CacheEvict(value = "heroVideos", allEntries = true)
    })
    @PostMapping("/add-hero")
    public ResponseEntity<String> addHero(
            @RequestParam("contentId") UUID contentId,
            @RequestParam("type") String type,
            @RequestParam("content") String content,
            @RequestParam("file") MultipartFile file) {
        try {
            if ("Film".equalsIgnoreCase(type) || "Movie".equalsIgnoreCase(type)) {
                type = "Movie";
            } else {
                type = "SoapOpera";
            }

            UUID heroId = heroRepository.addHero(contentId, content, type);

            copyHeroImage(contentId, heroId, type);

            if (file != null && !file.isEmpty()) {
                Path uploadPath = Paths.get(heroVideosPath).toAbsolutePath().normalize();
                if (!Files.exists(uploadPath))
                    Files.createDirectories(uploadPath);
                Path filePath = uploadPath.resolve(heroId.toString() + ".mp4");
                Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            }

            return ResponseEntity.ok("Hero video başarıyla eklendi.");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Hata: " + e.getMessage());
        }
    }

    @CacheEvict(value = "heroVideos", allEntries = true)
    @PostMapping("/update-hero-order")
    public ResponseEntity<String> updateHeroOrder(@RequestBody List<String> heroIds) {
        try {
            heroRepository.updateHeroOrder(heroIds);
            return ResponseEntity.ok("Sıralama güncellendi.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Sıralama hatası: " + e.getMessage());
        }
    }

    @CacheEvict(value = "heroVideos", allEntries = true)
    @DeleteMapping("/delete-hero")
    public ResponseEntity<String> deleteHero(@RequestParam("id") UUID id) {
        try {
            Path uploadPath = Paths.get(heroVideosPath).toAbsolutePath().normalize();
            Files.deleteIfExists(uploadPath.resolve(id.toString() + ".mp4"));

            String[] extensions = { ".jpg", ".jpeg", ".png", ".webp" };
            for (String ext : extensions) {
                Files.deleteIfExists(uploadPath.resolve(id.toString() + ext));
            }

            heroRepository.deleteHero(id);

            return ResponseEntity.ok("Hero video ve ilgili dosyalar silindi.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Silme hatası: " + e.getMessage());
        }
    }

    @PostMapping("/add-source")
    public ResponseEntity<String> addSource(@RequestBody VideoSource source) {
        try {
            if (source.getContentId() == null) {
                return ResponseEntity.badRequest().body("Hata: ContentId boş olamaz.");
            }
            videoSourceRepository.save(source);
            return ResponseEntity.ok("Kaynak eklendi.");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Hata: " + e.getMessage());
        }
    }

    @DeleteMapping("/delete-source")
    public ResponseEntity<String> deleteSource(@RequestParam("id") UUID id) {
        try {
            videoSourceRepository.delete(id);
            return ResponseEntity.ok("Kaynak silindi.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Hata: " + e.getMessage());
        }
    }

    @DeleteMapping("/delete-sources-for-content")
    public ResponseEntity<String> deleteSourcesForContent(@RequestParam("contentId") UUID contentId) {
        try {
            videoSourceRepository.deleteAllForContent(contentId);
            return ResponseEntity.ok("Tüm kaynaklar silindi.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Hata: " + e.getMessage());
        }
    }

    private void copyHeroImage(UUID contentId, UUID heroId, String type) {
        try {
            Path sourceBase = Paths.get("Movie".equalsIgnoreCase(type) ? moviesPath : soapOperasPath).toAbsolutePath()
                    .normalize();
            Path targetBase = Paths.get(heroVideosPath).toAbsolutePath().normalize();

            String[] extensions = { ".jpg", ".jpeg", ".png", ".webp" };
            for (String ext : extensions) {
                Path sourceFile = sourceBase.resolve(contentId.toString() + ext);
                if (Files.exists(sourceFile)) {
                    Files.copy(sourceFile, targetBase.resolve(heroId.toString() + ext),
                            StandardCopyOption.REPLACE_EXISTING);
                    break;
                }
            }
        } catch (Exception e) {
            System.err.println("Hero resmi kopyalanamadı: " + e.getMessage());
        }
    }

    // UPCOMING MANAGEMENT
    @GetMapping("/upcoming")
    public ResponseEntity<List<Map<String, Object>>> getUpcoming() {
        return ResponseEntity.ok(jdbcTemplate.queryForList("EXEC GetUpcoming"));
    }

    @PostMapping("/add-upcoming")
    public ResponseEntity<String> addUpcoming(
            @RequestParam("name") String name,
            @RequestParam("type") String type,
            @RequestParam("category") String category,
            @RequestParam("status") String status,
            @RequestParam("datetime") String datetimeStr,
            @RequestParam(value = "referenceId", required = false) String referenceId,
            @RequestParam(value = "image", required = false) MultipartFile image,
            @RequestParam(value = "imageUrl", required = false) String imageUrl) {
        try {
            java.sql.Timestamp timestamp = null;
            if (datetimeStr != null && !datetimeStr.isEmpty()) {
                String formatted = datetimeStr.replace("T", " ");
                if (formatted.length() == 16) {
                    formatted += ":00";
                }
                timestamp = java.sql.Timestamp.valueOf(formatted);
            }

            UUID uuid = null;
            if (referenceId != null && !referenceId.isEmpty()) {
                uuid = UUID.fromString(referenceId);
            }

            UUID upcomingId = UUID.randomUUID();

            jdbcTemplate.update("EXEC AddUpcomingCustom ?, ?, ?, ?, ?, ?, ?",
                    upcomingId,
                    name,
                    type,
                    status,
                    timestamp,
                    uuid,
                    category);

            if (image != null && !image.isEmpty()) {
                saveUpcomingImage(upcomingId, image);
            } else if (imageUrl != null && !imageUrl.isEmpty()) {
                saveUpcomingImageFromUrl(upcomingId, imageUrl);
            }

            return ResponseEntity.ok("Beklenen bölüm eklendi.");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Ekleme hatası: " + e.getMessage());
        }
    }

    private void saveUpcomingImage(UUID id, MultipartFile image) {
        try {
            Path targetPath = Paths.get(upcomingPath).toAbsolutePath().normalize();
            if (!Files.exists(targetPath)) {
                Files.createDirectories(targetPath);
            }
            String extension = ".jpg";
            String originalName = image.getOriginalFilename();
            if (originalName != null && originalName.lastIndexOf(".") > 0) {
                extension = originalName.substring(originalName.lastIndexOf("."));
            }
            Files.copy(image.getInputStream(), targetPath.resolve(id.toString() + extension),
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            System.err.println("Upcoming resmi kaydedilemedi: " + e.getMessage());
        }
    }

    private void saveUpcomingImageFromUrl(UUID id, String imageUrl) {
        try {
            byte[] imageBytes = tvMazeService.downloadImage(imageUrl);
            if (imageBytes != null) {
                Path targetPath = Paths.get(upcomingPath).toAbsolutePath().normalize().resolve(id.toString() + ".jpg");
                if (!Files.exists(targetPath.getParent())) {
                    Files.createDirectories(targetPath.getParent());
                }
                Files.write(targetPath, imageBytes);
            }
        } catch (Exception e) {
            System.err.println("URL'den upcoming resmi kaydedilemedi: " + e.getMessage());
        }
    }

    @DeleteMapping("/delete-upcoming")
    public ResponseEntity<String> deleteUpcoming(@RequestParam("id") UUID id) {
        try {
            jdbcTemplate.update("EXEC DeleteUpcoming ?", id);
            return ResponseEntity.ok("Beklenen bölüm silindi.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Silme hatası: " + e.getMessage());
        }
    }

    @GetMapping("/view-stats")
    public ResponseEntity<List<Map<String, Object>>> getViewStats() {
        String sql = """
                SELECT ID, Name, 'Movie' as Type, viewCount FROM Movie
                UNION ALL
                SELECT ID, Name, 'SoapOpera' as Type, viewCount FROM Series
                ORDER BY viewCount DESC, Name ASC
                """;
        return ResponseEntity.ok(jdbcTemplate.queryForList(sql));
    }

    @PostMapping("/update-view-count")
    public ResponseEntity<String> updateViewCount(
            @RequestParam("id") UUID id,
            @RequestParam("type") String type,
            @RequestParam("count") int count) {
        try {
            String tableName = "Movie".equalsIgnoreCase(type) ? "Movie" : "Series";
            jdbcTemplate.update("UPDATE " + tableName + " SET viewCount = ? WHERE ID = ?", count, id.toString());
            return ResponseEntity.ok("İzlenme sayısı güncellendi.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Güncelleme hatası: " + e.getMessage());
        }
    }

    @GetMapping("/users")
    public ResponseEntity<List<Person>> getAllUsers() {
        return ResponseEntity.ok(personRepository.findAllUsers());
    }

    @PostMapping("/ban-user")
    public ResponseEntity<String> toggleBanUser(
            @RequestParam("userId") UUID userId,
            @RequestParam("ban") boolean ban,
            @RequestParam(value = "reason", required = false) String reason) {
        try {
            personRepository.toggleUserBanStatus(userId, ban, reason);
            return ResponseEntity.ok(ban ? "Kullanıcı banlandı." : "Kullanıcı banı kaldırıldı.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Hata: " + e.getMessage());
        }
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser(Authentication authentication) {
        if (authentication == null)
            return ResponseEntity.status(401).build();

        Map<String, Object> user = new HashMap<>();
        user.put("username", authentication.getName());
        user.put("roles", authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList()));

        return ResponseEntity.ok(user);
    }

    @PostMapping("/update-role")
    public ResponseEntity<String> updateUserRole(
            @RequestParam("userId") UUID userId,
            @RequestParam("role") String role) {
        try {
            personRepository.updateUserRole(userId, role);
            return ResponseEntity.ok("Kullanıcı rolü güncellendi.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Hata: " + e.getMessage());
        }
    }

    @DeleteMapping("/delete-user")
    public ResponseEntity<String> deleteUser(@RequestParam("userId") UUID userId) {
        try {
            personRepository.deleteUser(userId);
            return ResponseEntity.ok("Kullanıcı başarıyla silindi.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Hata: " + e.getMessage());
        }
    }

    // Comment Moderation Endpoints

    @GetMapping("/comments/pending")
    public ResponseEntity<List<CommentViewModel>> getPendingComments() {
        try {
            return ResponseEntity.ok(commentRepository.getPendingComments());
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    @PostMapping("/comments/approve")
    public ResponseEntity<String> approveComment(@RequestParam("commentId") UUID commentId) {
        try {
            commentRepository.approveComment(commentId);
            return ResponseEntity.ok("Yorum onaylandı.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Hata: " + e.getMessage());
        }
    }

    @DeleteMapping("/comments/reject")
    public ResponseEntity<String> rejectComment(@RequestParam("commentId") UUID commentId) {
        try {
            commentRepository.rejectComment(commentId);
            return ResponseEntity.ok("Yorum reddedildi/silindi.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Hata: " + e.getMessage());
        }
    }
}