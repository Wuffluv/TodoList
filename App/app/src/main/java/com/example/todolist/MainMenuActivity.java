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

// Класс MainMenuActivity отвечает за основной экран приложения, где отображается список задач
public class MainMenuActivity extends AppCompatActivity {
    // Адаптер для отображения задач в RecyclerView
    private TaskAdapter taskAdapter;
    // Прогресс-бар для отображения процента выполненных задач
    private ProgressBar taskProgressBar;
    // Текстовое поле для отображения процента прогресса
    private TextView progressTextView;
    // Экземпляр Firestore для работы с базой данных
    private FirebaseFirestore db;
    // Экземпляр FirebaseAuth для управления авторизацией
    private FirebaseAuth auth;
    // Идентификатор текущего пользователя
    private String userId;
    // Календарь для выбора даты задач
    private CalendarView calendarView;
    // Объект Calendar для хранения выбранной даты
    private Calendar selectedCalendarDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Установка макета для активности
        setContentView(R.layout.mainmenu);

        // Инициализация Firebase Authentication
        auth = FirebaseAuth.getInstance();
        // Получение текущего пользователя
        FirebaseUser currentUser = auth.getCurrentUser();

        // Проверка, авторизован ли пользователь
        if (currentUser == null) {
            // Если пользователь не авторизован, показываем уведомление и перенаправляем на экран входа
            Toast.makeText(this, "Пожалуйста, войдите в систему", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        // Сохранение идентификатора пользователя
        userId = currentUser.getUid();
        // Инициализация Firestore
        db = FirebaseFirestore.getInstance();

        // Инициализация элементов интерфейса
        taskProgressBar = findViewById(R.id.taskProgressBar); // Прогресс-бар
        progressTextView = findViewById(R.id.progressTextView); // Текст прогресса
        RecyclerView taskRecyclerView = findViewById(R.id.taskRecyclerView); // Список задач
        calendarView = findViewById(R.id.calendarView); // Календарь для выбора даты
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigationView); // Нижняя панель навигации

        // Инициализация адаптера для списка задач
        List<Task> userTasks = new ArrayList<>(); // Создаём пустой список задач
        taskAdapter = new TaskAdapter(userTasks, this::updateProgressBar, this); // Создаём адаптер
        taskRecyclerView.setAdapter(taskAdapter); // Устанавливаем адаптер для RecyclerView
        taskRecyclerView.setLayoutManager(new LinearLayoutManager(this)); // Устанавливаем линейный менеджер компоновки

        // Инициализация даты для календаря
        selectedCalendarDate = Calendar.getInstance(); // Устанавливаем текущую дату
        calendarView.setDate(selectedCalendarDate.getTimeInMillis(), true, true); // Устанавливаем дату в календаре

        // Настройка слушателя для календаря
        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            // При изменении даты обновляем selectedCalendarDate и загружаем задачи
            selectedCalendarDate.set(year, month, dayOfMonth);
            loadTasks(selectedCalendarDate);
        });

        // Настройка слушателя для нижней панели навигации
        bottomNav.setOnNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_tasks) {
                // При выборе вкладки "Задачи" загружаем задачи
                loadTasks(selectedCalendarDate);
                return true;
            } else if (itemId == R.id.nav_add_task) {
                // При выборе вкладки "Добавить задачу" показываем диалог добавления
                showAddTaskDialog();
                return true;
            } else if (itemId == R.id.nav_settings) {
                // При выборе вкладки "Настройки" переходим в SettingsActivity
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            }
            return false;
        });

        // Первоначальная загрузка задач и обновление прогресса
        loadTasks(selectedCalendarDate); // Загружаем задачи для текущей даты
        updateProgressBar(); // Обновляем прогресс-бар
    }

    // Метод для отображения диалога добавления задачи
    private void showAddTaskDialog() {
        // Создаём диалог BottomSheetDialog
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        // Загружаем макет диалога
        View dialogView = getLayoutInflater().inflate(R.layout.bottom_sheet_add_task, null);
        bottomSheetDialog.setContentView(dialogView);

        // Инициализация элементов диалога
        EditText taskDescription = dialogView.findViewById(R.id.taskDescription); // Поле для описания задачи
        Button pickDateButton = dialogView.findViewById(R.id.pickDateButton); // Кнопка выбора даты
        Button pickTimeButton = dialogView.findViewById(R.id.pickTimeButton); // Кнопка выбора времени
        Button addTaskButton = dialogView.findViewById(R.id.addTaskButton); // Кнопка добавления задачи
        ImageButton collapseButton = dialogView.findViewById(R.id.collapseButton); // Кнопка сворачивания диалога

        // Копируем выбранную дату из календаря для использования в диалоге
        Calendar calendar = (Calendar) selectedCalendarDate.clone();

        // Установка начальных значений для даты и времени
        pickDateButton.setText(String.format("%02d/%02d/%d",
                calendar.get(Calendar.DAY_OF_MONTH),
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.YEAR))); // Устанавливаем текущую дату

        Calendar now = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, now.get(Calendar.HOUR_OF_DAY));
        calendar.set(Calendar.MINUTE, now.get(Calendar.MINUTE));
        pickTimeButton.setText(String.format("%02d:%02d",
                now.get(Calendar.HOUR_OF_DAY),
                now.get(Calendar.MINUTE))); // Устанавливаем текущее время

        // Обработчик кнопки сворачивания диалога
        collapseButton.setOnClickListener(v -> bottomSheetDialog.dismiss());

        // Обработчик кнопки выбора даты
        pickDateButton.setOnClickListener(v -> {
            // Открываем диалог выбора даты
            new DatePickerDialog(
                    this,
                    (view, year, month, dayOfMonth) -> {
                        // Обновляем дату в calendar и отображаем её на кнопке
                        calendar.set(year, month, dayOfMonth);
                        pickDateButton.setText(String.format("%02d/%02d/%d",
                                dayOfMonth, month + 1, year));
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            ).show();
        });

        // Обработчик кнопки выбора времени
        pickTimeButton.setOnClickListener(v -> {
            // Открываем диалог выбора времени
            new TimePickerDialog(
                    this,
                    (view, hourOfDay, minute) -> {
                        // Обновляем время в calendar и отображаем его на кнопке
                        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                        calendar.set(Calendar.MINUTE, minute);
                        pickTimeButton.setText(String.format("%02d:%02d", hourOfDay, minute));
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    true
            ).show();
        });

        // Обработчик кнопки добавления задачи
        addTaskButton.setOnClickListener(v -> {
            // Получаем описание задачи и удаляем пробелы
            String description = taskDescription.getText().toString().trim();
            // Проверяем, что описание не пустое
            if (description.isEmpty()) {
                Toast.makeText(this, "Введите описание задачи", Toast.LENGTH_SHORT).show();
                return;
            }

            // Формируем объект задачи для сохранения в Firestore
            Map<String, Object> task = new HashMap<>();
            task.put("userId", userId); // Идентификатор пользователя
            task.put("description", description); // Описание задачи
            task.put("dateTime", calendar.getTime()); // Дата и время задачи
            task.put("isCompleted", false); // Статус выполнения (по умолчанию false)
            task.put("isExpanded", false); // Флаг для интерфейса (по умолчанию false)

            // Сохраняем задачу в Firestore
            db.collection("tasks").add(task)
                    .addOnSuccessListener(doc -> {
                        // При успешном добавлении закрываем диалог и перезагружаем список задач
                        bottomSheetDialog.dismiss();
                        loadTasks(selectedCalendarDate);
                    })
                    .addOnFailureListener(e -> {
                        // При ошибке показываем уведомление
                        Log.e("FirestoreError", "Ошибка добавления задачи: " + e.getMessage());
                        Toast.makeText(this, "Ошибка добавления задачи", Toast.LENGTH_SHORT).show();
                    });
        });

        // Показываем диалог
        bottomSheetDialog.show();
    }

    // Метод для загрузки задач из Firestore
    private void loadTasks(Calendar selectedDate) {
        // Определяем начало дня для выбранной даты
        Calendar startOfDay = (Calendar) selectedDate.clone();
        startOfDay.set(Calendar.HOUR_OF_DAY, 0);
        startOfDay.set(Calendar.MINUTE, 0);
        startOfDay.set(Calendar.SECOND, 0);
        startOfDay.set(Calendar.MILLISECOND, 0);

        // Определяем конец дня для выбранной даты
        Calendar endOfDay = (Calendar) selectedDate.clone();
        endOfDay.set(Calendar.HOUR_OF_DAY, 23);
        endOfDay.set(Calendar.MINUTE, 59);
        endOfDay.set(Calendar.SECOND, 59);
        endOfDay.set(Calendar.MILLISECOND, 999);

        // Логируем информацию о запросе
        Log.d("Firestore", "Загрузка задач для userId: " + userId + ", дата: " + selectedDate.getTime());
        Log.d("Firestore", "Диапазон: " + startOfDay.getTime() + " - " + endOfDay.getTime());

        // Выполняем запрос к Firestore для получения задач
        db.collection("tasks")
                .whereEqualTo("userId", userId) // Фильтруем по пользователю
                .whereGreaterThanOrEqualTo("dateTime", startOfDay.getTime()) // Фильтруем по началу дня
                .whereLessThanOrEqualTo("dateTime", endOfDay.getTime()) // Фильтруем по концу дня
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    // Создаём список для хранения задач
                    List<Task> tasks = new ArrayList<>();
                    // Обрабатываем каждый документ из результата запроса
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        // Преобразуем документ в объект Task
                        Task task = doc.toObject(Task.class);
                        // Устанавливаем ID задачи
                        task.setId(doc.getId());
                        tasks.add(task);
                        // Логируем информацию о загруженной задаче
                        Log.d("Firestore", "Загружена задача: " + task.getDescription() + ", dateTime: " + task.getDateTime());
                    }
                    // Логируем общее количество загруженных задач
                    Log.d("Firestore", "Всего загружено задач: " + tasks.size());
                    // Обновляем адаптер новым списком задач
                    taskAdapter.setTasks(tasks);
                    // Обновляем прогресс-бар
                    updateProgressBar();
                })
                .addOnFailureListener(e -> {
                    // При ошибке показываем уведомление
                    Log.e("FirestoreError", "Ошибка загрузки задач: " + e.getMessage());
                    Toast.makeText(this, "Ошибка загрузки задач: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    // Метод для обновления прогресс-бара
    public void updateProgressBar() {
        // Получаем общее количество задач
        int totalTasks = taskAdapter.getItemCount();
        // Получаем количество выполненных задач
        int completedTasks = taskAdapter.getCompletedTaskCount();
        // Вычисляем процент выполненных задач
        int progress = totalTasks > 0 ? (completedTasks * 100 / totalTasks) : 0;

        // Логируем информацию о прогрессе
        Log.d("Progress", "Общее количество задач: " + totalTasks + ", выполнено: " + completedTasks + ", прогресс: " + progress + "%");
        // Устанавливаем значение прогресс-бара
        taskProgressBar.setProgress(progress);
        // Обновляем текст прогресса
        progressTextView.setText("Прогресс: " + progress + "%");
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Проверяем, авторизован ли пользователь при возвращении в активность
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            // Если пользователь не авторизован, перенаправляем на экран входа
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }
}