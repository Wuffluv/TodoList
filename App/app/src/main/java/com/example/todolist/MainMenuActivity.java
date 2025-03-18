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
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mainmenu);

        // Инициализация Firebase Authentication
        auth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = auth.getCurrentUser();

        // Проверка аутентификации
        if (currentUser == null) {
            Toast.makeText(this, "Пожалуйста, войдите в систему", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        userId = currentUser.getUid();
        db = FirebaseFirestore.getInstance();

        taskProgressBar = findViewById(R.id.taskProgressBar);
        progressTextView = findViewById(R.id.progressTextView);
        RecyclerView taskRecyclerView = findViewById(R.id.taskRecyclerView);

        List<Task> userTasks = new ArrayList<>();
        taskAdapter = new TaskAdapter(userTasks, this::updateProgressBar, this);
        taskRecyclerView.setAdapter(taskAdapter);
        taskRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        loadTasks();

        FloatingActionButton fabAddTask = findViewById(R.id.fab);
        fabAddTask.setOnClickListener(v -> showAddTaskDialog());

        FloatingActionButton fabLogout = findViewById(R.id.fab_settings);
        fabLogout.setOnClickListener(v -> {
            Intent intent = new Intent(MainMenuActivity.this, SettingsActivity.class);
            startActivity(intent);
        });

        updateProgressBar();
    }

    private void loadTasks() {
        Log.d("Firestore", "Загрузка задач для userId: " + userId);
        db.collection("tasks")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Task> tasks = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Task task = doc.toObject(Task.class);
                        task.setId(doc.getId());
                        tasks.add(task);
                    }
                    Log.d("Firestore", "Загружено задач: " + tasks.size());
                    taskAdapter.setTasks(tasks);
                    updateProgressBar();
                })
                .addOnFailureListener(e -> {
                    Log.e("FirestoreError", "Ошибка загрузки задач: " + e.getMessage());
                    Toast.makeText(this, "Ошибка загрузки задач", Toast.LENGTH_SHORT).show();
                });
    }

    private void showAddTaskDialog() {
        // Используем BottomSheetDialog вместо AlertDialog
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View dialogView = getLayoutInflater().inflate(R.layout.bottom_sheet_add_task, null);
        bottomSheetDialog.setContentView(dialogView);

        EditText taskDescription = dialogView.findViewById(R.id.taskDescription);
        Button pickDateButton = dialogView.findViewById(R.id.pickDateButton);
        Button pickTimeButton = dialogView.findViewById(R.id.pickTimeButton);
        Button addTaskButton = dialogView.findViewById(R.id.addTaskButton);
        ImageButton collapseButton = dialogView.findViewById(R.id.collapseButton);

        final Calendar calendar = Calendar.getInstance();

        // Обработчик для кнопки сворачивания
        collapseButton.setOnClickListener(v -> bottomSheetDialog.dismiss());

        pickDateButton.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    this,
                    (view, year, month, dayOfMonth) -> {
                        calendar.set(year, month, dayOfMonth);
                        pickDateButton.setText(dayOfMonth + "/" + (month + 1) + "/" + year);
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );
            datePickerDialog.show();
        });

        pickTimeButton.setOnClickListener(v -> {
            TimePickerDialog timePickerDialog = new TimePickerDialog(
                    this,
                    (view, hourOfDay, minute) -> {
                        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                        calendar.set(Calendar.MINUTE, minute);
                        pickTimeButton.setText(hourOfDay + ":" + minute);
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    true
            );
            timePickerDialog.show();
        });

        addTaskButton.setOnClickListener(v -> {
            String description = taskDescription.getText().toString().trim();
            if (description.isEmpty()) {
                Toast.makeText(this, "Введите описание задачи", Toast.LENGTH_SHORT).show();
                return;
            }
            Date date = calendar.getTime();
            Map<String, Object> task = new HashMap<>();
            task.put("userId", userId);
            task.put("description", description);
            task.put("dateTime", date);
            task.put("isCompleted", false);
            task.put("isExpanded", false);

            Log.d("Firestore", "Добавление задачи: " + description);
            db.collection("tasks").add(task)
                    .addOnSuccessListener(doc -> {
                        Log.d("Firestore", "Задача добавлена с ID: " + doc.getId());
                        bottomSheetDialog.dismiss();
                        loadTasks();
                    })
                    .addOnFailureListener(e -> {
                        Log.e("FirestoreError", "Ошибка добавления задачи: " + e.getMessage());
                        Toast.makeText(this, "Ошибка добавления задачи", Toast.LENGTH_SHORT).show();
                    });
        });

        // Показываем BottomSheetDialog
        bottomSheetDialog.show();
    }

    public void updateProgressBar() {
        int totalTasks = taskAdapter.getItemCount();
        int completedTasks = taskAdapter.getCompletedTaskCount();
        int progress = (totalTasks > 0) ? (completedTasks * 100 / totalTasks) : 0;

        taskProgressBar.setProgress(progress);
        progressTextView.setText("Прогресс: " + progress + "%");
    }
}