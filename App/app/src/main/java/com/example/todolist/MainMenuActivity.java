package com.example.todolist;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import androidx.annotation.NonNull;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

public class MainMenuActivity extends AppCompatActivity {
    private static final String TAG = "MainMenuActivity";
    private TaskAdapter taskAdapter;
    private ProgressBar taskProgressBar;
    private TextView progressTextView;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String userId;
    private CalendarView calendarView;
    private Calendar selectedCalendarDate;
    private FloatingActionButton showAddedTasksButton;
    private ImageButton backButton;
    private View calendarContainer;
    private RecyclerView taskRecyclerView;
    private ListenerRegistration tasksListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mainmenu);

        auth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Пожалуйста, войдите в систему", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        userId = currentUser.getUid();
        db = FirebaseFirestore.getInstance();

        db.enableNetwork().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.d(TAG, "Firestore network enabled");
            } else {
                Log.e(TAG, "Failed to enable Firestore network", task.getException());
            }
        });

        taskProgressBar = findViewById(R.id.taskProgressBar);
        progressTextView = findViewById(R.id.progressTextView);
        taskRecyclerView = findViewById(R.id.taskRecyclerView);
        calendarView = findViewById(R.id.calendarView);
        calendarContainer = findViewById(R.id.calendar_container);
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigationView);
        showAddedTasksButton = findViewById(R.id.showAddedTasksButton);
        backButton = findViewById(R.id.backButton);

        List<Task> userTasks = new ArrayList<>();
        taskAdapter = new TaskAdapter(userTasks, this::updateProgressBar, this);
        taskRecyclerView.setAdapter(taskAdapter);
        taskRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        selectedCalendarDate = Calendar.getInstance();
        if (calendarView != null) {
            calendarView.setDate(selectedCalendarDate.getTimeInMillis(), true, true);
        } else {
            Log.e(TAG, "calendarView is null during onCreate");
        }

        if (calendarView != null) {
            calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
                if (isFinishing()) return;
                selectedCalendarDate.set(year, month, dayOfMonth);
                Log.d(TAG, "Selected date changed to: " + year + "/" + (month + 1) + "/" + dayOfMonth);
                loadTasks(selectedCalendarDate);
            });
        }

        bottomNav.setOnNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_tasks) {
                if (selectedCalendarDate == null) {
                    Log.e(TAG, "selectedCalendarDate is null in nav_tasks");
                    selectedCalendarDate = Calendar.getInstance();
                }
                loadTasks(selectedCalendarDate);
                showCalendarView();
                return true;
            } else if (itemId == R.id.nav_add_task) {
                showAddTaskDialog();
                return true;
            } else if (itemId == R.id.nav_settings) {
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            }
            return false;
        });

        showAddedTasksButton.setOnClickListener(v -> {
            try {
                if (isFinishing()) return;
                Log.d(TAG, "showAddedTasksButton clicked");
                loadAllTasks();
                hideCalendarView();
            } catch (Exception e) {
                Log.e(TAG, "Error in showAddedTasksButton click: " + e.getMessage(), e);
                Toast.makeText(this, "Ошибка при загрузке всех задач", Toast.LENGTH_SHORT).show();
            }
        });

        backButton.setOnClickListener(v -> {
            try {
                if (isFinishing()) return;
                Log.d(TAG, "backButton clicked");
                debugState("Before loadTasks in backButton");
                if (selectedCalendarDate == null) {
                    Log.e(TAG, "selectedCalendarDate is null in backButton click");
                    selectedCalendarDate = Calendar.getInstance();
                }
                loadTasks(selectedCalendarDate);
                showCalendarView();
            } catch (Exception e) {
                Log.e(TAG, "Error in backButton click: " + e.getMessage(), e);
                Toast.makeText(this, "Ошибка при возврате к календарю", Toast.LENGTH_SHORT).show();
            }
        });

        if (selectedCalendarDate == null) {
            Log.e(TAG, "selectedCalendarDate is null during initial load");
            selectedCalendarDate = Calendar.getInstance();
        }
        loadTasks(selectedCalendarDate);
        updateProgressBar();
    }

    private void debugState(String checkpoint) {
        Log.d(TAG, "Debug state at " + checkpoint + ":");
        Log.d(TAG, "selectedCalendarDate: " + (selectedCalendarDate != null ? selectedCalendarDate.getTime().toString() : "null"));
        Log.d(TAG, "taskAdapter: " + (taskAdapter != null ? "not null" : "null"));
        Log.d(TAG, "taskRecyclerView: " + (taskRecyclerView != null ? "not null" : "null"));
        Log.d(TAG, "calendarContainer: " + (calendarContainer != null ? "not null" : "null"));
        Log.d(TAG, "calendarView: " + (calendarView != null ? "not null" : "null"));
        Log.d(TAG, "backButton: " + (backButton != null ? "not null" : "null"));
        Log.d(TAG, "showAddedTasksButton: " + (showAddedTasksButton != null ? "not null" : "null"));
        Log.d(TAG, "isGroupedView: " + (taskAdapter != null ? taskAdapter.isGroupedView() : "unknown"));
    }

    private void showAddTaskDialog() {
        if (isFinishing()) return;
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View dialogView = getLayoutInflater().inflate(R.layout.bottom_sheet_add_task, null);
        bottomSheetDialog.setContentView(dialogView);

        EditText taskDescription = dialogView.findViewById(R.id.taskDescription);
        Button pickDateButton = dialogView.findViewById(R.id.pickDateButton);
        Button pickTimeButton = dialogView.findViewById(R.id.pickTimeButton);
        Button addTaskButton = dialogView.findViewById(R.id.addTaskButton);
        ImageButton collapseButton = dialogView.findViewById(R.id.collapseButton);

        Calendar calendar = (Calendar) selectedCalendarDate.clone();
        pickDateButton.setText(String.format("%02d/%02d/%d",
                calendar.get(Calendar.DAY_OF_MONTH),
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.YEAR)));

        Calendar now = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, now.get(Calendar.HOUR_OF_DAY));
        calendar.set(Calendar.MINUTE, now.get(Calendar.MINUTE));
        pickTimeButton.setText(String.format("%02d:%02d",
                now.get(Calendar.HOUR_OF_DAY),
                now.get(Calendar.MINUTE)));

        collapseButton.setOnClickListener(v -> bottomSheetDialog.dismiss());

        pickDateButton.setOnClickListener(v -> {
            new DatePickerDialog(
                    this,
                    (view, year, month, dayOfMonth) -> {
                        calendar.set(year, month, dayOfMonth);
                        pickDateButton.setText(String.format("%02d/%02d/%d",
                                dayOfMonth, month + 1, year));
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            ).show();
        });

        pickTimeButton.setOnClickListener(v -> {
            new TimePickerDialog(
                    this,
                    (view, hourOfDay, minute) -> {
                        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                        calendar.set(Calendar.MINUTE, minute);
                        pickTimeButton.setText(String.format("%02d:%02d", hourOfDay, minute));
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    true
            ).show();
        });

        addTaskButton.setOnClickListener(v -> {
            String description = taskDescription.getText().toString().trim();
            if (description.isEmpty()) {
                Toast.makeText(this, "Введите описание задачи", Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, Object> task = new HashMap<>();
            task.put("userId", userId);
            task.put("description", description);
            task.put("dateTime", calendar.getTime());
            task.put("isCompleted", false);
            task.put("isExpanded", false);

            // Закрываем диалог сразу после вызова add(), так как Firestore обработает это локально
            bottomSheetDialog.dismiss();
            Toast.makeText(this, "Задача добавлена", Toast.LENGTH_SHORT).show();

            Executors.newSingleThreadExecutor().execute(() -> {
                db.collection("tasks").add(task)
                        .addOnFailureListener(e -> runOnUiThread(() ->
                                Toast.makeText(this, "Ошибка добавления задачи: " + e.getMessage(), Toast.LENGTH_SHORT).show()));
            });
        });

        bottomSheetDialog.show();
    }

    private void loadTasks(Calendar selectedDate) {
        if (isFinishing()) return;
        if (selectedDate == null) {
            Log.e(TAG, "selectedDate is null in loadTasks");
            selectedDate = Calendar.getInstance();
        }

        Calendar startOfDay = (Calendar) selectedDate.clone();
        startOfDay.set(Calendar.HOUR_OF_DAY, 0);
        startOfDay.set(Calendar.MINUTE, 0);
        startOfDay.set(Calendar.SECOND, 0);
        startOfDay.set(Calendar.MILLISECOND, 0);

        Calendar endOfDay = (Calendar) selectedDate.clone();
        endOfDay.set(Calendar.HOUR_OF_DAY, 23);
        endOfDay.set(Calendar.MINUTE, 59);
        endOfDay.set(Calendar.SECOND, 59);
        endOfDay.set(Calendar.MILLISECOND, 999);

        Log.d(TAG, "Loading tasks for date range: " + startOfDay.getTime() + " to " + endOfDay.getTime());

        if (tasksListener != null) {
            tasksListener.remove();
            tasksListener = null;
        }

        Query query = db.collection("tasks")
                .whereEqualTo("userId", userId)
                .whereGreaterThanOrEqualTo("dateTime", startOfDay.getTime())
                .whereLessThanOrEqualTo("dateTime", endOfDay.getTime());

        tasksListener = query.addSnapshotListener((querySnapshot, error) -> {
            if (error != null) {
                Log.e(TAG, "Failed to listen for tasks: " + error.getMessage(), error);
                Toast.makeText(this, "Ошибка загрузки задач", Toast.LENGTH_SHORT).show();
                return;
            }

            List<Task> tasks = new ArrayList<>();
            if (querySnapshot != null) {
                for (QueryDocumentSnapshot doc : querySnapshot) {
                    try {
                        Task task = doc.toObject(Task.class);
                        task.setId(doc.getId());
                        if (task.getDateTime() == null) {
                            Log.w(TAG, "Task " + task.getId() + " has null dateTime, skipping");
                            continue;
                        }
                        tasks.add(task);
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing task " + doc.getId() + ": " + e.getMessage(), e);
                    }
                }
            }

            Log.d(TAG, "Loaded " + tasks.size() + " tasks for the selected date");
            new Handler(Looper.getMainLooper()).post(() -> {
                try {
                    if (isFinishing()) return;
                    if (taskAdapter != null) {
                        taskRecyclerView.setAdapter(null);
                        taskAdapter.setTasks(tasks);
                        taskRecyclerView.setAdapter(taskAdapter);
                        updateProgressBar();
                    } else {
                        Log.e(TAG, "taskAdapter is null in loadTasks");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error updating UI in loadTasks: " + e.getMessage(), e);
                    Toast.makeText(this, "Ошибка обновления списка задач", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void loadAllTasks() {
        if (isFinishing()) return;
        Log.d(TAG, "Loading all tasks for user: " + userId);

        if (tasksListener != null) {
            tasksListener.remove();
            tasksListener = null;
        }

        Query query = db.collection("tasks")
                .whereEqualTo("userId", userId);

        tasksListener = query.addSnapshotListener((querySnapshot, error) -> {
            if (error != null) {
                Log.e(TAG, "Failed to listen for all tasks: " + error.getMessage(), error);
                Toast.makeText(this, "Ошибка загрузки всех задач", Toast.LENGTH_SHORT).show();
                return;
            }

            List<Task> tasks = new ArrayList<>();
            if (querySnapshot != null) {
                for (QueryDocumentSnapshot doc : querySnapshot) {
                    try {
                        Task task = doc.toObject(Task.class);
                        task.setId(doc.getId());
                        if (task.getDateTime() == null) {
                            Log.w(TAG, "Task " + task.getId() + " has null dateTime, skipping");
                            continue;
                        }
                        tasks.add(task);
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing task " + doc.getId() + ": " + e.getMessage(), e);
                    }
                }
            }

            Log.d(TAG, "Loaded " + tasks.size() + " tasks in total");
            new Handler(Looper.getMainLooper()).post(() -> {
                try {
                    if (isFinishing()) return;
                    if (taskAdapter != null) {
                        taskRecyclerView.setAdapter(null);
                        taskAdapter.setGroupedTasks(tasks);
                        taskRecyclerView.setAdapter(taskAdapter);
                        updateProgressBar();
                    } else {
                        Log.e(TAG, "taskAdapter is null in loadAllTasks");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error updating UI in loadAllTasks: " + e.getMessage(), e);
                    Toast.makeText(this, "Ошибка обновления списка задач", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void showCalendarView() {
        if (isFinishing()) return;
        Log.d(TAG, "Showing calendar view");
        try {
            if (calendarContainer != null) {
                calendarContainer.setVisibility(View.VISIBLE);
            } else {
                Log.e(TAG, "calendarContainer is null in showCalendarView");
            }
            if (backButton != null) {
                backButton.setVisibility(View.GONE);
            } else {
                Log.e(TAG, "backButton is null in showCalendarView");
            }
            if (showAddedTasksButton != null) {
                showAddedTasksButton.setVisibility(View.VISIBLE);
            } else {
                Log.e(TAG, "showAddedTasksButton is null in showCalendarView");
            }
            if (calendarView != null && selectedCalendarDate != null) {
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    if (isFinishing()) return;
                    try {
                        calendarView.setDate(selectedCalendarDate.getTimeInMillis(), true, true);
                    } catch (Exception e) {
                        Log.e(TAG, "Error setting date on calendarView: " + e.getMessage(), e);
                    }
                }, 100);
            } else {
                Log.e(TAG, "calendarView or selectedCalendarDate is null in showCalendarView");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in showCalendarView: " + e.getMessage(), e);
            Toast.makeText(this, "Ошибка отображения календаря", Toast.LENGTH_SHORT).show();
        }
    }

    private void hideCalendarView() {
        if (isFinishing()) return;
        Log.d(TAG, "Hiding calendar view");
        try {
            if (calendarContainer != null) {
                calendarContainer.setVisibility(View.GONE);
            } else {
                Log.e(TAG, "calendarContainer is null in hideCalendarView");
            }
            if (backButton != null) {
                backButton.setVisibility(View.VISIBLE);
            } else {
                Log.e(TAG, "backButton is null in hideCalendarView");
            }
            if (showAddedTasksButton != null) {
                showAddedTasksButton.setVisibility(View.GONE);
            } else {
                Log.e(TAG, "showAddedTasksButton is null in hideCalendarView");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in hideCalendarView: " + e.getMessage(), e);
            Toast.makeText(this, "Ошибка скрытия календаря", Toast.LENGTH_SHORT).show();
        }
    }

    public void updateProgressBar() {
        if (isFinishing()) return;
        try {
            if (taskAdapter == null) {
                Log.e(TAG, "taskAdapter is null in updateProgressBar");
                return;
            }
            int totalTasks = taskAdapter.getItemCount();
            int completedTasks = taskAdapter.getCompletedTaskCount();
            int progress = totalTasks > 0 ? (completedTasks * 100 / totalTasks) : 0;

            if (taskProgressBar != null) {
                taskProgressBar.setProgress(progress);
            } else {
                Log.e(TAG, "taskProgressBar is null in updateProgressBar");
            }
            if (progressTextView != null) {
                progressTextView.setText("Прогресс: " + progress + "%");
            } else {
                Log.e(TAG, "progressTextView is null in updateProgressBar");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in updateProgressBar: " + e.getMessage(), e);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
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
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (selectedCalendarDate != null) {
            outState.putLong("selectedCalendarDate", selectedCalendarDate.getTimeInMillis());
        }
        outState.putBoolean("isGroupedView", taskAdapter != null && taskAdapter.isGroupedView());
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        long dateMillis = savedInstanceState.getLong("selectedCalendarDate", -1);
        if (dateMillis != -1) {
            selectedCalendarDate = Calendar.getInstance();
            selectedCalendarDate.setTimeInMillis(dateMillis);
        }
        boolean isGrouped = savedInstanceState.getBoolean("isGroupedView", false);
        if (isGrouped) {
            loadAllTasks();
            hideCalendarView();
        } else {
            loadTasks(selectedCalendarDate);
            showCalendarView();
        }
    }
}