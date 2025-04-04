package com.example.todolist;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class SettingsActivity extends AppCompatActivity {
    private Button logoutButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);

        logoutButton = findViewById(R.id.logoutButton);

        logoutButton.setOnClickListener(v -> {
            // Завершаем сессию пользователя в Firebase
            FirebaseAuth.getInstance().signOut();

            // Создаем Intent для перехода в MainActivity
            Intent intent = new Intent(SettingsActivity.this, MainActivity.class);
            // Очищаем стек активностей, чтобы пользователь не мог вернуться назад
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);

            // Завершаем текущую активность
            finish();
        });
    }
}