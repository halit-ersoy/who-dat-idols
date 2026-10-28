package com.ses.whodatidols.viewmodel;

import java.util.UUID;

public class StreamLiveChatMessageDto {
    private UUID id;
    private UUID streamLiveId;
    private UUID userId;
    private String nickname;
    private String message;
    private long sentAt;
    private boolean verified;

    public StreamLiveChatMessageDto() {
    }

    public StreamLiveChatMessageDto(UUID id, UUID streamLiveId, UUID userId, String nickname, String message, long sentAt) {
        this.id = id;
        this.streamLiveId = streamLiveId;
        this.userId = userId;
        this.nickname = nickname;
        this.message = message;
        this.sentAt = sentAt;
    }

    public StreamLiveChatMessageDto(UUID id, UUID streamLiveId, UUID userId, String nickname, String message, long sentAt, boolean verified) {
        this.id = id;
        this.streamLiveId = streamLiveId;
        this.userId = userId;
        this.nickname = nickname;
        this.message = message;
        this.sentAt = sentAt;
        this.verified = verified;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getStreamLiveId() {
        return streamLiveId;
    }

    public void setStreamLiveId(UUID streamLiveId) {
        this.streamLiveId = streamLiveId;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getSentAt() {
        return sentAt;
    }

    public void setSentAt(long sentAt) {
        this.sentAt = sentAt;
    }

    public long getSentAtMillis() {
        return sentAt;
    }

    public void setSentAtMillis(long sentAtMillis) {
        this.sentAt = sentAtMillis;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public boolean getIsVerified() {
        return verified;
    }

    public void setIsVerified(boolean isVerified) {
        this.verified = isVerified;
    }
}
