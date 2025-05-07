package com.ses.whodatidols.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class Movie {
    private UUID id;
    private String name;
    private String category;
    private String content;
    private int time;
    private String language;
    private int year;
    private LocalDateTime uploadDate;

    // Constructors
    public Movie() {
    }

    public Movie(UUID id, String name, String category, String content, int time,
                 String language, int year, LocalDateTime uploadDate) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.content = content;
        this.time = time;
        this.language = language;
        this.year = year;
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

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public int getTime() {
        return time;
    }

    public void setTime(int time) {
        this.time = time;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public LocalDateTime getUploadDate() {
        return uploadDate;
    }

    public void setUploadDate(LocalDateTime uploadDate) {
        this.uploadDate = uploadDate;
    }
}