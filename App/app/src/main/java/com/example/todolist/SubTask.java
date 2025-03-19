package com.example.todolist;

// Класс SubTask представляет модель данных для подзадачи
public class SubTask {
    // Уникальный идентификатор подзадачи
    private String subTaskId;
    // Описание подзадачи
    private String description;
    // Статус выполнения подзадачи (выполнена или нет)
    private boolean isCompleted;

    // Пустой конструктор, необходимый для десериализации данных из Firestore
    public SubTask() {
    }

    // Конструктор с параметрами для создания подзадачи с заданным описанием и статусом
    public SubTask(String description, boolean isCompleted) {
        this.description = description; // Установка описания подзадачи
        this.isCompleted = isCompleted; // Установка статуса выполнения
    }

    // Геттер для получения идентификатора подзадачи
    public String getSubTaskId() {
        return subTaskId;
    }

    // Сеттер для установки идентификатора подзадачи
    public void setSubTaskId(String subTaskId) {
        this.subTaskId = subTaskId;
    }

    // Геттер для получения описания подзадачи
    public String getDescription() {
        return description;
    }

    // Сеттер для установки описания подзадачи
    public void setDescription(String description) {
        this.description = description;
    }

    // Геттер для проверки статуса выполнения подзадачи
    public boolean isCompleted() {
        return isCompleted;
    }

    // Сеттер для установки статуса выполнения подзадачи
    public void setCompleted(boolean completed) {
        this.isCompleted = completed;
    }
}