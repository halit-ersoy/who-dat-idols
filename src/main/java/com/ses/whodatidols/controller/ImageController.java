package com.ses.whodatidols.controller;

import com.ses.whodatidols.repository.MovieRepository;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.UUID;

@RestController
@RequestMapping("/api/images")
public class ImageController {
    private final MovieRepository movieRepository;

    public ImageController(MovieRepository movieRepository) {
        this.movieRepository = movieRepository;
    }

    @GetMapping
    public ResponseEntity<?> getImage(@RequestParam("id") UUID id) {
        try {

            // Get image path from repository
            String imagePath = movieRepository.getImagePathById(id);

            if (imagePath == null || imagePath.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            // Create file object and check if it exists
            File file = new File(imagePath);
            if (!file.exists()) {
                return ResponseEntity.notFound().build();
            }

            // Determine media type
            MediaType mediaType;
            String contentType = Files.probeContentType(file.toPath());
            if (contentType != null) {
                mediaType = MediaType.parseMediaType(contentType);
            } else {
                mediaType = MediaType.APPLICATION_OCTET_STREAM;
            }

            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .body(new FileSystemResource(file));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Invalid image ID format");
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("Error reading image file");
        }
    }
}