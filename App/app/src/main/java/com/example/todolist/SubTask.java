package com.example.todolist;

// Класс SubTask представляет подзадачу, которая может быть частью основной задачи
public class SubTask {
    // Уникальный идентификатор подзадачи
    private String subTaskId;
    // Описание подзадачи
    private String description;
    // Флаг, указывающий, выполнена ли подзадача
    private boolean isCompleted;

    // Пустой конструктор, необходим для десериализации Firestore
    public SubTask() {
    }

    // Конструктор с параметрами для создания подзадачи
    public SubTask(String description, boolean isCompleted) {
        // Инициализация описания подзадачи
        this.description = description;
        // Инициализация статуса выполнения подзадачи
        this.isCompleted = isCompleted;
    }

    // Метод для получения идентификатора подзадачи
    public String getSubTaskId() {
        return subTaskId;
    }

    // Метод для установки идентификатора подзадачи
    public void setSubTaskId(String subTaskId) {
        this.subTaskId = subTaskId;
    }

    // Метод для получения описания подзадачи
    public String getDescription() {
        return description;
    }

    // Метод для установки описания подзадачи
    public void setDescription(String description) {
        this.description = description;
    }

    // Метод для проверки, выполнена ли подзадача
    public boolean isCompleted() {
        return isCompleted;
    }

    // Метод для установки статуса выполнения подзадачи
    public void setCompleted(boolean completed) {
        this.isCompleted = completed;
    }
}