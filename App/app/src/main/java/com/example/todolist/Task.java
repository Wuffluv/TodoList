package com.example.todolist;

import java.util.Date;

public class Task {
    private int id;          // task_id из БД
    private int userId;      // какой пользователь владелец
    private String description;
    private Date dateTime;
    private boolean completed;

    // Конструктор для чтения задачи из БД
    public Task(int id, int userId, String description, Date dateTime, boolean completed) {
        this.id = id;
        this.userId = userId;
        this.description = description;
        this.dateTime = dateTime;
        this.completed = completed;
    }

    // Конструктор для создания "новой" задачи (ещё без id, isCompleted = false)
    public Task(int userId, String description, Date dateTime) {
        this.userId = userId;
        this.description = description;
        this.dateTime = dateTime;
        this.completed = false;
    }

    // Геттеры/сеттеры
    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }
    public void setUserId(int userId) {
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

    public boolean isCompleted() {
        return completed;
    }
    public void setCompleted(boolean completed) {
        this.completed = completed;
    }
}
