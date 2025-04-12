package com.example.milestonemk_4.model;

public class Task {
    private String TaskName;
    private  String Status;

    public Task() {

    }

    public Task(String taskName, String status) {
        TaskName = taskName;
        Status = status;
    }

    public String getTaskName() {
        return TaskName;
    }

    public String getStatus() {
        return Status;
    }
}
