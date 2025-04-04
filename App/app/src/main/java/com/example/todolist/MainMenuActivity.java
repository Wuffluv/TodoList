package com.example.todolist;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainMenuActivity extends AppCompatActivity {
    private TaskAdapter taskAdapter;
    private ProgressBar taskProgressBar;
    private TextView progressTextView;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String userId;
    private CalendarView calendarView;
    private Calendar selectedCalendarDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mainmenu);

        // Инициализация Firebase
        auth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = auth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "Пожалуйста, войдите в систему", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        userId = currentUser.getUid();
        db = FirebaseFirestore.getInstance();

        // Инициализация элементов интерфейса
        taskProgressBar = findViewById(R.id.taskProgressBar);
        progressTextView = findViewById(R.id.progressTextView);
        RecyclerView taskRecyclerView = findViewById(R.id.taskRecyclerView);
        calendarView = findViewById(R.id.calendarView);
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigationView);

        // Инициализация адаптера
        List<Task> userTasks = new ArrayList<>();
        taskAdapter = new TaskAdapter(userTasks, this::updateProgressBar, this);
        taskRecyclerView.setAdapter(taskAdapter);
        taskRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Инициализация даты
        selectedCalendarDate = Calendar.getInstance();
        calendarView.setDate(selectedCalendarDate.getTimeInMillis(), true, true);

        // Настройка календаря
        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            selectedCalendarDate.set(year, month, dayOfMonth);
            loadTasks(selectedCalendarDate);
        });

        // Настройка BottomNavigationView
        bottomNav.setOnNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_tasks) {
                loadTasks(selectedCalendarDate);
                return true;
            } else if (itemId == R.id.nav_add_task) {
                showAddTaskDialog();
                return true;
            } else if (itemId == R.id.nav_settings) {
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            }
            return false;
        });

        // Первоначальная загрузка задач
        loadTasks(selectedCalendarDate);
        updateProgressBar();
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

        // Используем выбранную дату из календаря
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
                        loadTasks(selectedCalendarDate);
                    })
                    .addOnFailureListener(e -> {
                        Log.e("FirestoreError", "Ошибка добавления задачи: " + e.getMessage());
                        Toast.makeText(this, "Ошибка добавления задачи", Toast.LENGTH_SHORT).show();
                    });
        });

        bottomSheetDialog.show();
    }

    private void loadTasks(Calendar selectedDate) {
        Calendar startOfDay = (Calendar) selectedDate.clone();
        startOfDay.set(Calendar.HOUR_OF_DAY, 0);
        startOfDay.set(Calendar.MINUTE, 0);
        startOfDay.set(Calendar.SECOND, 0);
        startOfDay.set(Calendar.MILLISECOND, 0);

        Calendar endOfDay = (Calendar) selectedDate.clone();
        endOfDay.set(Calendar.HOUR_OF_DAY, 23);
        endOfDay.set(Calendar.MINUTE, 59);
        endOfDay.set(Calendar.SECOND, 59);
        endOfDay.set(Calendar.MILLISECOND, 999);

        Log.d("Firestore", "Загрузка задач для userId: " + userId + ", дата: " + selectedDate.getTime());
        Log.d("Firestore", "Диапазон: " + startOfDay.getTime() + " - " + endOfDay.getTime());

        db.collection("tasks")
                .whereEqualTo("userId", userId)
                .whereGreaterThanOrEqualTo("dateTime", startOfDay.getTime())
                .whereLessThanOrEqualTo("dateTime", endOfDay.getTime())
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Task> tasks = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Task task = doc.toObject(Task.class);
                        task.setId(doc.getId());
                        tasks.add(task);
                        Log.d("Firestore", "Загружена задача: " + task.getDescription() + ", dateTime: " + task.getDateTime());
                    }
                    Log.d("Firestore", "Всего загружено задач: " + tasks.size());
                    taskAdapter.setTasks(tasks);
                    updateProgressBar();
                })
                .addOnFailureListener(e -> {
                    Log.e("FirestoreError", "Ошибка загрузки задач: " + e.getMessage());
                    Toast.makeText(this, "Ошибка загрузки задач: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    public void updateProgressBar() {
        int totalTasks = taskAdapter.getItemCount();
        int completedTasks = taskAdapter.getCompletedTaskCount();
        int progress = totalTasks > 0 ? (completedTasks * 100 / totalTasks) : 0;

        Log.d("Progress", "Общее количество задач: " + totalTasks + ", выполнено: " + completedTasks + ", прогресс: " + progress + "%");
        taskProgressBar.setProgress(progress);
        progressTextView.setText("Прогресс: " + progress + "%");
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