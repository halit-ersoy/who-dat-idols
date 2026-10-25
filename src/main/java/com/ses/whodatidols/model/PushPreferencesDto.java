package com.ses.whodatidols.model;

public class PushPreferencesDto {
    private boolean allowMessagePush = true;
    private boolean allowCommentPush = true;
    private boolean allowMessagePreview = false;

    public PushPreferencesDto() {
    }

    public PushPreferencesDto(boolean allowMessagePush, boolean allowCommentPush, boolean allowMessagePreview) {
        this.allowMessagePush = allowMessagePush;
        this.allowCommentPush = allowCommentPush;
        this.allowMessagePreview = allowMessagePreview;
    }

    public boolean isAllowMessagePush() {
        return allowMessagePush;
    }

    public void setAllowMessagePush(boolean allowMessagePush) {
        this.allowMessagePush = allowMessagePush;
    }

    public boolean isAllowCommentPush() {
        return allowCommentPush;
    }

    public void setAllowCommentPush(boolean allowCommentPush) {
        this.allowCommentPush = allowCommentPush;
    }

    public boolean isAllowMessagePreview() {
        return allowMessagePreview;
    }

    public void setAllowMessagePreview(boolean allowMessagePreview) {
        this.allowMessagePreview = allowMessagePreview;
    }
}
