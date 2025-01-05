package com.example.todolist;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Класс, упрощающий работу с локальной БД:
 * - Создание и обновление таблиц (user и tasks)
 * - Открытие/закрытие базы
 * - Методы для пользователей (addUser, checkUserCredentials, getUserIdByEmail)
 * - Методы для задач (addTask, getTasksForUser, updateTaskCompletion, deleteTask)
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    // Название файла вашей базы (физический .db-файл на устройстве)
    private static final String DATABASE_NAME = "Todolist.db";
    // Версия схемы: меняем, если структура таблицы изменилась (добавили userId в tasks).
    private static final int DATABASE_VERSION = 3;

    // Таблица user
    private static final String TABLE_USER = "user";
    private static final String COLUMN_ID = "id";                // PK
    private static final String COLUMN_USERNAME = "username";
    private static final String COLUMN_PASSWORD = "password";
    private static final String COLUMN_EMAIL = "email";

    // Таблица tasks
    private static final String TABLE_TASKS = "tasks";
    private static final String COLUMN_TASK_ID = "task_id";      // PK
    private static final String COLUMN_USER_ID = "userId";       // FK -> user.id
    private static final String COLUMN_DESCRIPTION = "description";
    private static final String COLUMN_DATE_TIME = "dateTime";   // храним long (в мс)
    private static final String COLUMN_IS_COMPLETED = "isCompleted";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // Вызывается ОДИН раз при первом создании базы
    @Override
    public void onCreate(SQLiteDatabase db) {
        // Создаём таблицу user
        String createTableUser = "CREATE TABLE " + TABLE_USER + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_USERNAME + " TEXT, " +
                COLUMN_PASSWORD + " TEXT, " +
                COLUMN_EMAIL + " TEXT)";
        db.execSQL(createTableUser);

        // Создаём таблицу tasks (с userId, чтобы знать, кому принадлежит задача)
        String createTableTasks = "CREATE TABLE " + TABLE_TASKS + " (" +
                COLUMN_TASK_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_USER_ID + " INTEGER, " +
                COLUMN_DESCRIPTION + " TEXT, " +
                COLUMN_DATE_TIME + " INTEGER, " +
                COLUMN_IS_COMPLETED + " INTEGER, " +
                "FOREIGN KEY(" + COLUMN_USER_ID + ") REFERENCES " + TABLE_USER + "(" + COLUMN_ID + "))";
        db.execSQL(createTableTasks);
    }

    // Вызывается при изменении DATABASE_VERSION
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Удаляем старые таблицы и создаём заново (простой вариант миграции)
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TASKS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USER);
        onCreate(db);
    }

    // ----------------------- Методы для пользователей ------------------------

    /**
     * Добавляет пользователя в таблицу user (простая вставка).
     */
    public long addUser(String username, String password, String email) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_USERNAME, username);
        values.put(COLUMN_PASSWORD, password);
        values.put(COLUMN_EMAIL, email);

        long rowId = db.insert(TABLE_USER, null, values);
        db.close();
        return rowId;
    }

    /**
     * Проверяет, существует ли пользователь с указанным email и паролем.
     * @return true, если такой пользователь найден; false — если нет
     */
    public boolean checkUserCredentials(String email, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM " + TABLE_USER +
                " WHERE " + COLUMN_EMAIL + "=? AND " + COLUMN_PASSWORD + "=?";
        Cursor cursor = db.rawQuery(query, new String[]{email, password});

        boolean exists = (cursor.getCount() > 0);
        cursor.close();
        db.close();
        return exists;
    }

    /**
     * Получить ID пользователя (user.id) по его email.
     * Возвращает -1, если не найден.
     */
    public int getUserIdByEmail(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT " + COLUMN_ID + " FROM " + TABLE_USER + " WHERE " + COLUMN_EMAIL + "=?";
        Cursor cursor = db.rawQuery(query, new String[]{email});

        int userId = -1;
        if (cursor.moveToFirst()) {
            userId = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID));
        }
        cursor.close();
        db.close();
        return userId;
    }

    // ----------------------- Методы для задач ------------------------

    /**
     * Добавить новую задачу, принадлежащую userId
     */
    public long addTask(int userId, String description, long dateTime, boolean isCompleted) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_USER_ID, userId);
        values.put(COLUMN_DESCRIPTION, description);
        values.put(COLUMN_DATE_TIME, dateTime);
        values.put(COLUMN_IS_COMPLETED, isCompleted ? 1 : 0);

        long rowId = db.insert(TABLE_TASKS, null, values);
        db.close();
        return rowId;
    }

    /**
     * Получить все задачи для конкретного userId
     */
    public List<Task> getTasksForUser(int userId) {
        List<Task> tasks = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM " + TABLE_TASKS + " WHERE " + COLUMN_USER_ID + "=?";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(userId)});

        if (cursor.moveToFirst()) {
            do {
                int taskId = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_TASK_ID));
                int uId = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_USER_ID));
                String description = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DESCRIPTION));
                long dateTimeMillis = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_DATE_TIME));
                int completedInt = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_COMPLETED));
                boolean completed = (completedInt == 1);

                Task task = new Task(taskId, uId, description, new Date(dateTimeMillis), completed);
                tasks.add(task);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return tasks;
    }

    /**
     * Обновить isCompleted задачи
     */
    public void updateTaskCompletion(int taskId, boolean isCompleted) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_IS_COMPLETED, isCompleted ? 1 : 0);

        db.update(TABLE_TASKS, values, COLUMN_TASK_ID + "=?",
                new String[]{String.valueOf(taskId)});
        db.close();
    }

    /**
     * Удалить задачу
     */
    public void deleteTask(int taskId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_TASKS, COLUMN_TASK_ID + "=?",
                new String[]{String.valueOf(taskId)});
        db.close();
    }
}
