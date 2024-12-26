package com.example.todolist;

import java.util.Date;

public class Task {
    private String description;
    private Date dateTime;
    private boolean completed;

    public Task(String description, Date dateTime) {
        this.description = description;
        this.dateTime = dateTime;
        this.completed = false;
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
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }
}
