package com.ses.whodatidols.controller;

import com.ses.whodatidols.model.VideoSource;
import com.ses.whodatidols.repository.ContentRepository;
import com.ses.whodatidols.repository.VideoSourceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/media")
public class MediaController {

    private static final Logger logger = LoggerFactory.getLogger(MediaController.class);
    private static final long VIDEO_CHUNK_SIZE = 10 * 1024 * 1024; // 10MB

    // Desteklenen resim formatları
    public static final String[] SUPPORTED_IMAGE_EXTENSIONS = { ".jpg", ".jpeg", ".png", ".webp" };

    // --- PROPERTY DOSYASINDAN GELEN YOLLAR ---

    @Value("${media.source.movies.path}")
    private String moviesPath;

    @Value("${media.source.soap_operas.path}")
    private String soapOperasPath;

    @Value("${media.source.trailers.path}")
    private String trailersPath;

    @Value("${media.static.images.path}")
    private String staticImagesPath;

    @Value("${media.source.upcoming.path}")
    private String upcomingPath;

    private final JdbcTemplate jdbcTemplate;
    private final ContentRepository contentRepository;
    private final VideoSourceRepository videoSourceRepository;

    // Cache to avoid repeated content type probing
    private final Map<String, MediaType> mediaTypeCache = new ConcurrentHashMap<>();

    public MediaController(JdbcTemplate jdbcTemplate, ContentRepository contentRepository,
            VideoSourceRepository videoSourceRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.contentRepository = contentRepository;
        this.videoSourceRepository = videoSourceRepository;
    }

    // --- 1. STATİK RESİM SUNUCUSU ---
    @GetMapping("/static/image/{imageName:.+}")
    public ResponseEntity<Resource> getStaticImage(@PathVariable("imageName") String imageName) {
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

    // --- 2. VİDEO STREAMING (Movies, Soap Operas, Trailers, HLS) ---
    @SuppressWarnings("null")
    @GetMapping("/video/{id}")
    public ResponseEntity<ResourceRegion> getVideo(
            @PathVariable(name = "id") UUID id,
            @RequestHeader HttpHeaders headers) {

        logger.debug("Requesting video for id={}", id);

        try {
            String category = getContentTypeFromDatabase(id);
            Path basePath = getBasePathForCategory(category);

            if (basePath == null) {
                logger.warn("Unknown category '{}' for video id: {}", category, id);
                return ResponseEntity.notFound().build();
            }

            Path videoPath = basePath.resolve(id + ".mp4").normalize().toAbsolutePath();

            if (!Files.exists(videoPath) && "soap_opera".equalsIgnoreCase(category)) {
                UUID episodeId = findFirstEpisodeIdForSeries(id);
                if (episodeId != null) {
                    id = episodeId;
                    videoPath = basePath.resolve(id + ".mp4").normalize().toAbsolutePath();
                }
            }

            if (!Files.exists(videoPath) || !videoPath.startsWith(basePath)) {
                logger.warn("Video file not found: {} for id: {}", videoPath, id);
                return ResponseEntity.notFound().build();
            }

            UrlResource videoResource = new UrlResource(videoPath.toUri());
            long contentLength = videoResource.contentLength();
            ResourceRegion region = getResourceRegion(videoResource, headers, contentLength);

            MediaType mediaType = MediaTypeFactory.getMediaType(videoResource)
                    .orElse(MediaType.APPLICATION_OCTET_STREAM);

            return ResponseEntity
                    .status(HttpStatus.PARTIAL_CONTENT)
                    .contentType(mediaType)
                    .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                    .body(region);

        } catch (Exception e) {
            logger.error("Error serving video for id {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/video/{id}/sources")
    public ResponseEntity<List<VideoSource>> getSources(@PathVariable("id") UUID id) {
        try {
            List<VideoSource> sources = videoSourceRepository.findByContentId(id);
            if (sources == null)
                return ResponseEntity.ok(List.of());
            return ResponseEntity.ok(sources);
        } catch (Exception e) {
            logger.error("Error retrieving video sources for id {}: {}", id, e.getMessage());
            // Return empty list instead of 500 to keep UI functional
            return ResponseEntity.ok(List.of());
        }
    }

    @GetMapping("/video/{id}/playlist.m3u8")
    public ResponseEntity<Resource> getHlsPlaylist(@PathVariable("id") UUID id) {
        try {
            String category = getContentTypeFromDatabase(id);
            Path basePath = getBasePathForCategory(category);
            if (basePath == null)
                return ResponseEntity.notFound().build();

            // Try to find HLS directory: basePath/hls/{id}/playlist.m3u8
            Path hlsPath = basePath.resolve("hls").resolve(id.toString()).resolve("playlist.m3u8");

            if (!Files.exists(hlsPath)) {
                return ResponseEntity.notFound().build();
            }

            Resource resource = new UrlResource(hlsPath.toUri());
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("application/vnd.apple.mpegurl"))
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/video/{id}/{segment}.ts")
    public ResponseEntity<Resource> getHlsSegment(
            @PathVariable("id") UUID id,
            @PathVariable("segment") String segment) {
        try {
            String category = getContentTypeFromDatabase(id);
            Path basePath = getBasePathForCategory(category);
            if (basePath == null)
                return ResponseEntity.notFound().build();

            Path segmentPath = basePath.resolve("hls").resolve(id.toString()).resolve(segment + ".ts");

            if (!Files.exists(segmentPath)) {
                return ResponseEntity.notFound().build();
            }

            Resource resource = new UrlResource(segmentPath.toUri());
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("video/MP2T"))
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // --- 3. DİNAMİK RESİM (Thumbnail/Poster) SUNUCUSU ---
    @SuppressWarnings("null")
    @GetMapping("/image/{id}")
    public ResponseEntity<Resource> getImage(@PathVariable(name = "id") UUID id) {
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

            // Fallback for series: if id is an episode, try parent series image
            if (imagePath == null
                    && ("soap_opera".equalsIgnoreCase(category) || "unknown".equalsIgnoreCase(category))) {
                UUID parentId = findParentSeriesId(id);
                if (parentId != null) {
                    imagePath = findExistingImage(Paths.get(soapOperasPath).toAbsolutePath().normalize(), parentId);
                }
            }

            if (imagePath == null) {
                logger.warn("Image file not found for id: {} (Category: {})", id, category);
                return ResponseEntity.notFound().build();
            }

            // Resmi sun (1 günlük önbellek ile)
            MediaType mediaType = determineMediaType(imagePath);
            if (mediaType == null) {
                mediaType = MediaType.IMAGE_JPEG;
            }

            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .cacheControl(CacheControl.maxAge(java.time.Duration.ofDays(1)))
                    .body(new UrlResource(imagePath.toUri()));

        } catch (IOException e) {
            logger.error("Error serving image for id {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // --- 4. İZLENME SAYISI ARTIRMA ---
    @PostMapping("/{id}/increment-view")
    public ResponseEntity<Void> incrementViewCount(@PathVariable("id") UUID id) {
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
        if (category == null)
            return null;

        // Veritabanından dönen string değerlerine göre eşleştirme
        // Not: DB'den dönen değerlerin 'movie', 'soap_opera', 'trailer' olduğu
        // varsayılmıştır.
        switch (category.toLowerCase()) {
            case "movie":
                return Paths.get(moviesPath).toAbsolutePath().normalize();
            case "soap_opera": // DB'den 'soap-opera' veya 'soap_opera' dönebilir, kontrol edin
            case "soap-opera":
            case "soapopera":
            case "series":
                return Paths.get(soapOperasPath).toAbsolutePath().normalize();
            case "trailer":
                return Paths.get(trailersPath).toAbsolutePath().normalize();
            case "upcoming":
                return Paths.get(upcomingPath).toAbsolutePath().normalize();
            default:
                return null;
        }
    }

    private String getContentTypeFromDatabase(UUID id) {
        try {
            // SQL Function might be broken due to renaming, so we prioritize direct checks
            // or assume it's updated.
            // For robustness, let's try the function first, but if it fails or returns
            // unknown, we check tables.
            try {
                String sql = "SELECT [dbo].[GetContentTypeById](?);";
                String type = jdbcTemplate.queryForObject(sql, String.class, id.toString());
                if (type != null && !"unknown".equalsIgnoreCase(type))
                    return type;
            } catch (Exception e) {
                // Function might be missing or broken
            }

            // Check Series (formerly SoapOperas)
            Integer isSeries = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM Series WHERE ID = ?", Integer.class,
                    id.toString());
            if (isSeries != null && isSeries > 0)
                return "soap_opera";

            // Check Episode (formerly SoapOpera)
            Integer isEpisode = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM Episode WHERE ID = ?", Integer.class,
                    id.toString());
            if (isEpisode != null && isEpisode > 0)
                return "soap_opera";

            // Check Movie
            Integer isMovie = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM Movie WHERE ID = ?", Integer.class,
                    id.toString());
            if (isMovie != null && isMovie > 0)
                return "movie";

            // Check Upcoming
            Integer isUpcoming = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM Upcoming WHERE ID = ? OR ReferenceId = ?", Integer.class,
                    id.toString(), id.toString());
            if (isUpcoming != null && isUpcoming > 0)
                return "upcoming";

            // Check Hero (Trailers)
            Integer isHero = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM Hero WHERE ID = ?", Integer.class,
                    id.toString());
            if (isHero != null && isHero > 0)
                return "trailer";

            return "unknown";
        } catch (Exception e) {
            logger.error("DB Error retrieving content type for id {}: {}", id, e.getMessage());
            return "unknown";
        }
    }

    private UUID findParentSeriesId(UUID episodeId) {
        try {
            // Try FK first
            try {
                String sqlFK = "SELECT CAST(SeriesId AS NVARCHAR(36)) FROM Episode WHERE ID = ?";
                String parentIdStr = jdbcTemplate.queryForObject(sqlFK, String.class, episodeId.toString());
                if (parentIdStr != null)
                    return UUID.fromString(parentIdStr);
            } catch (Exception e) {
            }

            // Fallback to XML search (Legacy)
            String sql = "SELECT CAST(ID AS NVARCHAR(36)) FROM Series WHERE EpisodeMetadataXml LIKE ?";
            String searchTerm = "%" + episodeId.toString() + "%";
            String parentIdStr = jdbcTemplate.queryForObject(sql, String.class, searchTerm);
            return parentIdStr != null ? UUID.fromString(parentIdStr) : null;
        } catch (Exception e) {
            return null;
        }
    }

    private UUID findFirstEpisodeIdForSeries(UUID seriesId) {
        try {
            // New schema: Query Episode table directly via SeriesId
            String sql = "SELECT TOP 1 ID FROM Episode WHERE SeriesId = ? ORDER BY uploadDate DESC";
            // If FK not populated, fallback to name match (risky if names duplicate, but
            // fits legacy logic)
            // The old logic was: WHERE name = (SELECT name FROM SoapOperas WHERE ID = ?)

            try {
                String idStr = jdbcTemplate.queryForObject(sql, String.class, seriesId.toString());
                if (idStr != null)
                    return UUID.fromString(idStr);
            } catch (Exception e) {
            }

            // Fallback
            String sqlFallback = "SELECT TOP 1 ID FROM Episode WHERE name = (SELECT name FROM Series WHERE ID = ?) ORDER BY uploadDate DESC";
            String idStr = jdbcTemplate.queryForObject(sqlFallback, String.class, seriesId.toString());
            return idStr != null ? UUID.fromString(idStr) : null;
        } catch (Exception e) {
            return null;
        }
    }

    private Path findExistingImage(Path basePath, UUID id) {
        // Temel klasörde ID + Uzantı kombinasyonlarını dener
        for (String ext : SUPPORTED_IMAGE_EXTENSIONS) {
            // 1. Try lowercase UUID (Standard)
            Path path = basePath.resolve(id.toString() + ext).normalize();
            if (Files.exists(path) && path.startsWith(basePath)) {
                return path;
            }

            // 2. Try Uppercase UUID (Linux/Legacy compatibility)
            Path upperPath = basePath.resolve(id.toString().toUpperCase() + ext).normalize();
            if (Files.exists(upperPath) && upperPath.startsWith(basePath)) {
                return upperPath;
            }
        }
        return null;
    }

    @SuppressWarnings("null")
    private ResponseEntity<Resource> serveResource(Path path) throws IOException {
        UrlResource resource = new UrlResource(path.toUri());
        MediaType mediaType = determineMediaType(path);
        if (mediaType == null) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }
        return ResponseEntity.ok()
                .contentType(mediaType)
                .body(resource);
    }

    @SuppressWarnings("null")
    private ResourceRegion getResourceRegion(UrlResource resource, HttpHeaders headers, long contentLength)
            throws IOException {
        String range = headers.getFirst(HttpHeaders.RANGE);
        if (range == null || range.isEmpty()) {
            return new ResourceRegion(resource, 0, Math.min(VIDEO_CHUNK_SIZE, contentLength));
        }

        try {
            String[] ranges = range.replace("bytes=", "").split("-");
            long start = Long.parseLong(ranges[0]);
            if (start >= contentLength)
                return new ResourceRegion(resource, 0, Math.min(VIDEO_CHUNK_SIZE, contentLength));

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
        if (mediaTypeCache.containsKey(pathStr))
            return mediaTypeCache.get(pathStr);

        String contentType = Files.probeContentType(path);
        MediaType mediaType = (contentType != null) ? MediaType.parseMediaType(contentType)
                : MediaType.APPLICATION_OCTET_STREAM;

        // Fallback checks (Dosya uzantısına göre manuel belirleme)
        if (contentType == null) {
            String filename = path.getFileName().toString().toLowerCase();
            if (filename.endsWith(".mp4"))
                mediaType = MediaType.valueOf("video/mp4");
            else if (filename.endsWith(".jpg") || filename.endsWith(".jpeg"))
                mediaType = MediaType.IMAGE_JPEG;
            else if (filename.endsWith(".png"))
                mediaType = MediaType.IMAGE_PNG;
            else if (filename.endsWith(".webp"))
                mediaType = MediaType.valueOf("image/webp");
        }

        mediaTypeCache.put(pathStr, mediaType);
        return mediaType;
    }
}