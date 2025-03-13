package com.example.todolist;

import com.google.firebase.firestore.Exclude;

import java.util.Date;

public class Task {
    private String id; // Для Firebase
    private int taskId; // Для SQLite
    private String userId; // Для Firebase
    private int intUserId; // Для SQLite
    private String description;
    private Date dateTime;
    private boolean isCompleted;

    public Task() {
    }

    // Конструктор для Firebase
    public Task(String userId, String description, Date dateTime) {
        this.userId = userId;
        this.description = description;
        this.dateTime = dateTime;
        this.isCompleted = false;
    }

    // Конструктор для SQLite
    public Task(int taskId, int userId, String description, Date dateTime, boolean isCompleted) {
        this.taskId = taskId;
        this.intUserId = userId;
        this.description = description;
        this.dateTime = dateTime;
        this.isCompleted = isCompleted;
    }

    @Exclude
    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }

    @Exclude
    public int getTaskId() {
        return taskId;
    }
    public void setTaskId(int taskId) {
        this.taskId = taskId;
    }

    public String getUserId() {
        return userId;
    }
    public void setUserId(String userId) {
        this.userId = userId;
    }

    @Exclude
    public int getIntUserId() {
        return intUserId;
    }
    public void setIntUserId(int intUserId) {
        this.intUserId = intUserId;
    }

    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }

    public Date getDateTime() {
        return dateTime;
    }
    public void setDateTime(Date dateTime) {
        this.dateTime = dateTime;
    }

    public boolean isCompleted() {
        return isCompleted;
    }
    public void setCompleted(boolean completed) {
        this.isCompleted = completed;
    }
}