package com.ses.whodatidols.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class Series {
    private UUID id;
    private String name;
    private String category;
    private String summary; // Formerly _content
    private String language; // Formerly country
    private int finalStatus;
    private String episodeMetadataXml; // Formerly soapOperaSeries (To be deprecated)
    private int episodeCount;
    private LocalDateTime uploadDate;

    // Getter and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public int getFinalStatus() {
        return finalStatus;
    }

    public void setFinalStatus(int finalStatus) {
        this.finalStatus = finalStatus;
    }

    public String getEpisodeMetadataXml() {
        return episodeMetadataXml;
    }

    public void setEpisodeMetadataXml(String episodeMetadataXml) {
        this.episodeMetadataXml = episodeMetadataXml;
    }

    public int getEpisodeCount() {
        return episodeCount;
    }

    public void setEpisodeCount(int episodeCount) {
        this.episodeCount = episodeCount;
    }

    public LocalDateTime getUploadDate() {
        return uploadDate;
    }

    public void setUploadDate(LocalDateTime uploadDate) {
        this.uploadDate = uploadDate;
    }
}
