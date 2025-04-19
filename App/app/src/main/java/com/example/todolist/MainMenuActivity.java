package com.example.todolist;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.CalendarView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Главная активность приложения, отображающая список задач и календарь.
 */
public class MainMenuActivity extends AppCompatActivity {
    // Тег для логирования
    private static final String TAG = "MainMenuActivity";
    // Объекты Firebase для авторизации и работы с базой данных
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    // Идентификатор текущего пользователя
    private String userId;
    // Элементы интерфейса
    private CalendarView calendarView;
    private LinearLayout calendarContainer;
    private RecyclerView taskRecyclerView;
    private TaskAdapter taskAdapter;
    private ProgressBar taskProgressBar;
    private TextView progressTextView;
    private FloatingActionButton showAddedTasksButton;
    private ImageButton backButton;
    // Выбранная дата в календаре
    private Calendar selectedCalendarDate;
    // Слушатель изменений в коллекции задач
    private ListenerRegistration tasksListener;
    // Флаг группированного отображения задач
    private boolean isGroupedView = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Установка layout для активности
        setContentView(R.layout.mainmenu);

        // Инициализация Firebase
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        // Проверка авторизации пользователя
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            // Если пользователь не авторизован, перенаправляем на экран входа
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }
        userId = currentUser.getUid();

        // Инициализация элементов интерфейса
        calendarView = findViewById(R.id.calendarView);
        calendarContainer = findViewById(R.id.calendar_container);
        taskRecyclerView = findViewById(R.id.taskRecyclerView);
        taskProgressBar = findViewById(R.id.taskProgressBar);
        progressTextView = findViewById(R.id.progressTextView);
        showAddedTasksButton = findViewById(R.id.showAddedTasksButton);
        backButton = findViewById(R.id.backButton);
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigationView);

        // Настройка RecyclerView для отображения задач
        selectedCalendarDate = Calendar.getInstance();
        taskRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        taskAdapter = new TaskAdapter(new ArrayList<>(), this::updateProgressBar, this);
        taskRecyclerView.setAdapter(taskAdapter);

        // Обработчик изменения даты в календаре
        calendarView.setOnDateChangeListener((view, year, month, day) -> {
            selectedCalendarDate.set(year, month, day);
            // Загрузка задач для выбранной даты
            loadTasks(selectedCalendarDate);
        });

        // Обработчик нажатия на кнопку отображения всех задач
        showAddedTasksButton.setOnClickListener(v -> {
            isGroupedView = true;
            // Загрузка всех задач пользователя
            loadAllTasks();
            // Скрытие календаря
            hideCalendarView();
        });

        // Обработчик нажатия на кнопку возврата к календарю
        backButton.setOnClickListener(v -> {
            isGroupedView = false;
            // Загрузка задач для выбранной даты
            loadTasks(selectedCalendarDate);
            // Отображение календаря
            showCalendarView();
        });

        // Настройка навигации через BottomNavigationView
        bottomNav.setOnNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_tasks) {
                // Остаемся на текущем экране задач
                return true;
            } else if (itemId == R.id.nav_add_task) {
                // Отображение диалога добавления задачи
                showAddTaskDialog();
                return true;
            } else if (itemId == R.id.nav_settings) {

                if (tasksListener != null) {
                    tasksListener.remove();
                    tasksListener = null;
                }
                // Переход на экран настроек
                Intent intent = new Intent(MainMenuActivity.this, SettingsActivity.class);
                startActivity(intent);
                finish();
                return true;
            }
            return false;
        });
        // Установка активного пункта навигации
        bottomNav.setSelectedItemId(R.id.nav_tasks);

        // Восстановление состояния при повороте экрана
        if (savedInstanceState != null) {
            long dateMillis = savedInstanceState.getLong("selectedCalendarDate", -1);
            if (dateMillis != -1) {
                selectedCalendarDate.setTimeInMillis(dateMillis);
            }
            isGroupedView = savedInstanceState.getBoolean("isGroupedView", false);
        }

        // Загрузка начальных данных в зависимости от режима
        if (isGroupedView) {
            loadAllTasks();
            hideCalendarView();
        } else {
            loadTasks(selectedCalendarDate);
            showCalendarView();
        }
    }

    /**
     * Отображает диалог добавления новой задачи.
     */
    private void showAddTaskDialog() {
        TaskAdapter.AddTaskDialogFragment dialog = TaskAdapter.AddTaskDialogFragment.newInstance(taskAdapter);
        dialog.show(getSupportFragmentManager(), "AddTaskDialog");
    }

    /**
     * Загружает задачи для выбранной даты из Firestore.
     * @param date Выбранная дата.
     */
    private void loadTasks(Calendar date) {
        if (isFinishing() || isDestroyed()) return;
        Log.d(TAG, "Загрузка задач для даты: " + date.getTime());


        if (tasksListener != null) {
            tasksListener.remove();
        }

        // Определение временного диапазона для выбранной даты
        Date start = getStartOfDay(date);
        Date end = getEndOfDay(date);

        // Создание запроса к Firestore
        Query query = db.collection("tasks")
                .whereEqualTo("userId", userId)
                .whereGreaterThanOrEqualTo("dateTime", start)
                .whereLessThan("dateTime", end)
                .orderBy("dateTime");

        // Установка слушателя для получения задач
        tasksListener = query.addSnapshotListener((querySnapshot, error) -> {
            if (error != null) {
                handleError("Ошибка загрузки задач для даты: " + error.getMessage(), error);
                return;
            }
            List<Task> tasks = parseTasks(querySnapshot);
            Log.d(TAG, "Загружено " + tasks.size() + " задач для выбранной даты");
            updateUI(tasks, false);
        });
    }

    /**
     * Загружает все задачи пользователя из Firestore.
     */
    private void loadAllTasks() {
        if (isFinishing() || isDestroyed()) return;
        Log.d(TAG, "Загрузка всех задач для пользователя: " + userId);


        if (tasksListener != null) {
            tasksListener.remove();
        }

        // Создание запроса к Firestore для всех задач пользователя
        Query query = db.collection("tasks")
                .whereEqualTo("userId", userId)
                .orderBy("dateTime");

        // Установка слушателя для получения всех задач
        tasksListener = query.addSnapshotListener((querySnapshot, error) -> {
            if (error != null) {
                handleError("Ошибка загрузки всех задач: " + error.getMessage(), error);
                return;
            }
            List<Task> tasks = parseTasks(querySnapshot);
            Log.d(TAG, "Загружено " + tasks.size() + " задач всего");
            updateUI(tasks, true);
        });
    }

    /**
     * Парсит данные задач из Firestore.
     * @param querySnapshot Результат запроса Firestore.
     * @return Список задач.
     */
    private List<Task> parseTasks(QuerySnapshot querySnapshot) {
        List<Task> tasks = new ArrayList<>();
        if (querySnapshot != null) {
            for (QueryDocumentSnapshot doc : querySnapshot) {
                try {
                    Task task = doc.toObject(Task.class);
                    task.setId(doc.getId());
                    if (task.getDateTime() != null) {
                        tasks.add(task);
                    } else {
                        Log.w(TAG, "Задача " + task.getId() + " имеет null dateTime, пропускается");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Ошибка парсинга задачи " + doc.getId() + ": " + e.getMessage(), e);
                }
            }
        }
        return tasks;
    }

    /**
     * Обновляет интерфейс с новыми задачами.
     * @param tasks Список задач.
     * @param isGrouped Флаг группированного отображения.
     */
    private void updateUI(List<Task> tasks, boolean isGrouped) {
        if (isFinishing() || isDestroyed()) return;
        new Handler(Looper.getMainLooper()).post(() -> {
            if (isFinishing() || isDestroyed()) return;
            if (taskAdapter != null) {
                if (isGrouped) {
                    taskAdapter.setGroupedTasks(tasks);
                } else {
                    taskAdapter.setTasks(tasks);
                }
                // Обновление прогресс-бара
                updateProgressBar();
            } else {
                Log.e(TAG, "taskAdapter равен null в updateUI");
            }
        });
    }

    /**
     * Показывает календарь и связанные элементы интерфейса.
     */
    private void showCalendarView() {
        if (isFinishing() || isDestroyed()) return;
        Log.d(TAG, "Отображение календаря");
        calendarContainer.setVisibility(View.VISIBLE);
        backButton.setVisibility(View.GONE);
        showAddedTasksButton.setVisibility(View.VISIBLE);
        calendarView.setDate(selectedCalendarDate.getTimeInMillis(), true, true);
    }

    /**
     * Скрывает календарь
     */
    private void hideCalendarView() {
        if (isFinishing() || isDestroyed()) return;
        Log.d(TAG, "Скрытие календаря");
        calendarContainer.setVisibility(View.GONE);
        backButton.setVisibility(View.VISIBLE);
        showAddedTasksButton.setVisibility(View.GONE);
    }

    /**
     * Обновляет прогресс-бар и текстовое отображение прогресса задач.
     */
    public void updateProgressBar() {
        if (isFinishing() || isDestroyed() || taskAdapter == null) return;
        int totalTasks = taskAdapter.getItemCount();
        int completedTasks = taskAdapter.getCompletedTaskCount();
        int progress = totalTasks > 0 ? (completedTasks * 100 / totalTasks) : 0;
        taskProgressBar.setProgress(progress);
        progressTextView.setText("Прогресс: " + progress + "%");
    }

    /**
     * Возвращает начало дня для указанной даты.
     * @param date Дата.
     * @return Начало дня.
     */
    private Date getStartOfDay(Calendar date) {
        Calendar start = (Calendar) date.clone();
        start.set(Calendar.HOUR_OF_DAY, 0);
        start.set(Calendar.MINUTE, 0);
        start.set(Calendar.SECOND, 0);
        start.set(Calendar.MILLISECOND, 0);
        return start.getTime();
    }

    /**
     * Возвращает конец дня для указанной даты.
     * @param date Дата.
     * @return Конец дня.
     */
    private Date getEndOfDay(Calendar date) {
        Calendar end = (Calendar) date.clone();
        end.set(Calendar.HOUR_OF_DAY, 23);
        end.set(Calendar.MINUTE, 59);
        end.set(Calendar.SECOND, 59);
        end.set(Calendar.MILLISECOND, 999);
        return end.getTime();
    }

    /**
     * Обрабатывает ошибки и отображает сообщение пользователю.
     * @param message Сообщение об ошибке.
     * @param e Исключение.
     */
    private void handleError(String message, Exception e) {
        Log.e(TAG, "Ошибка: " + message, e);
        Toast.makeText(this, "Произошла ошибка: " + message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Проверка авторизации при старте активности
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (tasksListener != null) {
            tasksListener.remove();
            tasksListener = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Очистка ресурсов при уничтожении активности
        if (taskAdapter != null) {
            taskAdapter.cleanup();
            taskAdapter = null;
        }
        if (taskRecyclerView != null) {
            taskRecyclerView.setAdapter(null);
            taskRecyclerView = null;
        }
        if (tasksListener != null) {
            tasksListener.remove();
            tasksListener = null;
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // Сохранение состояния выбранной даты и режима отображения
        if (selectedCalendarDate != null) {
            outState.putLong("selectedCalendarDate", selectedCalendarDate.getTimeInMillis());
        }
        outState.putBoolean("isGroupedView", isGroupedView);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        // Восстановление состояния активности
        long dateMillis = savedInstanceState.getLong("selectedCalendarDate", -1);
        if (dateMillis != -1) {
            selectedCalendarDate.setTimeInMillis(dateMillis);
        }
        isGroupedView = savedInstanceState.getBoolean("isGroupedView", false);
        if (isGroupedView) {
            loadAllTasks();
            hideCalendarView();
        } else {
            loadTasks(selectedCalendarDate);
            showCalendarView();
        }
    }
}