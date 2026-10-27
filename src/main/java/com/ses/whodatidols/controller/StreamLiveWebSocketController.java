package com.ses.whodatidols.controller;

import com.ses.whodatidols.service.StreamLiveService;
import com.ses.whodatidols.viewmodel.StreamLiveChatMessageDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.Map;
import java.util.UUID;

@Controller
public class StreamLiveWebSocketController {

    private static final Logger logger = LoggerFactory.getLogger(StreamLiveWebSocketController.class);

    private final StreamLiveService streamLiveService;
    private final SimpMessagingTemplate messagingTemplate;

    public StreamLiveWebSocketController(StreamLiveService streamLiveService,
                                         SimpMessagingTemplate messagingTemplate) {
        this.streamLiveService = streamLiveService;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/streamlive/{id}/chat.send")
    public void sendChatMessage(@DestinationVariable("id") UUID streamLiveId,
                                Map<String, String> payload,
                                SimpMessageHeaderAccessor headerAccessor) {
        Map<String, Object> sessionAttributes = headerAccessor.getSessionAttributes();
        if (sessionAttributes == null || !sessionAttributes.containsKey("userId")) {
            logger.warn("StreamLive: Giriş yapmamış kullanıcı mesaj göndermeye çalıştı.");
            return;
        }

        try {
            UUID userId = UUID.fromString(sessionAttributes.get("userId").toString());
            String nickname = sessionAttributes.getOrDefault("nickname", "Kullanıcı").toString();
            String message = payload != null ? payload.get("message") : null;

            StreamLiveChatMessageDto dto = streamLiveService.processChatMessage(streamLiveId, userId, nickname, message);

            // Tüm odaya canlı olarak yayınla
            messagingTemplate.convertAndSend("/topic/streamlive/" + streamLiveId + "/chat", dto);

        } catch (IllegalArgumentException | IllegalStateException e) {
            logger.info("StreamLive chat kısıtlaması: {}", e.getMessage());
            // Hata mesajını gönderene bildir (varsa oturum ID'si üzerinden)
            String sessionId = headerAccessor.getSessionId();
            if (sessionId != null) {
                messagingTemplate.convertAndSendToUser(
                        sessionId,
                        "/queue/streamlive/errors",
                        Map.of("error", e.getMessage()),
                        headerAccessor.getMessageHeaders()
                );
            }
        } catch (Exception e) {
            logger.error("StreamLive mesajı işlenirken beklenmeyen hata: {}", e.getMessage(), e);
        }
    }
}
