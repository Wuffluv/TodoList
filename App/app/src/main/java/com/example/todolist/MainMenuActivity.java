package com.example.todolist;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.View;           // Добавлен импорт
import android.widget.Button;      // Добавлен импорт
import android.widget.EditText;    // Добавлен импорт
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;        // Добавлен импорт

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.Calendar;

public class MainMenuActivity extends AppCompatActivity {

    private TaskAdapter taskAdapter;
    private ProgressBar taskProgressBar;
    private TextView progressTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mainmenu);

        taskProgressBar = findViewById(R.id.taskProgressBar);
        progressTextView = findViewById(R.id.progressTextView);

        RecyclerView taskRecyclerView = findViewById(R.id.taskRecyclerView);
        taskAdapter = new TaskAdapter(this::updateProgressBar);
        taskRecyclerView.setAdapter(taskAdapter);
        taskRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(v -> showAddTaskDialog());
    }

    private void showAddTaskDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        // Инфлейтим layout для диалога
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_task, null);
        builder.setView(dialogView);

        // Находим поля и кнопки в dialog_add_task.xml
        EditText taskDescription = dialogView.findViewById(R.id.taskDescription);
        Button pickDateButton = dialogView.findViewById(R.id.pickDateButton);
        Button pickTimeButton = dialogView.findViewById(R.id.pickTimeButton);
        Button addTaskButton = dialogView.findViewById(R.id.addTaskButton);

        final Calendar calendar = Calendar.getInstance();

        // Установка даты
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

        // Установка времени
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

        // Создаём и отображаем диалог
        AlertDialog dialog = builder.create();

        // Кнопка "Добавить задачу"
        addTaskButton.setOnClickListener(v -> {
            String description = taskDescription.getText().toString();
            if (description.isEmpty()) {
                Toast.makeText(this, "Введите описание задачи", Toast.LENGTH_SHORT).show();
            } else {
                taskAdapter.addTask(new Task(description, calendar.getTime()));
                updateProgressBar();
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    private void updateProgressBar() {
        int totalTasks = taskAdapter.getItemCount();
        int completedTasks = taskAdapter.getCompletedTaskCount();
        int progress = totalTasks > 0 ? (completedTasks * 100 / totalTasks) : 0;
        taskProgressBar.setProgress(progress);
        progressTextView.setText("Прогресс: " + progress + "%");
    }
}
