package com.ses.whodatidols.controller;

import com.ses.whodatidols.repository.SecurityViolationRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/security")
public class SecurityViolationController {

    private final SecurityViolationRepository securityViolationRepository;

    public SecurityViolationController(SecurityViolationRepository securityViolationRepository) {
        this.securityViolationRepository = securityViolationRepository;
    }

    @PostMapping("/report")
    public ResponseEntity<Void> reportViolation(HttpServletRequest request,
            @RequestBody ViolationRequest violationRequest) {
        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isEmpty()) {
            ipAddress = request.getHeader("X-Real-IP");
        }
        if (ipAddress == null || ipAddress.isEmpty()) {
            ipAddress = request.getRemoteAddr();
        } else {
            ipAddress = ipAddress.split(",")[0].trim();
        }

        String userAgent = request.getHeader("User-Agent");
        String pageUrl = violationRequest.getPageUrl();

        securityViolationRepository.save(ipAddress, userAgent, pageUrl);

        return ResponseEntity.ok().build();
    }

    public static class ViolationRequest {
        private String pageUrl;

        public String getPageUrl() {
            return pageUrl;
        }

        public void setPageUrl(String pageUrl) {
            this.pageUrl = pageUrl;
        }
    }
}
