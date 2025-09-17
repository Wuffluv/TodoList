package com.example.todolist;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.PropertyName;

import java.util.Date;

public class Task {
    private String id;
    private String userId;
    private String description;
    private Date dateTime;
    private boolean isCompleted;
    private boolean isExpanded = false;
    private Date reminderTime; // Добавлено поле для времени напоминания

    public Task() {
    }

    public Task(String userId, String description, Date dateTime) {
        this.userId = userId;
        this.description = description;
        this.dateTime = dateTime;
        this.isCompleted = false;
        this.isExpanded = false;
    }

    @Exclude
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
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

    @PropertyName("isCompleted")
    public boolean isCompleted() {
        return isCompleted;
    }

    @PropertyName("isCompleted")
    public void setCompleted(boolean completed) {
        this.isCompleted = completed;
    }

    @PropertyName("isExpanded")
    public boolean isExpanded() {
        return isExpanded;
    }

    @PropertyName("isExpanded")
    public void setExpanded(boolean expanded) {
        this.isExpanded = expanded;
    }

    @PropertyName("reminderTime") // Аннотация для Firestore
    public Date getReminderTime() {
        return reminderTime;
    }

    @PropertyName("reminderTime")
    public void setReminderTime(Date reminderTime) {
        this.reminderTime = reminderTime;
    }
}