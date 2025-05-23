package com.example.milestonemk_4.model;

public class Task {
    private String taskName;
    private String status;
    private String stage;

    public Task(String taskName, String status, String stage) {
        this.taskName = taskName;
        this.status = status;
        this.stage = stage;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStage() {
        return stage;
    }

    public void setStage(String stage) {
        this.stage = stage;
    }
}