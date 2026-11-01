package com.ses.whodatidols.viewmodel;

public class MailAccountDto {
    private String id;
    private String email;
    private String displayName;
    private String imapHost;
    private String smtpHost;
    private boolean isDefault;

    public MailAccountDto() {
    }

    public MailAccountDto(String id, String email, String displayName, String imapHost, String smtpHost, boolean isDefault) {
        this.id = id;
        this.email = email;
        this.displayName = displayName;
        this.imapHost = imapHost;
        this.smtpHost = smtpHost;
        this.isDefault = isDefault;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getImapHost() {
        return imapHost;
    }

    public void setImapHost(String imapHost) {
        this.imapHost = imapHost;
    }

    public String getSmtpHost() {
        return smtpHost;
    }

    public void setSmtpHost(String smtpHost) {
        this.smtpHost = smtpHost;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean aDefault) {
        isDefault = aDefault;
    }
}
