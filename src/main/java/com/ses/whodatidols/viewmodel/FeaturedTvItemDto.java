package com.ses.whodatidols.viewmodel;

import java.util.UUID;

public class FeaturedTvItemDto {
    private String id;
    private String title;
    private Integer season;
    private Integer episode;
    private boolean isNew;
    private boolean isFinal;
    private String image;
    private String country;
    private String language;
    private int finalStatus;
    private String seriesType;

    // Getters
    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public Integer getSeason() {
        return season;
    }

    public Integer getEpisode() {
        return episode;
    }

    public boolean isNew() {
        return isNew;
    }

    public boolean isFinal() {
        return isFinal;
    }

    public String getImage() {
        return image;
    }

    public String getCountry() {
        return country;
    }

    public String getLanguage() {
        return language;
    }

    public int getFinalStatus() {
        return finalStatus;
    }

    public String getSeriesType() {
        return seriesType;
    }

    // Setters
    public void setId(String id) {
        this.id = id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setSeason(Integer season) {
        this.season = season;
    }

    public void setEpisode(Integer episode) {
        this.episode = episode;
    }

    public void setNew(boolean isNew) {
        this.isNew = isNew;
    }

    public void setFinal(boolean isFinal) {
        this.isFinal = isFinal;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public void setFinalStatus(int finalStatus) {
        this.finalStatus = finalStatus;
    }

    public void setSeriesType(String seriesType) {
        this.seriesType = seriesType;
    }
}
