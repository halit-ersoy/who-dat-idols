package com.ses.whodatidols.controller;

import com.ses.whodatidols.config.WebPushConfig;
import com.ses.whodatidols.model.PushPreferences;
import com.ses.whodatidols.model.PushPreferencesDto;
import com.ses.whodatidols.model.PushSubscriptionRequestDto;
import com.ses.whodatidols.repository.PersonRepository;
import com.ses.whodatidols.repository.PushSubscriptionRepository;
import com.ses.whodatidols.service.WebPushClientService;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
public class WebPushController {

    private final WebPushConfig webPushConfig;
    private final PushSubscriptionRepository pushSubscriptionRepository;
    private final PersonRepository personRepository;
    private final WebPushClientService webPushClientService;

    public WebPushController(
            WebPushConfig webPushConfig,
            PushSubscriptionRepository pushSubscriptionRepository,
            PersonRepository personRepository,
            WebPushClientService webPushClientService) {
        this.webPushConfig = webPushConfig;
        this.pushSubscriptionRepository = pushSubscriptionRepository;
        this.personRepository = personRepository;
        this.webPushClientService = webPushClientService;
    }

    @GetMapping("/api/push/public-key")
    public ResponseEntity<?> getPublicKey() {
        return ResponseEntity.ok(Map.of(
                "publicKey", webPushConfig.getPublicKey(),
                "enabled", webPushConfig.isEnabled()
        ));
    }

    @PostMapping("/api/push/subscribe")
    public ResponseEntity<?> subscribe(
            @CookieValue(name = "wdiAuth", required = false) String cookie,
            @RequestHeader(value = "User-Agent", required = false) String headerUserAgent,
            @RequestBody PushSubscriptionRequestDto request) {

        if (cookie == null || cookie.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "Abonelik kaydı için oturum açmalısınız."));
        }

        UUID userId = personRepository.findUserIdByCookie(cookie);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "Geçersiz veya süresi dolmuş oturum."));
        }

        if (request == null || request.getEndpoint() == null || request.getEndpoint().isBlank()
                || request.getKeys() == null
                || request.getKeys().getP256dh() == null || request.getKeys().getP256dh().isBlank()
                || request.getKeys().getAuth() == null || request.getKeys().getAuth().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Eksik abonelik bilgileri (endpoint, p256dh, auth)."));
        }

        try {
            // Validate endpoint URL and protect against SSRF early
            webPushClientService.validateEndpoint(request.getEndpoint());

            String userAgent = request.getUserAgent() != null ? request.getUserAgent() : headerUserAgent;
            if (userAgent != null && userAgent.length() > 500) {
                userAgent = userAgent.substring(0, 500);
            }

            pushSubscriptionRepository.saveOrUpdateSubscription(
                    userId,
                    request.getEndpoint().trim(),
                    request.getKeys().getP256dh().trim(),
                    request.getKeys().getAuth().trim(),
                    userAgent
            );

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Push bildirimi aboneliği başarıyla kaydedildi."
            ));
        } catch (SecurityException se) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", se.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Abonelik kaydedilemedi: " + e.getMessage()));
        }
    }

    @PostMapping("/api/push/unsubscribe")
    public ResponseEntity<?> unsubscribe(
            @CookieValue(name = "wdiAuth", required = false) String cookie,
            @RequestBody Map<String, String> request) {

        if (cookie == null || cookie.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "Oturum açmanız gerekiyor."));
        }

        UUID userId = personRepository.findUserIdByCookie(cookie);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "Geçersiz oturum."));
        }

        String endpoint = request != null ? request.get("endpoint") : null;
        if (endpoint == null || endpoint.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Endpoint parametresi gerekli."));
        }

        try {
            boolean removed = pushSubscriptionRepository.deactivateSubscriptionByEndpoint(userId, endpoint.trim());
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "removed", removed,
                    "message", "Abonelik bu cihaz için başarıyla kapatıldı."
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Abonelik kapatılamadı: " + e.getMessage()));
        }
    }

    @GetMapping("/api/push/preferences")
    public ResponseEntity<?> getPreferences(@CookieValue(name = "wdiAuth", required = false) String cookie) {
        if (cookie == null || cookie.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "Oturum açmanız gerekiyor."));
        }

        UUID userId = personRepository.findUserIdByCookie(cookie);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "Geçersiz oturum."));
        }

        PushPreferences prefs = pushSubscriptionRepository.getPreferences(userId);
        return ResponseEntity.ok(new PushPreferencesDto(
                prefs.isAllowMessagePush(),
                prefs.isAllowCommentPush(),
                prefs.isAllowMessagePreview()
        ));
    }

    @PostMapping("/api/push/preferences")
    public ResponseEntity<?> savePreferences(
            @CookieValue(name = "wdiAuth", required = false) String cookie,
            @RequestBody PushPreferencesDto dto) {

        if (cookie == null || cookie.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "Oturum açmanız gerekiyor."));
        }

        UUID userId = personRepository.findUserIdByCookie(cookie);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "Geçersiz oturum."));
        }

        if (dto == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Tercih verisi eksik."));
        }

        pushSubscriptionRepository.savePreferences(
                userId,
                dto.isAllowMessagePush(),
                dto.isAllowCommentPush(),
                dto.isAllowMessagePreview()
        );

        return ResponseEntity.ok(Map.of("success", true, "message", "Bildirim tercihleri güncellendi."));
    }

    @GetMapping(value = "/sw.js")
    public ResponseEntity<Resource> getServiceWorker() {
        Resource swResource = new ClassPathResource("static/sw.js");
        if (!swResource.exists()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "application/javascript; charset=UTF-8")
                .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                .header("Service-Worker-Allowed", "/")
                .body(swResource);
    }
}
