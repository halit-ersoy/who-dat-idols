package com.ses.whodatidols.model;

import java.time.Instant;
import java.util.UUID;

public class Message {
    private UUID id;
    private UUID senderId;
    private UUID receiverId;
    private String content;
    private Instant timestamp;
    private boolean isRead;
    private boolean isDelivered;

    // Additional fields for UI convenience
    private String senderNickname;
    private String receiverNickname;
    private String senderRole;
    private String receiverRole;
    private boolean senderVerified;
    private boolean receiverVerified;

    public Message() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getSenderId() {
        return senderId;
    }

    public void setSenderId(UUID senderId) {
        this.senderId = senderId;
    }

    public UUID getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(UUID receiverId) {
        this.receiverId = receiverId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    public boolean isDelivered() {
        return isDelivered;
    }

    public void setDelivered(boolean delivered) {
        isDelivered = delivered;
    }

    public String getSenderNickname() {
        return senderNickname;
    }

    public void setSenderNickname(String senderNickname) {
        this.senderNickname = senderNickname;
    }

    public String getReceiverNickname() {
        return receiverNickname;
    }

    public void setReceiverNickname(String receiverNickname) {
        this.receiverNickname = receiverNickname;
    }

    public String getSenderRole() {
        return senderRole;
    }

    public void setSenderRole(String senderRole) {
        this.senderRole = senderRole;
    }

    public String getReceiverRole() {
        return receiverRole;
    }

    public void setReceiverRole(String receiverRole) {
        this.receiverRole = receiverRole;
    }

    public boolean isSenderVerified() {
        return senderVerified;
    }

    public void setSenderVerified(boolean senderVerified) {
        this.senderVerified = senderVerified;
    }

    public boolean isReceiverVerified() {
        return receiverVerified;
    }

    public void setReceiverVerified(boolean receiverVerified) {
        this.receiverVerified = receiverVerified;
    }
}
