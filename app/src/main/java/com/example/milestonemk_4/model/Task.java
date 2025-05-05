package com.example.milestonemk_4.model;

public class Task {
    private String taskName;
    private String status;
    private String stage;
    // Fields for user assignment
    private String assignedUserId;
    private String assignedUsername;

    public Task() {

    }

    public Task(String taskName, String status, String projectId) {
        this.taskName = taskName;
        this.status = status;
        this.stage = "To Do";
    }

//    public Task(String taskName, String status, String stage, String assignedUserId, String assignedUsername) {
//        this.taskName = taskName;
//        this.status = status;
//        this.stage = stage;
//        this.assignedUserId = assignedUserId;
//        this.assignedUsername = assignedUsername;
//    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
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
    public void setAssignedUserId(String assignedUserId) {
        this.assignedUserId = assignedUserId;
    }
    public String getAssignedUserId() {
        return assignedUserId;
    }

    public String getAssignedUsername() {
        return assignedUsername;
    }
    public void setAssignedUsername(String assignedUsername) {
        this.assignedUsername = assignedUsername;
    }
}