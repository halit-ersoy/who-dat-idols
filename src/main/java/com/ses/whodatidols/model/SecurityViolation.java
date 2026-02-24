package com.ses.whodatidols.model;

import java.time.LocalDateTime;

public class SecurityViolation {
    private Long id;
    private String ipAddress;
    private String userAgent;
    private String pageUrl;
    private LocalDateTime timestamp;

    public SecurityViolation() {
    }

    public SecurityViolation(Long id, String ipAddress, String userAgent, String pageUrl, LocalDateTime timestamp) {
        this.id = id;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.pageUrl = pageUrl;
        this.timestamp = timestamp;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getPageUrl() {
        return pageUrl;
    }

    public void setPageUrl(String pageUrl) {
        this.pageUrl = pageUrl;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
