package com.example.todolist;

// Импорты необходимых библиотек и классов для работы с Android, Firebase, уведомлениями и UI
import android.Manifest;
import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.pm.PackageManager;
import androidx.annotation.NonNull;

public class MainMenuActivity extends AppCompatActivity {
    // Объявление переменных экземпляра для управления UI и данными
    private TaskAdapter taskAdapter; // Адаптер для отображения списка задач в RecyclerView
    private ProgressBar taskProgressBar; // Индикатор прогресса выполнения задач
    private TextView progressTextView; // Текстовое поле для отображения процента прогресса
    private FirebaseFirestore db; // Экземпляр Firestore для работы с базой данных
    private FirebaseAuth auth; // Экземпляр Firebase для аутентификации пользователя
    private String userId; // Уникальный идентификатор текущего пользователя
    private CalendarView calendarView; // Виджет календаря для выбора даты
    private LinearLayout calendarContainer; // Контейнер для отображения календаря
    private LinearLayout progressContainer; // Контейнер для индикатора прогресса и текста
    private Calendar selectedCalendarDate; // Объект Calendar для хранения выбранной даты
    private RecyclerView taskRecyclerView; // Список для отображения задач
    private ImageButton toggleViewButton; // Кнопка для переключения между видами календаря и списка
    private boolean isCalendarViewVisible = true; // Флаг, указывающий, отображается ли календарь

    // Константы для форматирования даты и настройки напоминаний
    private final SimpleDateFormat listDateFormatter = new SimpleDateFormat("EEEE, dd MMMM", Locale.getDefault()); // Форматтер для отображения даты в списке
    private final String[] reminderOptions = {"Без напоминания", "В момент задачи", "За 15 минут", "За 30 минут", "За 1 час", "За 1 день"}; // Варианты времени напоминаний
    private final int[] reminderOffsets = {-1, 0, -15, -30, -60, -1440}; // Смещения в минутах для напоминаний

    // Метод жизненного цикла для инициализации активности
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mainmenu); // Установка макета активности из XML-ресурса

        // Проверка версии Android и запрос разрешения на уведомления для Android 13 и выше
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 100); // Запрос разрешения
            }
        }

        // Инициализация Firebase для аутентификации и базы данных
        auth = FirebaseAuth.getInstance(); // Получение экземпляра FirebaseAuth
        FirebaseUser currentUser = auth.getCurrentUser(); // Проверка текущего пользователя
        if (currentUser == null) {
            handleLogout(); // Выход из системы, если пользователь не авторизован
            return;
        }
        userId = currentUser.getUid(); // Сохранение ID пользователя
        db = FirebaseFirestore.getInstance(); // Инициализация Firestore

        // Настройка Firestore для поддержки оффлайн-режима
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true) // Включение локального кэширования данных
                .build();
        db.setFirestoreSettings(settings); // Применение настроек

        // Инициализация элементов пользовательского интерфейса
        taskProgressBar = findViewById(R.id.taskProgressBar); // Связывание с индикатором прогресса
        progressTextView = findViewById(R.id.progressTextView); // Связывание с текстовым полем прогресса
        taskRecyclerView = findViewById(R.id.taskRecyclerView); // Связывание с RecyclerView для задач
        calendarView = findViewById(R.id.calendarView); // Связывание с виджетом календаря
        calendarContainer = findViewById(R.id.calendar_container); // Связывание с контейнером календаря
        progressContainer = findViewById(R.id.progress_container); // Связывание с контейнером прогресса
        toggleViewButton = findViewById(R.id.toggleViewButton); // Связывание с кнопкой переключения вида
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigationView); // Связывание с нижней навигацией

        // Настройка RecyclerView для отображения задач
        taskRecyclerView.setLayoutManager(new LinearLayoutManager(this)); // Установка линейного менеджера компоновки
        taskAdapter = new TaskAdapter(new ArrayList<>(), this::updateProgressBarUI, this); // Инициализация адаптера задач
        taskRecyclerView.setAdapter(taskAdapter); // Установка адаптера в RecyclerView

        // Настройка календаря для выбора даты
        selectedCalendarDate = Calendar.getInstance(); // Инициализация текущей датой
        calendarView.setDate(selectedCalendarDate.getTimeInMillis(), false, true); // Установка текущей даты в календаре
        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            if (isCalendarViewVisible) { // Проверка, отображается ли календарь
                selectedCalendarDate.set(year, month, dayOfMonth); // Обновление выбранной даты
                loadTasksForDate(selectedCalendarDate); // Загрузка задач для выбранной даты
            }
        });

        // Установка обработчиков событий
        toggleViewButton.setOnClickListener(v -> toggleView()); // Обработчик для переключения вида
        setupBottomNavigation(bottomNav); // Настройка нижней навигации

        // Создание канала уведомлений для Android 8.0 и выше
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("reminder_channel", "Task Reminders", NotificationManager.IMPORTANCE_DEFAULT); // Создание канала
            NotificationManager manager = getSystemService(NotificationManager.class); // Получение менеджера уведомлений
            manager.createNotificationChannel(channel); // Регистрация канала
            Log.d("Notification", "Notification channel created: reminder_channel"); // Логирование создания канала
        }

        // Начальное обновление интерфейса и прогресса
        updateView(); // Обновление вида (календарь или список)
        updateProgressBarUI(); // Обновление индикатора прогресса
    }

    // Обработка результатов запроса разрешений
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100) { // Проверка кода запроса
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("Permission", "POST_NOTIFICATIONS permission granted"); // Логирование успешного получения разрешения
            } else {
                Toast.makeText(this, "Для уведомлений нужно разрешение", Toast.LENGTH_LONG).show(); // Уведомление об отказе
            }
        }
    }

    // Проверка авторизации при запуске активности
    @Override
    protected void onStart() {
        super.onStart();
        if (auth.getCurrentUser() == null) { // Проверка, авторизован ли пользователь
            handleLogout(); // Выход, если пользователь не авторизован
        }
    }

    // Метод для обработки выхода из системы
    private void handleLogout() {
        Toast.makeText(this, "Пожалуйста, войдите в систему", Toast.LENGTH_SHORT).show(); // Уведомление о необходимости входа
        startActivity(new Intent(this, MainActivity.class)); // Переход на экран авторизации
        finish(); // Завершение текущей активности
    }

    // Настройка нижней навигационной панели
    private void setupBottomNavigation(BottomNavigationView bottomNav) {
        bottomNav.setOnNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId(); // Получение ID выбранного пункта
            if (itemId == R.id.nav_tasks) {
                return true; // Остаемся в текущей активности
            } else if (itemId == R.id.nav_add_task) {
                showAddTaskDialog(); // Отображение диалога добавления задачи
                return true;
            } else if (itemId == R.id.nav_settings) {
                Intent intent = new Intent(MainMenuActivity.this, SettingsActivity.class); // Переход в настройки
                startActivity(intent);
                finish(); // Завершение текущей активности
                return true;
            }
            return false; // Ничего не выбрано
        });
    }

    // Переключение между видами календаря и списка задач
    private void toggleView() {
        isCalendarViewVisible = !isCalendarViewVisible; // Инверсия флага видимости календаря
        updateView(); // Обновление интерфейса
    }

    // Обновление пользовательского интерфейса в зависимости от выбранного вида
    public void updateView() {
        // Проверка инициализации всех необходимых элементов UI
        if (calendarContainer == null || progressContainer == null || toggleViewButton == null || taskRecyclerView == null) {
            Log.w("MainMenuActivity", "One or more views are null"); // Логирование ошибки
            return;
        }

        // Получение корневого макета для изменения constraints
        ConstraintLayout constraintLayout = findViewById(R.id.mainmenu_root_layout);
        ConstraintSet constraintSet = new ConstraintSet();
        constraintSet.clone(constraintLayout); // Копирование текущей конфигурации

        if (isCalendarViewVisible) { // Если отображается календарь
            calendarContainer.setVisibility(View.VISIBLE); // Показать календарь
            progressContainer.setVisibility(View.VISIBLE); // Показать индикатор прогресса
            toggleViewButton.setImageResource(android.R.drawable.ic_menu_agenda); // Установить иконку списка
            constraintSet.connect(R.id.taskRecyclerView, ConstraintSet.TOP, R.id.progress_container, ConstraintSet.BOTTOM, 8); // Привязка RecyclerView к прогрессу
            if (selectedCalendarDate == null) selectedCalendarDate = Calendar.getInstance(); // Инициализация даты, если null
            loadTasksForDate(selectedCalendarDate); // Загрузка задач для текущей даты
        } else { // Если отображается список
            calendarContainer.setVisibility(View.GONE); // Скрыть календарь
            progressContainer.setVisibility(View.GONE); // Скрыть индикатор прогресса
            toggleViewButton.setImageResource(android.R.drawable.ic_menu_today); // Установить иконку календаря
            constraintSet.connect(R.id.taskRecyclerView, ConstraintSet.TOP, R.id.mainmenu_root_layout, ConstraintSet.TOP, 8); // Привязка RecyclerView к верху
            loadAllTasks(); // Загрузка всех задач
        }

        constraintSet.applyTo(constraintLayout); // Применение новых constraints
    }

    // Загрузка задач за определенную дату из Firestore
    private void loadTasksForDate(Calendar selectedDate) {
        if (selectedDate == null) return; // Выход, если дата не выбрана

        // Создание временного диапазона для выбранной даты (начало и конец дня)
        Calendar startOfDay = (Calendar) selectedDate.clone();
        startOfDay.set(Calendar.HOUR_OF_DAY, 0); // Установка времени на 00:00
        startOfDay.set(Calendar.MINUTE, 0);
        startOfDay.set(Calendar.SECOND, 0);
        startOfDay.set(Calendar.MILLISECOND, 0);
        Calendar endOfDay = (Calendar) selectedDate.clone();
        endOfDay.set(Calendar.HOUR_OF_DAY, 23); // Установка времени на 23:59:59.999
        endOfDay.set(Calendar.MINUTE, 59);
        endOfDay.set(Calendar.SECOND, 59);
        endOfDay.set(Calendar.MILLISECOND, 999);

        Log.d("Firestore", "Loading tasks for date: " + selectedDate.getTime()); // Логирование запроса
        // Запрос задач из Firestore с фильтрацией по пользователю и дате
        db.collection("tasks")
                .whereEqualTo("userId", userId) // Фильтр по ID пользователя
                .whereGreaterThanOrEqualTo("dateTime", startOfDay.getTime()) // Задачи с датой >= начала дня
                .whereLessThanOrEqualTo("dateTime", endOfDay.getTime()) // Задачи с датой <= конца дня
                .orderBy("dateTime", Query.Direction.ASCENDING) // Сортировка по дате (возрастание)
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null) { // Проверка на ошибки
                        Log.e("FirestoreError", "Error loading tasks for date: ", error); // Логирование ошибки
                        if (error.getMessage() != null && error.getMessage().contains("UNAVAILABLE")) {
                            Toast.makeText(this, "Нет интернета. Используем локальные данные.", Toast.LENGTH_LONG).show(); // Уведомление об оффлайн-режиме
                        } else {
                            Toast.makeText(this, "Ошибка загрузки задач", Toast.LENGTH_LONG).show(); // Уведомление об ошибке
                        }
                        return;
                    }

                    if (taskAdapter == null) return; // Выход, если адаптер не инициализирован
                    List<Task> tasks = new ArrayList<>(); // Список для хранения задач
                    if (querySnapshot != null) { // Проверка наличия данных
                        boolean fromCache = querySnapshot.getMetadata().isFromCache(); // Проверка, из кэша ли данные
                        if (fromCache) {
                            Toast.makeText(this, "Загружено из кэша (оффлайн)", Toast.LENGTH_SHORT).show(); // Уведомление о кэшированных данных
                        }
                        // Обработка каждого документа в результате запроса
                        for (QueryDocumentSnapshot doc : querySnapshot) {
                            try {
                                Task task = doc.toObject(Task.class); // Преобразование документа в объект Task
                                task.setId(doc.getId()); // Установка ID задачи
                                tasks.add(task); // Добавление задачи в список
                            } catch (Exception e) {
                                Log.e("Firestore", "Error converting document " + doc.getId() + " to Task", e); // Логирование ошибки преобразования
                            }
                        }
                        Log.d("Firestore", "Loaded " + tasks.size() + " tasks for date, fromCache=" + fromCache); // Логирование количества загруженных задач
                        taskAdapter.setCalendarTasks(tasks); // Обновление адаптера списком задач
                    } else {
                        taskAdapter.setCalendarTasks(new ArrayList<>()); // Установка пустого списка, если данных нет
                    }
                    updateProgressBarUI(); // Обновление индикатора прогресса
                });
    }

    // Загрузка всех задач пользователя из Firestore
    private void loadAllTasks() {
        Log.d("Firestore", "Loading all tasks for user: " + userId); // Логирование запроса
        // Запрос всех задач пользователя с сортировкой по дате
        db.collection("tasks")
                .whereEqualTo("userId", userId) // Фильтр по ID пользователя
                .orderBy("dateTime", Query.Direction.ASCENDING) // Сортировка по дате (возрастание)
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null) { // Проверка на ошибки
                        Log.e("FirestoreError", "Error loading all tasks: ", error); // Логирование ошибки
                        if (error.getMessage() != null && error.getMessage().contains("UNAVAILABLE")) {
                            Toast.makeText(this, "Нет интернета. Используем локальные данные.", Toast.LENGTH_LONG).show(); // Уведомление об оффлайн-режиме
                        } else {
                            Toast.makeText(this, "Ошибка загрузки всех задач", Toast.LENGTH_LONG).show(); // Уведомление об ошибке
                        }
                        return;
                    }

                    if (taskAdapter == null) return; // Выход, если адаптер не инициализирован
                    List<Task> allTasks = new ArrayList<>(); // Список для хранения всех задач
                    if (querySnapshot != null) { // Проверка наличия данных
                        boolean fromCache = querySnapshot.getMetadata().isFromCache(); // Проверка, из кэша ли данные
                        if (fromCache) {
                            Toast.makeText(this, "Загружено из кэша (оффлайн)", Toast.LENGTH_SHORT).show(); // Уведомление о кэшированных данных
                        }
                        // Обработка каждого документа в результате запроса
                        for (QueryDocumentSnapshot doc : querySnapshot) {
                            try {
                                Task task = doc.toObject(Task.class); // Преобразование документа в объект Task
                                task.setId(doc.getId()); // Установка ID задачи
                                allTasks.add(task); // Добавление задачи в список
                            } catch (Exception e) {
                                Log.e("Firestore", "Error converting document " + doc.getId() + " to Task", e); // Логирование ошибки преобразования
                            }
                        }
                        Log.d("Firestore", "Loaded " + allTasks.size() + " total tasks, fromCache=" + fromCache); // Логирование количества загруженных задач
                        Map<Date, List<Task>> groupedTasks = groupTasksByDate(allTasks); // Группировка задач по датам
                        List<Object> displayList = createDisplayList(groupedTasks); // Создание списка для отображения
                        taskAdapter.setAllTasksList(displayList); // Обновление адаптера списком задач
                    } else {
                        taskAdapter.setAllTasksList(new ArrayList<>()); // Установка пустого списка, если данных нет
                    }
                    updateProgressBarUI(); // Обновление индикатора прогресса
                });
    }

    // Группировка задач по датам для отображения в списке
    private Map<Date, List<Task>> groupTasksByDate(List<Task> tasks) {
        Map<Date, List<Task>> grouped = new LinkedHashMap<>(); // Карта для хранения задач по датам
        Calendar cal = Calendar.getInstance(); // Календарь для обработки дат
        for (Task task : tasks) { // Перебор всех задач
            if (task.getDateTime() == null) continue; // Пропуск задач без даты
            cal.setTime(task.getDateTime()); // Установка времени задачи
            cal.set(Calendar.HOUR_OF_DAY, 0); // Обнуление времени до начала дня
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            Date dateOnly = cal.getTime(); // Получение даты без времени
            grouped.computeIfAbsent(dateOnly, k -> new ArrayList<>()).add(task); // Добавление задачи в группу по дате
        }
        return grouped; // Возврат сгруппированных задач
    }

    // Создание списка для отображения с заголовками дат
    private List<Object> createDisplayList(Map<Date, List<Task>> groupedTasks) {
        List<Object> displayList = new ArrayList<>(); // Список для отображения
        List<Date> sortedDates = new ArrayList<>(groupedTasks.keySet()); // Список всех дат
        Collections.sort(sortedDates); // Сортировка дат по возрастанию
        for (Date date : sortedDates) { // Перебор отсортированных дат
            displayList.add(listDateFormatter.format(date)); // Добавление форматированной даты как заголовка
            displayList.addAll(groupedTasks.get(date)); // Добавление всех задач за эту дату
        }
        return displayList; // Возврат списка для отображения
    }

    // Отображение диалога для добавления новой задачи
    private void showAddTaskDialog() {
        Log.d("AddTaskDialog", "Opening add task dialog"); // Логирование открытия диалога
        AlertDialog.Builder builder = new AlertDialog.Builder(this); // Создание конструктора диалога
        View dialogView = getLayoutInflater().inflate(R.layout.bottom_sheet_add_task, null); // Загрузка макета диалога
        builder.setView(dialogView); // Установка макета в диалог

        // Инициализация элементов пользовательского интерфейса диалога
        final EditText taskDescription = dialogView.findViewById(R.id.taskDescription); // Поле для ввода описания задачи
        final Button pickDateButton = dialogView.findViewById(R.id.pickDateButton); // Кнопка выбора даты
        final Button pickTimeButton = dialogView.findViewById(R.id.pickTimeButton); // Кнопка выбора времени
        final TextView reminderTimeTextView = dialogView.findViewById(R.id.reminderTimeTextView); // Текст для выбора напоминания
        final Button addTaskButton = dialogView.findViewById(R.id.addTaskButton); // Кнопка добавления задачи

        // Установка начальной даты и времени для диалога
        final Calendar calendar = isCalendarViewVisible && selectedCalendarDate != null
                ? (Calendar) selectedCalendarDate.clone() // Использование выбранной даты календаря
                : Calendar.getInstance(); // Использование текущей даты

        // Форматтеры для отображения даты и времени
        final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        pickDateButton.setText(dateFormat.format(calendar.getTime())); // Установка начальной даты
        pickTimeButton.setText(timeFormat.format(calendar.getTime())); // Установка начального времени

        Log.d("AddTaskDialog", "Initialized dialog with date: " + dateFormat.format(calendar.getTime()) + ", time: " + timeFormat.format(calendar.getTime())); // Логирование инициализации

        // Обработчик нажатия на кнопку выбора даты
        pickDateButton.setOnClickListener(v -> {
            final Calendar dateCalendar = (Calendar) calendar.clone(); // Копия календаря для выбора даты
            DatePickerDialog datePickerDialog = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                calendar.set(year, month, dayOfMonth); // Обновление выбранной даты
                pickDateButton.setText(dateFormat.format(calendar.getTime())); // Обновление текста кнопки
                Log.d("AddTaskDialog", "Selected date: " + dateFormat.format(calendar.getTime())); // Логирование выбора
            }, dateCalendar.get(Calendar.YEAR), dateCalendar.get(Calendar.MONTH), dateCalendar.get(Calendar.DAY_OF_MONTH));
            datePickerDialog.show(); // Отображение диалога выбора даты
        });

        // Обработчик нажатия на кнопку выбора времени
        pickTimeButton.setOnClickListener(v -> {
            final Calendar timeCalendar = (Calendar) calendar.clone(); // Копия календаря для выбора времени
            TimePickerDialog timePickerDialog = new TimePickerDialog(this, (view, hourOfDay, minute) -> {
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay); // Обновление выбранного времени
                calendar.set(Calendar.MINUTE, minute);
                pickTimeButton.setText(timeFormat.format(calendar.getTime())); // Обновление текста кнопки
                Log.d("AddTaskDialog", "Selected time: " + timeFormat.format(calendar.getTime())); // Логирование выбора
            }, timeCalendar.get(Calendar.HOUR_OF_DAY), timeCalendar.get(Calendar.MINUTE), true);
            timePickerDialog.show(); // Отображение диалога выбора времени
        });

        // Обработчик выбора времени напоминания
        final int[] selectedReminderIndex = {0}; // Индекс выбранного варианта напоминания
        final String[] finalReminderOptions = reminderOptions; // Массив вариантов напоминаний
        reminderTimeTextView.setOnClickListener(v -> {
            AlertDialog.Builder reminderBuilder = new AlertDialog.Builder(this); // Создание диалога для напоминаний
            reminderBuilder.setTitle("Выберите напоминание"); // Установка заголовка
            reminderBuilder.setSingleChoiceItems(finalReminderOptions, selectedReminderIndex[0], (dialog, which) -> {
                selectedReminderIndex[0] = which; // Обновление выбранного индекса
                if (which == 0) {
                    reminderTimeTextView.setText("Добавить напоминание"); // Установка текста для "без напоминания"
                } else {
                    reminderTimeTextView.setText("Напомнить: " + finalReminderOptions[which]); // Установка текста напоминания
                }
                Log.d("AddTaskDialog", "Selected reminder: " + finalReminderOptions[which]); // Логирование выбора
                dialog.dismiss(); // Закрытие диалога
            });
            reminderBuilder.show(); // Отображение диалога выбора напоминания
        });

        final AlertDialog dialog = builder.create(); // Создание диалога
        // Обработчик нажатия на кнопку добавления задачи
        addTaskButton.setOnClickListener(v -> {
            final String description = taskDescription.getText().toString().trim(); // Получение описания задачи
            if (description.isEmpty()) { // Проверка, что описание не пустое
                Toast.makeText(this, "Введите описание", Toast.LENGTH_SHORT).show(); // Уведомление об ошибке
                Log.w("AddTaskDialog", "Empty description entered"); // Логирование ошибки
                return;
            }

            final Date dueDateTime = calendar.getTime(); // Получение времени выполнения задачи
            final Date reminderTime; // Время напоминания
            if (selectedReminderIndex[0] > 0) { // Если выбрано напоминание
                int offset = reminderOffsets[selectedReminderIndex[0]]; // Получение смещения
                Calendar reminderCal = (Calendar) calendar.clone(); // Копия календаря
                reminderCal.add(Calendar.MINUTE, offset); // Применение смещения
                reminderTime = reminderCal.getTime(); // Установка времени напоминания
            } else {
                reminderTime = null; // Без напоминания
            }

            Log.d("AddTaskDialog", "Adding task: description=" + description + ", dueDateTime=" + dueDateTime + ", reminderTime=" + (reminderTime != null ? reminderTime : "null")); // Логирование данных задачи

            // Создание объекта задачи для сохранения в Firestore
            Map<String, Object> task = new HashMap<>();
            task.put("userId", userId); // ID пользователя
            task.put("description", description); // Описание задачи
            task.put("dateTime", dueDateTime); // Время выполнения
            task.put("isCompleted", false); // Статус выполнения
            task.put("isExpanded", false); // Статус развернутости
            task.put("reminderTime", reminderTime); // Время напоминания

            // Сохранение задачи в Firestore
            db.collection("tasks").add(task)
                    .addOnSuccessListener(doc -> {
                        String taskId = doc.getId(); // Получение ID новой задачи
                        Log.d("AddTaskDialog", "Task added successfully, taskId=" + taskId); // Логирование успеха
                        if (reminderTime != null) { // Если есть напоминание
                            scheduleNotification(taskId, reminderTime, description); // Планирование уведомления
                        }
                        dialog.dismiss(); // Закрытие диалога
                        Toast.makeText(MainMenuActivity.this, "Задача добавлена", Toast.LENGTH_SHORT).show(); // Уведомление об успехе
                        updateView(); // Обновление интерфейса
                    })
                    .addOnFailureListener(e -> {
                        Log.e("FirestoreError", "Error adding task: ", e); // Логирование ошибки
                        Toast.makeText(MainMenuActivity.this, "Ошибка добавления задачи", Toast.LENGTH_SHORT).show(); // Уведомление об ошибке
                    });
        });

        dialog.show(); // Отображение диалога добавления задачи
        Log.d("AddTaskDialog", "Dialog shown"); // Логирование отображения диалога
    }

    // Планирование уведомления для задачи
    private void scheduleNotification(String taskId, Date reminderTime, String description) {
        Log.d("Notification", "Scheduling notification for taskId=" + taskId + ", time=" + reminderTime + ", description=" + description); // Логирование планирования
        if (reminderTime.before(new Date())) { // Проверка, что время напоминания не в прошлом
            Log.w("Notification", "Reminder time is in the past: " + reminderTime); // Логирование ошибки
            Toast.makeText(this, "Время напоминания в прошлом", Toast.LENGTH_SHORT).show(); // Уведомление об ошибке
            return;
        }

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE); // Получение менеджера будильников
        Intent intent = new Intent(this, ReminderBroadcastReceiver.class); // Создание Intent для BroadcastReceiver
        intent.putExtra("description", description); // Добавление описания задачи
        intent.putExtra("taskId", taskId); // Добавление ID задачи

        // Установка флагов для PendingIntent
        int flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, taskId.hashCode(), intent, flags); // Создание PendingIntent

        long triggerTime = reminderTime.getTime(); // Время срабатывания уведомления
        try {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent); // Установка уведомления
            Log.d("Notification", "Notification scheduled at " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(reminderTime)); // Логирование успешного планирования
        } catch (Exception e) {
            Log.e("NotificationError", "Failed to schedule notification: ", e); // Логирование ошибки
            Toast.makeText(this, "Ошибка при установке уведомления", Toast.LENGTH_SHORT).show(); // Уведомление об ошибке
        }
    }

    // Обновление индикатора прогресса выполнения задач
    public void updateProgressBarUI() {
        // Проверка инициализации необходимых компонентов
        if (taskAdapter == null || taskProgressBar == null || progressTextView == null) {
            Log.w("ProgressUI", "updateProgressBarUI called but adapter or views are null"); // Логирование ошибки
            return;
        }
        int progress = taskAdapter.getCurrentProgressPercentage(); // Получение процента прогресса
        int totalTasks = taskAdapter.getTotalTaskCount(); // Получение общего количества задач
        int completedTasks = taskAdapter.getCompletedTaskCount(); // Получение количества выполненных задач

        Log.d("ProgressUI", "Updating ProgressBar UI: Total=" + totalTasks + ", Completed=" + completedTasks + ", Progress=" + progress + "%"); // Логирование данных прогресса

        taskProgressBar.setProgress(progress); // Установка значения прогресса
        progressTextView.setText(String.format(Locale.getDefault(), "Прогресс: %d%%", progress)); // Обновление текста прогресса
    }
}