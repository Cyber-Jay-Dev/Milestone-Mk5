package com.example.milestonemk_4.model;

public class Task {
    private String taskName;
    private String status; // urgency
    private String stage;  // To Do, In Progress, Completed

    public Task() {}

    public Task(String taskName, String status, String stage) {
        this.taskName = taskName;
        this.status = status;
        this.stage = stage;
    }

    public String getTaskName() {
        return taskName;
    }

    public String getStatus() {
        return status;
    }

    public String getStage() {
        return stage;
    }

    public void setStage(String stage) {
        this.stage = stage;
    }
}
