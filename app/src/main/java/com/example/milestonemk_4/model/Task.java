package com.example.milestonemk_4.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Task {
    private String taskName;
    private String status;
    private String stage;
    // Fields for user assignment
    private String assignedUserId;
    private String assignedUsername;
    private List<Map<String, String>> attachments;


    public Task() {

    }

    public Task(String taskName, String status, String stage) {
        this.taskName = taskName;
        this.status = status;
        this.stage = "To Do";
        this.attachments = new ArrayList<>();
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

    public List<Map<String, String>> getAttachments() {
        if (attachments == null) {
            attachments = new ArrayList<>();
        }
        return attachments;
    }

    public void setAttachments(List<Map<String, String>> attachments) {
        this.attachments = attachments;
    }

    // Helper method to add an attachment
    public void addAttachment(String fileName, String fileUri) {
        if (attachments == null) {
            attachments = new ArrayList<>();
        }
        Map<String, String> attachment = new HashMap<>();
        attachment.put("name", fileName);
        attachment.put("uri", fileUri);
        attachments.add(attachment);
    }
}