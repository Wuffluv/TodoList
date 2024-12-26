package com.example.todolist;

import java.util.Date;

public class Task {
    private String description;
    private Date dateTime;
    private boolean isCompleted;

    public Task(String description, Date dateTime) {
        this.description = description;
        this.dateTime = dateTime;
        this.isCompleted = false;
    }

    public String getDescription() {
        return description;
    }

    public Date getDateTime() {
        return dateTime;
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public void setCompleted(boolean completed) {
        isCompleted = completed;
    }
}
