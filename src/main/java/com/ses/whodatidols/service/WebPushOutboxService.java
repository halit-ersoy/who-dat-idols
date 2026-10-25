package com.ses.whodatidols.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ses.whodatidols.exception.PushPermanentException;
import com.ses.whodatidols.exception.PushSubscriptionExpiredException;
import com.ses.whodatidols.exception.PushTemporaryException;
import com.ses.whodatidols.model.Person;
import com.ses.whodatidols.model.PushOutboxTask;
import com.ses.whodatidols.model.PushPayloadDto;
import com.ses.whodatidols.model.PushPreferences;
import com.ses.whodatidols.model.PushSubscription;
import com.ses.whodatidols.repository.MessageRepository;
import com.ses.whodatidols.repository.PersonRepository;
import com.ses.whodatidols.repository.PushSubscriptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@SuppressWarnings({"SqlResolve", "SqlNoDataSourceInspection"})
public class WebPushOutboxService {

    private static final Logger logger = LoggerFactory.getLogger(WebPushOutboxService.class);

    private final PushSubscriptionRepository pushSubscriptionRepository;
    private final WebPushClientService webPushClientService;
    private final PersonRepository personRepository;
    private final MessageRepository messageRepository;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    private final AtomicBoolean isProcessing = new AtomicBoolean(false);

    public WebPushOutboxService(
            PushSubscriptionRepository pushSubscriptionRepository,
            WebPushClientService webPushClientService,
            PersonRepository personRepository,
            MessageRepository messageRepository,
            JdbcTemplate jdbcTemplate,
            ObjectMapper objectMapper) {
        this.pushSubscriptionRepository = pushSubscriptionRepository;
        this.webPushClientService = webPushClientService;
        this.personRepository = personRepository;
        this.messageRepository = messageRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Enqueues direct message push notifications for all active subscriptions of the receiver.
     */
    public void enqueueDirectMessageNotification(UUID messageId, UUID senderId, UUID receiverId, String content) {
        try {
            if (messageId == null || senderId == null || receiverId == null) return;
            if (senderId.equals(receiverId)) return; // Do not notify sender

            // Verify receiver exists, is not banned, and allows messages
            Optional<Person> receiverOpt = personRepository.findById(receiverId);
            if (receiverOpt.isEmpty()) return;
            Person receiver = receiverOpt.get();
            if (receiver.isBanned() || !receiver.isAllowMessages()) return;

            // Check if blocked
            if (messageRepository.isBlocked(senderId, receiverId)) return;

            // Check receiver's push preferences
            PushPreferences prefs = pushSubscriptionRepository.getPreferences(receiverId);
            if (!prefs.isAllowMessagePush()) return;

            // Fetch active subscriptions
            List<PushSubscription> subscriptions = pushSubscriptionRepository.findActiveSubscriptionsByUserId(receiverId);
            if (subscriptions.isEmpty()) return;

            // Fetch sender info
            Optional<Person> senderOpt = personRepository.findById(senderId);
            String senderNickname = senderOpt.map(Person::getNickname).orElse("Biri");

            // Build payload
            PushPayloadDto payload = new PushPayloadDto();
            payload.setTitle(senderNickname);
            if (prefs.isAllowMessagePreview()) {
                String preview = (content != null && content.length() > 60)
                        ? content.substring(0, 57) + "..."
                        : (content != null ? content : "Yeni bir mesajın var");
                payload.setBody(preview);
            } else {
                payload.setBody("Yeni bir mesajın var");
            }
            payload.setUrl("/messages?user=" + senderNickname);
            payload.setTag("dm-" + senderNickname);
            payload.setData(Map.of(
                    "type", "DIRECT_MESSAGE",
                    "sender", senderNickname,
                    "messageId", messageId.toString()
            ));

            String payloadJson = objectMapper.writeValueAsString(payload);

            // Enqueue task for each subscription with idempotency
            for (PushSubscription sub : subscriptions) {
                String idempotencyKey = receiverId + ":DM:" + messageId + ":" + sub.getId();
                pushSubscriptionRepository.enqueueOutboxTask(
                        sub.getId(),
                        receiverId,
                        "DIRECT_MESSAGE",
                        messageId.toString(),
                        idempotencyKey,
                        payloadJson
                );
            }

            triggerDispatchAsync();
        } catch (Exception e) {
            logger.error("Mesaj push bildirimi sıraya eklenirken hata: {}", e.getMessage());
        }
    }

    /**
     * Enqueues comment reply push notifications when a reply comment is approved or auto-approved.
     */
    public void enqueueCommentReplyNotification(UUID commentId) {
        try {
            if (commentId == null) return;

            String query = """
                SELECT
                    c.ID as CommentId,
                    c.UserId as ReplierId,
                    c.IsApproved as CommentApproved,
                    c.ContentId,
                    p_replier.Nickname as ReplierNickname,
                    parent.ID as ParentCommentId,
                    parent.UserId as ParentUserId,
                    p_parent.isBanned as ParentBanned,
                    COALESCE(m.Slug, e.Slug, s.Slug, u.Slug, CAST(c.ContentId AS VARCHAR(36))) as ContentSlug
                FROM Comments c
                JOIN Comments parent ON c.ParentId = parent.ID
                JOIN Person p_replier ON c.UserId = p_replier.ID
                JOIN Person p_parent ON parent.UserId = p_parent.ID
                LEFT JOIN Movie m ON c.ContentId = m.ID
                LEFT JOIN Episode e ON c.ContentId = e.ID
                LEFT JOIN Series s ON c.ContentId = s.ID
                LEFT JOIN Upcoming u ON c.ContentId = u.ID
                WHERE c.ID = ?
            """;

            List<Map<String, Object>> rows = jdbcTemplate.queryForList(query, commentId.toString());
            if (rows.isEmpty()) return;

            Map<String, Object> row = rows.get(0);
            boolean isApproved = Boolean.TRUE.equals(row.get("CommentApproved"))
                    || (row.get("CommentApproved") instanceof Number && ((Number) row.get("CommentApproved")).intValue() == 1);
            if (!isApproved) {
                // Must not send notifications for unapproved comments!
                return;
            }

            UUID replierId = UUID.fromString(row.get("ReplierId").toString());
            UUID parentUserId = UUID.fromString(row.get("ParentUserId").toString());

            // Do not notify if user replied to their own comment
            if (replierId.equals(parentUserId)) return;

            // Check if parent user is banned
            Boolean isBanned = (Boolean) row.get("ParentBanned");
            if (Boolean.TRUE.equals(isBanned)) return;

            // Check user blocks
            if (messageRepository.isBlocked(replierId, parentUserId)) return;

            // Check parent user's preferences
            PushPreferences prefs = pushSubscriptionRepository.getPreferences(parentUserId);
            if (!prefs.isAllowCommentPush()) return;

            // Fetch active subscriptions
            List<PushSubscription> subscriptions = pushSubscriptionRepository.findActiveSubscriptionsByUserId(parentUserId);
            if (subscriptions.isEmpty()) return;

            String replierNickname = (String) row.get("ReplierNickname");
            String contentSlug = (String) row.get("ContentSlug");
            if (contentSlug == null || contentSlug.isBlank()) {
                contentSlug = row.get("ContentId").toString();
            }

            // Build payload
            PushPayloadDto payload = new PushPayloadDto();
            payload.setTitle("Who Dat Idols");
            payload.setBody(replierNickname + " yorumuna yanıt verdi.");
            payload.setUrl("/" + contentSlug + "#comment-" + commentId);
            payload.setTag("comment-" + commentId);
            payload.setData(Map.of(
                    "type", "COMMENT_REPLY",
                    "commentId", commentId.toString(),
                    "contentSlug", contentSlug
            ));

            String payloadJson = objectMapper.writeValueAsString(payload);

            // Enqueue task for each subscription with idempotency
            for (PushSubscription sub : subscriptions) {
                String idempotencyKey = parentUserId + ":COMMENT:" + commentId + ":" + sub.getId();
                pushSubscriptionRepository.enqueueOutboxTask(
                        sub.getId(),
                        parentUserId,
                        "COMMENT_REPLY",
                        commentId.toString(),
                        idempotencyKey,
                        payloadJson
                );
            }

            triggerDispatchAsync();
        } catch (Exception e) {
            logger.error("Yorum yanıtı push bildirimi sıraya eklenirken hata: {}", e.getMessage());
        }
    }

    /**
     * Triggers asynchronous background processing of pending outbox tasks without blocking the HTTP request.
     */
    public void triggerDispatchAsync() {
        CompletableFuture.runAsync(this::processPendingOutbox);
    }

    /**
     * Regular scheduled poller to process any pending or retrying tasks.
     */
    @Scheduled(fixedDelay = 5000)
    public void scheduledOutboxWorker() {
        processPendingOutbox();
    }

    /**
     * Nightly cleanup of old completed and failed outbox records (keeps 7 days of history).
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupOldRecords() {
        try {
            int deleted = pushSubscriptionRepository.cleanupOldOutbox(7);
            if (deleted > 0) {
                logger.info("Temizlenen eski outbox push kayıt sayısı: {}", deleted);
            }
        } catch (Exception e) {
            logger.error("Eski outbox kayıtlarını temizleme hatası: {}", e.getMessage());
        }
    }

    public void processPendingOutbox() {
        if (!isProcessing.compareAndSet(false, true)) {
            // Already being processed by another thread
            return;
        }

        try {
            List<PushOutboxTask> tasks = pushSubscriptionRepository.fetchPendingTasks(20);
            while (!tasks.isEmpty()) {
                for (PushOutboxTask task : tasks) {
                    processSingleTask(task);
                }
                tasks = pushSubscriptionRepository.fetchPendingTasks(20);
            }
        } catch (Exception e) {
            logger.error("Outbox görevlerini işleme hatası: {}", e.getMessage());
        } finally {
            isProcessing.set(false);
        }
    }

    private void processSingleTask(PushOutboxTask task) {
        pushSubscriptionRepository.markTaskProcessing(task.getId());

        Optional<PushSubscription> subOpt = pushSubscriptionRepository.findSubscriptionById(task.getSubscriptionId());
        if (subOpt.isEmpty() || !subOpt.get().isActive()) {
            pushSubscriptionRepository.markTaskFailed(task.getId(), "Abonelik aktif değil veya silinmiş.", false, 0);
            return;
        }

        PushSubscription sub = subOpt.get();

        // Check if recipient is still active and preferences still allow notification
        Optional<Person> personOpt = personRepository.findById(task.getUserId());
        if (personOpt.isEmpty() || personOpt.get().isBanned()) {
            pushSubscriptionRepository.markTaskFailed(task.getId(), "Kullanıcı bulunamadı veya yasaklanmış.", false, 0);
            return;
        }

        PushPreferences prefs = pushSubscriptionRepository.getPreferences(task.getUserId());
        if ("DIRECT_MESSAGE".equals(task.getEventType()) && !prefs.isAllowMessagePush()) {
            pushSubscriptionRepository.markTaskCompleted(task.getId()); // Silently discard per user preference
            return;
        }
        if ("COMMENT_REPLY".equals(task.getEventType()) && !prefs.isAllowCommentPush()) {
            pushSubscriptionRepository.markTaskCompleted(task.getId());
            return;
        }

        try {
            // TTL is 24 hours (86400 seconds)
            webPushClientService.sendPush(sub.getEndpoint(), sub.getP256dh(), sub.getAuth(), task.getPayload(), 86400);
            pushSubscriptionRepository.markTaskCompleted(task.getId());
        } catch (PushSubscriptionExpiredException e) {
            logger.info("Push aboneliği süresi dolmuş veya iptal edilmiş. Abonelik pasifleştiriliyor.");
            pushSubscriptionRepository.deactivateSubscriptionById(sub.getId());
            pushSubscriptionRepository.markTaskFailed(task.getId(), e.getMessage(), false, 0);
        } catch (PushTemporaryException e) {
            int retries = task.getRetryCount();
            if (retries < task.getMaxRetries()) {
                // Exponential backoff: 30s, 120s, 600s
                int delay = retries == 0 ? 30 : (retries == 1 ? 120 : 600);
                logger.warn("Push sağlayıcısı geçici hata verdi (HTTP {}). {} saniye sonra tekrar denenecek. Deneme: {}/{}",
                        e.getStatusCode(), delay, retries + 1, task.getMaxRetries());
                pushSubscriptionRepository.markTaskFailed(task.getId(), e.getMessage(), true, delay);
            } else {
                logger.error("Push bildirimi maksimum yeniden deneme sınırına ulaştı: {}", e.getMessage());
                pushSubscriptionRepository.markTaskFailed(task.getId(), "Maksimum deneme sınırına ulaşıldı: " + e.getMessage(), false, 0);
            }
        } catch (PushPermanentException e) {
            logger.warn("Push bildirimi kalıcı hata nedeniyle iptal edildi (HTTP {}): {}", e.getStatusCode(), e.getMessage());
            pushSubscriptionRepository.markTaskFailed(task.getId(), e.getMessage(), false, 0);
        } catch (SecurityException se) {
            logger.warn("Push endpoint güvenlik denetimini geçemedi: {}", se.getMessage());
            pushSubscriptionRepository.markTaskFailed(task.getId(), se.getMessage(), false, 0);
        } catch (Exception ex) {
            logger.error("Beklenmeyen push gönderim hatası: {}", ex.getMessage());
            pushSubscriptionRepository.markTaskFailed(task.getId(), ex.getMessage(), false, 0);
        }
    }
}
