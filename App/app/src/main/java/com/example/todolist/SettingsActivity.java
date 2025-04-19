package com.example.todolist;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;

/**
 * Активность настроек, позволяющая пользователю выйти из системы
 */
public class SettingsActivity extends AppCompatActivity {
    // Кнопка для выхода из системы
    private Button logoutButton;
    // Объект Firebase для авторизации
    private FirebaseAuth auth;
    // Объект Firebase для работы с базой данных
    private FirebaseFirestore db;
    // Идентификатор текущего пользователя
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Установка layout для активности
        setContentView(R.layout.settings_activity);

        // Инициализация Firebase
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        // Получение идентификатора текущего пользователя
        userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;

        // Проверка авторизации пользователя
        if (userId == null) {
            // Если пользователь не авторизован, показываем сообщение и перенаправляем на экран входа
            Toast.makeText(this, "Пожалуйста, войдите в систему", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        // Инициализация элементов интерфейса
        logoutButton = findViewById(R.id.logoutButton);
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigationView);

        // Обработчик нажатия на кнопку выхода
        logoutButton.setOnClickListener(v -> {
            // Выход из системы
            FirebaseAuth.getInstance().signOut();
            // Создание интента для перехода на экран входа
            Intent intent = new Intent(SettingsActivity.this, MainActivity.class);
            // Очистка стека активностей
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            // Завершение текущей активности
            finish();
        });

        // Настройка навигации через BottomNavigationView
        bottomNav.setOnNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_tasks) {
                // Переход на экран списка задач
                Intent intent = new Intent(SettingsActivity.this, MainMenuActivity.class);
                startActivity(intent);
                finish();
                return true;
            } else if (itemId == R.id.nav_add_task) {
                // Отображение диалога добавления задачи
                showAddTaskDialog();
                return true;
            } else if (itemId == R.id.nav_settings) {
                // Остаемся на текущем экране настроек
                return true;
            }
            return false;
        });

        // Установка активного пункта навигации
        bottomNav.setSelectedItemId(R.id.nav_settings);
    }

    /**
     * Отображает диалог добавления новой задачи.
     */
    private void showAddTaskDialog() {
        // Создание экземпляра адаптера задач для диалога
        TaskAdapter.AddTaskDialogFragment dialog = TaskAdapter.AddTaskDialogFragment.newInstance(
                new TaskAdapter(new ArrayList<>(), null, this));
        // Показ диалога
        dialog.show(getSupportFragmentManager(), "AddTaskDialog");
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Проверка авторизации при старте активности
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            // Если пользователь не авторизован, перенаправляем на экран входа
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }
}