package com.example.todolist;

import android.os.Bundle;

import android.content.Intent;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // Это начальный экран

        //Инициализация кнопок
        Button loginButton = findViewById(R.id.LoginButt); // Кнопка Login
        Button newUserButton = findViewById(R.id.NewLoginButt); // Кнопка New User

        // Обработчик для кнопки Login
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Переход на mainmenu.xml (MainMenuActivity)
                Intent intent = new Intent(MainActivity.this, MainMenuActivity.class);
                startActivity(intent);
            }
        });

        // Обработчик для кнопки New User
        newUserButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Переход на activity_registration.xml (ActivityRegistration)
                Intent intent = new Intent(MainActivity.this, ActivityRegistration.class);
                startActivity(intent);
            }
        });


    }
}