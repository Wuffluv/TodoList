package com.example.todolist;

import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.todolist.R;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    private final List<Task> taskList;
    private final DatabaseHelper dbHelper;          // Для операций с БД
    private final Runnable progressUpdateCallback;

    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

    public TaskAdapter(List<Task> tasks,
                       DatabaseHelper dbHelper,
                       Runnable progressUpdateCallback) {
        this.taskList = tasks;
        this.dbHelper = dbHelper;
        this.progressUpdateCallback = progressUpdateCallback;

        // Сортируем сразу при создании
        Collections.sort(taskList, Comparator.comparing(Task::getDateTime));
    }

    /** Добавление новой задачи (связь с БД) */
    public void addTask(Task task) {
        // 1) Сохраняем в БД, получаем ID
        long newId = dbHelper.addTask(
                task.getUserId(),
                task.getDescription(),
                task.getDateTime().getTime(),
                task.isCompleted()
        );
        task.setId((int) newId);

        // 2) Добавляем в список
        taskList.add(task);
        Collections.sort(taskList, Comparator.comparing(Task::getDateTime));
        notifyDataSetChanged();

        // 3) Обновляем прогресс
        progressUpdateCallback.run();
    }

    /** Удаляем задачу: из БД и локального списка */
    private void removeTask(int position) {
        if (position >= 0 && position < taskList.size()) {
            Task t = taskList.get(position);
            dbHelper.deleteTask(t.getId()); // удаляем из БД
            taskList.remove(position);
            notifyItemRemoved(position);

            progressUpdateCallback.run();
        }
    }

    /** Отметить задачу выполненной (или нет) */
    private void updateTaskCompletion(Task task, boolean isCompleted) {
        task.setCompleted(isCompleted);
        dbHelper.updateTaskCompletion(task.getId(), isCompleted);
        notifyDataSetChanged();
        progressUpdateCallback.run();
    }

    public int getCompletedTaskCount() {
        int count = 0;
        for (Task t : taskList) {
            if (t.isCompleted()) {
                count++;
            }
        }
        return count;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.task_item, parent, false);
        return new TaskViewHolder(view, this);
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

    // ------------------- ViewHolder --------------------
    static class TaskViewHolder extends RecyclerView.ViewHolder {

        private final TextView taskTextView;
        private final CheckBox taskCheckBox;
        private final ImageButton deleteButton;
        private final TaskAdapter adapter;

        public TaskViewHolder(@NonNull View itemView, TaskAdapter adapter) {
            super(itemView);
            this.adapter = adapter;

            taskTextView = itemView.findViewById(R.id.taskTextView);
            taskCheckBox = itemView.findViewById(R.id.taskCheckBox);
            deleteButton = itemView.findViewById(R.id.deleteButton);

            // При изменении чекбокса
            taskCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    Task t = adapter.taskList.get(position);
                    adapter.updateTaskCompletion(t, isChecked);
                }
            });

            // Удаление
            deleteButton.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    adapter.removeTask(position);
                }
            });
        }

        public void bind(Task task) {
            // Устанавливаем текст: описание + дата
            String dateTime = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                    .format(task.getDateTime());
            taskTextView.setText(task.getDescription() + " (" + dateTime + ")");

            // Зачёркиваем, если выполнено
            if (task.isCompleted()) {
                taskTextView.setPaintFlags(
                        taskTextView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG
                );
            } else {
                taskTextView.setPaintFlags(
                        taskTextView.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG)
                );
            }

            // Чекбокс
            taskCheckBox.setChecked(task.isCompleted());
        }
    }
}
