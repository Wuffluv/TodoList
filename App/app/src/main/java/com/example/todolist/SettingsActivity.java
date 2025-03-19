package com.example.todolist;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {
    // Объявление переменной для кнопки выхода
    private Button logoutButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Установка макета активности настроек
        setContentView(R.layout.settings_activity);

        // Привязка кнопки выхода к переменной
        logoutButton = findViewById(R.id.logoutButton);

        // Установка слушателя нажатия на кнопку выхода
        logoutButton.setOnClickListener(v -> {

            // Создание намерения для перехода на экран входа (MainActivity)
            Intent intent = new Intent(SettingsActivity.this, MainActivity.class);

            // Установка флагов для сброса стека активности
            // FLAG_ACTIVITY_NEW_TASK: создаёт новую задачу
            // FLAG_ACTIVITY_CLEAR_TASK: очищает существующий стек активностей
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

            // Запуск активности входа
            startActivity(intent);
            // Завершение текущей активности
            finish();
        });
    }
}