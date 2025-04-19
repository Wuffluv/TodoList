package com.example.todolist;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Главная активность приложения, отвечающая за авторизацию пользователя.
 */
public class MainActivity extends AppCompatActivity {
    // Поля для ввода email и пароля
    private EditText enterEmail, enterPassword;
    // Кнопки для входа и перехода к регистрации
    private Button loginButton, newUserButton;
    // Объект для работы с Firebase Authentication
    private FirebaseAuth mAuth;
    // Пул потоков для выполнения асинхронных задач
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Установка layout для активности
        setContentView(R.layout.activity_main);

        // Инициализация Firebase Authentication
        mAuth = FirebaseAuth.getInstance();
        // Проверка, авторизован ли пользователь
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // Если пользователь уже авторизован, перенаправляем его в MainMenuActivity
            Intent intent = new Intent(MainActivity.this, MainMenuActivity.class);
            intent.putExtra("USER_ID", currentUser.getUid());
            startActivity(intent);
            // Завершаем текущую активность
            finish();
            return;
        }

        // Инициализация элементов интерфейса
        enterEmail = findViewById(R.id.EnterEmail);
        enterPassword = findViewById(R.id.EnterPassword);
        loginButton = findViewById(R.id.LoginButt);
        newUserButton = findViewById(R.id.NewLoginButt);

        // Обработчик нажатия на кнопку "Войти"
        loginButton.setOnClickListener(v -> {
            // Получение введенных данных и удаление пробелов
            String email = enterEmail.getText().toString().trim();
            String password = enterPassword.getText().toString().trim();

            // Проверка, заполнены ли поля
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Введите Email и пароль!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Выполнение авторизации
            executor.execute(() -> {
                mAuth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                // Успешная авторизация
                                FirebaseUser user = mAuth.getCurrentUser();
                                // Обновление UI в главном потоке
                                runOnUiThread(() -> {
                                    // Проверка, что активность не завершена
                                    if (!isFinishing() && !isDestroyed()) {
                                        Toast.makeText(this, "Добро пожаловать!", Toast.LENGTH_SHORT).show();
                                        // Переход на главную активность приложения
                                        Intent intent = new Intent(MainActivity.this, MainMenuActivity.class);
                                        intent.putExtra("USER_ID", user.getUid());
                                        startActivity(intent);
                                        // Очистка полей ввода
                                        enterEmail.setText("");
                                        enterPassword.setText("");
                                        // Завершение текущей активности
                                        finish();
                                    }
                                });
                            } else {
                                // Ошибка авторизации
                                runOnUiThread(() -> {
                                    if (!isFinishing() && !isDestroyed()) {
                                        Toast.makeText(this, "Неверный Email или пароль!", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        });
            });
        });

        // Обработчик нажатия на кнопку "Зарегистрироваться"
        newUserButton.setOnClickListener(v -> {
            // Переход на активность регистрации
            Intent intent = new Intent(MainActivity.this, ActivityRegistration.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Завершение работы пула потоков при уничтожении активности
        executor.shutdownNow();
    }
}