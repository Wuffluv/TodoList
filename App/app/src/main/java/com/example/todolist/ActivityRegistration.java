package com.example.todolist;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast; // Чтобы показывать Toast
import androidx.appcompat.app.AppCompatActivity;

public class ActivityRegistration extends AppCompatActivity {

    private EditText newEmail;
    private EditText newName;
    private EditText newPassword;
    private Button registryButton;

    private DatabaseHelper dbHelper; // наш класс для работы с БД

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        // Инициализируем Helper (это создаст/откроет базу)
        dbHelper = new DatabaseHelper(this);

        // Находим поля
        newEmail = findViewById(R.id.NewEmail);
        newName = findViewById(R.id.NewName);
        newPassword = findViewById(R.id.NewPassword);

        // Кнопка "Registry"
        registryButton = findViewById(R.id.RegistryButt);

        // Обработчик нажатия
        registryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String email = newEmail.getText().toString().trim();
                String name = newName.getText().toString().trim();
                String password = newPassword.getText().toString().trim();

                // Простейшая проверка на пустоту
                if (email.isEmpty() || name.isEmpty() || password.isEmpty()) {
                    Toast.makeText(ActivityRegistration.this,
                            "Заполните все поля!",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                // Добавляем пользователя в базу
                long result = dbHelper.addUser(name, password, email);
                if (result == -1) {
                    // Ошибка при вставке
                    Toast.makeText(ActivityRegistration.this,
                            "Ошибка при регистрации",
                            Toast.LENGTH_SHORT).show();
                } else {
                    // Успешная вставка
                    Toast.makeText(ActivityRegistration.this,
                            "Регистрация успешна!",
                            Toast.LENGTH_SHORT).show();

                    // Переход на MainMenuActivity
                    Intent intent = new Intent(ActivityRegistration.this, MainMenuActivity.class);
                    startActivity(intent);
                    finish();
                }
            }
        });
    }
}
