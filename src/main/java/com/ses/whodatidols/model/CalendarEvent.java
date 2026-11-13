package com.ses.whodatidols.model;

import java.util.UUID;

public class CalendarEvent {
    private UUID id;
    private String dayOfWeek;
    private String title;
    private String episode;
    private String showTime;
    private int sortOrder;
    private String posterUrl;
    private String platform; // "whodatidols", "bldizi", "all"
    private Boolean isBl;
    private String channel;
    private String category;
    private String summary;
    private String slug;
    private UUID seriesId;
    private Integer tmdbId;
    private Integer tvmazeId;

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

    public String getPosterUrl() {
        return posterUrl;
    }

    public void setPosterUrl(String posterUrl) {
        this.posterUrl = posterUrl;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public Boolean getIsBl() {
        return isBl;
    }

    public void setIsBl(Boolean isBl) {
        this.isBl = isBl;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
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

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public UUID getSeriesId() {
        return seriesId;
    }

    public void setSeriesId(UUID seriesId) {
        this.seriesId = seriesId;
    }

    public Integer getTmdbId() {
        return tmdbId;
    }

    public void setTmdbId(Integer tmdbId) {
        this.tmdbId = tmdbId;
    }

    public Integer getTvmazeId() {
        return tvmazeId;
    }

    public void setTvmazeId(Integer tvmazeId) {
        this.tvmazeId = tvmazeId;
    }
}
