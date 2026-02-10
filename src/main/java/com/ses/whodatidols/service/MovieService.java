package com.ses.whodatidols.service;

import com.ses.whodatidols.model.Movie;
import com.ses.whodatidols.repository.MovieRepository;
import com.ses.whodatidols.util.FFmpegUtils;
import com.ses.whodatidols.controller.MediaController;
import com.ses.whodatidols.util.ImageUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class MovieService {

    private final MovieRepository movieRepository;
    private final TvMazeService tvMazeService;
    private final NotificationService notificationService;

    @Value("${media.source.movies.path}")
    private String moviesPath;

    public MovieService(MovieRepository movieRepository, TvMazeService tvMazeService,
            NotificationService notificationService) {
        this.movieRepository = movieRepository;
        this.tvMazeService = tvMazeService;
        this.notificationService = notificationService;
    }

    // Listeyi Getir
    public List<Movie> getAllMovies() {
        return movieRepository.findAll();
    }

    // Güncelleme Operasyonu
    public void updateMovie(Movie movie, MultipartFile file, MultipartFile image) throws IOException {
        if (file != null && !file.isEmpty()) {
            Path uploadPath = Paths.get(moviesPath).toAbsolutePath().normalize();
            if (!Files.exists(uploadPath))
                Files.createDirectories(uploadPath);

            String fileName = movie.getId().toString() + ".mp4";
            Path filePath = uploadPath.resolve(fileName);

            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // FFprobe ile süreyi hesapla
            int duration = FFmpegUtils.getVideoDurationInMinutes(filePath.toString());
            if (duration > 0) {
                movie.setDurationMinutes(duration);
            } else if (movie.getDurationMinutes() <= 0) {
                movie.setDurationMinutes(1);
            }

            // Automate HLS Conversion
            final String input = filePath.toString();
            final String output = uploadPath.resolve("hls").resolve(movie.getId().toString()).toString();
            java.util.concurrent.CompletableFuture.runAsync(() -> {
                try {
                    FFmpegUtils.convertToHls(input, output);
                } catch (Exception e) {
                    System.err.println("HLS auto-conversion failed: " + e.getMessage());
                }
            });
        }

        // Burada sadece metadata (isim, yıl vb.) güncellenir.
        movieRepository.update(movie);

        if (image != null && !image.isEmpty()) {
            saveImage(movie.getId(), image);
        }
    }

    public void saveMovieWithFile(Movie movie, MultipartFile file, MultipartFile image, String summary)
            throws IOException {
        UUID uuid = UUID.randomUUID();
        movie.setId(uuid);
        movie.setUploadDate(LocalDateTime.now());
        movie.setSummary(summary); // Özet summary'e gidiyor

        if (file != null && !file.isEmpty()) {
            Path uploadPath = Paths.get(moviesPath).toAbsolutePath().normalize();
            if (!Files.exists(uploadPath))
                Files.createDirectories(uploadPath);

            String fileName = uuid.toString() + ".mp4";
            Path filePath = uploadPath.resolve(fileName);

            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // FFprobe ile süreyi hesapla
            int duration = FFmpegUtils.getVideoDurationInMinutes(filePath.toString());

            if (duration > 0) {
                movie.setDurationMinutes(duration);
                System.out.println("Süre Hesaplandı: " + duration + " dakika");
            } else {
                System.out.println("Uyarı: Süre 0 döndü. FFmpeg sunucuda kurulu mu?");
                movie.setDurationMinutes(1); // Güvenlik önlemi olarak en az 1 dk
            }

            // Automate HLS Conversion
            final String input = filePath.toString();
            final String output = uploadPath.resolve("hls").resolve(uuid.toString()).toString();
            java.util.concurrent.CompletableFuture.runAsync(() -> {
                try {
                    FFmpegUtils.convertToHls(input, output);
                } catch (Exception e) {
                    System.err.println("HLS auto-conversion failed: " + e.getMessage());
                }
            });
        }

        if (image != null && !image.isEmpty()) {
            saveImage(uuid, image);
        }

        movieRepository.save(movie);

        // Bildirim oluştur
        try {
            notificationService.createNotification(
                    "Yeni Film Eklendi!",
                    movie.getName() + " sitemize eklenmiştir. Hemen izleyin!",
                    uuid,
                    "Movie");
        } catch (Exception e) {
            System.err.println("Bildirim oluşturulamadı: " + e.getMessage());
        }
    }

    public void deleteMovieById(UUID id) {
        // 1. Veritabanından sil
        movieRepository.deleteById(id);

        // 2. Dosyaları fiziksel olarak sil
        deletePhysicalFiles(id);
    }

    private void deletePhysicalFiles(UUID id) {
        try {
            Path uploadPath = Paths.get(moviesPath).toAbsolutePath().normalize();

            // Video sil
            Files.deleteIfExists(uploadPath.resolve(id.toString() + ".mp4"));

            // Resimleri sil
            for (String ext : MediaController.SUPPORTED_IMAGE_EXTENSIONS) {
                Files.deleteIfExists(uploadPath.resolve(id.toString() + ext));
            }

            // HLS sil
            deleteDirectory(uploadPath.resolve("hls").resolve(id.toString()));
        } catch (IOException e) {
            System.err.println("Film dosyaları silinirken hata oluştu: " + e.getMessage());
        }
    }

    private void deleteDirectory(Path path) throws IOException {
        if (Files.exists(path)) {
            Files.walk(path)
                    .sorted(java.util.Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(java.io.File::delete);
        }
    }

    private void saveImage(UUID id, MultipartFile image) throws IOException {
        Path uploadPath = Paths.get(moviesPath).toAbsolutePath().normalize();
        if (!Files.exists(uploadPath))
            Files.createDirectories(uploadPath);

        // Eski resimleri temizle
        for (String ext : MediaController.SUPPORTED_IMAGE_EXTENSIONS) {
            Files.deleteIfExists(uploadPath.resolve(id.toString() + ext));
        }

        Path finalPath = uploadPath.resolve(id.toString() + ".jpg");
        try {
            ImageUtils.saveAsJpg(image.getInputStream(), finalPath);
        } catch (Exception e) {
            System.err.println("JPG dönüşüm hatası: " + e.getMessage());
            throw new RuntimeException("JPG_CONVERSION_ERROR_UPLOAD: " + e.getMessage(), e);
        }
    }

    public void saveImageFromUrl(UUID id, String imageUrl) throws IOException {
        byte[] imageBytes = tvMazeService.downloadImage(imageUrl);
        if (imageBytes == null)
            return;

        Path uploadPath = Paths.get(moviesPath).toAbsolutePath().normalize();
        if (!Files.exists(uploadPath))
            Files.createDirectories(uploadPath);

        // Eski resimleri temizle
        for (String ext : MediaController.SUPPORTED_IMAGE_EXTENSIONS) {
            Files.deleteIfExists(uploadPath.resolve(id.toString() + ext));
        }

        Path finalPath = uploadPath.resolve(id.toString() + ".jpg");
        try {
            ImageUtils.saveImageFromUrlAsJpg(imageUrl, finalPath);
        } catch (Exception e) {
            System.err.println("JPG dönüşüm hatası (URL): " + e.getMessage());
            throw new RuntimeException("JPG_CONVERSION_ERROR_URL: " + e.getMessage(), e);
        }
    }
}