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
    // Объявление переменных для адаптера задач, прогресс-бара и Firestore
    private TaskAdapter taskAdapter;
    private ProgressBar taskProgressBar;
    private TextView progressTextView;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Установка макета активности
        setContentView(R.layout.mainmenu);

        // Инициализация Firebase Authentication
        auth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = auth.getCurrentUser();

        // Проверка аутентификации пользователя
        if (currentUser == null) {
            // Показ сообщения, если пользователь не аутентифицирован
            Toast.makeText(this, "Пожалуйста, войдите в систему", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, MainActivity.class)); // Переход на экран входа
            finish(); // Завершение текущей активности
            return;
        }

        // Получение ID текущего пользователя
        userId = currentUser.getUid();
        // Инициализация Firestore
        db = FirebaseFirestore.getInstance();

        // Привязка элементов интерфейса к переменным
        taskProgressBar = findViewById(R.id.taskProgressBar); // Прогресс-бар задач
        progressTextView = findViewById(R.id.progressTextView); // Текст прогресса
        RecyclerView taskRecyclerView = findViewById(R.id.taskRecyclerView); // Список задач

        // Инициализация списка задач и адаптера
        List<Task> userTasks = new ArrayList<>();
        taskAdapter = new TaskAdapter(userTasks, this::updateProgressBar, this);
        taskRecyclerView.setAdapter(taskAdapter); // Установка адаптера для RecyclerView
        taskRecyclerView.setLayoutManager(new LinearLayoutManager(this)); // Установка линейного менеджера макета

        // Загрузка задач из Firestore
        loadTasks();

        // Настройка кнопки добавления задачи
        FloatingActionButton fabAddTask = findViewById(R.id.fab);
        fabAddTask.setOnClickListener(v -> showAddTaskDialog()); // Показ диалога добавления задачи

        // Настройка кнопки перехода к настройкам
        FloatingActionButton fabLogout = findViewById(R.id.fab_settings);
        fabLogout.setOnClickListener(v -> {
            Intent intent = new Intent(MainMenuActivity.this, SettingsActivity.class);
            startActivity(intent); // Запуск активности настроек
        });

        // Обновление прогресс-бара
        updateProgressBar();
    }

    // Метод для загрузки задач из Firestore
    private void loadTasks() {
        Log.d("Firestore", "Загрузка задач для userId: " + userId);
        db.collection("tasks")
                .whereEqualTo("userId", userId) // Фильтрация задач по ID пользователя
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Task> tasks = new ArrayList<>();
                    // Преобразование документов в объекты Task
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Task task = doc.toObject(Task.class);
                        task.setId(doc.getId()); // Установка ID задачи
                        tasks.add(task);
                    }
                    Log.d("Firestore", "Загружено задач: " + tasks.size());
                    taskAdapter.setTasks(tasks); // Обновление списка задач в адаптере
                    updateProgressBar(); // Обновление прогресс-бара
                })
                .addOnFailureListener(e -> {
                    // Логирование ошибки загрузки
                    Log.e("FirestoreError", "Ошибка загрузки задач: " + e.getMessage());
                    Toast.makeText(this, "Ошибка загрузки задач", Toast.LENGTH_SHORT).show();
                });
    }

    // Метод для показа диалога добавления задачи
    private void showAddTaskDialog() {
        // Создание BottomSheetDialog для ввода новой задачи
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View dialogView = getLayoutInflater().inflate(R.layout.bottom_sheet_add_task, null);
        bottomSheetDialog.setContentView(dialogView);

        // Привязка элементов диалога к переменным
        EditText taskDescription = dialogView.findViewById(R.id.taskDescription); // Поле описания
        Button pickDateButton = dialogView.findViewById(R.id.pickDateButton); // Кнопка выбора даты
        Button pickTimeButton = dialogView.findViewById(R.id.pickTimeButton); // Кнопка выбора времени
        Button addTaskButton = dialogView.findViewById(R.id.addTaskButton); // Кнопка добавления задачи
        ImageButton collapseButton = dialogView.findViewById(R.id.collapseButton); // Кнопка сворачивания

        final Calendar calendar = Calendar.getInstance(); // Объект для работы с датой и временем

        // Обработчик кнопки сворачивания диалога
        collapseButton.setOnClickListener(v -> bottomSheetDialog.dismiss());

        // Обработчик выбора даты
        pickDateButton.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    this,
                    (view, year, month, dayOfMonth) -> {
                        calendar.set(year, month, dayOfMonth); // Установка выбранной даты
                        pickDateButton.setText(dayOfMonth + "/" + (month + 1) + "/" + year); // Обновление текста кнопки
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );
            datePickerDialog.show(); // Показ диалога выбора даты
        });

        // Обработчик выбора времени
        pickTimeButton.setOnClickListener(v -> {
            TimePickerDialog timePickerDialog = new TimePickerDialog(
                    this,
                    (view, hourOfDay, minute) -> {
                        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay); // Установка часов
                        calendar.set(Calendar.MINUTE, minute); // Установка минут
                        pickTimeButton.setText(hourOfDay + ":" + minute); // Обновление текста кнопки
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    true
            );
            timePickerDialog.show(); // Показ диалога выбора времени
        });

        // Обработчик добавления задачи
        addTaskButton.setOnClickListener(v -> {
            String description = taskDescription.getText().toString().trim();
            // Проверка, заполнено ли описание задачи
            if (description.isEmpty()) {
                Toast.makeText(this, "Введите описание задачи", Toast.LENGTH_SHORT).show();
                return;
            }
            Date date = calendar.getTime(); // Получение даты и времени
            // Создание объекта задачи для сохранения в Firestore
            Map<String, Object> task = new HashMap<>();
            task.put("userId", userId); // ID пользователя
            task.put("description", description); // Описание задачи
            task.put("dateTime", date); // Дата и время
            task.put("isCompleted", false); // Статус выполнения
            task.put("isExpanded", false); // Статус раскрытия

            Log.d("Firestore", "Добавление задачи: " + description);
            // Добавление задачи в Firestore
            db.collection("tasks").add(task)
                    .addOnSuccessListener(doc -> {
                        Log.d("Firestore", "Задача добавлена с ID: " + doc.getId());
                        bottomSheetDialog.dismiss(); // Закрытие диалога
                        loadTasks(); // Перезагрузка списка задач
                    })
                    .addOnFailureListener(e -> {
                        // Логирование ошибки добавления
                        Log.e("FirestoreError", "Ошибка добавления задачи: " + e.getMessage());
                        Toast.makeText(this, "Ошибка добавления задачи", Toast.LENGTH_SHORT).show();
                    });
        });

        // Показ диалога
        bottomSheetDialog.show();
    }

    // Метод для обновления прогресс-бара
    public void updateProgressBar() {
        int totalTasks = taskAdapter.getItemCount(); // Общее количество задач
        int completedTasks = taskAdapter.getCompletedTaskCount(); // Количество выполненных задач
        int progress = (totalTasks > 0) ? (completedTasks * 100 / totalTasks) : 0; // Вычисление прогресса в процентах

        taskProgressBar.setProgress(progress); // Установка значения прогресс-бара
        progressTextView.setText("Прогресс: " + progress + "%"); // Обновление текста прогресса
    }
}