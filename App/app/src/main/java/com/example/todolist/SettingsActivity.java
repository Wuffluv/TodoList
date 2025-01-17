package com.example.todolist;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    private Button logoutButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);

        logoutButton = findViewById(R.id.logoutButton);

        logoutButton.setOnClickListener(v -> {
            // Если храните пользовательские данные (логин/пароль) в SharedPreferences:
            // getSharedPreferences("myPrefs", MODE_PRIVATE).edit().clear().apply();

            // Возвращаемся на экран логина (MainActivity)
            Intent intent = new Intent(SettingsActivity.this, MainActivity.class);

            // Сбрасываем back stack, чтобы кнопка "Назад" не вернула в MainMenuActivity
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

            startActivity(intent);
            finish();
        });
    }
}
