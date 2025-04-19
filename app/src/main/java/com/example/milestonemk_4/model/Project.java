package com.example.milestonemk_4.model;

import com.google.firebase.firestore.IgnoreExtraProperties;
import java.util.ArrayList;
import java.util.List;

@IgnoreExtraProperties
public class Project {
    private String id;
    private String title;
    private String userId;
    private int taskCount;
    private List<String> allowedUsers; // List of user IDs who are allowed to access

    public Project() {
        // Required empty constructor for Firestore
        this.allowedUsers = new ArrayList<>();
    }

    public Project(String title, String userId, int taskCount) {
        this.title = title;
        this.userId = userId;
        this.taskCount = taskCount;
        this.allowedUsers = new ArrayList<>();
    }

    public int getTaskCount() {
        return taskCount;
    }

    public void setTaskCount(int taskCount) {
        this.taskCount = taskCount;
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

    public String getUserId() {
        return userId;
    }

    public List<String> getAllowedUsers() {
        return allowedUsers;
    }

    public void setAllowedUsers(List<String> allowedUsers) {
        this.allowedUsers = allowedUsers;
    }

    public void addAllowedUser(String userId) {
        if (this.allowedUsers == null) {
            this.allowedUsers = new ArrayList<>();
        }
        if (!this.allowedUsers.contains(userId)) {
            this.allowedUsers.add(userId);
        }
    }

    public void removeAllowedUser(String userId) {
        if (this.allowedUsers != null) {
            this.allowedUsers.remove(userId);
        }
    }
}