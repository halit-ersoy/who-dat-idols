package com.ses.whodatidols.viewmodel;

import java.time.LocalDateTime;

public class CommentViewModel {
    private java.util.UUID id;
    private java.util.UUID parentId;
    private String nickname;
    private String comment;
    private LocalDateTime date;
    private boolean spoiler;
    private int likeCount;
    private boolean likedByCurrentUser;
    private boolean author;
    private java.util.List<CommentViewModel> replies;
    private String profilePhoto;

    public java.util.UUID getId() {
        return id;
    }

    public void setId(java.util.UUID id) {
        this.id = id;
    }

    public java.util.UUID getParentId() {
        return parentId;
    }

    public void setParentId(java.util.UUID parentId) {
        this.parentId = parentId;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }

    public boolean isSpoiler() {
        return spoiler;
    }

    public void setSpoiler(boolean spoiler) {
        this.spoiler = spoiler;
    }

    public int getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(int likeCount) {
        this.likeCount = likeCount;
    }

    public boolean isLikedByCurrentUser() {
        return likedByCurrentUser;
    }

    public void setLikedByCurrentUser(boolean likedByCurrentUser) {
        this.likedByCurrentUser = likedByCurrentUser;
    }

    public java.util.List<CommentViewModel> getReplies() {
        return replies;
    }

    public void setReplies(java.util.List<CommentViewModel> replies) {
        this.replies = replies;
    }

    public boolean isAuthor() {
        return author;
    }

    public void setAuthor(boolean author) {
        this.author = author;
    }

    public String getProfilePhoto() {
        return profilePhoto;
    }

    public void setProfilePhoto(String profilePhoto) {
        this.profilePhoto = profilePhoto;
    }
}
