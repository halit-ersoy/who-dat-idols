package com.ses.whodatidols.controller;

import com.ses.whodatidols.repository.MovieRepository;
import com.ses.whodatidols.repository.SoapOperaRepository;
import com.ses.whodatidols.repository.StaticImageRepository;
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
    private final SoapOperaRepository soapOperaRepository;
    private final StaticImageRepository staticImageRepository;

    public ImageController(MovieRepository movieRepository, SoapOperaRepository soapOperaRepository, StaticImageRepository staticImageRepository) {
        this.movieRepository = movieRepository;
        this.soapOperaRepository = soapOperaRepository;
        this.staticImageRepository = staticImageRepository;
    }

    @GetMapping("/movie")
    public ResponseEntity<?> getImageMovie(@RequestParam("id") UUID id) {
        return getImageResponse(id, movieRepository::getImagePathById);
    }

    @GetMapping("/soap-opera")
    public ResponseEntity<?> getImageSoapOpera(@RequestParam("id") UUID id) {
        return getImageResponse(id, soapOperaRepository::getImagePathById);
    }

    @GetMapping("/static")
    public ResponseEntity<?> getImageStatic(@RequestParam("id") UUID id) {
        return getImageResponse(id, staticImageRepository::getImagePathById);
    }

    private ResponseEntity<?> getImageResponse(UUID id, ImagePathProvider imagePathProvider) {
        try {
            String imagePath = imagePathProvider.getImagePath(id);

            if (imagePath == null || imagePath.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            File file = new File(imagePath);
            if (!file.exists()) {
                return ResponseEntity.notFound().build();
            }

            MediaType mediaType = determineMediaType(file);
            return ResponseEntity.ok().contentType(mediaType).body(new FileSystemResource(file));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Invalid image ID format");
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("Error reading image file");
        }
    }

    private MediaType determineMediaType(File file) throws IOException {
        String contentType = Files.probeContentType(file.toPath());
        return (contentType != null) ? MediaType.parseMediaType(contentType) : MediaType.APPLICATION_OCTET_STREAM;
    }

    @FunctionalInterface
    private interface ImagePathProvider {
        String getImagePath(UUID id);
    }
}