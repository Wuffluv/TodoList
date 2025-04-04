package com.example.todolist;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class SettingsActivity extends AppCompatActivity {
    private Button logoutButton;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private String userId;
    private Calendar selectedCalendarDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);

        // Инициализация Firebase
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        selectedCalendarDate = Calendar.getInstance(); // Для диалога добавления задачи

        // Проверка авторизации
        if (userId == null) {
            Toast.makeText(this, "Пожалуйста, войдите в систему", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        // Инициализация элементов интерфейса
        logoutButton = findViewById(R.id.logoutButton);
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigationView);

        // Обработчик кнопки выхода
        logoutButton.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(SettingsActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        // Настройка BottomNavigationView
        bottomNav.setOnNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_tasks) {
                // Переход в MainMenuActivity
                Intent intent = new Intent(SettingsActivity.this, MainMenuActivity.class);
                startActivity(intent);
                finish();
                return true;
            } else if (itemId == R.id.nav_add_task) {
                // Открытие диалога добавления задачи
                showAddTaskDialog();
                return true;
            } else if (itemId == R.id.nav_settings) {
                // Уже находимся в настройках
                return true;
            }
            return false;
        });

        // Установка текущей вкладки как "Настройки"
        bottomNav.setSelectedItemId(R.id.nav_settings);
    }

    private void showAddTaskDialog() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View dialogView = getLayoutInflater().inflate(R.layout.bottom_sheet_add_task, null);
        bottomSheetDialog.setContentView(dialogView);

        EditText taskDescription = dialogView.findViewById(R.id.taskDescription);
        Button pickDateButton = dialogView.findViewById(R.id.pickDateButton);
        Button pickTimeButton = dialogView.findViewById(R.id.pickTimeButton);
        Button addTaskButton = dialogView.findViewById(R.id.addTaskButton);
        ImageButton collapseButton = dialogView.findViewById(R.id.collapseButton);

        // Используем текущую дату
        Calendar calendar = (Calendar) selectedCalendarDate.clone();

        // Установка начальных значений
        pickDateButton.setText(String.format("%02d/%02d/%d",
                calendar.get(Calendar.DAY_OF_MONTH),
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.YEAR)));

        Calendar now = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, now.get(Calendar.HOUR_OF_DAY));
        calendar.set(Calendar.MINUTE, now.get(Calendar.MINUTE));
        pickTimeButton.setText(String.format("%02d:%02d",
                now.get(Calendar.HOUR_OF_DAY),
                now.get(Calendar.MINUTE)));

        // Обработчики
        collapseButton.setOnClickListener(v -> bottomSheetDialog.dismiss());

        pickDateButton.setOnClickListener(v -> {
            new DatePickerDialog(
                    this,
                    (view, year, month, dayOfMonth) -> {
                        calendar.set(year, month, dayOfMonth);
                        pickDateButton.setText(String.format("%02d/%02d/%d",
                                dayOfMonth, month + 1, year));
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            ).show();
        });

        pickTimeButton.setOnClickListener(v -> {
            new TimePickerDialog(
                    this,
                    (view, hourOfDay, minute) -> {
                        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                        calendar.set(Calendar.MINUTE, minute);
                        pickTimeButton.setText(String.format("%02d:%02d", hourOfDay, minute));
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    true
            ).show();
        });

        addTaskButton.setOnClickListener(v -> {
            String description = taskDescription.getText().toString().trim();
            if (description.isEmpty()) {
                Toast.makeText(this, "Введите описание задачи", Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, Object> task = new HashMap<>();
            task.put("userId", userId);
            task.put("description", description);
            task.put("dateTime", calendar.getTime());
            task.put("isCompleted", false);
            task.put("isExpanded", false);

            db.collection("tasks").add(task)
                    .addOnSuccessListener(doc -> {
                        bottomSheetDialog.dismiss();
                        Toast.makeText(this, "Задача добавлена", Toast.LENGTH_SHORT).show();
                        // Перенаправление в MainMenuActivity после добавления задачи
                        Intent intent = new Intent(SettingsActivity.this, MainMenuActivity.class);
                        startActivity(intent);
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Log.e("FirestoreError", "Ошибка добавления задачи: " + e.getMessage());
                        Toast.makeText(this, "Ошибка добавления задачи", Toast.LENGTH_SHORT).show();
                    });
        });

        bottomSheetDialog.show();
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }
}