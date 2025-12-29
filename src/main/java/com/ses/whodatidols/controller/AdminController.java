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

    // FİLM KAYDETME ENDPOINT'İ
    @PostMapping("/add-movie")
    public ResponseEntity<String> addMovie(
            @ModelAttribute Movie movie, // Form alanlarını nesneye çevirir
            @RequestParam("file") MultipartFile file) { // Dosyayı yakalar
        try {
            // Servis katmanına yönlendiriyoruz
            movieService.saveMovieWithFile(movie, file);

            System.out.println("Operasyon Başarılı: " + movie.getName() + " - " + movie.getId());
            return ResponseEntity.ok("Film ve dosya başarıyla sisteme işlendi Efendim.");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Sistem Hatası: " + e.getMessage());
        }
    }

    // DİZİ KAYDETME ENDPOINT'İ
    @PostMapping("/add-series")
    public ResponseEntity<String> addSoapOpera(
            @ModelAttribute SoapOpera soapOpera,
            @RequestParam("file") MultipartFile file) {
        try {
            // Service katmanına dosyayı iletiyoruz
            soapOperaService.saveSoapOperaWithFile(soapOpera, file);

            System.out.println("Yeni Dizi Eklendi: " + soapOpera.getName());
            return ResponseEntity.ok("Dizi ve dosya başarıyla sisteme işlendi Efendim.");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Dizi eklenirken hata oluştu: " + e.getMessage());
        }
    }
}