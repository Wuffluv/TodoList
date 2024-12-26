package com.example.todolist;

import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.todolist.Task;
import com.example.todolist.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    // Основной список с задачами
    private final List<Task> taskList = new ArrayList<>();
    // Формат даты
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
    // Колбэк, который будет вызываться при обновлении прогресса
    private final Runnable progressUpdateCallback;

    public TaskAdapter(Runnable progressUpdateCallback) {
        this.progressUpdateCallback = progressUpdateCallback;
    }

    // Добавляет задачу и сортирует список
    public void addTask(Task task) {
        taskList.add(task);
        Collections.sort(taskList, Comparator.comparing(Task::getDateTime));
        notifyDataSetChanged();
        progressUpdateCallback.run();
    }

    // Подсчитывает количество выполненных задач
    public int getCompletedTaskCount() {
        int count = 0;
        for (Task task : taskList) {
            if (task.isCompleted()) {
                count++;
            }
        }
        return count;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Привязываем layout для одного элемента списка
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.task_item, parent, false);
        // Передаём ссылку и на список, и на колбэк
        return new TaskViewHolder(view, progressUpdateCallback, taskList);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        // Извлекаем задачу по позиции и привязываем к ViewHolder
        Task task = taskList.get(position);
        holder.bind(task);
    }

    @Override
    public int getItemCount() {
        return taskList.size();
    }

    // Статический ViewHolder. В него передаём список taskList отдельным полем,

    static class TaskViewHolder extends RecyclerView.ViewHolder {

        private final TextView taskTextView;
        private final CheckBox taskCheckBox;

        // Храним локальную копию списка задач и колбэк, чтобы вызывать прогресс-обновление
        private final List<Task> localTaskList;
        private final Runnable progressUpdateCallback;

        public TaskViewHolder(@NonNull View itemView,
                              Runnable progressUpdateCallback,
                              List<Task> localTaskList) {
            super(itemView);

            this.localTaskList = localTaskList;
            this.progressUpdateCallback = progressUpdateCallback;

            taskTextView = itemView.findViewById(R.id.taskTextView);
            taskCheckBox = itemView.findViewById(R.id.taskCheckBox);

            // Обработчик изменения чекбокса
            taskCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                int position = getAdapterPosition(); // Вместо getBindingAdapterPosition()
                if (position != RecyclerView.NO_POSITION) {
                    // Берём задачу из локального списка по позиции
                    Task task = localTaskList.get(position);
                    task.setCompleted(isChecked);

                    // Перечёркиваем/снимаем зачёркивание текста
                    if (isChecked) {
                        taskTextView.setPaintFlags(taskTextView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                    } else {
                        taskTextView.setPaintFlags(taskTextView.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                    }

                    // Вызываем колбэк для обновления прогресса
                    progressUpdateCallback.run();
                }
            });
        }

        // Привязываем данные задачи к полям
        public void bind(Task task) {
            String dateTime = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                    .format(task.getDateTime());
            taskTextView.setText(task.getDescription() + " (" + dateTime + ")");

            // Стиль текста: зачёркнутый или нет
            if (task.isCompleted()) {
                taskTextView.setPaintFlags(taskTextView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            } else {
                taskTextView.setPaintFlags(taskTextView.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            }

            // Устанавливаем состояние чекбокса
            taskCheckBox.setChecked(task.isCompleted());
        }
    }
}
