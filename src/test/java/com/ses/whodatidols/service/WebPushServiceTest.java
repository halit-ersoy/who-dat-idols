package com.ses.whodatidols.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ses.whodatidols.config.WebPushConfig;
import com.ses.whodatidols.exception.PushSubscriptionExpiredException;
import com.ses.whodatidols.exception.PushTemporaryException;
import com.ses.whodatidols.model.Person;
import com.ses.whodatidols.model.PushOutboxTask;
import com.ses.whodatidols.model.PushPreferences;
import com.ses.whodatidols.model.PushSubscription;
import com.ses.whodatidols.repository.MessageRepository;
import com.ses.whodatidols.repository.PersonRepository;
import com.ses.whodatidols.repository.PushSubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebPushServiceTest {

    @Mock
    private PushSubscriptionRepository pushSubscriptionRepository;

    @Mock
    private WebPushClientService webPushClientService;

    @Mock
    private PersonRepository personRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private JdbcTemplate jdbcTemplate;

    private ObjectMapper objectMapper;
    private WebPushOutboxService outboxService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        outboxService = new WebPushOutboxService(
                pushSubscriptionRepository,
                webPushClientService,
                personRepository,
                messageRepository,
                jdbcTemplate,
                objectMapper
        );
    }

    @Test
    @DisplayName("SSRF Doğrulaması: Güvenilir push sağlayıcıları kabul edilir")
    void testValidateEndpoint_TrustedProviders() {
        WebPushConfig config = new WebPushConfig();
        WebPushClientService clientService = new WebPushClientService(config);

        assertDoesNotThrow(() ->
                clientService.validateEndpoint("https://fcm.googleapis.com/fcm/send/sample-token"));
        assertDoesNotThrow(() ->
                clientService.validateEndpoint("https://updates.push.services.mozilla.com/wpush/v2/sample-token"));
        assertDoesNotThrow(() ->
                clientService.validateEndpoint("https://web.push.apple.com/sample-token"));
    }

    @Test
    @DisplayName("SSRF Doğrulaması: HTTP protokolü ve güvensiz şemalar engellenir")
    void testValidateEndpoint_RejectsNonHttps() {
        WebPushConfig config = new WebPushConfig();
        WebPushClientService clientService = new WebPushClientService(config);

        assertThrows(SecurityException.class, () ->
                clientService.validateEndpoint("http://fcm.googleapis.com/fcm/send/sample-token"));
    }

    @Test
    @DisplayName("SSRF Doğrulaması: Bilinmeyen ve yabancı hostlar engellenir")
    void testValidateEndpoint_RejectsUntrustedDomain() {
        WebPushConfig config = new WebPushConfig();
        WebPushClientService clientService = new WebPushClientService(config);

        assertThrows(SecurityException.class, () ->
                clientService.validateEndpoint("https://evil-attacker.com/webhook"));
    }

    @Test
    @DisplayName("SSRF Doğrulaması: Loopback ve yerel IP adresleri engellenir")
    void testValidateEndpoint_RejectsLocalHost() {
        WebPushConfig config = new WebPushConfig();
        WebPushClientService clientService = new WebPushClientService(config);

        assertThrows(SecurityException.class, () ->
                clientService.validateEndpoint("https://127.0.0.1:8080/push"));
        assertThrows(SecurityException.class, () ->
                clientService.validateEndpoint("https://localhost:8080/push"));
    }

    @Test
    @DisplayName("Kullanıcı kendi mesajına bildirim almamalıdır")
    void testEnqueueDirectMessage_SelfMessageIgnored() {
        UUID userId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();

        outboxService.enqueueDirectMessageNotification(messageId, userId, userId, "Merhaba");

        verifyNoInteractions(pushSubscriptionRepository);
    }

    @Test
    @DisplayName("Engellenmiş kullanıcıdan gelen mesaj için push sıraya alınmamalıdır")
    void testEnqueueDirectMessage_BlockedUserIgnored() {
        UUID senderId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();

        Person receiver = new Person();
        receiver.setId(receiverId);
        receiver.setBanned(false);
        receiver.setAllowMessages(true);

        when(personRepository.findById(receiverId)).thenReturn(Optional.of(receiver));
        when(messageRepository.isBlocked(senderId, receiverId)).thenReturn(true);

        outboxService.enqueueDirectMessageNotification(messageId, senderId, receiverId, "Selam");

        verify(pushSubscriptionRepository, never()).enqueueOutboxTask(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Kullanıcı DM push bildirimini kapattıysa push sıraya alınmamalıdır")
    void testEnqueueDirectMessage_PreferenceDisabled() {
        UUID senderId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();

        Person receiver = new Person();
        receiver.setId(receiverId);
        receiver.setBanned(false);
        receiver.setAllowMessages(true);

        PushPreferences prefs = new PushPreferences();
        prefs.setUserId(receiverId);
        prefs.setAllowMessagePush(false); // push off

        when(personRepository.findById(receiverId)).thenReturn(Optional.of(receiver));
        when(messageRepository.isBlocked(senderId, receiverId)).thenReturn(false);
        when(pushSubscriptionRepository.getPreferences(receiverId)).thenReturn(prefs);

        outboxService.enqueueDirectMessageNotification(messageId, senderId, receiverId, "Selam");

        verify(pushSubscriptionRepository, never()).enqueueOutboxTask(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Mesaj önizlemesi kapalıysa gizlilik korunmalı ve genel metin gösterilmelidir")
    void testEnqueueDirectMessage_PrivacyPreviewOff() {
        UUID senderId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();
        UUID subId = UUID.randomUUID();

        Person receiver = new Person();
        receiver.setId(receiverId);
        receiver.setBanned(false);
        receiver.setAllowMessages(true);

        Person sender = new Person();
        sender.setId(senderId);
        sender.setNickname("ahmet");

        PushPreferences prefs = new PushPreferences();
        prefs.setUserId(receiverId);
        prefs.setAllowMessagePush(true);
        prefs.setAllowMessagePreview(false); // gizlilik: mesaj metni gizlensin

        PushSubscription sub = new PushSubscription();
        sub.setId(subId);
        sub.setUserId(receiverId);
        sub.setActive(true);

        when(personRepository.findById(receiverId)).thenReturn(Optional.of(receiver));
        when(personRepository.findById(senderId)).thenReturn(Optional.of(sender));
        when(messageRepository.isBlocked(senderId, receiverId)).thenReturn(false);
        when(pushSubscriptionRepository.getPreferences(receiverId)).thenReturn(prefs);
        when(pushSubscriptionRepository.findActiveSubscriptionsByUserId(receiverId)).thenReturn(List.of(sub));

        outboxService.enqueueDirectMessageNotification(messageId, senderId, receiverId, "Çok gizli özel mesaj metni");

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> idempotencyCaptor = ArgumentCaptor.forClass(String.class);

        verify(pushSubscriptionRepository, times(1)).enqueueOutboxTask(
                eq(subId),
                eq(receiverId),
                eq("DIRECT_MESSAGE"),
                eq(messageId.toString()),
                idempotencyCaptor.capture(),
                payloadCaptor.capture()
        );

        String payload = payloadCaptor.getValue();
        assertTrue(payload.contains("Yeni bir mesaj\\u0131n var") || payload.contains("Yeni bir mesajın var"),
                "Önizleme kapalıyken 'Yeni bir mesajın var' genel metni olmalıdır.");
        assertFalse(payload.contains("Çok gizli özel mesaj"),
                "Önizleme kapalıyken özel mesaj içeriği sızmamalıdır.");

        String expectedIdempotency = receiverId + ":DM:" + messageId + ":" + subId;
        assertEquals(expectedIdempotency, idempotencyCaptor.getValue());
    }

    @Test
    @DisplayName("Mesaj önizlemesi açıksa mesaj metni bildirimde yer almalıdır")
    void testEnqueueDirectMessage_PreviewOn() {
        UUID senderId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();
        UUID subId = UUID.randomUUID();

        Person receiver = new Person();
        receiver.setId(receiverId);
        receiver.setBanned(false);
        receiver.setAllowMessages(true);

        Person sender = new Person();
        sender.setId(senderId);
        sender.setNickname("mehmet");

        PushPreferences prefs = new PushPreferences();
        prefs.setUserId(receiverId);
        prefs.setAllowMessagePush(true);
        prefs.setAllowMessagePreview(true); // önizleme açık

        PushSubscription sub = new PushSubscription();
        sub.setId(subId);
        sub.setUserId(receiverId);
        sub.setActive(true);

        when(personRepository.findById(receiverId)).thenReturn(Optional.of(receiver));
        when(personRepository.findById(senderId)).thenReturn(Optional.of(sender));
        when(messageRepository.isBlocked(senderId, receiverId)).thenReturn(false);
        when(pushSubscriptionRepository.getPreferences(receiverId)).thenReturn(prefs);
        when(pushSubscriptionRepository.findActiveSubscriptionsByUserId(receiverId)).thenReturn(List.of(sub));

        outboxService.enqueueDirectMessageNotification(messageId, senderId, receiverId, "Harika bir proje!");

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(pushSubscriptionRepository).enqueueOutboxTask(any(), any(), any(), any(), any(), payloadCaptor.capture());

        assertTrue(payloadCaptor.getValue().contains("Harika bir proje!"));
    }

    @Test
    @DisplayName("Push aboneliği süresi dolmuşsa (HTTP 404/410), abonelik otomatik pasifleştirilmelidir")
    void testProcessPendingOutbox_DeactivatesExpiredSubscription() {
        UUID taskId = UUID.randomUUID();
        UUID subId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        PushOutboxTask task = new PushOutboxTask();
        task.setId(taskId);
        task.setSubscriptionId(subId);
        task.setUserId(userId);
        task.setEventType("DIRECT_MESSAGE");
        task.setPayload("{\"title\":\"test\"}");
        task.setRetryCount(0);
        task.setMaxRetries(3);

        PushSubscription sub = new PushSubscription();
        sub.setId(subId);
        sub.setUserId(userId);
        sub.setActive(true);
        sub.setEndpoint("https://fcm.googleapis.com/fcm/send/expired");
        sub.setP256dh("p256dh");
        sub.setAuth("auth");

        Person person = new Person();
        person.setId(userId);
        person.setBanned(false);

        PushPreferences prefs = new PushPreferences();
        prefs.setUserId(userId);
        prefs.setAllowMessagePush(true);

        when(pushSubscriptionRepository.fetchPendingTasks(20))
                .thenReturn(List.of(task))
                .thenReturn(Collections.emptyList());
        when(pushSubscriptionRepository.findSubscriptionById(subId)).thenReturn(Optional.of(sub));
        when(personRepository.findById(userId)).thenReturn(Optional.of(person));
        when(pushSubscriptionRepository.getPreferences(userId)).thenReturn(prefs);

        doThrow(new PushSubscriptionExpiredException("Subscription expired (HTTP 410)"))
                .when(webPushClientService).sendPush(anyString(), anyString(), anyString(), anyString(), anyInt());

        outboxService.processPendingOutbox();

        verify(pushSubscriptionRepository, times(1)).deactivateSubscriptionById(subId);
        verify(pushSubscriptionRepository, times(1)).markTaskFailed(eq(taskId), anyString(), eq(false), eq(0));
    }

    @Test
    @DisplayName("Geçici hata durumunda (HTTP 429/500) kontrollü üstel gecikmeyle (backoff) yeniden denenmelidir")
    void testProcessPendingOutbox_RetryWithExponentialBackoff() {
        UUID taskId = UUID.randomUUID();
        UUID subId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        PushOutboxTask task = new PushOutboxTask();
        task.setId(taskId);
        task.setSubscriptionId(subId);
        task.setUserId(userId);
        task.setEventType("DIRECT_MESSAGE");
        task.setPayload("{\"title\":\"test\"}");
        task.setRetryCount(0); // ilk deneme
        task.setMaxRetries(3);

        PushSubscription sub = new PushSubscription();
        sub.setId(subId);
        sub.setUserId(userId);
        sub.setActive(true);
        sub.setEndpoint("https://fcm.googleapis.com/fcm/send/valid");
        sub.setP256dh("p256dh");
        sub.setAuth("auth");

        Person person = new Person();
        person.setId(userId);
        person.setBanned(false);

        PushPreferences prefs = new PushPreferences();
        prefs.setUserId(userId);
        prefs.setAllowMessagePush(true);

        when(pushSubscriptionRepository.fetchPendingTasks(20))
                .thenReturn(List.of(task))
                .thenReturn(Collections.emptyList());
        when(pushSubscriptionRepository.findSubscriptionById(subId)).thenReturn(Optional.of(sub));
        when(personRepository.findById(userId)).thenReturn(Optional.of(person));
        when(pushSubscriptionRepository.getPreferences(userId)).thenReturn(prefs);

        doThrow(new PushTemporaryException("Rate limited", 429))
                .when(webPushClientService).sendPush(anyString(), anyString(), anyString(), anyString(), anyInt());

        outboxService.processPendingOutbox();

        // retryable = true, delaySeconds = 30 for first failure
        verify(pushSubscriptionRepository, times(1)).markTaskFailed(eq(taskId), anyString(), eq(true), eq(30));
        verify(pushSubscriptionRepository, never()).deactivateSubscriptionById(any());
    }
}
