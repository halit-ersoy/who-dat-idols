package com.ses.whodatidols.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class Notification {
    private UUID id;
    private String title;
    private String message;
    private UUID contentId;
    private String type; // 'Movie' or 'SoapOpera'
    private LocalDateTime createdAt;
    private boolean isRead;
    private String slug; // Used for UI routing

    public Notification() {
    }

    public Notification(String title, String message, UUID contentId, String type) {
        this.id = UUID.randomUUID();
        this.title = title;
        this.message = message;
        this.contentId = contentId;
        this.type = type;
        this.createdAt = LocalDateTime.now();
        this.isRead = false;
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

    public UUID getContentId() {
        return contentId;
    }

    public void setContentId(UUID contentId) {
        this.contentId = contentId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }
}
