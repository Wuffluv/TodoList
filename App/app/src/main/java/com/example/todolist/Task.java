package com.example.todolist;

import com.google.firebase.firestore.Exclude;
import java.util.Date;

// Класс Task представляет модель данных для задачи
public class Task {
    // Уникальный идентификатор задачи
    private String id;
    // Идентификатор пользователя, которому принадлежит задача
    private String userId;
    // Описание задачи
    private String description;
    // Дата и время выполнения задачи
    private Date dateTime;
    // Статус выполнения задачи (выполнена или нет)
    private boolean isCompleted;
    // Статус раскрытия задачи в интерфейсе (например, для отображения подзадач)
    private boolean isExpanded = false;

    // Пустой конструктор, необходимый для десериализации данных из Firestore
    public Task() {
    }

    // Конструктор с параметрами для создания задачи с заданными значениями
    public Task(String userId, String description, Date dateTime) {
        this.userId = userId; // Установка ID пользователя
        this.description = description; // Установка описания задачи
        this.dateTime = dateTime; // Установка даты и времени
        this.isCompleted = false; // Установка начального статуса выполнения (не выполнена)
        this.isExpanded = false; // Установка начального статуса раскрытия (не раскрыта)
    }

    // Геттер для получения идентификатора задачи, исключён из сериализации в Firestore
    @Exclude
    public String getId() {
        return id;
    }

    // Сеттер для установки идентификатора задачи
    public void setId(String id) {
        this.id = id;
    }

    // Геттер для получения идентификатора пользователя
    public String getUserId() {
        return userId;
    }

    // Сеттер для установки идентификатора пользователя
    public void setUserId(String userId) {
        this.userId = userId;
    }

    // Геттер для получения описания задачи
    public String getDescription() {
        return description;
    }

    // Сеттер для установки описания задачи
    public void setDescription(String description) {
        this.description = description;
    }

    // Геттер для получения даты и времени задачи
    public Date getDateTime() {
        return dateTime;
    }

    // Сеттер для установки даты и времени задачи
    public void setDateTime(Date dateTime) {
        this.dateTime = dateTime;
    }

    // Геттер для проверки статуса выполнения задачи
    public boolean isCompleted() {
        return isCompleted;
    }

    // Сеттер для установки статуса выполнения задачи
    public void setCompleted(boolean completed) {
        this.isCompleted = completed;
    }

    // Геттер для проверки статуса раскрытия задачи
    public boolean isExpanded() {
        return isExpanded;
    }

    // Сеттер для установки статуса раскрытия задачи
    public void setExpanded(boolean expanded) {
        this.isExpanded = expanded;
    }
}