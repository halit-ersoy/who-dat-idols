package com.ses.whodatidols.controller;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.UUID;

@RestController
@RequestMapping("/media")
public class MediaController {

    private static final String MEDIA_ROOT_PATH = "D:\\SourceFiles\\mssql\\media";
    private static final String STATIC_IMAGES_PATH = "D:\\SourceFiles\\mssql\\static";

    private final JdbcTemplate jdbcTemplate;

    public MediaController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/static/image/{imageName}")
    public ResponseEntity<?> getStaticImage(@PathVariable String imageName) {
        try {
            // Sanitize the filename to prevent directory traversal attacks
            String sanitizedName = imageName.replaceAll("[^a-zA-Z0-9.-]", "_");
            File imageFile = new File(STATIC_IMAGES_PATH + "\\" + sanitizedName);

            if (!imageFile.exists()) {
                return ResponseEntity.notFound().build();
            }

            MediaType mediaType = determineMediaType(imageFile);
            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .body(new FileSystemResource(imageFile));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error serving static image: " + e.getMessage());
        }
    }

    @GetMapping("/video/{id}")
    public ResponseEntity<ResourceRegion> getVideo(
            @PathVariable UUID id,
            @RequestHeader(value = "Range", required = false) String rangeHeader) {
        try {
            File videoFile = new File(MEDIA_ROOT_PATH + "\\" + id + ".mp4");
            if (!videoFile.exists()) {
                return ResponseEntity.notFound().build();
            }

            FileSystemResource videoResource = new FileSystemResource(videoFile);
            long contentLength = videoResource.contentLength();

            // Create resource region
            ResourceRegion region;
            long chunkSize = 1024 * 1024; // 1 MB chunk size

            if (rangeHeader == null) {
                long rangeLength = Math.min(chunkSize, contentLength);
                region = new ResourceRegion(videoResource, 0, rangeLength);
            } else {
                String[] ranges = rangeHeader.replace("bytes=", "").split("-");
                long start = Long.parseLong(ranges[0]);
                long end = (ranges.length > 1 && !ranges[1].isEmpty())
                        ? Long.parseLong(ranges[1])
                        : contentLength - 1;
                long rangeLength = Math.min(chunkSize, end - start + 1);
                region = new ResourceRegion(videoResource, start, rangeLength);
            }

            MediaType mediaType = MediaType.parseMediaType("video/mp4");

            return ResponseEntity
                    .status(HttpStatus.PARTIAL_CONTENT)
                    .contentType(mediaType)
                    .body(region);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(null);
        }
    }

    @GetMapping("/image/{id}")
    public ResponseEntity<?> getImage(@PathVariable UUID id) {
        try {
            File imageFile = new File(MEDIA_ROOT_PATH + "\\" + id + ".webp");
            if (!imageFile.exists()) {
                // Fallback to .jpg if .webp doesn't exist
                imageFile = new File(MEDIA_ROOT_PATH + "\\" + id + ".jpg");
                if (!imageFile.exists()) {
                    return ResponseEntity.notFound().build();
                }
            }

            MediaType mediaType = determineMediaType(imageFile);
            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .body(new FileSystemResource(imageFile));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error serving image: " + e.getMessage());
        }
    }

    private MediaType determineMediaType(File file) throws IOException {
        String contentType = Files.probeContentType(file.toPath());
        return (contentType != null) ? MediaType.parseMediaType(contentType) : MediaType.APPLICATION_OCTET_STREAM;
    }

    @PostMapping("/{id}/increment-view")
    public ResponseEntity<?> incrementViewCount(@PathVariable UUID id) {
        try {
            jdbcTemplate.update("EXEC IncrementViewCount @ID = ?", id.toString());
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failed to increment view count");
        }
    }
}