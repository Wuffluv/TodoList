package com.example.todolist;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private EditText enterEmail, enterPassword;
    private Button loginButton, newUserButton;

    private DatabaseHelper dbHelper; // для работы с SQLite

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // Это ваш XML с полями Email/Password и кнопками

        // Инициализация полей ввода
        enterEmail = findViewById(R.id.EnterEmail);
        enterPassword = findViewById(R.id.EnterPassword);

        // Инициализация кнопок
        loginButton = findViewById(R.id.LoginButt);    // Кнопка "Login"
        newUserButton = findViewById(R.id.NewLoginButt); // Кнопка "New User"

        // Создаём экземпляр DatabaseHelper
        dbHelper = new DatabaseHelper(this);

        // Обработчик для кнопки Login
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // Считываем email и пароль, введённые пользователем
                String email = enterEmail.getText().toString().trim();
                String password = enterPassword.getText().toString().trim();

                // Проверка на пустоту
                if (email.isEmpty() || password.isEmpty()) {
                    Toast.makeText(MainActivity.this,
                            "Введите Email и пароль!",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                // Проверяем в базе, существует ли такой пользователь
                boolean exists = dbHelper.checkUserCredentials(email, password);
                if (exists) {
                    // Успешный вход
                    Toast.makeText(MainActivity.this,
                            "Добро пожаловать!",
                            Toast.LENGTH_SHORT).show();

                    // Переход на главное меню (MainMenuActivity)
                    Intent intent = new Intent(MainActivity.this, MainMenuActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    // Неверные данные
                    Toast.makeText(MainActivity.this,
                            "Неверный Email или пароль!",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Обработчик для кнопки New User
        newUserButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent =
                        new Intent(MainActivity.this, ActivityRegistration.class);
                startActivity(intent);
            }
        });
    }
}
