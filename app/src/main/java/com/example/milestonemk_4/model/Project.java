package com.example.milestonemk_4.model;

public class Project {
    private String id;
    private String title;
    private String userId;
    private int taskCount;


    public Project() {
    }

    public Project(String title, String userId, int taskCount) {
        this.title = title;
        this.userId = userId;
        this.taskCount = taskCount;
    }
    public int getTaskCount() { return taskCount;}
    public void setTaskCount(int taskCount) { this.taskCount = taskCount;}
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
    public String getTitle() {
        return title;
    }
    public String getUserId() {
        return userId;
    }
}
