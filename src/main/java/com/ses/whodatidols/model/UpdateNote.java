package com.ses.whodatidols.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class UpdateNote {
    private UUID id;
    private String title;
    private String message;
    private LocalDateTime createdAt;
    private boolean active;

    public UpdateNote() {
    }

    public UpdateNote(String title, String message) {
        this.id = UUID.randomUUID();
        this.title = title;
        this.message = message;
        this.createdAt = LocalDateTime.now();
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
