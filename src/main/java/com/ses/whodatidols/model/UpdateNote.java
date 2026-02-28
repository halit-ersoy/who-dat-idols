package com.ses.whodatidols.model;

import java.time.Instant;
import java.util.UUID;

public class UpdateNote {
    private UUID id;
    private String title;
    private String message;
    private Instant createdAt;
    private boolean active;

    public UpdateNote() {
    }

    public UpdateNote(String title, String message) {
        this.id = UUID.randomUUID();
        this.title = title;
        this.message = message;
        this.createdAt = Instant.now();
        this.active = true;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
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

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
