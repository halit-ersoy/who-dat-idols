package com.ses.whodatidols.controller;

import com.ses.whodatidols.model.Movie;
import com.ses.whodatidols.model.SoapOpera;
import com.ses.whodatidols.service.MovieService; // Servisinizin olduğunu varsayıyorum
import com.ses.whodatidols.service.SoapOperaService; // Servisinizin olduğunu varsayıyorum
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin") // Bu ön ek sayesinde SecurityConfig şifre soracak
public class AdminController {

    private final MovieService movieService;
    private final SoapOperaService soapOperaService;

    @Autowired
    public AdminController(MovieService movieService, SoapOperaService soapOperaService) {
        this.movieService = movieService;
        this.soapOperaService = soapOperaService;
    }

    // İSTEDİĞİNİZ GET METODUNUN DÜZENLENMİŞ HALİ
    @GetMapping("/panel")
    public ResponseEntity<Resource> getAdminPanel() {
        try {
            // Dosya yolu: src/main/resources/static/admin/html/panel.html
            Resource htmlPage = new ClassPathResource("static/panel/html/panel.html");

            if (!htmlPage.exists()) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML_VALUE)
                    .body(htmlPage);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }

    // 1. FİLM LİSTESİNİ ÇEKME (JSON)
    @GetMapping("/movies")
    public ResponseEntity<List<Movie>> getMovies() {
        return ResponseEntity.ok(movieService.getAllMovies());
    }

    @GetMapping("/series")
    public ResponseEntity<List<SoapOpera>> getSeriesList() {
        return ResponseEntity.ok(soapOperaService.getAllSeries());
    }

    // FİLM KAYDETME ENDPOINT'İ
    @PostMapping("/add-movie")
    public ResponseEntity<String> addMovie(
            @ModelAttribute Movie movie,
            @RequestParam("file") MultipartFile file,
            @RequestParam("summary") String summary) { // Özet parametresi eklendi
        try {
            // Summary artık _content alanına işleniyor, Time otomatik hesaplanıyor
            movieService.saveMovieWithFile(movie, file, summary);

            return ResponseEntity.ok("Film başarıyla işlendi. Süre otomatik hesaplandı Efendim.");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Hata: " + e.getMessage());
        }
    }

    @PostMapping("/update-movie")
    public ResponseEntity<String> updateMovie(@RequestBody Movie movie) {
        try {
            // ID boş olamaz
            if (movie.getId() == null) {
                return ResponseEntity.badRequest().body("Film ID bulunamadı Efendim.");
            }

            movieService.updateMovie(movie);
            return ResponseEntity.ok("Film verileri başarıyla revize edildi.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Güncelleme hatası: " + e.getMessage());
        }
    }

    @PostMapping("/update-series")
    public ResponseEntity<String> updateSeries(@RequestBody SoapOpera s) {
        try {
            if (s.getId() == null) return ResponseEntity.badRequest().body("Dizi ID yok.");
            soapOperaService.updateSeriesMetadata(s);
            return ResponseEntity.ok("Dizi bilgileri güncellendi.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Hata: " + e.getMessage());
        }
    }

    // DİZİ KAYDETME ENDPOINT'İ
    @PostMapping("/add-series") // Endpoint adı eski HTML ile uyumlu kalsın
    public ResponseEntity<String> addSoapOpera(
            @ModelAttribute SoapOpera soapOpera,
            @RequestParam("file") MultipartFile file,
            @RequestParam("summary") String summary,
            @RequestParam("season") int season,
            @RequestParam("episode") int episode) {
        try {
            soapOpera.setContent(summary);
            soapOpera.setSeasonNumber(season);
            soapOpera.setEpisodeNumber(episode);

            soapOperaService.saveEpisodeWithFile(soapOpera, file);

            return ResponseEntity.ok("Bölüm başarıyla işlendi (S" + season + "E" + episode + "). XML güncellendi.");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Hata: " + e.getMessage());
        }
    }
}