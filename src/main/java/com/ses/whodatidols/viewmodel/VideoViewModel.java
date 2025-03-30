package com.ses.whodatidols.viewmodel;

public class VideoViewModel {
    private String id;
    private String title;
    private String info;
    private String thumbnailUrl;
    private String videoUrl;

    public VideoViewModel() {}

    public VideoViewModel(String id, String title, String info, String thumbnailUrl, String videoUrl) {
        this.id = id;
        this.title = title;
        this.info = info;
        this.thumbnailUrl = thumbnailUrl;
        this.videoUrl = videoUrl;
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }
}