package com.ses.whodatidols.viewmodel;

public class MailFolderDto {
    private String id;
    private String name;
    private String displayName;
    private String icon;
    private int unreadCount;
    private int totalCount;

    public MailFolderDto() {
    }

    public MailFolderDto(String id, String name, String displayName, String icon, int unreadCount, int totalCount) {
        this.id = id;
        this.name = name;
        this.displayName = displayName;
        this.icon = icon;
        this.unreadCount = unreadCount;
        this.totalCount = totalCount;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(int unreadCount) {
        this.unreadCount = unreadCount;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }
}
