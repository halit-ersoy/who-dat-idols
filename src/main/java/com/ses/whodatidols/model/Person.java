package com.ses.whodatidols.model;

import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Person {
    private UUID id;
    private String nickname;
    private String name;
    private String surname;
    private String email;
    private String password;
    private String profilePhoto;
    private boolean isBanned;
    private String banReason;
    private String role;
    private boolean allowMessages = true;

    // Default constructor
    public Person() {
    }

    public Person(String nickname, String name, String surname, String email, String password) {
        this.nickname = nickname;
        this.name = name;
        this.surname = surname;
        this.email = email;
        this.password = password;
    }

    // Getters and setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getProfilePhoto() {
        return profilePhoto;
    }

    public void setProfilePhoto(String profilePhoto) {
        this.profilePhoto = profilePhoto;
    }

    @JsonProperty("isBanned")
    public boolean isBanned() {
        return isBanned;
    }

    public void setBanned(boolean banned) {
        isBanned = banned;
    }

    public String getBanReason() {
        return banReason;
    }

    public void setBanReason(String banReason) {
        this.banReason = banReason;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public boolean isAllowMessages() {
        return allowMessages;
    }

    public void setAllowMessages(boolean allowMessages) {
        this.allowMessages = allowMessages;
    }
}