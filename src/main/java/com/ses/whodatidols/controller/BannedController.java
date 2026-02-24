package com.ses.whodatidols.controller;

import com.ses.whodatidols.model.BannedIp;
import com.ses.whodatidols.repository.BannedIpRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
public class BannedController {

    private final BannedIpRepository bannedIpRepository;

    public BannedController(BannedIpRepository bannedIpRepository) {
        this.bannedIpRepository = bannedIpRepository;
    }

    @GetMapping("/banned")
    public ResponseEntity<Object> getBannedPage(HttpServletRequest request) {
        String ip = getClientIp(request);
        if (!bannedIpRepository.isBanned(ip)) {
            return ResponseEntity.status(302).header("Location", "/").build();
        }
        try {
            Resource htmlPage = new ClassPathResource("static/error/html/banned.html");
            if (!htmlPage.exists()) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML_VALUE)
                    .body(htmlPage);
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping("/api/security/ban-info")
    public ResponseEntity<Map<String, String>> getBanInfo(HttpServletRequest request) {
        String ip = getClientIp(request);
        Optional<BannedIp> banOp = bannedIpRepository.findByIp(ip);

        if (banOp.isPresent()) {
            BannedIp ban = banOp.get();
            return ResponseEntity.ok(Map.of(
                    "reason", ban.getReason() != null ? ban.getReason() : "Güvenlik ihlali nedeniyle yasaklandınız.",
                    "hasAppealed",
                    ban.getAppealMessage() != null && !ban.getAppealMessage().isEmpty() ? "true" : "false"));
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/api/security/appeal")
    public ResponseEntity<Map<String, String>> submitAppeal(HttpServletRequest request,
            @RequestBody Map<String, String> body) {
        String ip = getClientIp(request);
        String message = body.get("message");

        if (message == null || message.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Mesaj boş olamaz."));
        }

        if (message.length() > 100) {
            message = message.substring(0, 100);
        }

        bannedIpRepository.updateAppeal(ip, message);
        return ResponseEntity.ok(Map.of("message", "İtirazınız başarıyla gönderildi."));
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        } else {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
