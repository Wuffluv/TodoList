package com.example.todolist;

import java.util.Date;

/**
 * Модель подзадачи (SubTask), которая относится к определённой задаче (Task).
 */
public class SubTask {
    private int subTaskId;     // первичный ключ в таблице subtasks
    private int parentTaskId;  // к какой задаче относится (FK -> tasks.task_id)
    private String description;
    private boolean completed;

    // Конструктор для чтения из БД (уже с ID)
    public SubTask(int subTaskId, int parentTaskId, String description, boolean completed) {
        this.subTaskId = subTaskId;
        this.parentTaskId = parentTaskId;
        this.description = description;
        this.completed = completed;
    }

    // Конструктор для "новой" подзадачи (без ID)
    public SubTask(int parentTaskId, String description) {
        this.parentTaskId = parentTaskId;
        this.description = description;
        this.completed = false;
    }

    public int getSubTaskId() {
        return subTaskId;
    }

    public void setSubTaskId(int subTaskId) {
        this.subTaskId = subTaskId;
    }

    public int getParentTaskId() {
        return parentTaskId;
    }

    public void setParentTaskId(int parentTaskId) {
        this.parentTaskId = parentTaskId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }
}
