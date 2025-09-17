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
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class ActivityRegistration extends AppCompatActivity {
    // Объявление переменных для полей ввода и кнопки
    private EditText newEmail, newName, newPassword;
    private Button registryButton;
    // Объявление объектов для работы с Firebase Authentication и Firestore
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Установка макета активности
        setContentView(R.layout.activity_registration);

        // Инициализация экземпляров Firebase Auth и Firestore
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Привязка элементов интерфейса к переменным
        newEmail = findViewById(R.id.NewEmail);    // Поле ввода email
        newName = findViewById(R.id.NewName);      // Поле ввода имени
        newPassword = findViewById(R.id.NewPassword); // Поле ввода пароля
        registryButton = findViewById(R.id.RegistryButt); // Кнопка регистрации

        // Установка слушателя нажатия на кнопку регистрации
        registryButton.setOnClickListener(v -> {
            // Получение введенных данных и удаление лишних пробелов
            String email = newEmail.getText().toString().trim();
            String name = newName.getText().toString().trim();
            String password = newPassword.getText().toString().trim();

            // Проверка, заполнены ли все поля
            if (email.isEmpty() || name.isEmpty() || password.isEmpty()) {
                // Показ сообщения об ошибке, если поля пустые
                Toast.makeText(this, "Заполните все поля!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Логирование попытки регистрации
            Log.d("Auth", "Попытка регистрации с email: " + email);
            // Создание нового пользователя с помощью email и пароля
            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        // Проверка успешности регистрации
                        if (task.isSuccessful()) {
                            // Получение данных текущего пользователя
                            FirebaseUser user = mAuth.getCurrentUser();
                            // Создание объекта для хранения пользовательских данных
                            Map<String, Object> userData = new HashMap<>();
                            userData.put("username", name); // Добавление имени
                            userData.put("email", email);   // Добавление email

                            // Сохранение данных пользователя в Firestore
                            db.collection("users").document(user.getUid())
                                    .set(userData)
                                    .addOnSuccessListener(aVoid -> {
                                        // Логирование успешного сохранения данных
                                        Log.d("Firestore", "Данные пользователя сохранены для UID: " + user.getUid());
                                        // Показ сообщения об успешной регистрации
                                        Toast.makeText(this, "Регистрация успешна!", Toast.LENGTH_SHORT).show();
                                        // Создание намерения для перехода в главное меню
                                        Intent intent = new Intent(ActivityRegistration.this, MainMenuActivity.class);
                                        intent.putExtra("USER_ID", user.getUid()); // Передача ID пользователя
                                        startActivity(intent); // Запуск новой активности
                                        finish(); // Завершение текущей активности
                                    })
                                    .addOnFailureListener(e -> {
                                        // Логирование ошибки сохранения данных
                                        Log.e("FirestoreError", "Ошибка сохранения данных: " + e.getMessage());
                                        // Показ сообщения об ошибке сохранения
                                        Toast.makeText(this, "Ошибка сохранения данных", Toast.LENGTH_SHORT).show();
                                    });
                        } else {
                            // Логирование ошибки регистрации
                            Log.e("AuthError", "Ошибка регистрации: " + task.getException().getMessage());
                            // Показ сообщения об ошибке регистрации
                            Toast.makeText(this, "Ошибка регистрации", Toast.LENGTH_SHORT).show();
                        }
                    });
        });
    }
}