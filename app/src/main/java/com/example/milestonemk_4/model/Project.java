package com.example.milestonemk_4.model;

public class Project {
    private String id;
    private String title;
    private String userId;

    public Project() {
    }

    public Project(String title, String userId) {
        this.title = title;
        this.userId = userId;
    }
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
    public String getTitle() {
        return title;
    }
}
