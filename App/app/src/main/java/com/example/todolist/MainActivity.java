package com.example.todolist;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {
    // Объявление переменных для полей ввода и кнопок
    private EditText enterEmail, enterPassword;
    private Button loginButton, newUserButton;
    // Объявление объекта для работы с Firebase Authentication
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Установка макета активности
        setContentView(R.layout.activity_main);

        // Инициализация экземпляра Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Проверка, авторизован ли пользователь
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // Если пользователь уже авторизован
            Log.d("Auth", "Пользователь уже авторизован, UID: " + currentUser.getUid());
            // Создание намерения для перехода в главное меню
            Intent intent = new Intent(MainActivity.this, MainMenuActivity.class);
            intent.putExtra("USER_ID", currentUser.getUid()); // Передача ID пользователя
            startActivity(intent); // Запуск MainMenuActivity
            finish(); // Завершение текущей активности, чтобы не возвращаться назад
            return; // Прерываем выполнение onCreate
        }

        // Если пользователь не авторизован, инициализируем интерфейс входа
        // Привязка элементов интерфейса к переменным
        enterEmail = findViewById(R.id.EnterEmail);       // Поле ввода email
        enterPassword = findViewById(R.id.EnterPassword); // Поле ввода пароля
        loginButton = findViewById(R.id.LoginButt);       // Кнопка входа
        newUserButton = findViewById(R.id.NewLoginButt);  // Кнопка для нового пользователя

        // Установка слушателя нажатия на кнопку входа
        loginButton.setOnClickListener(v -> {
            // Получение введённых данных и удаление лишних пробелов
            String email = enterEmail.getText().toString().trim();
            String password = enterPassword.getText().toString().trim();

            // Проверка, заполнены ли поля email и пароля
            if (email.isEmpty() || password.isEmpty()) {
                // Показ сообщения об ошибке, если поля пустые
                Toast.makeText(this, "Введите Email и пароль!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Логирование попытки входа
            Log.d("Auth", "Попытка входа с email: " + email);
            // Выполнение входа с помощью email и пароля
            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        // Проверка успешности входа
                        if (task.isSuccessful()) {
                            // Получение данных текущего пользователя
                            FirebaseUser user = mAuth.getCurrentUser();
                            // Логирование успешного входа
                            Log.d("Auth", "Успешный вход для UID: " + user.getUid());
                            // Показ приветственного сообщения
                            Toast.makeText(this, "Добро пожаловать!", Toast.LENGTH_SHORT).show();
                            // Создание намерения для перехода в главное меню
                            Intent intent = new Intent(MainActivity.this, MainMenuActivity.class);
                            intent.putExtra("USER_ID", user.getUid()); // Передача ID пользователя
                            startActivity(intent); // Запуск новой активности
                            finish(); // Завершение текущей активности
                        } else {
                            // Получение информации об ошибке
                            Exception e = task.getException();
                            String errorMsg = e != null ? e.getMessage() : "Неизвестная ошибка";
                            // Логирование ошибки входа
                            Log.e("AuthError", "Ошибка входа: " + errorMsg);
                            // Показ сообщения об ошибке аутентификации
                            Toast.makeText(this, "Неверный Email или пароль!", Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        // Установка слушателя нажатия на кнопку регистрации нового пользователя
        newUserButton.setOnClickListener(v -> {
            // Логирование перехода к регистрации
            Log.d("Auth", "Переход к регистрации");
            // Создание намерения для перехода к активности регистрации
            Intent intent = new Intent(MainActivity.this, ActivityRegistration.class);
            startActivity(intent); // Запуск активности регистрации
        });
    }
}