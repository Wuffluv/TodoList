package com.example.todolist;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Класс, упрощающий работу с локальной БД:
 * - Создание и обновление таблиц
 * - Открытие/закрытие базы
 * - Методы для вставки, получения и т.д.
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    // Название файла вашей базы (физический .db-файл на устройстве)
    private static final String DATABASE_NAME = "Todolist.db";
    // Версия схемы: меняете, если структура таблицы изменилась.
    private static final int DATABASE_VERSION = 1;

    // Название таблицы и её колонки
    private static final String TABLE_USER = "user";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_USERNAME = "username";
    private static final String COLUMN_PASSWORD = "password";
    private static final String COLUMN_EMAIL = "email";

    public DatabaseHelper(Context context) {
        // context, имя_файла_базы, курсор_фабрика(обычно null), версия
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Вызывается ОДИН раз при первом создании базы
        // Создаём таблицу user
        String createTableUser = "CREATE TABLE " + TABLE_USER + " ("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_USERNAME + " TEXT, "
                + COLUMN_PASSWORD + " TEXT, "
                + COLUMN_EMAIL + " TEXT)";
        db.execSQL(createTableUser);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Вызывается при изменении DATABASE_VERSION
        // Например, если добавляете новую колонку, логика может быть другой:
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USER);
        onCreate(db);
    }

    /**
     * Добавляет пользователя в таблицу user (простая вставка).
     *
     * @param username имя пользователя
     * @param password пароль
     * @param email    email
     * @return id добавленной записи (или -1, если ошибка)
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
     *
     * @param email    email пользователя
     * @param password пароль пользователя
     * @return true, если такой пользователь найден; false — если нет
     */
    public boolean checkUserCredentials(String email, String password) {
        SQLiteDatabase db = this.getReadableDatabase();

        // Ищем запись в таблице user, где email=? AND password=?
        String query = "SELECT * FROM " + TABLE_USER
                + " WHERE " + COLUMN_EMAIL + "=? AND " + COLUMN_PASSWORD + "=?";
        Cursor cursor = db.rawQuery(query, new String[]{email, password});

        boolean exists = (cursor.getCount() > 0);
        cursor.close();
        db.close();

        return exists;
    }
}
