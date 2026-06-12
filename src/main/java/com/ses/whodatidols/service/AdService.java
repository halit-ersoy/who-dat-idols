package com.ses.whodatidols.service;

import com.ses.whodatidols.model.Ad;
import com.ses.whodatidols.repository.AdRepository;
import com.ses.whodatidols.util.FFmpegUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class AdService {
    private final AdRepository adRepository;
    private final FFmpegUtils ffmpegUtils;

    @Value("${media.source.ads.path}")
    private String adsPath;

    public AdService(AdRepository adRepository, FFmpegUtils ffmpegUtils) {
        this.adRepository = adRepository;
        this.ffmpegUtils = ffmpegUtils;
    }

    public List<Ad> getAllAds() {
        return adRepository.findAll();
    }

    public Ad getAdById(UUID id) {
        return adRepository.findById(id);
    }

    public void saveAd(String name, MultipartFile file) throws IOException {
        UUID id = UUID.randomUUID();
        Path uploadPath = Paths.get(adsPath).toAbsolutePath().normalize();
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String fileName = id.toString() + ".mp4";
        Path filePath = uploadPath.resolve(fileName);

        // Save MP4 file
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // Process HLS with 2 second segments asynchronously
        final String input = filePath.toString();
        final String output = uploadPath.resolve("hls").resolve(id.toString()).toString();
        CompletableFuture.runAsync(() -> {
            try {
                ffmpegUtils.convertToHls(input, output, 2);
            } catch (Exception e) {
                System.err.println("Ad HLS auto-conversion failed: " + e.getMessage());
            }
        });

        // Save metadata
        Ad ad = new Ad(id, name, Instant.now());
        adRepository.save(ad);
    }

    public void deleteAd(UUID id) {
        // Delete metadata
        adRepository.deleteById(id);

        // Delete physical files
        Path uploadPath = Paths.get(adsPath).toAbsolutePath().normalize();
        try {
            Files.deleteIfExists(uploadPath.resolve(id.toString() + ".mp4"));
        } catch (IOException e) {
            System.err.println("Failed to delete ad MP4: " + e.getMessage());
        }

        Path hlsPath = uploadPath.resolve("hls").resolve(id.toString());
        deleteDirectory(hlsPath);
    }

    private void deleteDirectory(Path path) {
        try {
            if (Files.exists(path)) {
                Files.walk(path)
                        .sorted(java.util.Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(java.io.File::delete);
            }
        } catch (IOException e) {
            System.err.println("Failed to delete directory: " + e.getMessage());
        }
    }
}
