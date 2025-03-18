package com.example.todolist;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.graphics.Paint;
import android.util.Log;
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

    public int getCompletedTaskCount() {
        int count = 0;
        for (Task task : taskList) {
            if (task.isCompleted()) {
                count++;
            }
        }
        return count;
    }

    private void removeTask(int position) {
        if (position < 0 || position >= taskList.size()) return;
        Task task = taskList.get(position);
        db.collection("tasks").document(task.getId()).delete()
                .addOnSuccessListener(aVoid -> {
                    taskList.remove(position);
                    notifyDataSetChanged();
                    progressUpdateCallback.run();
                    Log.d("Firestore", "Задача удалена: " + task.getDescription());
                })
                .addOnFailureListener(e -> {
                    Log.e("FirestoreError", "Ошибка удаления: " + e.getMessage());
                    Toast.makeText(context, "Ошибка удаления задачи", Toast.LENGTH_SHORT).show();
                });
    }

    private void updateTaskCompletion(Task task, boolean isCompleted) {
        task.setCompleted(isCompleted);
        db.collection("tasks").document(task.getId())
                .update("isCompleted", isCompleted)
                .addOnSuccessListener(aVoid -> {
                    notifyDataSetChanged();
                    progressUpdateCallback.run();
                })
                .addOnFailureListener(e -> Toast.makeText(context, "Ошибка обновления статуса", Toast.LENGTH_SHORT).show());
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.task_item, parent, false);
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
        private List<SubTask> subTasks;

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
                if (position != RecyclerView.NO_POSITION) removeTask(position);
            });

            editButton.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) showEditTaskDialog(taskList.get(position));
            });

            expandButton.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    Task task = taskList.get(position);
                    if (subTasks != null && !subTasks.isEmpty()) {
                        task.setExpanded(!task.isExpanded());
                        updateExpandedState(task);
                        db.collection("tasks").document(task.getId())
                                .update("isExpanded", task.isExpanded())
                                .addOnSuccessListener(aVoid -> Log.d("Firestore", "Состояние isExpanded обновлено: " + task.isExpanded()))
                                .addOnFailureListener(e -> Log.e("FirestoreError", "Ошибка обновления isExpanded: " + e.getMessage()));
                    } else {
                        Toast.makeText(context, "Нет подзадач для отображения", Toast.LENGTH_SHORT).show();
                    }
                }
            });

            addSubTaskButton.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) showAddSubTaskDialog(taskList.get(position));
            });
        }

        public void bind(Task task) {
            taskCheckBox.setOnCheckedChangeListener(null);
            taskCheckBox.setChecked(task.isCompleted());
            taskCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> updateTaskCompletion(task, isChecked));

            String dateTimeStr = dateFormat.format(task.getDateTime());
            taskTextView.setText(task.getDescription() + " (" + dateTimeStr + ")");
            taskTextView.setPaintFlags(task.isCompleted() ? taskTextView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG : taskTextView.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));

            subTasks = new ArrayList<>();
            subTaskAdapter = new SubTaskAdapter(subTasks, progressUpdateCallback, context, task.getId());
            subTaskRecyclerView.setLayoutManager(new LinearLayoutManager(context));
            subTaskRecyclerView.setAdapter(subTaskAdapter);

            // Загрузка подзадач
            db.collection("tasks").document(task.getId()).collection("subtasks")
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        subTasks.clear();
                        for (QueryDocumentSnapshot doc : querySnapshot) {
                            SubTask st = doc.toObject(SubTask.class);
                            st.setSubTaskId(doc.getId());
                            subTasks.add(st);
                        }
                        subTaskAdapter.setSubTasks(subTasks);
                        Log.d("Firestore", "Загружено подзадач: " + subTasks.size() + " для задачи: " + task.getDescription());
                        updateButtonVisibility(task);
                        updateExpandedState(task);
                    })
                    .addOnFailureListener(e -> {
                        Log.e("FirestoreError", "Ошибка загрузки подзадач: " + e.getMessage());
                        Toast.makeText(context, "Ошибка загрузки подзадач", Toast.LENGTH_SHORT).show();
                    });
        }

        private void updateButtonVisibility(Task task) {
            boolean hasSubtasks = subTasks != null && !subTasks.isEmpty();
            expandButton.setVisibility(hasSubtasks ? View.VISIBLE : View.GONE);
            editButton.setVisibility(View.VISIBLE);
            deleteButton.setVisibility(View.VISIBLE);
            addSubTaskButton.setVisibility(View.VISIBLE);
            Log.d("TaskAdapter", "Задача: " + task.getDescription() + ", hasSubtasks: " + hasSubtasks + ", expandButton видима: " + (expandButton.getVisibility() == View.VISIBLE));
        }

        private void updateExpandedState(Task task) {
            boolean hasSubtasks = subTasks != null && !subTasks.isEmpty();
            if (hasSubtasks && task.isExpanded()) {
                subTaskRecyclerView.setVisibility(View.VISIBLE);
                expandButton.setImageResource(android.R.drawable.arrow_up_float);
            } else {
                subTaskRecyclerView.setVisibility(View.GONE);
                expandButton.setImageResource(android.R.drawable.arrow_down_float);
            }
            Log.d("TaskAdapter", "Задача: " + task.getDescription() + ", isExpanded: " + task.isExpanded() + ", subTaskRecyclerView видима: " + (subTaskRecyclerView.getVisibility() == View.VISIBLE));
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
            taskDescription.setText(task.getDescription());
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(task.getDateTime());
            pickDateButton.setText(dateFormat.format(task.getDateTime()).split(" ")[0]);
            pickTimeButton.setText(dateFormat.format(task.getDateTime()).split(" ")[1]);

            pickDateButton.setOnClickListener(v -> {
                new DatePickerDialog(context, (view, year, month, day) -> {
                    calendar.set(year, month, day);
                    pickDateButton.setText(day + "/" + (month + 1) + "/" + year);
                }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
            });

            pickTimeButton.setOnClickListener(v -> {
                new TimePickerDialog(context, (view, hour, minute) -> {
                    calendar.set(Calendar.HOUR_OF_DAY, hour);
                    calendar.set(Calendar.MINUTE, minute);
                    pickTimeButton.setText(hour + ":" + minute);
                }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show();
            });

            AlertDialog dialog = builder.create();
            addTaskButton.setOnClickListener(v -> {
                String newDesc = taskDescription.getText().toString().trim();
                if (newDesc.isEmpty()) return;
                Date newDate = calendar.getTime();
                db.collection("tasks").document(task.getId())
                        .update("description", newDesc, "dateTime", newDate)
                        .addOnSuccessListener(aVoid -> {
                            task.setDescription(newDesc);
                            task.setDateTime(newDate);
                            notifyDataSetChanged();
                            dialog.dismiss();
                        })
                        .addOnFailureListener(e -> Toast.makeText(context, "Ошибка редактирования", Toast.LENGTH_SHORT).show());
            });
            dialog.show();
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
                String description = taskDescription.getText().toString().trim();
                if (description.isEmpty()) return;
                Map<String, Object> subTask = new HashMap<>();
                subTask.put("description", description);
                subTask.put("isCompleted", false);

                db.collection("tasks").document(task.getId()).collection("subtasks")
                        .add(subTask)
                        .addOnSuccessListener(doc -> {
                            task.setExpanded(true);
                            dialog.dismiss();
                            notifyItemChanged(getAdapterPosition());
                        })
                        .addOnFailureListener(e -> Toast.makeText(context, "Ошибка добавления подзадачи", Toast.LENGTH_SHORT).show());
            });
            dialog.show();
        }
    }
}