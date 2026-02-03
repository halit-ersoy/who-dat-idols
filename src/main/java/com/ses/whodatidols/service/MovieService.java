package com.ses.whodatidols.service;

import com.ses.whodatidols.model.Movie;
import com.ses.whodatidols.repository.MovieRepository;
import com.ses.whodatidols.util.FFmpegUtils; // Yeni import
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

    @Value("${media.source.movies.path}")
    private String moviesPath;

    public MovieService(MovieRepository movieRepository, TvMazeService tvMazeService) {
        this.movieRepository = movieRepository;
        this.tvMazeService = tvMazeService;
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
                movie.setTime(duration);
            } else if (movie.getTime() <= 0) {
                movie.setTime(1);
            }
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
        movie.setContent(summary); // Özet _content'e gidiyor

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
                movie.setTime(duration);
                System.out.println("Süre Hesaplandı: " + duration + " dakika");
            } else {
                System.out.println("Uyarı: Süre 0 döndü. FFmpeg sunucuda kurulu mu?");
                movie.setTime(1); // Güvenlik önlemi olarak en az 1 dk
            }
        }

        if (image != null && !image.isEmpty()) {
            saveImage(uuid, image);
        }

        movieRepository.save(movie);
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
            String[] supportedExtensions = { ".webp", ".jpg", ".jpeg", ".png" };
            for (String ext : supportedExtensions) {
                Files.deleteIfExists(uploadPath.resolve(id.toString() + ext));
            }
        } catch (IOException e) {
            System.err.println("Film dosyaları silinirken hata oluştu: " + e.getMessage());
        }
    }

    private void saveImage(UUID id, MultipartFile image) throws IOException {
        Path uploadPath = Paths.get(moviesPath).toAbsolutePath().normalize();
        if (!Files.exists(uploadPath))
            Files.createDirectories(uploadPath);

        // Eski resimleri temizle
        String[] supportedExtensions = { ".webp", ".jpg", ".jpeg", ".png" };
        for (String ext : supportedExtensions) {
            Files.deleteIfExists(uploadPath.resolve(id.toString() + ext));
        }

        // Geçici bir dosya oluşturup oraya kaydedelim, sonra WebP'ye çevirelim
        String originalExt = getExtension(image.getOriginalFilename());
        Path tempPath = uploadPath.resolve(id.toString() + "_temp" + originalExt);
        Files.copy(image.getInputStream(), tempPath, StandardCopyOption.REPLACE_EXISTING);

        Path finalPath = uploadPath.resolve(id.toString() + ".webp");
        try {
            FFmpegUtils.convertImageToWebP(tempPath.toString(), finalPath.toString());
        } catch (Exception e) {
            System.err.println("WebP dönüşüm hatası, ham dosya kullanılıyor: " + e.getMessage());
            Files.move(tempPath, uploadPath.resolve(id.toString() + originalExt), StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(tempPath);
        }
    }

    private String getExtension(String fileName) {
        if (fileName == null || !fileName.contains("."))
            return ".jpg";
        return fileName.substring(fileName.lastIndexOf(".")).toLowerCase();
    }

    public void saveImageFromUrl(UUID id, String imageUrl) throws IOException {
        byte[] imageBytes = tvMazeService.downloadImage(imageUrl);
        if (imageBytes == null)
            return;

        Path uploadPath = Paths.get(moviesPath).toAbsolutePath().normalize();
        if (!Files.exists(uploadPath))
            Files.createDirectories(uploadPath);

        // Eski resimleri temizle
        String[] supportedExtensions = { ".webp", ".jpg", ".jpeg", ".png" };
        for (String ext : supportedExtensions) {
            Files.deleteIfExists(uploadPath.resolve(id.toString() + ext));
        }

        String extension = ".jpg";
        if (imageUrl.toLowerCase().endsWith(".png"))
            extension = ".png";
        else if (imageUrl.toLowerCase().endsWith(".webp"))
            return; // already webp? usually unlikely from url, but let's handle

        Path tempPath = uploadPath.resolve(id.toString() + "_temp" + extension);
        Files.write(tempPath, imageBytes);

        Path finalPath = uploadPath.resolve(id.toString() + ".webp");
        try {
            FFmpegUtils.convertImageToWebP(tempPath.toString(), finalPath.toString());
        } catch (Exception e) {
            System.err.println("WebP dönüşüm hatası (URL): " + e.getMessage());
            Files.move(tempPath, uploadPath.resolve(id.toString() + extension), StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(tempPath);
        }
    }
}