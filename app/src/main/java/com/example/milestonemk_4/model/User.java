package com.example.milestonemk_4.model;

public class User {
    private String uid;
    private String username;
    private String email;
    private int avatarId;
    private String profileBgColor;

    public User() {
        // Required empty constructor for Firestore
    }

    public User(String uid, String username, String email, int avatarId, String profileBgColor) {
        this.uid = uid;
        this.username = username;
        this.email = email;
        this.avatarId = avatarId;
        this.profileBgColor = profileBgColor;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public int getAvatarId() {
        return avatarId;
    }

    public void setAvatarId(int avatarId) {
        this.avatarId = avatarId;
    }

    public String getProfileBgColor() {
        return profileBgColor;
    }

    public void setProfileBgColor(String profileBgColor) {
        this.profileBgColor = profileBgColor;
    }

    @Override
    public String toString() {
        return username; // This will be displayed in the dropdown
    }
}