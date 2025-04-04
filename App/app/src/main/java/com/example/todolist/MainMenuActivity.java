package com.example.todolist; // Объявление пакета приложения

// Импорт необходимых классов Android и библиотек
import android.app.DatePickerDialog; // Класс для выбора даты
import android.app.TimePickerDialog; // Класс для выбора времени
import android.content.Intent; // Класс для работы с намерениями (intents)
import android.os.Bundle; // Класс для передачи данных между активностями
import android.util.Log; // Класс для логирования
import android.view.View; // Класс для работы с представлениями
import android.widget.Button; // Класс кнопки
import android.widget.EditText; // Класс текстового поля ввода
import android.widget.ImageButton; // Класс кнопки с изображением
import android.widget.ProgressBar; // Класс прогресс-бара
import android.widget.TextView; // Класс текстового поля
import android.widget.Toast; // Класс для отображения кратких сообщений

import androidx.appcompat.app.AppCompatActivity; // Базовый класс активности
import androidx.recyclerview.widget.LinearLayoutManager; // Менеджер линейного расположения для RecyclerView
import androidx.recyclerview.widget.RecyclerView; // Класс для отображения списков

import com.google.android.material.bottomsheet.BottomSheetDialog; // Класс для нижнего диалогового окна
import com.google.android.material.floatingactionbutton.FloatingActionButton; // Класс плавающей кнопки
import com.google.firebase.auth.FirebaseAuth; // Класс для аутентификации Firebase
import com.google.firebase.auth.FirebaseUser; // Класс для представления пользователя Firebase
import com.google.firebase.firestore.FirebaseFirestore; // Класс для работы с Firestore
import com.google.firebase.firestore.QueryDocumentSnapshot; // Класс для обработки документов из запросов Firestore

import java.util.ArrayList; // Класс для работы с динамическими списками
import java.util.Calendar; // Класс для работы с датой и временем
import java.util.Date; // Класс для представления даты
import java.util.HashMap; // Класс для работы с ассоциативными массивами
import java.util.List; // Интерфейс для списков
import java.util.Map; // Интерфейс для словарей

// Объявление класса активности главного меню
public class MainMenuActivity extends AppCompatActivity {
    private TaskAdapter taskAdapter; // Адаптер для списка задач
    private ProgressBar taskProgressBar; // Прогресс-бар для отображения выполнения задач
    private TextView progressTextView; // Текстовое поле для отображения процента выполнения
    private FirebaseFirestore db; // Экземпляр Firestore для работы с базой данных
    private FirebaseAuth auth; // Экземпляр Firebase Authentication для аутентификации
    private String userId; // ID текущего пользователя

    @Override
    protected void onCreate(Bundle savedInstanceState) { // Метод жизненного цикла создания активности
        super.onCreate(savedInstanceState); // Вызов метода родительского класса
        setContentView(R.layout.mainmenu); // Установка layout для активности

        auth = FirebaseAuth.getInstance(); // Инициализация Firebase Authentication
        FirebaseUser currentUser = auth.getCurrentUser(); // Получение текущего пользователя

        if (currentUser == null) { // Проверка, авторизован ли пользователь
            Toast.makeText(this, "Пожалуйста, войдите в систему", Toast.LENGTH_SHORT).show(); // Показ сообщения о необходимости входа
            startActivity(new Intent(this, MainActivity.class)); // Запуск активности входа
            finish(); // Завершение текущей активности
            return; // Выход из метода
        }

        userId = currentUser.getUid(); // Получение ID текущего пользователя
        db = FirebaseFirestore.getInstance(); // Инициализация Firestore

        taskProgressBar = findViewById(R.id.taskProgressBar); // Привязка прогресс-бара из layout
        progressTextView = findViewById(R.id.progressTextView); // Привязка текстового поля из layout
        RecyclerView taskRecyclerView = findViewById(R.id.taskRecyclerView); // Привязка RecyclerView из layout

        List<Task> userTasks = new ArrayList<>(); // Создание списка задач пользователя
        taskAdapter = new TaskAdapter(userTasks, this::updateProgressBar, this); // Инициализация адаптера задач
        taskRecyclerView.setAdapter(taskAdapter); // Установка адаптера для RecyclerView
        taskRecyclerView.setLayoutManager(new LinearLayoutManager(this)); // Установка линейного менеджера расположения

        loadTasks(); // Вызов метода загрузки задач

        FloatingActionButton fabAddTask = findViewById(R.id.fab); // Привязка кнопки добавления задачи
        fabAddTask.setOnClickListener(v -> showAddTaskDialog()); // Установка обработчика нажатия для показа диалога

        FloatingActionButton fabLogout = findViewById(R.id.fab_settings); // Привязка кнопки настроек
        fabLogout.setOnClickListener(v -> { // Установка обработчика нажатия
            Intent intent = new Intent(MainMenuActivity.this, SettingsActivity.class); // Создание намерения для перехода в настройки
            startActivity(intent); // Запуск активности настроек
        });

        updateProgressBar(); // Вызов метода обновления прогресс-бара
    }

    @Override
    protected void onStart() { // Метод жизненного цикла старта активности
        super.onStart(); // Вызов метода родительского класса
        FirebaseUser currentUser = auth.getCurrentUser(); // Получение текущего пользователя
        if (currentUser == null) { // Проверка авторизации пользователя
            Intent intent = new Intent(MainMenuActivity.this, MainActivity.class); // Создание намерения для перехода на экран входа
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // Установка флагов для очистки стека активностей
            startActivity(intent); // Запуск активности входа
            finish(); // Завершение текущей активности
        }
    }

    private void loadTasks() { // Метод для загрузки задач из Firestore
        Log.d("Firestore", "Загрузка задач для userId: " + userId); // Логирование начала загрузки
        db.collection("tasks") // Обращение к коллекции задач в Firestore
                .whereEqualTo("userId", userId) // Фильтр задач по ID пользователя
                .get() // Выполнение запроса
                .addOnSuccessListener(querySnapshot -> { // Обработчик успешного выполнения запроса
                    List<Task> tasks = new ArrayList<>(); // Создание списка для хранения задач
                    for (QueryDocumentSnapshot doc : querySnapshot) { // Перебор всех документов в результате
                        Task task = doc.toObject(Task.class); // Преобразование документа в объект Task
                        task.setId(doc.getId()); // Установка ID задачи
                        tasks.add(task); // Добавление задачи в список
                    }
                    Log.d("Firestore", "Загружено задач: " + tasks.size()); // Логирование количества загруженных задач
                    taskAdapter.setTasks(tasks); // Обновление адаптера новым списком задач
                    updateProgressBar(); // Обновление прогресс-бара
                })
                .addOnFailureListener(e -> { // Обработчик ошибки запроса
                    Log.e("FirestoreError", "Ошибка загрузки задач: " + e.getMessage()); // Логирование ошибки
                    Toast.makeText(this, "Ошибка загрузки задач", Toast.LENGTH_SHORT).show(); // Показ сообщения об ошибке
                });
    }

    private void showAddTaskDialog() { // Метод для отображения диалога добавления задачи
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this); // Создание нижнего диалогового окна
        View dialogView = getLayoutInflater().inflate(R.layout.bottom_sheet_add_task, null); // Надувание layout диалога
        bottomSheetDialog.setContentView(dialogView); // Установка представления для диалога

        EditText taskDescription = dialogView.findViewById(R.id.taskDescription); // Привязка поля ввода описания
        Button pickDateButton = dialogView.findViewById(R.id.pickDateButton); // Привязка кнопки выбора даты
        Button pickTimeButton = dialogView.findViewById(R.id.pickTimeButton); // Привязка кнопки выбора времени
        Button addTaskButton = dialogView.findViewById(R.id.addTaskButton); // Привязка кнопки добавления задачи
        ImageButton collapseButton = dialogView.findViewById(R.id.collapseButton); // Привязка кнопки сворачивания

        final Calendar calendar = Calendar.getInstance(); // Создание экземпляра календаря с текущей датой

        collapseButton.setOnClickListener(v -> bottomSheetDialog.dismiss()); // Установка обработчика для закрытия диалога

        pickDateButton.setOnClickListener(v -> { // Установка обработчика для выбора даты
            DatePickerDialog datePickerDialog = new DatePickerDialog( // Создание диалога выбора даты
                    this, // Контекст
                    (view, year, month, dayOfMonth) -> { // Обработчик выбора даты
                        calendar.set(year, month, dayOfMonth); // Установка выбранной даты в календарь
                        pickDateButton.setText(dayOfMonth + "/" + (month + 1) + "/" + year); // Обновление текста кнопки
                    },
                    calendar.get(Calendar.YEAR), // Текущий год
                    calendar.get(Calendar.MONTH), // Текущий месяц
                    calendar.get(Calendar.DAY_OF_MONTH) // Текущий день
            );
            datePickerDialog.show(); // Показ диалога выбора даты
        });

        pickTimeButton.setOnClickListener(v -> { // Установка обработчика для выбора времени
            TimePickerDialog timePickerDialog = new TimePickerDialog( // Создание диалога выбора времени
                    this, // Контекст
                    (view, hourOfDay, minute) -> { // Обработчик выбора времени
                        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay); // Установка выбранного часа
                        calendar.set(Calendar.MINUTE, minute); // Установка выбранных минут
                        pickTimeButton.setText(hourOfDay + ":" + minute); // Обновление текста кнопки
                    },
                    calendar.get(Calendar.HOUR_OF_DAY), // Текущий час
                    calendar.get(Calendar.MINUTE), // Текущие минуты
                    true // 24-часовой формат
            );
            timePickerDialog.show(); // Показ диалога выбора времени
        });

        addTaskButton.setOnClickListener(v -> { // Установка обработчика для добавления задачи
            String description = taskDescription.getText().toString().trim(); // Получение описания задачи
            if (description.isEmpty()) { // Проверка, пустое ли описание
                Toast.makeText(this, "Введите описание задачи", Toast.LENGTH_SHORT).show(); // Показ сообщения об ошибке
                return; // Выход из обработчика
            }
            Date date = calendar.getTime(); // Получение даты и времени из календаря
            Map<String, Object> task = new HashMap<>(); // Создание словаря для данных задачи
            task.put("userId", userId); // Добавление ID пользователя
            task.put("description", description); // Добавление описания задачи
            task.put("dateTime", date); // Добавление даты и времени
            task.put("isCompleted", false); // Установка статуса выполнения (не выполнена)
            task.put("isExpanded", false); // Установка статуса раскрытия (не раскрыта)

            Log.d("Firestore", "Добавление задачи: " + description); // Логирование добавления задачи
            db.collection("tasks").add(task) // Добавление задачи в коллекцию Firestore
                    .addOnSuccessListener(doc -> { // Обработчик успешного добавления
                        Log.d("Firestore", "Задача добавлена с ID: " + doc.getId()); // Логирование ID добавленной задачи
                        bottomSheetDialog.dismiss(); // Закрытие диалога
                        loadTasks(); // Перезагрузка списка задач
                    })
                    .addOnFailureListener(e -> { // Обработчик ошибки добавления
                        Log.e("FirestoreError", "Ошибка добавления задачи: " + e.getMessage()); // Логирование ошибки
                        Toast.makeText(this, "Ошибка добавления задачи", Toast.LENGTH_SHORT).show(); // Показ сообщения об ошибке
                    });
        });

        bottomSheetDialog.show(); // Показ диалогового окна
    }

    public void updateProgressBar() { // Метод для обновления прогресс-бара
        int totalTasks = taskAdapter.getItemCount(); // Получение общего количества задач
        int completedTasks = taskAdapter.getCompletedTaskCount(); // Получение количества выполненных задач
        int progress = (totalTasks > 0) ? (completedTasks * 100 / totalTasks) : 0; // Вычисление процента выполнения

        taskProgressBar.setProgress(progress); // Установка значения прогресс-бара
        progressTextView.setText("Прогресс: " + progress + "%"); // Обновление текста прогресса
    }
}