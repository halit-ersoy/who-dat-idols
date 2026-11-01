package com.ses.whodatidols.viewmodel;

import com.ses.whodatidols.model.CustomPage;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

public class CustomPageDto {
    private UUID id;
    private String title;
    private String slug;
    private String url;
    private String fullUrl;
    private String targetSite;
    private String siteDisplayName;
    private String entryFile;
    private long totalSize;
    private String formattedSize;
    private int fileCount;
    private boolean active;
    private String formattedCreatedAt;
    private String formattedUpdatedAt;

    public CustomPageDto() {
    }

    public static CustomPageDto fromEntity(CustomPage page) {
        CustomPageDto dto = new CustomPageDto();
        dto.setId(page.getId());
        dto.setTitle(page.getTitle());
        dto.setSlug(page.getSlug());
        dto.setUrl("/" + page.getSlug());
        String site = page.getTargetSite() != null ? page.getTargetSite() : "whodatidols";
        dto.setTargetSite(site);
        if ("bldizi".equalsIgnoreCase(site)) {
            dto.setSiteDisplayName("BL Dizi");
            dto.setFullUrl("https://bldizi.com/" + page.getSlug());
        } else if ("all".equalsIgnoreCase(site)) {
            dto.setSiteDisplayName("Tümü (Her İkisi)");
            dto.setFullUrl("/" + page.getSlug());
        } else {
            dto.setSiteDisplayName("Who Dat Idols");
            dto.setFullUrl("https://whodatidols.com/" + page.getSlug());
        }
        dto.setEntryFile(page.getEntryFile());
        dto.setTotalSize(page.getTotalSize());
        dto.setFormattedSize(formatBytes(page.getTotalSize()));
        dto.setFileCount(page.getFileCount());
        dto.setActive(page.isActive());

        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm");
        if (page.getCreatedAt() != null) {
            dto.setFormattedCreatedAt(sdf.format(Date.from(page.getCreatedAt())));
        }
        if (page.getUpdatedAt() != null) {
            dto.setFormattedUpdatedAt(sdf.format(Date.from(page.getUpdatedAt())));
        }
        return dto;
    }

    public String getFullUrl() {
        return fullUrl;
    }

    public void setFullUrl(String fullUrl) {
        this.fullUrl = fullUrl;
    }

    public String getTargetSite() {
        return targetSite;
    }

    public void setTargetSite(String targetSite) {
        this.targetSite = targetSite;
    }

    public String getSiteDisplayName() {
        return siteDisplayName;
    }

    public void setSiteDisplayName(String siteDisplayName) {
        this.siteDisplayName = siteDisplayName;
    }

    private static String formatBytes(long bytes) {
        if (bytes <= 0) return "0 B";
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
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

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getEntryFile() {
        return entryFile;
    }

    public void setEntryFile(String entryFile) {
        this.entryFile = entryFile;
    }

    public long getTotalSize() {
        return totalSize;
    }

    public void setTotalSize(long totalSize) {
        this.totalSize = totalSize;
    }

    public String getFormattedSize() {
        return formattedSize;
    }

    public void setFormattedSize(String formattedSize) {
        this.formattedSize = formattedSize;
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

    public String getFormattedCreatedAt() {
        return formattedCreatedAt;
    }

    public void setFormattedCreatedAt(String formattedCreatedAt) {
        this.formattedCreatedAt = formattedCreatedAt;
    }

    public String getFormattedUpdatedAt() {
        return formattedUpdatedAt;
    }

    public void setFormattedUpdatedAt(String formattedUpdatedAt) {
        this.formattedUpdatedAt = formattedUpdatedAt;
    }
}
