package com.example.todolist;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    private List<Task> taskList;
    private final Runnable progressUpdateCallback;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
    private final Context context;
    private final FirebaseFirestore db;

    public TaskAdapter(List<Task> tasks, Runnable progressUpdateCallback, Context context) {
        this.taskList = tasks != null ? tasks : new ArrayList<>();
        this.progressUpdateCallback = progressUpdateCallback;
        this.context = context;
        this.db = FirebaseFirestore.getInstance();
        Collections.sort(taskList, Comparator.comparing(Task::getDateTime));
    }

    public void setTasks(List<Task> tasks) {
        this.taskList = tasks != null ? tasks : new ArrayList<>();
        Collections.sort(taskList, Comparator.comparing(Task::getDateTime));
        notifyDataSetChanged();
        progressUpdateCallback.run();
    }

    private void removeTask(int position) {
        // Проверяем валидность индекса
        if (position < 0 || position >= taskList.size()) {
            return; // Ничего не делаем, если индекс недействителен
        }

        // Получаем задачу по позиции
        Task task = taskList.get(position);

        // Удаляем задачу из Firestore
        db.collection("tasks").document(task.getId()).delete()
                .addOnSuccessListener(aVoid -> {
                    // Удаляем задачу из списка по её ID, а не по позиции
                    taskList.removeIf(t -> t.getId().equals(task.getId()));
                    notifyDataSetChanged();
                    progressUpdateCallback.run();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Ошибка удаления задачи: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void updateTaskCompletion(Task task, boolean isCompleted) {
        task.setCompleted(isCompleted);
        db.collection("tasks").document(task.getId())
                .update("isCompleted", isCompleted)
                .addOnSuccessListener(aVoid -> progressUpdateCallback.run())
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Ошибка обновления статуса: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void showEditTaskDialog(Task task) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_task, null);
        builder.setView(dialogView);

        EditText taskDescription = dialogView.findViewById(R.id.taskDescription);
        Button pickDateButton = dialogView.findViewById(R.id.pickDateButton);
        Button pickTimeButton = dialogView.findViewById(R.id.pickTimeButton);
        Button addTaskButton = dialogView.findViewById(R.id.addTaskButton);

        addTaskButton.setText("Сохранить");

        final Calendar calendar = Calendar.getInstance();
        calendar.setTime(task.getDateTime());

        taskDescription.setText(task.getDescription());
        pickDateButton.setText(dateFormat.format(task.getDateTime()).split(" ")[0]);
        pickTimeButton.setText(dateFormat.format(task.getDateTime()).split(" ")[1]);

        pickDateButton.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    context,
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

        pickTimeButton.setOnClickListener(v -> {
            TimePickerDialog timePickerDialog = new TimePickerDialog(
                    context,
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

        AlertDialog dialog = builder.create();
        addTaskButton.setOnClickListener(v -> {
            String newDesc = taskDescription.getText().toString();
            if (newDesc.isEmpty()) return;

            Date newDate = calendar.getTime();
            db.collection("tasks").document(task.getId())
                    .update("description", newDesc, "dateTime", newDate)
                    .addOnSuccessListener(aVoid -> {
                        task.setDescription(newDesc);
                        task.setDateTime(newDate);
                        notifyDataSetChanged();
                        progressUpdateCallback.run();
                        dialog.dismiss();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(context, "Ошибка редактирования: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
        });
        dialog.show();
    }

    public int getCompletedTaskCount() {
        int count = 0;
        for (Task t : taskList) {
            if (t.isCompleted()) count++;
        }
        return count;
    }

    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.task_item, parent, false);
        return new TaskViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = taskList.get(position);
        holder.bind(task);
    }

    @Override
    public int getItemCount() {
        return taskList.size();
    }

    class TaskViewHolder extends RecyclerView.ViewHolder {

        private final TextView taskTextView;
        private final CheckBox taskCheckBox;
        private final ImageButton deleteButton;
        private final ImageButton editButton;
        private final ImageButton expandButton;
        private final ImageButton addSubTaskButton;
        private final RecyclerView subTaskRecyclerView;
        private SubTaskAdapter subTaskAdapter;
        private boolean isExpanded = false;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            taskTextView = itemView.findViewById(R.id.taskTextView);
            taskCheckBox = itemView.findViewById(R.id.taskCheckBox);
            deleteButton = itemView.findViewById(R.id.deleteButton);
            editButton = itemView.findViewById(R.id.editButton);
            expandButton = itemView.findViewById(R.id.expandButton);
            addSubTaskButton = itemView.findViewById(R.id.addSubTaskButton);
            subTaskRecyclerView = itemView.findViewById(R.id.subTaskRecyclerView);

            deleteButton.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    removeTask(position);
                }
            });

            editButton.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    Task t = taskList.get(position);
                    showEditTaskDialog(t);
                }
            });

            expandButton.setOnClickListener(v -> {
                isExpanded = !isExpanded;
                if (isExpanded) {
                    expandButton.setImageResource(android.R.drawable.arrow_up_float);
                    subTaskRecyclerView.setVisibility(View.VISIBLE);
                    addSubTaskButton.setVisibility(View.VISIBLE);
                } else {
                    expandButton.setImageResource(android.R.drawable.arrow_down_float);
                    subTaskRecyclerView.setVisibility(View.GONE);
                    addSubTaskButton.setVisibility(View.GONE);
                }
            });

            addSubTaskButton.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    Task t = taskList.get(position);
                    showAddSubTaskDialog(t);
                }
            });
        }

        public void bind(Task task) {
            taskCheckBox.setOnCheckedChangeListener(null);
            taskCheckBox.setChecked(task.isCompleted());
            taskCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    Task t = taskList.get(position);
                    updateTaskCompletion(t, isChecked);
                }
            });

            String dateTimeStr = dateFormat.format(task.getDateTime());
            taskTextView.setText(task.getDescription() + " (" + dateTimeStr + ")");

            if (task.isCompleted()) {
                taskTextView.setPaintFlags(taskTextView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            } else {
                taskTextView.setPaintFlags(taskTextView.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            }

            List<SubTask> subTasks = new ArrayList<>();
            subTaskAdapter = new SubTaskAdapter(subTasks, progressUpdateCallback, context, task.getId());
            subTaskRecyclerView.setLayoutManager(new LinearLayoutManager(itemView.getContext()));
            subTaskRecyclerView.setAdapter(subTaskAdapter);

            db.collection("tasks").document(task.getId()).collection("subtasks")
                    .addSnapshotListener((snapshots, e) -> {
                        if (e != null) return;
                        List<SubTask> updatedSubTasks = new ArrayList<>();
                        for (QueryDocumentSnapshot doc : snapshots) {
                            SubTask st = doc.toObject(SubTask.class);
                            st.setSubTaskId(doc.getId());
                            updatedSubTasks.add(st);
                        }
                        subTaskAdapter.setSubTasks(updatedSubTasks);
                    });

            subTaskRecyclerView.setVisibility(View.GONE);
            addSubTaskButton.setVisibility(View.GONE);
            expandButton.setImageResource(android.R.drawable.arrow_down_float);
            isExpanded = false;
        }

        private void showAddSubTaskDialog(Task task) {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_task, null);
            builder.setView(dialogView);

            EditText taskDescription = dialogView.findViewById(R.id.taskDescription);
            Button pickDateButton = dialogView.findViewById(R.id.pickDateButton);
            Button pickTimeButton = dialogView.findViewById(R.id.pickTimeButton);
            Button addTaskButton = dialogView.findViewById(R.id.addTaskButton);

            pickDateButton.setVisibility(View.GONE);
            pickTimeButton.setVisibility(View.GONE);
            addTaskButton.setText("Добавить подзадачу");

            AlertDialog dialog = builder.create();
            addTaskButton.setOnClickListener(v -> {
                String description = taskDescription.getText().toString();
                if (description.isEmpty()) {
                    Toast.makeText(context, "Введите описание подзадачи", Toast.LENGTH_SHORT).show();
                    return;
                }
                Map<String, Object> subTask = new HashMap<>();
                subTask.put("description", description);
                subTask.put("isCompleted", false);

                db.collection("tasks").document(task.getId()).collection("subtasks")
                        .add(subTask)
                        .addOnSuccessListener(doc -> dialog.dismiss())
                        .addOnFailureListener(e -> {
                            Toast.makeText(context, "Ошибка добавления подзадачи: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        });
            });
            dialog.show();
        }
    }
}