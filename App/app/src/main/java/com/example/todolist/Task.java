package com.example.todolist;

// Импорт класса Date для работы с датой и временем
import java.util.Date;

// Класс Task представляет задачу в приложении
public class Task {
    // Уникальный идентификатор задачи
    private String id;
    // Описание задачи
    private String description;
    // Дата и время выполнения задачи
    private Date dateTime;
    // Флаг, указывающий, выполнена ли задача
    private boolean isCompleted;
    // Флаг, указывающий, развернут ли список подзадач
    private boolean isExpanded;
    // Флаг, указывающий, загружены ли подзадачи (для кэширования)
    private boolean isSubTasksLoaded;

    // Пустой конструктор, необходим для десериализации Firestore
    public Task() {
        // Инициализация флага загрузки подзадач как false
        this.isSubTasksLoaded = false;
    }

    // Метод для получения идентификатора задачи
    public String getId() {
        return id;
    }

    // Метод для установки идентификатора задачи
    public void setId(String id) {
        this.id = id;
    }

    // Метод для получения описания задачи
    public String getDescription() {
        return description;
    }

    // Метод для установки описания задачи
    public void setDescription(String description) {
        this.description = description;
    }

    // Метод для получения даты и времени задачи
    public Date getDateTime() {
        return dateTime;
    }

    // Метод для установки даты и времени задачи
    public void setDateTime(Date dateTime) {
        this.dateTime = dateTime;
    }

    // Метод для проверки, выполнена ли задача
    public boolean isCompleted() {
        return isCompleted;
    }

    // Метод для установки статуса выполнения задачи
    public void setCompleted(boolean completed) {
        isCompleted = completed;
    }

    // Метод для проверки, развернут ли список подзадач
    public boolean isExpanded() {
        return isExpanded;
    }

    // Метод для установки статуса развернутости списка подзадач
    public void setExpanded(boolean expanded) {
        isExpanded = expanded;
    }

    // Метод для проверки, загружены ли подзадачи
    public boolean isSubTasksLoaded() {
        return isSubTasksLoaded;
    }

    // Метод для установки статуса загрузки подзадач
    public void setSubTasksLoaded(boolean subTasksLoaded) {
        isSubTasksLoaded = subTasksLoaded;
    }
}