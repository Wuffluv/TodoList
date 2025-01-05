package com.example.todolist;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Экран логина: ввод Email и Password.
 * При успешном логине -> MainMenuActivity (передаём userId).
 */
public class MainActivity extends AppCompatActivity {

    private EditText enterEmail, enterPassword;
    private Button loginButton, newUserButton;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Инициализируем поля
        enterEmail = findViewById(R.id.EnterEmail);
        enterPassword = findViewById(R.id.EnterPassword);
        loginButton = findViewById(R.id.LoginButt);
        newUserButton = findViewById(R.id.NewLoginButt);

        // Создаём helper
        dbHelper = new DatabaseHelper(this);

        // Кнопка Login
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String email = enterEmail.getText().toString().trim();
                String password = enterPassword.getText().toString().trim();

                if (email.isEmpty() || password.isEmpty()) {
                    Toast.makeText(MainActivity.this,
                            "Введите Email и пароль!",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                boolean exists = dbHelper.checkUserCredentials(email, password);
                if (exists) {
                    // Получаем userId
                    int userId = dbHelper.getUserIdByEmail(email);

                    Toast.makeText(MainActivity.this,
                            "Добро пожаловать!",
                            Toast.LENGTH_SHORT).show();

                    // Переходим на MainMenuActivity, передаём userId
                    Intent intent = new Intent(MainActivity.this, MainMenuActivity.class);
                    intent.putExtra("USER_ID", userId);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(MainActivity.this,
                            "Неверный Email или пароль!",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Кнопка "New User" -> ActivityRegistration
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
