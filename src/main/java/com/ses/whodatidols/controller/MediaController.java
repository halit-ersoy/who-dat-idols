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
    private static final Map<String, String> CATEGORY_PATHS = Map.of(
            "movie", "movies",
            "soap_opera", "soap_operas",
            "trailer", "trailers"
    );
    private static final String[] SUPPORTED_IMAGE_EXTENSIONS = {".webp", ".jpg", ".jpeg", ".png"};

    @Value("${media.root.path}")
    private String mediaRootPath;

    @Value("${media.static.images.path}")
    private String staticImagesPath;

    private final JdbcTemplate jdbcTemplate;

    // Cache to avoid repeated content type probing
    private final Map<String, MediaType> mediaTypeCache = new ConcurrentHashMap<>();

    public MediaController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/static/image/{imageName:.+}")
    public ResponseEntity<Resource> getStaticImage(@PathVariable String imageName) {
        logger.debug("Requesting static image: {}", imageName);

        try {
            // Sanitize and normalize path
            String sanitizedName = Paths.get(imageName).getFileName().toString();
            Path imagePath = Paths.get(staticImagesPath)
                    .resolve(sanitizedName)
                    .normalize()
                    .toAbsolutePath();

            // Validate path is within allowed directory
            Path staticImagesAbsPath = Paths.get(staticImagesPath).toAbsolutePath();
            if (!Files.exists(imagePath) || !imagePath.startsWith(staticImagesAbsPath)) {
                logger.warn("Static image not found or path traversal attempt: {}", imagePath);
                return ResponseEntity.notFound().build();
            }

            // Create and return resource
            UrlResource resource = new UrlResource(imagePath.toUri());
            MediaType mediaType = determineMediaType(imagePath);

            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .body(resource);
        } catch (IOException e) {
            logger.error("Error serving static image {}: {}", imageName, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/video/{category}/{id}")
    public ResponseEntity<ResourceRegion> getVideo(
            @PathVariable String category,
            @PathVariable UUID id,
            @RequestHeader HttpHeaders headers) {

        logger.debug("Requesting video: category={}, id={}", category, id);

        // Validate category
        if (!CATEGORY_PATHS.containsKey(category)) {
            logger.warn("Invalid category requested: {}", category);
            return ResponseEntity.badRequest().build();
        }

        try {
            // Build and validate path
            Path categoryPath = Paths.get(mediaRootPath, CATEGORY_PATHS.get(category)).normalize().toAbsolutePath();
            Path videoPath = categoryPath.resolve(id + ".mp4").normalize().toAbsolutePath();
            Path rootPath = Paths.get(mediaRootPath).toAbsolutePath();

            if (!Files.exists(videoPath) || !videoPath.startsWith(rootPath)) {
                logger.warn("Video file not found or invalid path: {}", videoPath);
                return ResponseEntity.notFound().build();
            }

            // Create video resource and region
            UrlResource videoResource = new UrlResource(videoPath.toUri());
            long contentLength = videoResource.contentLength();
            ResourceRegion region = getResourceRegion(videoResource, headers, contentLength);

            // Return appropriate response with media type
            return ResponseEntity
                    .status(HttpStatus.PARTIAL_CONTENT)
                    .contentType(MediaTypeFactory.getMediaType(videoResource)
                            .orElse(MediaType.APPLICATION_OCTET_STREAM))
                    .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                    .body(region);

        } catch (IOException e) {
            logger.error("Error serving video {}/{}: {}", category, id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/image/{category}/{id}")
    public ResponseEntity<Resource> getImage(
            @PathVariable String category,
            @PathVariable UUID id) {

        logger.debug("Requesting image: category={}, id={}", category, id);

        // Validate category
        if (!CATEGORY_PATHS.containsKey(category)) {
            logger.warn("Invalid category requested: {}", category);
            return ResponseEntity.badRequest().build();
        }

        try {
            // Build and validate path
            Path basePath = Paths.get(mediaRootPath, CATEGORY_PATHS.get(category)).normalize().toAbsolutePath();
            Path rootPath = Paths.get(mediaRootPath).toAbsolutePath();

            if (!basePath.startsWith(rootPath)) {
                logger.warn("Path traversal attempt detected for category: {}", category);
                return ResponseEntity.badRequest().build();
            }

            Path imagePath = findExistingImage(basePath, id);
            if (imagePath == null) {
                logger.warn("Image not found for category={}, id={}", category, id);
                return ResponseEntity.notFound().build();
            }

            // Create and return resource
            UrlResource resource = new UrlResource(imagePath.toUri());
            MediaType mediaType = determineMediaType(imagePath);

            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .cacheControl(CacheControl.maxAge(java.time.Duration.ofDays(1)))
                    .body(resource);

        } catch (IOException e) {
            logger.error("Error serving image {}/{}: {}", category, id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/{id}/increment-view")
    public ResponseEntity<Void> incrementViewCount(@PathVariable UUID id) {
        logger.debug("Incrementing view count for id: {}", id);

        try {
            int updatedRows = jdbcTemplate.update("EXEC IncrementViewCount @ID = ?", id.toString());

            if (updatedRows > 0) {
                return ResponseEntity.ok().build();
            } else {
                logger.warn("No rows updated when incrementing view count for id: {}", id);
                return ResponseEntity.noContent().build();
            }

        } catch (Exception e) {
            logger.error("Failed to increment view count for id {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private ResourceRegion getResourceRegion(UrlResource resource, HttpHeaders headers, long contentLength) throws IOException {
        String range = headers.getFirst(HttpHeaders.RANGE);
        long chunkSize = VIDEO_CHUNK_SIZE;

        if (range == null || range.isEmpty()) {
            long rangeLength = Math.min(chunkSize, contentLength);
            return new ResourceRegion(resource, 0, rangeLength);
        }

        try {
            String[] ranges = range.replace("bytes=", "").split("-");
            long start = Long.parseLong(ranges[0]);

            // Handle invalid range start
            if (start >= contentLength) {
                logger.warn("Invalid range request: start position {} exceeds content length {}", start, contentLength);
                return new ResourceRegion(resource, 0, Math.min(chunkSize, contentLength));
            }

            // Parse end position or default to end of content
            long end = ranges.length > 1 && !ranges[1].isEmpty()
                    ? Math.min(Long.parseLong(ranges[1]), contentLength - 1)
                    : contentLength - 1;

            long rangeLength = Math.min(chunkSize, end - start + 1);
            return new ResourceRegion(resource, start, rangeLength);

        } catch (NumberFormatException e) {
            logger.warn("Invalid range format: {}", range);
            return new ResourceRegion(resource, 0, Math.min(chunkSize, contentLength));
        }
    }

    private Path findExistingImage(Path basePath, UUID id) {
        for (String ext : SUPPORTED_IMAGE_EXTENSIONS) {
            Path path = basePath.resolve(id + ext).normalize();
            if (Files.exists(path) && path.startsWith(basePath)) {
                return path;
            }
        }
        return null;
    }

    private MediaType determineMediaType(Path path) throws IOException {
        String pathStr = path.toString();

        // Check cache first
        if (mediaTypeCache.containsKey(pathStr)) {
            return mediaTypeCache.get(pathStr);
        }

        // Determine content type
        String contentType = Files.probeContentType(path);
        MediaType mediaType;

        if (contentType != null) {
            mediaType = MediaType.parseMediaType(contentType);
        } else {
            // Fallback based on file extension
            String filename = path.getFileName().toString().toLowerCase();
            if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) {
                mediaType = MediaType.IMAGE_JPEG;
            } else if (filename.endsWith(".png")) {
                mediaType = MediaType.IMAGE_PNG;
            } else if (filename.endsWith(".webp")) {
                mediaType = MediaType.valueOf("image/webp");
            } else if (filename.endsWith(".mp4")) {
                mediaType = MediaType.valueOf("video/mp4");
            } else {
                mediaType = MediaType.APPLICATION_OCTET_STREAM;
            }
        }

        // Cache the result
        mediaTypeCache.put(pathStr, mediaType);
        return mediaType;
    }
}