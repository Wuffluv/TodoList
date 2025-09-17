package com.example.todolist;

// Импорты необходимых библиотек и классов
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
    // Объявление переменных экземпляра
    private Button logoutButton; // Кнопка выхода из системы
    private FirebaseAuth auth; // Экземпляр аутентификации Firebase
    private FirebaseFirestore db; // Экземпляр Firestore
    private String userId; // ID текущего пользователя
    private Calendar selectedCalendarDate; // Выбранная дата для диалога добавления задачи

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity); // Установка макета активности настроек

        // Инициализация Firebase
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        selectedCalendarDate = Calendar.getInstance(); // Инициализация календаря текущей датой

        // Проверка авторизации пользователя
        if (userId == null) {
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
            FirebaseAuth.getInstance().signOut(); // Выход из учетной записи
            Intent intent = new Intent(SettingsActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // Очистка стека активностей
            startActivity(intent);
            finish();
        });

        // Настройка нижней навигации
        bottomNav.setOnNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_tasks) {
                // Переход в MainMenuActivity для просмотра задач
                Intent intent = new Intent(SettingsActivity.this, MainMenuActivity.class);
                startActivity(intent);
                finish();
                return true;
            } else if (itemId == R.id.nav_add_task) {
                // Открытие диалога добавления новой задачи
                showAddTaskDialog();
                return true;
            } else if (itemId == R.id.nav_settings) {
                // Текущая активность — уже настройки
                return true;
            }
            return false;
        });

        // Установка текущей вкладки как "Настройки"
        bottomNav.setSelectedItemId(R.id.nav_settings);
    }

    // Метод отображения диалога добавления задачи
    private void showAddTaskDialog() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this); // Создание диалога BottomSheet
        View dialogView = getLayoutInflater().inflate(R.layout.bottom_sheet_add_task, null); // Загрузка макета диалога
        bottomSheetDialog.setContentView(dialogView);

        // Инициализация элементов диалога
        EditText taskDescription = dialogView.findViewById(R.id.taskDescription); // Поле для описания задачи
        Button pickDateButton = dialogView.findViewById(R.id.pickDateButton); // Кнопка выбора даты
        Button pickTimeButton = dialogView.findViewById(R.id.pickTimeButton); // Кнопка выбора времени
        Button addTaskButton = dialogView.findViewById(R.id.addTaskButton); // Кнопка добавления задачи
        ImageButton collapseButton = dialogView.findViewById(R.id.collapseButton); // Кнопка сворачивания диалога

        // Использование текущей даты для диалога
        Calendar calendar = (Calendar) selectedCalendarDate.clone();

        // Установка начальных значений для даты
        pickDateButton.setText(String.format("%02d/%02d/%d",
                calendar.get(Calendar.DAY_OF_MONTH),
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.YEAR)));

        // Установка текущего времени
        Calendar now = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, now.get(Calendar.HOUR_OF_DAY));
        calendar.set(Calendar.MINUTE, now.get(Calendar.MINUTE));
        pickTimeButton.setText(String.format("%02d:%02d",
                now.get(Calendar.HOUR_OF_DAY),
                now.get(Calendar.MINUTE)));

        // Обработчик сворачивания диалога
        collapseButton.setOnClickListener(v -> bottomSheetDialog.dismiss());

        // Обработчик выбора даты
        pickDateButton.setOnClickListener(v -> {
            new DatePickerDialog(
                    this,
                    (view, year, month, dayOfMonth) -> {
                        calendar.set(year, month, dayOfMonth);
                        pickDateButton.setText(String.format("%02d/%02d/%d",
                                dayOfMonth, month + 1, year)); // Обновление текста кнопки
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            ).show();
        });

        // Обработчик выбора времени
        pickTimeButton.setOnClickListener(v -> {
            new TimePickerDialog(
                    this,
                    (view, hourOfDay, minute) -> {
                        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                        calendar.set(Calendar.MINUTE, minute);
                        pickTimeButton.setText(String.format("%02d:%02d", hourOfDay, minute)); // Обновление текста кнопки
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    true
            ).show();
        });

        // Обработчик добавления задачи
        addTaskButton.setOnClickListener(v -> {
            String description = taskDescription.getText().toString().trim();
            if (description.isEmpty()) {
                Toast.makeText(this, "Введите описание задачи", Toast.LENGTH_SHORT).show();
                return;
            }

            // Создание объекта задачи для Firestore
            Map<String, Object> task = new HashMap<>();
            task.put("userId", userId);
            task.put("description", description);
            task.put("dateTime", calendar.getTime());
            task.put("isCompleted", false);
            task.put("isExpanded", false);

            // Сохранение задачи в Firestore
            db.collection("tasks").add(task)
                    .addOnSuccessListener(doc -> {
                        bottomSheetDialog.dismiss(); // Закрытие диалога
                        Toast.makeText(this, "Задача добавлена", Toast.LENGTH_SHORT).show();
                        // Переход в MainMenuActivity после добавления задачи
                        Intent intent = new Intent(SettingsActivity.this, MainMenuActivity.class);
                        startActivity(intent);
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Log.e("FirestoreError", "Ошибка добавления задачи: " + e.getMessage());
                        Toast.makeText(this, "Ошибка добавления задачи", Toast.LENGTH_SHORT).show();
                    });
        });

        bottomSheetDialog.show(); // Отображение диалога
    }

    // Проверка авторизации при старте активности
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