package com.example.todolist;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.Calendar;

public class MainMenuActivity extends AppCompatActivity {

    private TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mainmenu);

        FloatingActionButton fab = findViewById(R.id.fab);
        textView = findViewById(R.id.textView);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddTaskDialog();
            }
        });
    }

    private void showAddTaskDialog() {
        // Создаем AlertDialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_task, null);
        builder.setView(dialogView);

        // Элементы диалога
        EditText taskDescription = dialogView.findViewById(R.id.taskDescription);
        Button pickDateButton = dialogView.findViewById(R.id.pickDateButton);
        Button pickTimeButton = dialogView.findViewById(R.id.pickTimeButton);
        Button addTaskButton = dialogView.findViewById(R.id.addTaskButton);

        final Calendar calendar = Calendar.getInstance();

        pickDateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DatePickerDialog datePickerDialog = new DatePickerDialog(MainMenuActivity.this,
                        (view, year, month, dayOfMonth) -> {
                            calendar.set(Calendar.YEAR, year);
                            calendar.set(Calendar.MONTH, month);
                            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                            pickDateButton.setText(dayOfMonth + "/" + (month + 1) + "/" + year);
                        },
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH));
                datePickerDialog.show();
            }
        });

        pickTimeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TimePickerDialog timePickerDialog = new TimePickerDialog(MainMenuActivity.this,
                        (view, hourOfDay, minute) -> {
                            calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                            calendar.set(Calendar.MINUTE, minute);
                            pickTimeButton.setText(hourOfDay + ":" + minute);
                        },
                        calendar.get(Calendar.HOUR_OF_DAY),
                        calendar.get(Calendar.MINUTE),
                        true);
                timePickerDialog.show();
            }
        });

        AlertDialog dialog = builder.create();

        addTaskButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String taskText = taskDescription.getText().toString();
                if (taskText.isEmpty()) {
                    Toast.makeText(MainMenuActivity.this, "Введите описание задачи", Toast.LENGTH_SHORT).show();
                } else {
                    textView.setText("Добавлена задача: " + taskText + "\n" +
                            "Дата: " + pickDateButton.getText().toString() + "\n" +
                            "Время: " + pickTimeButton.getText().toString());
                    dialog.dismiss();
                }
            }
        });

        dialog.show();
    }
}
