package com.ses.whodatidols.controller;

import com.ses.whodatidols.repository.SystemSettingRepository;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

@Controller
public class MaintenanceController {

    private final SystemSettingRepository systemSettingRepository;

    public MaintenanceController(SystemSettingRepository systemSettingRepository) {
        this.systemSettingRepository = systemSettingRepository;
    }

    @GetMapping("/maintenance")
    public ResponseEntity<?> showMaintenancePage() {
        if (!systemSettingRepository.isMaintenanceModeWdi()) {
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header(HttpHeaders.LOCATION, "/")
                    .build();
        }

        try {
            Resource htmlPage = new ClassPathResource("templates/maintenance.html");
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

    @GetMapping("/api/public/maintenance/status")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getPublicMaintenanceStatus() {
        return ResponseEntity.ok(Map.of(
                "wdi", systemSettingRepository.isMaintenanceModeWdi(),
                "bldizi", systemSettingRepository.isMaintenanceModeBlDizi()
        ));
    }
}
