package com.ses.whodatidols.controller;

import com.ses.whodatidols.repository.FeedbackRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class FeedbackController {

    private final FeedbackRepository feedbackRepository;

    @Autowired
    public FeedbackController(FeedbackRepository feedbackRepository) {
        this.feedbackRepository = feedbackRepository;
    }

    @PostMapping("/api/feedback")
    public ResponseEntity<?> submitFeedback(
            @CookieValue(name = "wdiAuth", required = false) String cookie,
            @RequestBody Map<String, String> request) {

        // Cookie kontrolü
        if (cookie == null || cookie.isBlank()) {
            return ResponseEntity.status(401).body(Map.of(
                    "success", false,
                    "message", "Bu işlemi yapmak için oturum açmanız gerekmektedir."));
        }

        String subject = request.get("subject");
        String message = request.get("message");

        if (subject == null || subject.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Konu alanı boş bırakılamaz."));
        }

        if (message == null || message.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Mesaj alanı boş bırakılamaz."));
        }

        try {
            Map<String, Object> result = feedbackRepository.submitFeedback(cookie, subject.trim(), message.trim());
            boolean success = Boolean.TRUE.equals(result.get("success"));
            if (success) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.status(401).body(result);
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Sunucu hatası: " + e.getMessage()));
        }
    }
}
