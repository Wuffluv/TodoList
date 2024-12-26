package com.example.todolist;

import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;   // Для кнопки удаления
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

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

    /** Добавляет задачу и сортирует список по дате */
    public void addTask(Task task) {
        taskList.add(task);
        Collections.sort(taskList, Comparator.comparing(Task::getDateTime));
        notifyDataSetChanged();
        progressUpdateCallback.run();
    }

    /** Удаляет задачу по позиции */
    private void removeTask(int position) {
        if (position >= 0 && position < taskList.size()) {
            taskList.remove(position);
            notifyItemRemoved(position);
            // Обновляем прогресс (ProgressBar, процент и т.д.)
            progressUpdateCallback.run();
        }
    }

    /** Возвращает количество выполненных задач */
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
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.task_item, parent, false);
        // Передаём список и колбэк во ViewHolder, а также сам адаптер (this)
        return new TaskViewHolder(view, progressUpdateCallback, taskList, this);
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

    /** ViewHolder для хранения и привязки данных задачи */
    static class TaskViewHolder extends RecyclerView.ViewHolder {

        private final TextView taskTextView;
        private final CheckBox taskCheckBox;
        private final ImageButton deleteButton;           // Кнопка удаления
        private final List<Task> localTaskList;
        private final Runnable progressUpdateCallback;
        private final TaskAdapter adapter;                // Ссылка на адаптер

        public TaskViewHolder(@NonNull View itemView,
                              Runnable progressUpdateCallback,
                              List<Task> localTaskList,
                              TaskAdapter adapter) {
            super(itemView);

            this.localTaskList = localTaskList;
            this.progressUpdateCallback = progressUpdateCallback;
            this.adapter = adapter;

            taskTextView = itemView.findViewById(R.id.taskTextView);
            taskCheckBox = itemView.findViewById(R.id.taskCheckBox);
            deleteButton = itemView.findViewById(R.id.deleteButton);

            // Обработчик изменения чекбокса (выполнено/не выполнено)
            taskCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    Task task = localTaskList.get(position);
                    task.setCompleted(isChecked);

                    // Перечёркиваем/снимаем зачёркивание текста
                    if (isChecked) {
                        taskTextView.setPaintFlags(
                                taskTextView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG
                        );
                    } else {
                        taskTextView.setPaintFlags(
                                taskTextView.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG)
                        );
                    }

                    // Вызываем колбэк для обновления прогресса
                    progressUpdateCallback.run();
                }
            });

            // Кнопка удаления задачи (иконка крестика)
            deleteButton.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    // Вызываем метод removeTask(...) из адаптера
                    adapter.removeTask(position);
                }
            });
        }

        /** Привязываем данные задачи к элементам разметки */
        public void bind(Task task) {
            // Устанавливаем текст: описание и дата
            String dateTime = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                    .format(task.getDateTime());
            taskTextView.setText(task.getDescription() + " (" + dateTime + ")");

            // Если задача выполнена — зачёркиваем текст
            if (task.isCompleted()) {
                taskTextView.setPaintFlags(
                        taskTextView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG
                );
            } else {
                taskTextView.setPaintFlags(
                        taskTextView.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG)
                );
            }

            // Отмечаем состояние чекбокса
            taskCheckBox.setChecked(task.isCompleted());
        }
    }
}
