package com.ses.whodatidols.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/media")
public class MediaController {

    private static final Logger logger = LoggerFactory.getLogger(MediaController.class);
    private static final long VIDEO_CHUNK_SIZE = 1024 * 1024; // 1MB

    // Desteklenen resim formatları
    private static final String[] SUPPORTED_IMAGE_EXTENSIONS = {".webp", ".jpg", ".jpeg", ".png"};

    // --- PROPERTY DOSYASINDAN GELEN YOLLAR ---

    @Value("${media.source.movies.path}")
    private String moviesPath;

    @Value("${media.source.soap_operas.path}")
    private String soapOperasPath;

    @Value("${media.source.trailers.path}")
    private String trailersPath;

    @Value("${media.static.images.path}")
    private String staticImagesPath;

    private final JdbcTemplate jdbcTemplate;

    // Cache to avoid repeated content type probing
    private final Map<String, MediaType> mediaTypeCache = new ConcurrentHashMap<>();

    public MediaController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // --- 1. STATİK RESİM SUNUCUSU ---
    @GetMapping("/static/image/{imageName:.+}")
    public ResponseEntity<Resource> getStaticImage(@PathVariable String imageName) {
        logger.debug("Requesting static image: {}", imageName);

        try {
            // Güvenlik: Yol temizleme
            String sanitizedName = Paths.get(imageName).getFileName().toString();

            // Yapılandırmadaki yola göre hedefi belirle
            Path rootPath = Paths.get(staticImagesPath).toAbsolutePath().normalize();
            Path imagePath = rootPath.resolve(sanitizedName).normalize();

            // Güvenlik: Path Traversal (../) kontrolü
            if (!Files.exists(imagePath) || !imagePath.startsWith(rootPath)) {
                logger.warn("Static image not found or invalid path: {}", imagePath);
                return ResponseEntity.notFound().build();
            }

            return serveResource(imagePath);

        } catch (IOException e) {
            logger.error("Error serving static image {}: {}", imageName, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // --- 2. VİDEO STREAMING (Movies, Soap Operas, Trailers) ---
    @GetMapping("/video/{id}")
    public ResponseEntity<ResourceRegion> getVideo(
            @PathVariable UUID id,
            @RequestHeader HttpHeaders headers) {

        logger.debug("Requesting video for id={}", id);

        try {
            // Veritabanından içerik türünü (category) öğren
            String category = getContentTypeFromDatabase(id);

            // Kategoriye uygun ana klasörü seç
            Path basePath = getBasePathForCategory(category);

            if (basePath == null) {
                logger.warn("Unknown category '{}' for video id: {}", category, id);
                return ResponseEntity.notFound().build();
            }

            // Dosya yolunu oluştur
            Path videoPath = basePath.resolve(id + ".mp4").normalize().toAbsolutePath();

            // Dosya var mı ve güvenli klasörde mi kontrol et
            if (!Files.exists(videoPath) || !videoPath.startsWith(basePath)) {
                logger.warn("Video file not found or path traversal attempt: {}", videoPath);
                return ResponseEntity.notFound().build();
            }

            // Video kaynağını hazırla
            UrlResource videoResource = new UrlResource(videoPath.toUri());
            long contentLength = videoResource.contentLength();
            ResourceRegion region = getResourceRegion(videoResource, headers, contentLength);

            return ResponseEntity
                    .status(HttpStatus.PARTIAL_CONTENT)
                    .contentType(MediaTypeFactory.getMediaType(videoResource)
                            .orElse(MediaType.APPLICATION_OCTET_STREAM))
                    .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                    .body(region);

        } catch (IOException e) {
            logger.error("Error serving video for id {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // --- 3. DİNAMİK RESİM (Thumbnail/Poster) SUNUCUSU ---
    @GetMapping("/image/{id}")
    public ResponseEntity<Resource> getImage(@PathVariable UUID id) {
        logger.debug("Requesting image for id={}", id);

        try {
            // Veritabanından içerik türünü öğren
            String category = getContentTypeFromDatabase(id);

            // Kategoriye uygun ana klasörü seç
            Path basePath = getBasePathForCategory(category);

            if (basePath == null) {
                logger.warn("Unknown category '{}' for image id: {}", category, id);
                return ResponseEntity.notFound().build();
            }

            // İlgili klasörde resmi ara (.jpg, .png vs.)
            Path imagePath = findExistingImage(basePath, id);

            if (imagePath == null) {
                logger.warn("Image file not found for id: {}", id);
                return ResponseEntity.notFound().build();
            }

            // Resmi sun (1 günlük önbellek ile)
            return ResponseEntity.ok()
                    .contentType(determineMediaType(imagePath))
                    .cacheControl(CacheControl.maxAge(java.time.Duration.ofDays(1)))
                    .body(new UrlResource(imagePath.toUri()));

        } catch (IOException e) {
            logger.error("Error serving image for id {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // --- 4. İZLENME SAYISI ARTIRMA ---
    @PostMapping("/{id}/increment-view")
    public ResponseEntity<Void> incrementViewCount(@PathVariable UUID id) {
        try {
            // Stored Procedure çağrısı
            int updatedRows = jdbcTemplate.update("EXEC IncrementViewCount @ID = ?", id.toString());
            return (updatedRows > 0) ? ResponseEntity.ok().build() : ResponseEntity.noContent().build();
        } catch (Exception e) {
            logger.error("Failed to increment view count for id {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ================= YARDIMCI METODLAR =================

    /**
     * Veritabanından gelen kategori ismine göre (movie, soap_opera, trailer)
     * application.properties dosyasındaki doğru yolu eşleştirir.
     */
    private Path getBasePathForCategory(String category) {
        if (category == null) return null;

        // Veritabanından dönen string değerlerine göre eşleştirme
        // Not: DB'den dönen değerlerin 'movie', 'soap_opera', 'trailer' olduğu varsayılmıştır.
        switch (category.toLowerCase()) {
            case "movie":
                return Paths.get(moviesPath).toAbsolutePath().normalize();
            case "soap_opera": // DB'den 'soap-opera' veya 'soap_opera' dönebilir, kontrol edin
            case "soap-opera":
            case "series":
                return Paths.get(soapOperasPath).toAbsolutePath().normalize();
            case "trailer":
                return Paths.get(trailersPath).toAbsolutePath().normalize();
            default:
                return null;
        }
    }

    private String getContentTypeFromDatabase(UUID id) {
        try {
            // SQL Function: Geriye 'movie', 'soap_opera' vb. döndürmeli
            String sql = "SELECT [dbo].[GetContentTypeById](?);";
            return jdbcTemplate.queryForObject(sql, String.class, id.toString());
        } catch (Exception e) {
            logger.error("DB Error retrieving content type for id {}: {}", id, e.getMessage());
            return "unknown";
        }
    }

    private Path findExistingImage(Path basePath, UUID id) {
        // Temel klasörde ID + Uzantı kombinasyonlarını dener
        for (String ext : SUPPORTED_IMAGE_EXTENSIONS) {
            Path path = basePath.resolve(id + ext).normalize();
            if (Files.exists(path) && path.startsWith(basePath)) {
                return path;
            }
        }
        return null;
    }

    private ResponseEntity<Resource> serveResource(Path path) throws IOException {
        UrlResource resource = new UrlResource(path.toUri());
        return ResponseEntity.ok()
                .contentType(determineMediaType(path))
                .body(resource);
    }

    private ResourceRegion getResourceRegion(UrlResource resource, HttpHeaders headers, long contentLength) throws IOException {
        String range = headers.getFirst(HttpHeaders.RANGE);
        if (range == null || range.isEmpty()) {
            return new ResourceRegion(resource, 0, Math.min(VIDEO_CHUNK_SIZE, contentLength));
        }

        try {
            String[] ranges = range.replace("bytes=", "").split("-");
            long start = Long.parseLong(ranges[0]);
            if (start >= contentLength) return new ResourceRegion(resource, 0, Math.min(VIDEO_CHUNK_SIZE, contentLength));

            long end = ranges.length > 1 && !ranges[1].isEmpty()
                    ? Math.min(Long.parseLong(ranges[1]), contentLength - 1)
                    : contentLength - 1;

            return new ResourceRegion(resource, start, Math.min(VIDEO_CHUNK_SIZE, end - start + 1));
        } catch (NumberFormatException e) {
            return new ResourceRegion(resource, 0, Math.min(VIDEO_CHUNK_SIZE, contentLength));
        }
    }

    private MediaType determineMediaType(Path path) throws IOException {
        String pathStr = path.toString();
        if (mediaTypeCache.containsKey(pathStr)) return mediaTypeCache.get(pathStr);

        String contentType = Files.probeContentType(path);
        MediaType mediaType = (contentType != null) ? MediaType.parseMediaType(contentType) : MediaType.APPLICATION_OCTET_STREAM;

        // Fallback checks (Dosya uzantısına göre manuel belirleme)
        if (contentType == null) {
            String filename = path.getFileName().toString().toLowerCase();
            if (filename.endsWith(".mp4")) mediaType = MediaType.valueOf("video/mp4");
            else if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) mediaType = MediaType.IMAGE_JPEG;
            else if (filename.endsWith(".png")) mediaType = MediaType.IMAGE_PNG;
            else if (filename.endsWith(".webp")) mediaType = MediaType.valueOf("image/webp");
        }

        mediaTypeCache.put(pathStr, mediaType);
        return mediaType;
    }
}