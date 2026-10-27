package com.ses.whodatidols.viewmodel;

import java.util.UUID;

public class CreateStreamLiveRequest {
    private String title;
    private String description;
    private String contentType; // "Movie" or "Episode"
    private UUID contentId;
    private long scheduledStartTime; // Epoch millis
    private int durationMinutes;

    public CreateStreamLiveRequest() {
    }

    public CreateStreamLiveRequest(String title, String description, String contentType, UUID contentId, long scheduledStartTime, int durationMinutes) {
        this.title = title;
        this.description = description;
        this.contentType = contentType;
        this.contentId = contentId;
        this.scheduledStartTime = scheduledStartTime;
        this.durationMinutes = durationMinutes;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public UUID getContentId() {
        return contentId;
    }

    public void setContentId(UUID contentId) {
        this.contentId = contentId;
    }

    public long getScheduledStartTime() {
        return scheduledStartTime;
    }

    public void setScheduledStartTime(long scheduledStartTime) {
        this.scheduledStartTime = scheduledStartTime;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(int durationMinutes) {
        this.durationMinutes = durationMinutes;
    }
}
