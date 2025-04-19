package com.example.todolist;

// Импорт необходимых классов для работы с Android
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

// Импорт классов для работы с Firebase
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

// Импорт классов для работы с коллекциями и асинхронными операциями
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

// Класс ActivityRegistration, наследующий AppCompatActivity, отвечает за регистрацию пользователя
public class ActivityRegistration extends AppCompatActivity {
    // Объявление полей для ввода данных: email, имя и пароль
    private EditText newEmail, newName, newPassword;
    // Объявление кнопки для регистрации
    private Button registryButton;
    // Объявление объекта для работы с аутентификацией Firebase
    private FirebaseAuth mAuth;
    // Объявление объекта для работы с базой данных Firestore
    private FirebaseFirestore db;

    // Метод, вызываемый при создании активности
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Установка разметки для активности из файла activity_registration.xml
        setContentView(R.layout.activity_registration);

        // Инициализация объекта FirebaseAuth для работы с аутентификацией
        mAuth = FirebaseAuth.getInstance();
        // Инициализация объекта FirebaseFirestore для работы с базой данных
        db = FirebaseFirestore.getInstance();

        // Связывание полей ввода с соответствующими элементами интерфейса по их ID
        newEmail = findViewById(R.id.NewEmail);
        newName = findViewById(R.id.NewName);
        newPassword = findViewById(R.id.NewPassword);
        // Связывание кнопки регистрации с элементом интерфейса по ID
        registryButton = findViewById(R.id.RegistryButt);

        // Установка слушателя нажатий на кнопку регистрации
        registryButton.setOnClickListener(v -> {
            // Получение введенных данных из полей ввода и удаление пробелов
            String email = newEmail.getText().toString().trim();
            String name = newName.getText().toString().trim();
            String password = newPassword.getText().toString().trim();

            // Проверка, заполнены ли все поля
            if (email.isEmpty() || name.isEmpty() || password.isEmpty()) {
                // Вывод сообщения об ошибке, если хотя бы одно поле пустое
                Toast.makeText(this, "Заполните все поля!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Проверка минимальной длины пароля (не менее 6 символов)
            if (password.length() < 6) {
                // Вывод сообщения об ошибке, если пароль слишком короткий
                Toast.makeText(this, "Пароль должен быть не менее 6 символов", Toast.LENGTH_SHORT).show();
                return;
            }

            // Выполнение регистрации
            Executors.newSingleThreadExecutor().execute(() -> {
                // Создание нового пользователя с помощью email и пароля
                mAuth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(task -> {
                            // Проверка успешности регистрации
                            if (task.isSuccessful()) {
                                // Получение текущего пользователя
                                FirebaseUser user = mAuth.getCurrentUser();
                                // Создание объекта для хранения данных пользователя
                                Map<String, Object> userData = new HashMap<>();
                                userData.put("username", name); // Добавление имени пользователя
                                userData.put("email", email);   // Добавление email пользователя

                                // Сохранение данных пользователя в коллекции "users" в Firestore
                                db.collection("users").document(user.getUid())
                                        .set(userData)
                                        .addOnSuccessListener(aVoid -> runOnUiThread(() -> {
                                            // Вывод сообщения об успешной регистрации
                                            Toast.makeText(this, "Регистрация успешна!", Toast.LENGTH_SHORT).show();
                                            // Создание намерения для перехода в MainMenuActivity
                                            Intent intent = new Intent(ActivityRegistration.this, MainMenuActivity.class);
                                            // Передача ID пользователя в следующую активность
                                            intent.putExtra("USER_ID", user.getUid());
                                            // Запуск MainMenuActivity
                                            startActivity(intent);
                                            // Завершение текущей активности
                                            finish();
                                        }))
                                        .addOnFailureListener(e -> runOnUiThread(() ->
                                                // Вывод сообщения об ошибке при сохранении данных
                                                Toast.makeText(this, "Ошибка сохранения данных", Toast.LENGTH_SHORT).show()));
                            } else {
                                // Вывод сообщения об ошибке при регистрации
                                runOnUiThread(() -> Toast.makeText(this, "Ошибка регистрации: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show());
                            }
                        });
            });
        });
    }
}