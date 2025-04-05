package com.example.milestonemk_4.model;

public class Project {
    private String title;
    private String userId;


    public Project() {
    }

    public Project(String title, String userId)
    {
        this.title = title;
        this.userId = userId;
    }

    public String getTitle() {
        return title;
    }

    public String getUserId() {
        return userId;
    }
}

