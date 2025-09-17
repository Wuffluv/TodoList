package com.example.todolist;

import com.google.firebase.firestore.PropertyName;

public class SubTask {
    private String subTaskId;
    private String description;
    private boolean isCompleted;

    public SubTask() {
    }

    public SubTask(String description, boolean isCompleted) {
        this.description = description;
        this.isCompleted = isCompleted;
    }

    public String getSubTaskId() {
        return subTaskId;
    }

    public void setSubTaskId(String subTaskId) {
        this.subTaskId = subTaskId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @PropertyName("isCompleted")
    public boolean isCompleted() {
        return isCompleted;
    }

    @PropertyName("isCompleted")
    public void setCompleted(boolean completed) {
        this.isCompleted = completed;
    }
}