package com.ses.whodatidols.model;

import java.time.Instant;
import java.util.UUID;

public class Feedback {
    private UUID id;
    private UUID userId;
    private String subject;
    private String message;
    private Instant createdAt;

    public Feedback(UUID id, UUID userId, String subject, String message, Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.subject = subject;
        this.message = message;
        this.createdAt = createdAt;
    }

    public Feedback() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
