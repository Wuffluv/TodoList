package com.example.todolist;

public class SubTask {
    private int subTaskId;
    private int parentTaskId;
    private String description;
    private boolean completed;

    // Для чтения из БД (с ID)
    public SubTask(int subTaskId, int parentTaskId, String description, boolean completed) {
        this.subTaskId = subTaskId;
        this.parentTaskId = parentTaskId;
        this.description = description;
        this.completed = completed;
    }

    // Для «новой» подзадачи (без ID)
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
