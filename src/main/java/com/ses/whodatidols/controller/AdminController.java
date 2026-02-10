package com.ses.whodatidols.controller;

import com.ses.whodatidols.model.Movie;
import com.ses.whodatidols.model.Series;
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

import java.nio.file.*;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/admin")
public class AdminController {

    private final MovieService movieService;
    private final SeriesService seriesService;
    private final TvMazeService tvMazeService;
    private final JdbcTemplate jdbcTemplate;

    @Value("${media.source.trailers.path}")
    private String trailersPath;

    @Value("${media.source.trailers.path}")
    private String heroVideosPath;

    @Value("${media.source.upcoming.path}")
    private String upcomingPath;

    @Value("${media.source.movies.path}")
    private String moviesPath;

    @Value("${media.source.soap_operas.path}")
    private String soapOperasPath; // Keep prop name same for now

    @Autowired
    public AdminController(MovieService movieService, SeriesService seriesService,
            TvMazeService tvMazeService, JdbcTemplate jdbcTemplate) {
        this.movieService = movieService;
        this.seriesService = seriesService;
        this.tvMazeService = tvMazeService;
        this.jdbcTemplate = jdbcTemplate;
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
            @RequestParam("summary") String summary) {
        try {
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body("Film dosyası seçilmelidir.");
            }
            // Note: MovieService also likely needs updates for DB column names if it uses
            // raw SQL,
            // but we are focusing on Series/Episode/Hero for now. assuming Movie table was
            // NOT renamed?
            // Actually I did NOT rename Movie table.

            movieService.saveMovieWithFile(movie, file, image, summary);

            if ((image == null || image.isEmpty()) && imageUrl != null && !imageUrl.isEmpty()) {
                movieService.saveImageFromUrl(movie.getId(), imageUrl);
            }

            return ResponseEntity.ok("Film başarıyla işlendi.");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Hata: " + e.getMessage());
        }
    }

    @PostMapping("/update-movie")
    public ResponseEntity<String> updateMovie(
            @ModelAttribute Movie movie,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "image", required = false) MultipartFile image) {
        try {
            if (movie.getId() == null) {
                return ResponseEntity.badRequest().body("Film ID bulunamadı.");
            }
            movieService.updateMovie(movie, file, image);
            return ResponseEntity.ok("Film güncellendi.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Güncelleme hatası: " + e.getMessage());
        }
    }

    @PostMapping("/update-series")
    public ResponseEntity<String> updateSeries(
            @ModelAttribute Series s,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "image", required = false) MultipartFile image) {
        try {
            if (s.getId() == null)
                return ResponseEntity.badRequest().body("Dizi ID yok.");
            seriesService.updateSeriesMetadata(s, file, image);
            return ResponseEntity.ok("Dizi güncellendi.");
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
            // season/episode are passed separately now

            seriesService.saveEpisodeWithFile(seriesInfo, season, episode, file, image, existingSeriesId);

            if (existingSeriesId == null && (image == null || image.isEmpty()) && imageUrl != null
                    && !imageUrl.isEmpty()) {
                Series parent = seriesService.findSeriesByName(seriesInfo.getName());
                if (parent != null) {
                    seriesService.saveImageFromUrl(parent.getId(), imageUrl);
                }
            }

            return ResponseEntity.ok("Bölüm başarıyla işlendi (S" + season + "E" + episode + ").");
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
        // Ensure column exists for existing installations
        try {
            jdbcTemplate.execute(
                    "IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('Hero') AND name = 'sortOrder') ALTER TABLE Hero ADD sortOrder INT DEFAULT 0");
        } catch (Exception e) {
        }

        // Updated SQL for renamed tables and columns
        String robustSql = "SELECT * FROM (" +
                "  SELECT H.[ID], M.[name], M.[category], H.[CustomSummary] as _content, 'Movie' AS [type], H.sortOrder "
                +
                "  FROM Hero H INNER JOIN Movie M ON H.ReferenceId = M.ID " +
                "  UNION ALL " +
                "  SELECT H.[ID], S.[name], S.[category], H.[CustomSummary] as _content, 'SoapOpera' AS [type], H.sortOrder "
                +
                "  FROM Hero H INNER JOIN Series S ON H.ReferenceId = S.ID " +
                ") AS Result ORDER BY sortOrder ASC";

        return ResponseEntity.ok(jdbcTemplate.queryForList(robustSql));
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
            try {
                jdbcTemplate.execute("ALTER TABLE Hero ADD sortOrder INT DEFAULT 0");
            } catch (Exception e) {
            }

            if ("Film".equalsIgnoreCase(type) || "Movie".equalsIgnoreCase(type)) {
                jdbcTemplate.queryForMap("SELECT name, category, Summary FROM Movie WHERE ID = ?",
                        contentId.toString());
                type = "Movie";
            } else {
                // Query renamed Series table
                jdbcTemplate.queryForMap("SELECT name, category, Summary FROM Series WHERE ID = ?",
                        contentId.toString());
                type = "SoapOpera"; // Keep type string legacy for now or update?
                                    // Frontend likely sends/expects 'SoapOpera'
            }

            // Execute stored procedure - assuming it handles the new table name or we
            // update it?
            // Actually 'AddHeroVideo' stored proc likely inserts into 'HeroVideo' (old
            // name).
            // We renamed the table 'HeroVideo' to 'Hero'.
            // Stored procedure usually DOES NOT auto-update. We should use direct SQL or
            // update SP.
            // I'll use direct SQL insertion to avoid dependency on SP that might be broken.

            jdbcTemplate.update(
                    "INSERT INTO Hero (ID, ReferenceId, CustomSummary, sortOrder) VALUES (NEWID(), ?, ?, (SELECT ISNULL(MAX(sortOrder), 0) + 1 FROM Hero))",
                    contentId, content);

            // We need the ID of the inserted row
            String heroIdStr = jdbcTemplate.queryForObject(
                    "SELECT TOP 1 CAST(ID AS NVARCHAR(36)) FROM Hero WHERE ReferenceId = ? ORDER BY sortOrder DESC",
                    String.class, contentId.toString());
            UUID heroId = UUID.fromString(heroIdStr);

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

    @PostMapping("/update-hero-order")
    public ResponseEntity<String> updateHeroOrder(@RequestBody List<String> heroIds) {
        try {
            for (int i = 0; i < heroIds.size(); i++) {
                jdbcTemplate.update("UPDATE Hero SET sortOrder = ? WHERE ID = ?", i, heroIds.get(i));
            }
            return ResponseEntity.ok("Sıralama güncellendi.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Sıralama hatası: " + e.getMessage());
        }
    }

    @DeleteMapping("/delete-hero")
    public ResponseEntity<String> deleteHero(@RequestParam("id") UUID id) {
        try {
            Path uploadPath = Paths.get(heroVideosPath).toAbsolutePath().normalize();
            Files.deleteIfExists(uploadPath.resolve(id.toString() + ".mp4"));

            String[] extensions = { ".jpg", ".jpeg", ".png", ".webp" };
            for (String ext : extensions) {
                Files.deleteIfExists(uploadPath.resolve(id.toString() + ext));
            }

            jdbcTemplate.update("DELETE FROM Hero WHERE ID = ?", id.toString());

            return ResponseEntity.ok("Hero video ve ilgili dosyalar silindi.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Silme hatası: " + e.getMessage());
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
            String extension = ".jpg"; // Default
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
}