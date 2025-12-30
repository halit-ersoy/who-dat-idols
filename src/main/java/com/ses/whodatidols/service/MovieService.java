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

    @Value("${media.source.movies.path}")
    private String moviesPath;

    public MovieService(MovieRepository movieRepository) {
        this.movieRepository = movieRepository;
    }

    // Listeyi Getir
    public List<Movie> getAllMovies() {
        return movieRepository.findAll();
    }

    // Güncelleme Operasyonu
    public void updateMovie(Movie movie) {
        // Burada sadece metadata (isim, yıl vb.) güncellenir.
        // Eğer dosya yenilenmek istenirse eski yöntemle ID korunarak dosya ezilebilir,
        // ancak şimdilik kullanıcı sadece metin hatası düzeltmek istiyor.
        movieRepository.update(movie);
    }

    public void saveMovieWithFile(Movie movie, MultipartFile file, String summary) throws IOException {
        UUID uuid = UUID.randomUUID();
        movie.setId(uuid);
        movie.setUploadDate(LocalDateTime.now());
        movie.setContent(summary); // Özet _content'e gidiyor

        if (file != null && !file.isEmpty()) {
            Path uploadPath = Paths.get(moviesPath).toAbsolutePath().normalize();
            if (!Files.exists(uploadPath)) Files.createDirectories(uploadPath);

            String fileName = uuid.toString() + ".mp4";
            Path filePath = uploadPath.resolve(fileName);

            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // --- DEĞİŞİKLİK BURADA ---
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

        movieRepository.save(movie);
    }
}