package com.ses.whodatidols.controller;

import com.ses.whodatidols.model.Movie;
import com.ses.whodatidols.model.SoapOpera;
import com.ses.whodatidols.service.MovieService; // Servisinizin olduğunu varsayıyorum
import com.ses.whodatidols.service.SoapOperaService; // Servisinizin olduğunu varsayıyorum
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
@RequestMapping("/admin") // Bu ön ek sayesinde SecurityConfig şifre soracak
public class AdminController {

    private final MovieService movieService;
    private final SoapOperaService soapOperaService;
    private final JdbcTemplate jdbcTemplate;

    @Value("${media.source.trailers.path}")
    private String heroVideosPath;

    @Value("${media.source.movies.path}")
    private String moviesPath;

    @Value("${media.source.soap_operas.path}")
    private String soapOperasPath;

    @Autowired
    public AdminController(MovieService movieService, SoapOperaService soapOperaService, JdbcTemplate jdbcTemplate) {
        this.movieService = movieService;
        this.soapOperaService = soapOperaService;
        this.jdbcTemplate = jdbcTemplate;
    }

    // İSTEDİĞİNİZ GET METODUNUN DÜZENLENMİŞ HALİ
    @GetMapping("/panel")
    public ResponseEntity<Resource> getAdminPanel() {
        System.out.println("Admin Panel requested!");
        try {
            // Dosya yolu: src/main/resources/static/admin/html/panel.html
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

    // 1. FİLM LİSTESİNİ ÇEKME (JSON)
    @GetMapping("/movies")
    public ResponseEntity<List<Movie>> getMovies() {
        return ResponseEntity.ok(movieService.getAllMovies());
    }

    @GetMapping("/series")
    public ResponseEntity<List<SoapOpera>> getSeriesList() {
        return ResponseEntity.ok(soapOperaService.getAllSeries());
    }

    @GetMapping("/series/check")
    public ResponseEntity<Map<String, Boolean>> checkSeriesExists(@RequestParam String name) {
        boolean exists = soapOperaService.findSeriesByName(name) != null;
        return ResponseEntity.ok(Map.of("exists", exists));
    }

    // FİLM KAYDETME ENDPOINT'İ
    @PostMapping("/add-movie")
    public ResponseEntity<String> addMovie(
            @ModelAttribute Movie movie,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "image", required = false) MultipartFile image,
            @RequestParam(value = "imageUrl", required = false) String imageUrl,
            @RequestParam("summary") String summary) { // Özet parametresi eklendi
        try {
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body("Film dosyası seçilmelidir.");
            }
            // Summary artık _content alanına işleniyor, Time otomatik hesaplanıyor
            movieService.saveMovieWithFile(movie, file, image, summary);

            // Eğer manuel resim yoksa ama URL varsa indir
            if ((image == null || image.isEmpty()) && imageUrl != null && !imageUrl.isEmpty()) {
                movieService.saveImageFromUrl(movie.getId(), imageUrl);
            }

            return ResponseEntity.ok("Film başarıyla işlendi. Süre otomatik hesaplandı Efendim.");
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
            // ID boş olamaz
            if (movie.getId() == null) {
                return ResponseEntity.badRequest().body("Film ID bulunamadı Efendim.");
            }

            movieService.updateMovie(movie, file, image);
            return ResponseEntity.ok("Film verileri ve video başarıyla revize edildi.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Güncelleme hatası: " + e.getMessage());
        }
    }

    @PostMapping("/update-series")
    public ResponseEntity<String> updateSeries(
            @ModelAttribute SoapOpera s,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "image", required = false) MultipartFile image) {
        try {
            if (s.getId() == null)
                return ResponseEntity.badRequest().body("Dizi ID yok.");
            soapOperaService.updateSeriesMetadata(s, file, image);
            return ResponseEntity.ok("Dizi bilgileri ve video güncellendi.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Hata: " + e.getMessage());
        }
    }

    // DİZİ KAYDETME ENDPOINT'İ
    @PostMapping("/add-series") // Endpoint adı eski HTML ile uyumlu kalsın
    public ResponseEntity<String> addSoapOpera(
            @ModelAttribute SoapOpera soapOpera,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "image", required = false) MultipartFile image,
            @RequestParam(value = "imageUrl", required = false) String imageUrl,
            @RequestParam(value = "existingSeriesId", required = false) UUID existingSeriesId, // YENİ PARAMETRE
            @RequestParam(value = "summary", required = false) String summary, // Mevcut seride zorunlu değil
            @RequestParam("season") int season,
            @RequestParam("episode") int episode) {
        try {
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body("Bölüm dosyası seçilmelidir.");
            }

            // Eğer mevcut bir dizi seçildiyse ID'yi set et
            if (existingSeriesId != null) {
                soapOpera.setId(existingSeriesId);
                // Mevcut diziye eklerken isim, kategori vb. serviste halledilecek veya boş
                // gelebilir
            }

            soapOpera.setContent(summary);
            soapOpera.setSeasonNumber(season);
            soapOpera.setEpisodeNumber(episode);

            // Servis metodunu güncelleyeceğiz: existingSeriesId var mı yok mu kontrolü
            // orada yapılacak
            soapOperaService.saveEpisodeWithFile(soapOpera, file, image, existingSeriesId);

            // Eğer manuel resim yoksa ama URL varsa indir (Parent dizi için)
            // Sadece YENİ bir dizi oluşturuluyorsa veya mevcut dizinin resmi yoksa
            // (opsiyonel)
            // Şimdilik sadece yeni dizi oluştururken URL varsa indirelim, mevcut dizinin
            // resmi zaten vardır.
            if (existingSeriesId == null && (image == null || image.isEmpty()) && imageUrl != null
                    && !imageUrl.isEmpty()) {
                SoapOpera parent = soapOperaService.findSeriesByName(soapOpera.getName());
                if (parent != null) {
                    soapOperaService.saveImageFromUrl(parent.getId(), imageUrl);
                }
            }

            return ResponseEntity.ok("Bölüm başarıyla işlendi (S" + season + "E" + episode + "). XML güncellendi.");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Hata: " + e.getMessage());
        }
    }

    // TEK BİR BÖLÜMÜ SİL
    @DeleteMapping("/delete-episode")
    public ResponseEntity<String> deleteEpisode(@RequestParam("id") UUID id) {
        try {
            // Servisinizde delete metodu olmalı. Yoksa repository.deleteById(id)
            // çağrılmalı.
            soapOperaService.deleteEpisodeById(id);
            return ResponseEntity.ok("Bölüm silindi.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Silinemedi: " + e.getMessage());
        }
    }

    // BİR DİZİNİN TAMAMINI SİL
    @DeleteMapping("/delete-series-by-name")
    public ResponseEntity<String> deleteSeriesByName(@RequestParam("name") String name) {
        try {
            // Servisinizde isme göre silme olmalı
            soapOperaService.deleteSeriesByName(name);
            return ResponseEntity.ok("Dizi komple silindi.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Silinemedi: " + e.getMessage());
        }
    }

    // FİLM SİL
    @DeleteMapping("/delete-movie")
    public ResponseEntity<String> deleteMovie(@RequestParam("id") UUID id) {
        try {
            movieService.deleteMovieById(id);
            return ResponseEntity.ok("Film başarıyla silindi.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Film silinirken hata oluştu: " + e.getMessage());
        }
    }

    // HERO YÖNETİMİ
    @GetMapping("/hero-videos")
    public ResponseEntity<List<Map<String, Object>>> getAdminHeroVideos() {
        return getHeroVideos();
    }

    private ResponseEntity<List<Map<String, Object>>> getHeroVideos() {
        // Ensure column exists for existing installations
        try {
            jdbcTemplate.execute(
                    "IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('HeroVideo') AND name = 'sortOrder') ALTER TABLE HeroVideo ADD sortOrder INT DEFAULT 0");
        } catch (Exception e) {
        }

        String robustSql = "SELECT * FROM (" +
                "  SELECT H.[ID], M.[name], M.[category], M.[_content], 'Movie' AS [type], H.sortOrder " +
                "  FROM HeroVideo H INNER JOIN Movie M ON H.referanceID = M.ID " +
                "  UNION ALL " +
                "  SELECT H.[ID], S.[name], S.[category], S.[_content], 'SoapOpera' AS [type], H.sortOrder " +
                "  FROM HeroVideo H INNER JOIN SoapOperas S ON H.referanceID = S.ID " +
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
            @RequestParam("file") MultipartFile file) {
        try {
            // First ensure the column exists (one-time check or migration)
            try {
                jdbcTemplate.execute("ALTER TABLE HeroVideo ADD sortOrder INT DEFAULT 0");
            } catch (Exception e) {
                // Column might already exist
            }

            Map<String, Object> content;
            if ("Film".equalsIgnoreCase(type) || "Movie".equalsIgnoreCase(type)) {
                content = jdbcTemplate.queryForMap("SELECT name, category, _content FROM Movie WHERE ID = ?",
                        contentId.toString());
                type = "Movie";
            } else {
                content = jdbcTemplate.queryForMap("SELECT name, category, _content FROM SoapOperas WHERE ID = ?",
                        contentId.toString());
                type = "SoapOpera";
            }

            // Execute the stored procedure AddHeroVideo which only takes referanceID
            jdbcTemplate.update("EXEC [dbo].[AddHeroVideo] @referanceID = ?", contentId);

            // Set the sortOrder to max + 1
            jdbcTemplate.update(
                    "UPDATE HeroVideo SET sortOrder = (SELECT ISNULL(MAX(sortOrder), 0) + 1 FROM HeroVideo) WHERE ID = (SELECT TOP 1 ID FROM HeroVideo WHERE referanceID = ? ORDER BY sortOrder DESC)",
                    contentId.toString());

            // Since we need the ID of the newly created HeroVideo for the file naming (if
            // it's not the same as contentId)
            String heroIdStr = jdbcTemplate.queryForObject(
                    "SELECT TOP 1 CAST(ID AS NVARCHAR(36)) FROM HeroVideo WHERE referanceID = ? ORDER BY sortOrder DESC",
                    String.class, contentId.toString());
            UUID heroId = UUID.fromString(heroIdStr);

            // Resim kopyalama (Hero videosu için içerik resmini kullanıyoruz)
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
                jdbcTemplate.update("UPDATE HeroVideo SET sortOrder = ? WHERE ID = ?", i, heroIds.get(i));
            }
            return ResponseEntity.ok("Sıralama güncellendi.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Sıralama hatası: " + e.getMessage());
        }
    }

    @DeleteMapping("/delete-hero")
    public ResponseEntity<String> deleteHero(@RequestParam("id") UUID id) {
        try {
            // HeroVideo tablosundan silmeden önce referans ID'yi veya direkt ID'yi
            // kullanarak dosyaları temizlemeliyiz.
            // Bu metodda gelen id HeroVideo'nun kendi ID'sidir.

            Path uploadPath = Paths.get(heroVideosPath).toAbsolutePath().normalize();

            // Video sil
            Files.deleteIfExists(uploadPath.resolve(id.toString() + ".mp4"));

            // Resimleri sil
            String[] extensions = { ".webp", ".jpg", ".jpeg", ".png" };
            for (String ext : extensions) {
                Files.deleteIfExists(uploadPath.resolve(id.toString() + ext));
            }

            jdbcTemplate.update("DELETE FROM HeroVideo WHERE ID = ?", id.toString());

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

            String[] extensions = { ".webp", ".jpg", ".jpeg", ".png" };
            for (String ext : extensions) {
                Path sourceFile = sourceBase.resolve(contentId.toString() + ext);
                if (Files.exists(sourceFile)) {
                    Files.copy(sourceFile, targetBase.resolve(heroId.toString() + ext),
                            StandardCopyOption.REPLACE_EXISTING);
                    break; // Sadece bir resmi kopyalamak yeterli
                }
            }
        } catch (Exception e) {
            System.err.println("Hero resmi kopyalanamadı: " + e.getMessage());
        }
    }
}