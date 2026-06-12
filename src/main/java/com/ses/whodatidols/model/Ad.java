package com.ses.whodatidols.model;

import java.time.Instant;
import java.util.UUID;

public class Ad {
    private UUID id;
    private String name;
    private Instant uploadDate;

    public Ad() {
    }

    public Ad(UUID id, String name, Instant uploadDate) {
        this.id = id;
        this.name = name;
        this.uploadDate = uploadDate;
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
}
