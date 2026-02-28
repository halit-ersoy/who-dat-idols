package com.ses.whodatidols.model;

import java.time.Instant;

public class BannedIp {
    private Long id;
    private String ipAddress;
    private String reason;
    private String appealMessage;
    private Instant timestamp;

    public BannedIp() {
    }

    public BannedIp(Long id, String ipAddress, String reason, String appealMessage, Instant timestamp) {
        this.id = id;
        this.ipAddress = ipAddress;
        this.reason = reason;
        this.appealMessage = appealMessage;
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

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getAppealMessage() {
        return appealMessage;
    }

    public void setAppealMessage(String appealMessage) {
        this.appealMessage = appealMessage;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
