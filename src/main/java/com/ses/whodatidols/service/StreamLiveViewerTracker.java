package com.ses.whodatidols.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tracks real-time viewers for StreamLive broadcasts via WebSocket STOMP subscriptions.
 */
@Component
public class StreamLiveViewerTracker {

    private static final Logger logger = LoggerFactory.getLogger(StreamLiveViewerTracker.class);
    private static final Pattern VIEWERS_TOPIC_PATTERN = Pattern.compile("^/topic/streamlive/([a-fA-F0-9\\-]+)/viewers$");

    private final SimpMessagingTemplate messagingTemplate;

    // streamId -> Set of sessionIds
    private final Map<UUID, Set<String>> streamViewers = new ConcurrentHashMap<>();

    // sessionId -> (subscriptionId -> streamId)
    private final Map<String, Map<String, UUID>> sessionSubscriptions = new ConcurrentHashMap<>();

    public StreamLiveViewerTracker(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Returns the active viewer count for a given streamId.
     */
    public int getViewerCount(UUID streamId) {
        if (streamId == null) return 0;
        Set<String> sessions = streamViewers.get(streamId);
        return sessions != null ? sessions.size() : 0;
    }

    @EventListener
    public void handleSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String destination = headerAccessor.getDestination();
        if (destination == null) return;

        Matcher matcher = VIEWERS_TOPIC_PATTERN.matcher(destination);
        if (matcher.matches()) {
            try {
                UUID streamId = UUID.fromString(matcher.group(1));
                String sessionId = headerAccessor.getSessionId();
                String subscriptionId = headerAccessor.getSubscriptionId();

                if (sessionId != null) {
                    streamViewers.computeIfAbsent(streamId, k -> ConcurrentHashMap.newKeySet()).add(sessionId);

                    if (subscriptionId != null) {
                        sessionSubscriptions.computeIfAbsent(sessionId, k -> new ConcurrentHashMap<>())
                                .put(subscriptionId, streamId);
                    }

                    broadcastViewerCount(streamId);
                }
            } catch (Exception e) {
                logger.warn("StreamLiveViewerTracker subscription parse error: {}", e.getMessage());
            }
        }
    }

    @EventListener
    public void handleUnsubscribe(SessionUnsubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        String subscriptionId = headerAccessor.getSubscriptionId();

        if (sessionId != null && subscriptionId != null) {
            Map<String, UUID> subs = sessionSubscriptions.get(sessionId);
            if (subs != null) {
                UUID streamId = subs.remove(subscriptionId);
                if (streamId != null) {
                    boolean hasOtherSubs = subs.values().contains(streamId);
                    if (!hasOtherSubs) {
                        Set<String> viewers = streamViewers.get(streamId);
                        if (viewers != null) {
                            viewers.remove(sessionId);
                            broadcastViewerCount(streamId);
                        }
                    }
                }
                if (subs.isEmpty()) {
                    sessionSubscriptions.remove(sessionId);
                }
            }
        }
    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();

        if (sessionId != null) {
            Map<String, UUID> subs = sessionSubscriptions.remove(sessionId);
            Set<UUID> affectedStreams = new HashSet<>();

            if (subs != null) {
                affectedStreams.addAll(subs.values());
            }

            for (Map.Entry<UUID, Set<String>> entry : streamViewers.entrySet()) {
                if (entry.getValue().remove(sessionId)) {
                    affectedStreams.add(entry.getKey());
                }
            }

            for (UUID streamId : affectedStreams) {
                broadcastViewerCount(streamId);
            }
        }
    }

    /**
     * Broadcasts the current viewer count to all subscribers of /topic/streamlive/{streamId}/viewers
     */
    public void broadcastViewerCount(UUID streamId) {
        if (streamId == null) return;
        int count = getViewerCount(streamId);
        try {
            messagingTemplate.convertAndSend(
                    "/topic/streamlive/" + streamId + "/viewers",
                    Map.of("viewerCount", count)
            );
        } catch (Exception e) {
            logger.error("Failed to broadcast viewer count for stream {}: {}", streamId, e.getMessage());
        }
    }
}
