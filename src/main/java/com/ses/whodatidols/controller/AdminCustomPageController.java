package com.ses.whodatidols.controller;

import com.ses.whodatidols.model.CustomPage;
import com.ses.whodatidols.service.CustomPageService;
import com.ses.whodatidols.viewmodel.CustomPageDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@RestController
@RequestMapping("/admin/api/custom-pages")
public class AdminCustomPageController {

    private static final Logger logger = LoggerFactory.getLogger(AdminCustomPageController.class);

    private final CustomPageService customPageService;

    public AdminCustomPageController(CustomPageService customPageService) {
        this.customPageService = customPageService;
    }

    @GetMapping
    public ResponseEntity<?> getPages() {
        try {
            return ResponseEntity.ok(customPageService.getAllPages());
        } catch (Exception e) {
            logger.error("Error retrieving custom pages: ", e);
            return ResponseEntity.internalServerError().body(Map.of("success", false, "message", "Sayfalar alınamadı: " + e.getMessage()));
        }
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadCustomPage(
            @RequestParam("file") MultipartFile file,
            @RequestParam("slug") String slug,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "targetSite", required = false, defaultValue = "whodatidols") String targetSite) {
        Map<String, Object> response = new HashMap<>();
        try {
            CustomPage page = customPageService.processUpload(file, slug, title, targetSite);
            response.put("success", true);
            response.put("message", "Sayfa başarıyla yüklendi ve yayına alındı.");
            response.put("page", CustomPageDto.fromEntity(page));
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (SecurityException e) {
            logger.error("Security violation during upload: ", e);
            response.put("success", false);
            response.put("message", "Güvenlik uyarısı: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            logger.error("Unexpected error during custom page upload: ", e);
            response.put("success", false);
            response.put("message", "Dosya yüklenirken bir hata oluştu: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @PostMapping("/{id}/site")
    public ResponseEntity<?> updateSite(
            @PathVariable("id") UUID id,
            @RequestParam("targetSite") String targetSite) {
        Map<String, Object> response = new HashMap<>();
        boolean ok = customPageService.updateTargetSite(id, targetSite);
        response.put("success", ok);
        response.put("message", ok ? "Yayın platformu güncellendi." : "Sayfa bulunamadı.");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/toggle")
    public ResponseEntity<?> toggleActive(@PathVariable("id") UUID id) {
        Map<String, Object> response = new HashMap<>();
        boolean ok = customPageService.toggleActive(id);
        response.put("success", ok);
        response.put("message", ok ? "Sayfa durumu güncellendi." : "Sayfa bulunamadı.");
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePage(@PathVariable("id") UUID id) {
        Map<String, Object> response = new HashMap<>();
        boolean ok = customPageService.deletePage(id);
        response.put("success", ok);
        response.put("message", ok ? "Sayfa ve tüm ilişkili dosyalar sistemden tamamen silindi." : "Sayfa bulunamadı.");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/files")
    public ResponseEntity<?> getEditableFiles(@PathVariable("id") UUID id) {
        List<String> files = customPageService.listEditableFiles(id);
        return ResponseEntity.ok(files);
    }

    @GetMapping("/{id}/file")
    public ResponseEntity<?> getFileContent(
            @PathVariable("id") UUID id,
            @RequestParam("path") String path) {
        Map<String, Object> response = new HashMap<>();
        try {
            String content = customPageService.readFileContent(id, path);
            response.put("success", true);
            response.put("content", content);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Dosya okunamadı: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/{id}/file")
    public ResponseEntity<?> saveFileContent(
            @PathVariable("id") UUID id,
            @RequestBody Map<String, String> body) {
        Map<String, Object> response = new HashMap<>();
        String path = body.get("path");
        String content = body.get("content");

        if (path == null || path.isBlank()) {
            response.put("success", false);
            response.put("message", "Dosya yolu belirtilmedi.");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            customPageService.saveFileContent(id, path, content);
            response.put("success", true);
            response.put("message", "Dosya başarıyla kaydedildi.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Dosya kaydedilemedi: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}
