package com.ses.whodatidols.model;

import java.time.Instant;
import java.util.UUID;

public class CustomPage {
    private UUID id;
    private String title;
    private String slug;
    private String targetSite; // "whodatidols", "bldizi", "all"
    private String entryFile;
    private String storagePath;
    private long totalSize;
    private int fileCount;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;

    public CustomPage() {
        this.id = UUID.randomUUID();
        this.entryFile = "index.html";
        this.targetSite = "whodatidols";
        this.active = true;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public CustomPage(UUID id, String title, String slug, String entryFile, String storagePath,
                      long totalSize, int fileCount, boolean active, Instant createdAt, Instant updatedAt, String targetSite) {
        this.id = id != null ? id : UUID.randomUUID();
        this.title = title;
        this.slug = slug;
        this.targetSite = targetSite != null && !targetSite.isBlank() ? targetSite : "whodatidols";
        this.entryFile = entryFile != null && !entryFile.isBlank() ? entryFile : "index.html";
        this.storagePath = storagePath;
        this.totalSize = totalSize;
        this.fileCount = fileCount;
        this.active = active;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
    }

    public String getTargetSite() {
        return targetSite != null ? targetSite : "whodatidols";
    }

    public void setTargetSite(String targetSite) {
        this.targetSite = targetSite != null && !targetSite.isBlank() ? targetSite : "whodatidols";
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getEntryFile() {
        return entryFile;
    }

    public void setEntryFile(String entryFile) {
        this.entryFile = entryFile;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public void setStoragePath(String storagePath) {
        this.storagePath = storagePath;
    }

    public long getTotalSize() {
        return totalSize;
    }

    public void setTotalSize(long totalSize) {
        this.totalSize = totalSize;
    }

    public int getFileCount() {
        return fileCount;
    }

    public void setFileCount(int fileCount) {
        this.fileCount = fileCount;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
