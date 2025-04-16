package com.example.todolist;

import java.util.Date;

public class Task {
    private String id;
    private String description;
    private Date dateTime;
    private boolean isCompleted;
    private boolean isExpanded;
    private boolean isSubTasksLoaded; // Новое поле для кэширования подзадач

    public Task() {
        this.isSubTasksLoaded = false;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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
        isCompleted = completed;
    }

    public boolean isExpanded() {
        return isExpanded;
    }

    public void setExpanded(boolean expanded) {
        isExpanded = expanded;
    }

    public boolean isSubTasksLoaded() {
        return isSubTasksLoaded;
    }

    public void setSubTasksLoaded(boolean subTasksLoaded) {
        isSubTasksLoaded = subTasksLoaded;
    }
}