package com.ses.whodatidols.model;

import java.util.UUID;

public class CalendarEvent {
    private UUID id;
    private String dayOfWeek;
    private String title;
    private String episode;
    private String showTime;
    private int sortOrder;

    public CalendarEvent() {
    }

    public CalendarEvent(UUID id, String dayOfWeek, String title, String episode, String showTime, int sortOrder) {
        this.id = id;
        this.dayOfWeek = dayOfWeek;
        this.title = title;
        this.episode = episode;
        this.showTime = showTime;
        this.sortOrder = sortOrder;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(String dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getEpisode() {
        return episode;
    }

    public void setEpisode(String episode) {
        this.episode = episode;
    }

    public String getShowTime() {
        return showTime;
    }

    public void setShowTime(String showTime) {
        this.showTime = showTime;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }
}
