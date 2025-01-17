package com.example.todolist;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class MainMenuActivity extends AppCompatActivity {

    private TaskAdapter taskAdapter;
    private ProgressBar taskProgressBar;
    private TextView progressTextView;

    private DatabaseHelper dbHelper;
    private int userId; // текущий пользователь

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mainmenu);

        userId = getIntent().getIntExtra("USER_ID", -1);
        if (userId == -1) {
            Toast.makeText(this, "Неизвестный пользователь!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        taskProgressBar = findViewById(R.id.taskProgressBar);
        progressTextView = findViewById(R.id.progressTextView);
        RecyclerView taskRecyclerView = findViewById(R.id.taskRecyclerView);

        dbHelper = new DatabaseHelper(this);

        // Загружаем задачи
        List<Task> userTasks = dbHelper.getTasksForUser(userId);

        // Создаём адаптер (передаём контекст в конце)
        taskAdapter = new TaskAdapter(userTasks, dbHelper, this::updateProgressBar, this);
        taskRecyclerView.setAdapter(taskAdapter);
        taskRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(v -> showAddTaskDialog());

        updateProgressBar();
    }

    private void showAddTaskDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_task, null);
        builder.setView(dialogView);

        EditText taskDescription = dialogView.findViewById(R.id.taskDescription);
        Button pickDateButton = dialogView.findViewById(R.id.pickDateButton);
        Button pickTimeButton = dialogView.findViewById(R.id.pickTimeButton);
        Button addTaskButton = dialogView.findViewById(R.id.addTaskButton);

        final Calendar calendar = Calendar.getInstance();

        pickDateButton.setOnClickListener(v -> {
            DatePickerDialog dpd = new DatePickerDialog(
                    this,
                    (view, year, month, dayOfMonth) -> {
                        calendar.set(year, month, dayOfMonth);
                        pickDateButton.setText(dayOfMonth + "/" + (month + 1) + "/" + year);
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );
            dpd.show();
        });

        pickTimeButton.setOnClickListener(v -> {
            TimePickerDialog tpd = new TimePickerDialog(
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
            tpd.show();
        });

        AlertDialog dialog = builder.create();
        addTaskButton.setOnClickListener(v -> {
            String description = taskDescription.getText().toString();
            if (description.isEmpty()) {
                Toast.makeText(this, "Введите описание задачи", Toast.LENGTH_SHORT).show();
            } else {
                Date date = calendar.getTime();
                Task newTask = new Task(userId, description, date);
                taskAdapter.addTask(newTask);
                updateProgressBar();
                dialog.dismiss();
            }
        });
        dialog.show();
    }

    public void updateProgressBar() {
        int totalTasks = taskAdapter.getItemCount();
        int completedTasks = taskAdapter.getCompletedTaskCount();
        int progress = (totalTasks > 0) ? (completedTasks * 100 / totalTasks) : 0;
        taskProgressBar.setProgress(progress);
        progressTextView.setText("Прогресс: " + progress + "%");
    }
}
