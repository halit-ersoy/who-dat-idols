package com.ses.whodatidols.controller;

import org.springframework.core.io.FileSystemResource;
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
    private final JdbcTemplate jdbcTemplate;

    public MediaController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/video/{id}")
    public ResponseEntity<?> getVideo(
            @PathVariable UUID id,
            @RequestHeader(value = "Range", required = false) String rangeHeader) {

        try {
            File videoFile = new File(MEDIA_ROOT_PATH + "\\" + id + ".mp4");
            if (!videoFile.exists()) {
                return ResponseEntity.notFound().build();
            }

            // Video streaming logic
            long fileLength = videoFile.length();
            long start = 0;
            long end = fileLength - 1;

            if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
                String[] ranges = rangeHeader.replace("bytes=", "").split("-");
                try {
                    start = Long.parseLong(ranges[0]);
                    if (ranges.length > 1 && !ranges[1].isEmpty()) {
                        end = Long.parseLong(ranges[1]);
                    }
                } catch (NumberFormatException ignored) {}
            }

            long contentLength = end - start + 1;
            InputStream inputStream = new FileInputStream(videoFile);
            inputStream.skip(start);

            final long finalStart = start;
            final long finalEnd = end;

            return ResponseEntity.status(rangeHeader != null ? HttpStatus.PARTIAL_CONTENT : HttpStatus.OK)
                    .header("Content-Type", "video/mp4")
                    .header("Accept-Ranges", "bytes")
                    .header("Content-Length", String.valueOf(contentLength))
                    .header("Content-Range", String.format("bytes %d-%d/%d", finalStart, finalEnd, fileLength))
                    .body(new FileSystemResource(videoFile));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error serving video: " + e.getMessage());
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