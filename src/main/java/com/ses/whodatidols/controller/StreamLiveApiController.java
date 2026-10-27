package com.ses.whodatidols.controller;

import com.ses.whodatidols.repository.PersonRepository;
import com.ses.whodatidols.service.StreamLiveService;
import com.ses.whodatidols.viewmodel.StreamLiveChatMessageDto;
import com.ses.whodatidols.viewmodel.StreamLiveStatusDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/streamlive")
public class StreamLiveApiController {

    private final StreamLiveService streamLiveService;
    private final PersonRepository personRepository;

    public StreamLiveApiController(StreamLiveService streamLiveService, PersonRepository personRepository) {
        this.streamLiveService = streamLiveService;
        this.personRepository = personRepository;
    }

    @GetMapping("/current")
    public ResponseEntity<StreamLiveStatusDto> getCurrentStream() {
        StreamLiveStatusDto dto = streamLiveService.getCurrentOrUpcomingStreamStatus();
        if (dto == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/{id}")
    public ResponseEntity<StreamLiveStatusDto> getStreamById(@PathVariable("id") UUID id) {
        StreamLiveStatusDto dto = streamLiveService.getStreamStatusById(id);
        if (dto == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/{id}/chat/history")
    public ResponseEntity<List<StreamLiveChatMessageDto>> getChatHistory(
            @PathVariable("id") UUID id,
            @RequestParam(value = "limit", defaultValue = "50") int limit) {
        List<StreamLiveChatMessageDto> history = streamLiveService.getRecentChatHistory(id, limit);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/auth-status")
    public ResponseEntity<Map<String, Object>> getAuthStatus(
            @CookieValue(name = "wdiAuth", required = false) String cookie) {
        Map<String, Object> response = new HashMap<>();
        if (cookie != null && !cookie.isBlank()) {
            try {
                Map<String, Object> userInfo = personRepository.getUserInfoByCookie(cookie.trim());
                if (userInfo != null && userInfo.containsKey("nickname")) {
                    response.put("isLoggedIn", true);
                    response.put("userId", userInfo.get("ID"));
                    response.put("nickname", userInfo.get("nickname"));
                    response.put("role", userInfo.get("role"));
                    response.put("isVerified", userInfo.get("isVerified"));
                    return ResponseEntity.ok(response);
                }
            } catch (Exception ignored) {
            }
        }
        response.put("isLoggedIn", false);
        return ResponseEntity.ok(response);
    }
}
