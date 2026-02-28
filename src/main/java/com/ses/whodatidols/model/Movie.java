package com.ses.whodatidols.model;

import java.time.Instant;
import java.util.UUID;

public class Movie {
    private UUID id;
    private String name;
    private String category;
    private String summary;
    private int durationMinutes;
    private String language;
    private String country;
    private int releaseYear;
    private Instant uploadDate;
    private String slug;
    private boolean isAdult;

    // Constructors
    public Movie() {
    }

    public Movie(UUID id, String name, String category, String summary, int durationMinutes,
            String language, int releaseYear, Instant uploadDate) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.summary = summary;
        this.durationMinutes = durationMinutes;
        this.language = language;
        this.releaseYear = releaseYear;
        this.uploadDate = uploadDate;
    }

    // Getters and setters
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

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(int durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public int getReleaseYear() {
        return releaseYear;
    }

    public void setReleaseYear(int releaseYear) {
        this.releaseYear = releaseYear;
    }

    public Instant getUploadDate() {
        return uploadDate;
    }

    public void setUploadDate(Instant uploadDate) {
        this.uploadDate = uploadDate;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public boolean isAdult() {
        return isAdult;
    }

    public void setAdult(boolean adult) {
        isAdult = adult;
    }

    public void setIsAdult(boolean isAdult) {
        this.isAdult = isAdult;
    }
}