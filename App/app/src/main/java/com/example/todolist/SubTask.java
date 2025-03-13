package com.example.todolist;

import com.google.firebase.firestore.Exclude;

public class SubTask {
    private String subTaskId; // Для Firebase
    private int intSubTaskId; // Для SQLite
    private int parentTaskId; // Для SQLite
    private String description;
    private boolean isCompleted;

    public SubTask() {
    }

    // Конструктор для Firebase
    public SubTask(String description) {
        this.description = description;
        this.isCompleted = false;
    }

    // Конструктор для SQLite
    public SubTask(int subTaskId, int parentTaskId, String description, boolean isCompleted) {
        this.intSubTaskId = subTaskId;
        this.parentTaskId = parentTaskId;
        this.description = description;
        this.isCompleted = isCompleted;
    }

    @Exclude
    public String getSubTaskId() {
        return subTaskId;
    }
    public void setSubTaskId(String subTaskId) {
        this.subTaskId = subTaskId;
    }

    @Exclude
    public int getIntSubTaskId() {
        return intSubTaskId;
    }
    public void setIntSubTaskId(int intSubTaskId) {
        this.intSubTaskId = intSubTaskId;
    }

    @Exclude
    public int getParentTaskId() {
        return parentTaskId;
    }
    public void setParentTaskId(int parentTaskId) {
        this.parentTaskId = parentTaskId;
    }

    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isCompleted() {
        return isCompleted;
    }
    public void setCompleted(boolean completed) {
        this.isCompleted = completed;
    }
}