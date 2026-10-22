package com.ses.whodatidols.model;

import java.time.Instant;
import java.util.UUID;

public class Ad {
    private UUID id;
    private String name;
    private Instant uploadDate;
    private boolean isHidden;
    private String linkButtons;

    public Ad() {
    }

    public Ad(UUID id, String name, Instant uploadDate) {
        this.id = id;
        this.name = name;
        this.uploadDate = uploadDate;
        this.isHidden = false;
    }

    public Ad(UUID id, String name, Instant uploadDate, boolean isHidden) {
        this.id = id;
        this.name = name;
        this.uploadDate = uploadDate;
        this.isHidden = isHidden;
    }

    public Ad(UUID id, String name, Instant uploadDate, boolean isHidden, String linkButtons) {
        this.id = id;
        this.name = name;
        this.uploadDate = uploadDate;
        this.isHidden = isHidden;
        this.linkButtons = linkButtons;
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

    public Instant getUploadDate() {
        return uploadDate;
    }

    public void setUploadDate(Instant uploadDate) {
        this.uploadDate = uploadDate;
    }

    public boolean isHidden() {
        return isHidden;
    }

    public void setHidden(boolean hidden) {
        this.isHidden = hidden;
    }

    public String getLinkButtons() {
        return linkButtons;
    }

    public void setLinkButtons(String linkButtons) {
        this.linkButtons = linkButtons;
    }
}
