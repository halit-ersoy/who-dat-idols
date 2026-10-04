package com.ses.whodatidols.model;

import java.time.Instant;
import java.util.UUID;

public class Actor {
    private UUID id;
    private String name;
    private String photoUrl;
    private String remotePhotoUrl;
    private Integer tmdbId;
    private Integer tvmazeId;
    private Instant createdAt;

    public Actor() {
    }

    public Actor(UUID id, String name, String photoUrl, Integer tmdbId, Integer tvmazeId) {
        this.id = id;
        this.name = name;
        this.photoUrl = photoUrl;
        this.tmdbId = tmdbId;
        this.tvmazeId = tvmazeId;
        this.createdAt = Instant.now();
    }

    public Actor(UUID id, String name, String photoUrl, String remotePhotoUrl, Integer tmdbId, Integer tvmazeId) {
        this.id = id;
        this.name = name;
        this.photoUrl = photoUrl;
        this.remotePhotoUrl = remotePhotoUrl;
        this.tmdbId = tmdbId;
        this.tvmazeId = tvmazeId;
        this.createdAt = Instant.now();
    }

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

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public String getRemotePhotoUrl() {
        return remotePhotoUrl;
    }

    public void setRemotePhotoUrl(String remotePhotoUrl) {
        this.remotePhotoUrl = remotePhotoUrl;
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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
