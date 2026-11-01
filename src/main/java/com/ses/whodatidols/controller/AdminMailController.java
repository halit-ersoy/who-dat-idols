package com.ses.whodatidols.controller;

import com.ses.whodatidols.service.AdminMailService;
import com.ses.whodatidols.viewmodel.MailAccountDto;
import com.ses.whodatidols.viewmodel.MailDto;
import com.ses.whodatidols.viewmodel.MailFolderDto;
import com.ses.whodatidols.viewmodel.SendMailRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/mail")
public class AdminMailController {

    private final AdminMailService adminMailService;

    public AdminMailController(AdminMailService adminMailService) {
        this.adminMailService = adminMailService;
    }

    /**
     * Kullanılabilir hesapları listeler
     */
    @GetMapping("/accounts")
    public ResponseEntity<List<MailAccountDto>> getAccounts(
            @RequestParam(value = "account", required = false) String account,
            HttpServletRequest request) {
        return ResponseEntity.ok(adminMailService.getAccounts(account, request));
    }

    /**
     * Mail kutusundaki klasörleri ve sayaçları getirir
     */
    @GetMapping("/folders")
    public ResponseEntity<List<MailFolderDto>> getFolders(
            @RequestParam(value = "account", required = false) String account,
            HttpServletRequest request) {
        return ResponseEntity.ok(adminMailService.getFolders(account, request));
    }

    /**
     * Seçili klasördeki e-postaları sayfalı olarak getirir
     */
    @GetMapping("/messages")
    public ResponseEntity<Map<String, Object>> getMessages(
            @RequestParam(value = "account", required = false) String account,
            @RequestParam(value = "folder", defaultValue = "INBOX") String folder,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            HttpServletRequest request) {
        return ResponseEntity.ok(adminMailService.getMessages(account, folder, page, size, request));
    }

    /**
     * Belirli bir e-postanın detayını getirir ve otomatik okundu yapar
     */
    @GetMapping("/messages/{uid}")
    public ResponseEntity<?> getMessageDetail(
            @PathVariable("uid") long uid,
            @RequestParam(value = "account", required = false) String account,
            @RequestParam(value = "folder", defaultValue = "INBOX") String folder,
            HttpServletRequest request) {
        try {
            MailDto detail = adminMailService.getMessageDetail(account, folder, uid, request);
            return ResponseEntity.ok(detail);
        } catch (Exception e) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("message", e.getMessage());
            return ResponseEntity.status(500).body(err);
        }
    }

    /**
     * Yeni e-posta gönderir
     */
    @PostMapping("/send")
    public ResponseEntity<Map<String, Object>> sendMail(
            @RequestBody SendMailRequest sendRequest,
            HttpServletRequest httpRequest) {
        Map<String, Object> response = new HashMap<>();
        try {
            adminMailService.sendMail(sendRequest, httpRequest);
            response.put("success", true);
            response.put("message", "E-posta başarıyla gönderildi.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "E-posta gönderimi başarısız: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * E-postayı okundu / okunmadı olarak işaretler
     */
    @PostMapping("/messages/{uid}/read")
    public ResponseEntity<Map<String, Object>> markAsRead(
            @PathVariable("uid") long uid,
            @RequestParam(value = "account", required = false) String account,
            @RequestParam(value = "folder", defaultValue = "INBOX") String folder,
            @RequestParam(value = "read", defaultValue = "true") boolean read,
            HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        try {
            adminMailService.markAsRead(account, folder, uid, read, request);
            response.put("success", true);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * E-postayı siler veya çöpe taşır
     */
    @DeleteMapping("/messages/{uid}")
    public ResponseEntity<Map<String, Object>> deleteMessage(
            @PathVariable("uid") long uid,
            @RequestParam(value = "account", required = false) String account,
            @RequestParam(value = "folder", defaultValue = "INBOX") String folder,
            HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        try {
            adminMailService.deleteMessage(account, folder, uid, request);
            response.put("success", true);
            response.put("message", "E-posta başarıyla silindi/çöpe taşındı.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * E-postayı spama taşır
     */
    @PostMapping("/messages/{uid}/spam")
    public ResponseEntity<Map<String, Object>> moveToSpam(
            @PathVariable("uid") long uid,
            @RequestParam(value = "account", required = false) String account,
            @RequestParam(value = "folder", defaultValue = "INBOX") String folder,
            HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        try {
            adminMailService.moveToSpam(account, folder, uid, request);
            response.put("success", true);
            response.put("message", "E-posta spam klasörüne taşındı.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}
