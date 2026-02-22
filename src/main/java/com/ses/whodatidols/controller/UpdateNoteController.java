package com.ses.whodatidols.controller;

import com.ses.whodatidols.model.UpdateNote;
import com.ses.whodatidols.repository.UpdateNoteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/updates")
public class UpdateNoteController {

    @Autowired
    private UpdateNoteRepository updateNoteRepository;

    @GetMapping
    public List<UpdateNote> getActiveUpdates() {
        return updateNoteRepository.findActive();
    }

    @GetMapping("/all")
    public List<UpdateNote> getAllUpdates() {
        return updateNoteRepository.findAll();
    }

    @PostMapping("/admin/add")
    public ResponseEntity<?> addUpdate(@RequestBody Map<String, String> request) {
        try {
            String title = request.get("title");
            String message = request.get("message");

            if (title == null || title.trim().isEmpty() || message == null || message.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Başlık ve mesaj boş olamaz."));
            }

            UpdateNote note = new UpdateNote(title.trim(), message.trim());
            updateNoteRepository.save(note);

            return ResponseEntity.ok(Map.of("success", true, "message", "Güncelleme notu eklendi."));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/admin/toggle")
    public ResponseEntity<?> toggleUpdate(@RequestBody Map<String, Object> request) {
        try {
            UUID id = UUID.fromString((String) request.get("id"));
            boolean active = (Boolean) request.get("active");
            updateNoteRepository.toggleActive(id, active);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/admin/delete")
    public ResponseEntity<?> deleteUpdate(@RequestBody Map<String, String> request) {
        try {
            UUID id = UUID.fromString(request.get("id"));
            updateNoteRepository.delete(id);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
