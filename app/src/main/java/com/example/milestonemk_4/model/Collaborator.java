package com.example.milestonemk_4.model;

public class Collaborator {
    private String uid;
    private String email;
    private String username;
    public Collaborator() {

    }
    public Collaborator(String uid, String email, String username) {
        this.uid = uid;
        this.email = email;
        this.username = username;
    }

    public String getUid() {
        return uid;
    }

    public String getEmail() {
        return email;
    }

    public String getUsername() {
        return username;
    }

}