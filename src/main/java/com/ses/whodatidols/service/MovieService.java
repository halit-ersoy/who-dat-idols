package com.ses.whodatidols.service;

import com.ses.whodatidols.model.Movie;
import com.ses.whodatidols.repository.MovieRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class MovieService {

    private final MovieRepository movieRepository;

    // Properties dosyasındaki "Movies" yolunu buraya çekiyoruz
    @Value("${media.source.movies.path}")
    private String moviesPath;

    public MovieService(MovieRepository movieRepository) {
        this.movieRepository = movieRepository;
    }

    public void saveMovieWithFile(Movie movie, MultipartFile file) throws IOException {
        // 1. Kimlik (UUID) Oluştur
        UUID uuid = UUID.randomUUID();
        movie.setId(uuid);
        movie.setUploadDate(LocalDateTime.now());

        // 2. Dosya Kaydetme İşlemi
        if (file != null && !file.isEmpty()) {
            // Hedef klasörün varlığını kontrol et, yoksa oluştur
            Path uploadPath = Paths.get(moviesPath).toAbsolutePath().normalize();
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // Dosya adı sadece UUID.mp4 olacak (Temiz yapı)
            String extension = ".mp4"; // Genelde mp4 gelir, isterseniz orijinalden alabilirsiniz
            String fileName = uuid.toString() + extension;

            Path filePath = uploadPath.resolve(fileName);

            // Dosyayı diske yaz
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // 3. Veritabanı İçin "Sanal" Adres
            // Artık fiziksel yolu değil, Controller üzerinden erişilecek adresi kaydediyoruz.
            // Frontend bu adresi src="/media/video/..." olarak kullanacak.
            movie.setContent("/media/video/" + uuid.toString());
        }

        // 4. Veritabanına İşle
        movieRepository.save(movie);
    }
}