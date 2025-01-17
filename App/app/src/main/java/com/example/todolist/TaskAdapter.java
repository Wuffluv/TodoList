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

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.todolist.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    private final List<Task> taskList;
    private final DatabaseHelper dbHelper;          // Для операций с БД
    private final Runnable progressUpdateCallback;
    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
    private final Context context; // Нужно для диалогов

    public TaskAdapter(List<Task> tasks,
                       DatabaseHelper dbHelper,
                       Runnable progressUpdateCallback) {
        this.taskList = tasks;
        this.dbHelper = dbHelper;
        this.progressUpdateCallback = progressUpdateCallback;
        // Для удобства сортируем по дате
        Collections.sort(taskList, Comparator.comparing(Task::getDateTime));

        // Предположим, что context берём из первого элемента (не всегда лучшее решение, но для примера)
        // Или можно передать context параметром в конструктор.
        this.context = null;
    }

    // Лучше передавать context в этот конструктор
    public TaskAdapter(List<Task> tasks,
                       DatabaseHelper dbHelper,
                       Runnable progressUpdateCallback,
                       Context context) {
        this.taskList = tasks;
        this.dbHelper = dbHelper;
        this.progressUpdateCallback = progressUpdateCallback;
        Collections.sort(taskList, Comparator.comparing(Task::getDateTime));
        this.context = context;
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

    // Добавим метод для редактирования задачи
    private void showEditTaskDialog(Task task) {
        if (context == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_task, null);
        builder.setView(dialogView);

        EditText taskDescription = dialogView.findViewById(R.id.taskDescription);
        Button pickDateButton = dialogView.findViewById(R.id.pickDateButton);
        Button pickTimeButton = dialogView.findViewById(R.id.pickTimeButton);
        Button addTaskButton = dialogView.findViewById(R.id.addTaskButton);

        // Переименуем кнопку, чтобы было понятно, что это "Сохранить изменения"
        addTaskButton.setText("Сохранить");

        final Calendar calendar = Calendar.getInstance();
        calendar.setTime(task.getDateTime());

        // Заполним поля старыми значениями
        taskDescription.setText(task.getDescription());
        pickDateButton.setText(dateFormat.format(task.getDateTime()).split(" ")[0]); // "dd/MM/yyyy"
        pickTimeButton.setText(dateFormat.format(task.getDateTime()).split(" ")[1]); // "HH:mm"

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
            if (newDesc.isEmpty()) {
                // Простейшая проверка
                return;
            }

            Date newDate = calendar.getTime();
            // Обновляем в БД
            dbHelper.updateTask(task.getId(), newDesc, newDate.getTime(), task.isCompleted());

            // Обновляем в памяти
            task.setDescription(newDesc);
            task.setDateTime(newDate);
            notifyDataSetChanged();

            progressUpdateCallback.run();
            dialog.dismiss();
        });

        dialog.show();
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
    public TaskAdapter.TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.task_item, parent, false);
        return new TaskViewHolder(view, this);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskAdapter.TaskViewHolder holder, int position) {
        Task task = taskList.get(position);
        holder.bind(task);
    }

    @Override
    public int getItemCount() {
        return taskList.size();
    }

    // ------------------- ViewHolder --------------------
    class TaskViewHolder extends RecyclerView.ViewHolder {

        private final TextView taskTextView;
        private final CheckBox taskCheckBox;
        private final ImageButton deleteButton;
        private final ImageButton editButton;
        private final ImageButton expandButton;
        private final ImageButton addSubTaskButton;
        private final RecyclerView subTaskRecyclerView;

        private SubTaskAdapter subTaskAdapter;
        private boolean isExpanded = false; // флаг, раскрыты ли подзадачи

        public TaskViewHolder(@NonNull View itemView, TaskAdapter adapter) {
            super(itemView);

            taskTextView = itemView.findViewById(R.id.taskTextView);
            taskCheckBox = itemView.findViewById(R.id.taskCheckBox);
            deleteButton = itemView.findViewById(R.id.deleteButton);
            editButton = itemView.findViewById(R.id.editButton);
            expandButton = itemView.findViewById(R.id.expandButton);
            addSubTaskButton = itemView.findViewById(R.id.addSubTaskButton);
            subTaskRecyclerView = itemView.findViewById(R.id.subTaskRecyclerView);

            // При изменении чекбокса
            taskCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    Task t = taskList.get(position);
                    adapter.updateTaskCompletion(t, isChecked);
                }
            });

            // Удаление задачи
            deleteButton.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    adapter.removeTask(position);
                }
            });

            // Редактирование задачи
            editButton.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    Task t = taskList.get(position);
                    adapter.showEditTaskDialog(t);
                }
            });

            // Разворачиваем/сворачиваем подзадачи
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

            // Добавляем новую подзадачу
            addSubTaskButton.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    Task t = taskList.get(position);
                    showAddSubTaskDialog(t);
                }
            });
        }

        public void bind(Task task) {
            // Устанавливаем текст: описание + дата
            String dateTime = dateFormat.format(task.getDateTime());
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

            // Инициализируем SubTaskAdapter
            List<SubTask> subTasks = dbHelper.getSubTasksForTask(task.getId());
            subTaskAdapter = new SubTaskAdapter(subTasks, dbHelper, progressUpdateCallback);
            subTaskRecyclerView.setLayoutManager(new LinearLayoutManager(itemView.getContext()));
            subTaskRecyclerView.setAdapter(subTaskAdapter);

            // Начально свернуты
            subTaskRecyclerView.setVisibility(View.GONE);
            addSubTaskButton.setVisibility(View.GONE);
            expandButton.setImageResource(android.R.drawable.arrow_down_float);
            isExpanded = false;
        }

        private void showAddSubTaskDialog(Task task) {
            Context ctx = itemView.getContext();
            AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
            View dialogView = LayoutInflater.from(ctx).inflate(R.layout.dialog_add_task, null);
            builder.setView(dialogView);

            EditText taskDescription = dialogView.findViewById(R.id.taskDescription);
            Button pickDateButton = dialogView.findViewById(R.id.pickDateButton);
            Button pickTimeButton = dialogView.findViewById(R.id.pickTimeButton);
            Button addTaskButton = dialogView.findViewById(R.id.addTaskButton);

            // Скрываем выбор даты/времени, т.к. для подзадач не используем дату
            pickDateButton.setVisibility(View.GONE);
            pickTimeButton.setVisibility(View.GONE);

            addTaskButton.setText("Добавить подзадачу");

            AlertDialog dialog = builder.create();

            addTaskButton.setOnClickListener(v -> {
                String description = taskDescription.getText().toString();
                if (description.isEmpty()) {
                    return;
                }
                // Сохраним подзадачу в БД
                long newId = dbHelper.addSubTask(task.getId(), description);
                SubTask newSubTask = new SubTask((int) newId, task.getId(), description, false);
                subTaskAdapter.addSubTask(newSubTask);

                dialog.dismiss();
            });

            dialog.show();
        }
    }
}
