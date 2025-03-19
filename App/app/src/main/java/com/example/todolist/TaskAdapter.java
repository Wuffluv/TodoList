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

// Адаптер для отображения списка задач в RecyclerView
public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    // Список задач
    private List<Task> taskList;
    // Callback для обновления прогресс-бара
    private final Runnable progressUpdateCallback;
    // Формат даты и времени для отображения
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
    // Контекст активности
    private final Context context;
    // Экземпляр Firestore для работы с базой данных
    private final FirebaseFirestore db;

    // Конструктор адаптера
    public TaskAdapter(List<Task> tasks, Runnable progressUpdateCallback, Context context) {
        // Инициализация списка задач с проверкой на null
        this.taskList = tasks != null ? tasks : new ArrayList<>();
        this.progressUpdateCallback = progressUpdateCallback; // Callback для обновления прогресса
        this.context = context; // Контекст для Toast и диалогов
        this.db = FirebaseFirestore.getInstance(); // Инициализация Firestore
        // Сортировка задач по дате и времени
        Collections.sort(taskList, Comparator.comparing(Task::getDateTime));
    }

    // Метод для обновления списка задач
    public void setTasks(List<Task> tasks) {
        this.taskList = tasks != null ? tasks : new ArrayList<>(); // Установка нового списка с проверкой на null
        Collections.sort(taskList, Comparator.comparing(Task::getDateTime)); // Сортировка задач
        notifyDataSetChanged(); // Уведомление об изменении данных
        progressUpdateCallback.run(); // Обновление прогресса
    }

    // Метод для подсчёта выполненных задач
    public int getCompletedTaskCount() {
        int count = 0;
        for (Task task : taskList) {
            if (task.isCompleted()) {
                count++; // Увеличение счётчика для выполненных задач
            }
        }
        return count;
    }

    // Метод для удаления задачи
    private void removeTask(int position) {
        if (position < 0 || position >= taskList.size()) return; // Проверка валидности позиции
        Task task = taskList.get(position); // Получение задачи для удаления
        // Удаление задачи из Firestore
        db.collection("tasks").document(task.getId()).delete()
                .addOnSuccessListener(aVoid -> {
                    taskList.remove(position); // Удаление из локального списка
                    notifyDataSetChanged(); // Уведомление об изменении данных
                    progressUpdateCallback.run(); // Обновление прогресса
                    Log.d("Firestore", "Задача удалена: " + task.getDescription());
                })
                .addOnFailureListener(e -> {
                    Log.e("FirestoreError", "Ошибка удаления: " + e.getMessage());
                    Toast.makeText(context, "Ошибка удаления задачи", Toast.LENGTH_SHORT).show();
                });
    }

    // Метод для обновления статуса выполнения задачи
    private void updateTaskCompletion(Task task, boolean isCompleted) {
        task.setCompleted(isCompleted); // Локальное обновление статуса
        // Обновление статуса в Firestore
        db.collection("tasks").document(task.getId())
                .update("isCompleted", isCompleted)
                .addOnSuccessListener(aVoid -> {
                    notifyDataSetChanged(); // Уведомление об изменении данных
                    progressUpdateCallback.run(); // Обновление прогресса
                })
                .addOnFailureListener(e -> Toast.makeText(context, "Ошибка обновления статуса", Toast.LENGTH_SHORT).show());
    }

    // Создание нового ViewHolder для задачи
    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Надувание макета элемента задачи
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.task_item, parent, false);
        return new TaskViewHolder(v); // Возврат нового ViewHolder
    }

    // Привязка данных задачи к ViewHolder
    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = taskList.get(position); // Получение задачи по позиции
        holder.bind(task); // Привязка данных к ViewHolder
    }

    // Возвращает общее количество задач
    @Override
    public int getItemCount() {
        return taskList.size();
    }

    // Внутренний класс ViewHolder для элементов задачи
    class TaskViewHolder extends RecyclerView.ViewHolder {
        private final TextView taskTextView; // Текст задачи
        private final CheckBox taskCheckBox; // Чекбокс выполнения
        private final ImageButton deleteButton; // Кнопка удаления
        private final ImageButton editButton; // Кнопка редактирования
        private final ImageButton expandButton; // Кнопка раскрытия подзадач
        private final ImageButton addSubTaskButton; // Кнопка добавления подзадачи
        private final RecyclerView subTaskRecyclerView; // RecyclerView для подзадач
        private SubTaskAdapter subTaskAdapter; // Адаптер для подзадач
        private List<SubTask> subTasks; // Список подзадач

        // Конструктор ViewHolder
        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            // Привязка элементов интерфейса к переменным
            taskTextView = itemView.findViewById(R.id.taskTextView);
            taskCheckBox = itemView.findViewById(R.id.taskCheckBox);
            deleteButton = itemView.findViewById(R.id.deleteButton);
            editButton = itemView.findViewById(R.id.editButton);
            expandButton = itemView.findViewById(R.id.expandButton);
            addSubTaskButton = itemView.findViewById(R.id.addSubTaskButton);
            subTaskRecyclerView = itemView.findViewById(R.id.subTaskRecyclerView);

            // Слушатель для кнопки удаления
            deleteButton.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) removeTask(position);
            });

            // Слушатель для кнопки редактирования
            editButton.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) showEditTaskDialog(taskList.get(position));
            });

            // Слушатель для кнопки раскрытия/сворачивания подзадач
            expandButton.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    Task task = taskList.get(position);
                    if (subTasks != null && !subTasks.isEmpty()) {
                        task.setExpanded(!task.isExpanded()); // Переключение состояния раскрытия
                        updateExpandedState(task); // Обновление видимости подзадач
                        // Обновление состояния в Firestore
                        db.collection("tasks").document(task.getId())
                                .update("isExpanded", task.isExpanded())
                                .addOnSuccessListener(aVoid -> Log.d("Firestore", "Состояние isExpanded обновлено: " + task.isExpanded()))
                                .addOnFailureListener(e -> Log.e("FirestoreError", "Ошибка обновления isExpanded: " + e.getMessage()));
                    } else {
                        Toast.makeText(context, "Нет подзадач для отображения", Toast.LENGTH_SHORT).show();
                    }
                }
            });

            // Слушатель для кнопки добавления подзадачи
            addSubTaskButton.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) showAddSubTaskDialog(taskList.get(position));
            });
        }

        // Привязка данных задачи к элементам интерфейса
        public void bind(Task task) {
            taskCheckBox.setOnCheckedChangeListener(null); // Сброс слушателя
            taskCheckBox.setChecked(task.isCompleted()); // Установка статуса выполнения
            // Установка слушателя для обновления статуса
            taskCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> updateTaskCompletion(task, isChecked));

            // Форматирование текста задачи с датой
            String dateTimeStr = dateFormat.format(task.getDateTime());
            taskTextView.setText(task.getDescription() + " (" + dateTimeStr + ")");
            // Применение зачёркивания текста, если задача выполнена
            taskTextView.setPaintFlags(task.isCompleted() ? taskTextView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG : taskTextView.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));

            subTasks = new ArrayList<>(); // Инициализация списка подзадач
            subTaskAdapter = new SubTaskAdapter(subTasks, progressUpdateCallback, context, task.getId()); // Создание адаптера подзадач
            subTaskRecyclerView.setLayoutManager(new LinearLayoutManager(context)); // Установка менеджера макета
            subTaskRecyclerView.setAdapter(subTaskAdapter); // Установка адаптера

            // Загрузка подзадач из Firestore
            db.collection("tasks").document(task.getId()).collection("subtasks")
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        subTasks.clear(); // Очистка текущего списка подзадач
                        for (QueryDocumentSnapshot doc : querySnapshot) {
                            SubTask st = doc.toObject(SubTask.class); // Преобразование документа в объект SubTask
                            st.setSubTaskId(doc.getId()); // Установка ID подзадачи
                            subTasks.add(st); // Добавление в список
                        }
                        subTaskAdapter.setSubTasks(subTasks); // Обновление адаптера подзадач
                        Log.d("Firestore", "Загружено подзадач: " + subTasks.size() + " для задачи: " + task.getDescription());
                        updateButtonVisibility(task); // Обновление видимости кнопок
                        updateExpandedState(task); // Обновление состояния раскрытия
                    })
                    .addOnFailureListener(e -> {
                        Log.e("FirestoreError", "Ошибка загрузки подзадач: " + e.getMessage());
                        Toast.makeText(context, "Ошибка загрузки подзадач", Toast.LENGTH_SHORT).show();
                    });
        }

        // Обновление видимости кнопок в зависимости от наличия подзадач
        private void updateButtonVisibility(Task task) {
            boolean hasSubtasks = subTasks != null && !subTasks.isEmpty();
            expandButton.setVisibility(hasSubtasks ? View.VISIBLE : View.GONE); // Показ кнопки раскрытия, если есть подзадачи
            editButton.setVisibility(View.VISIBLE); // Кнопка редактирования всегда видима
            deleteButton.setVisibility(View.VISIBLE); // Кнопка удаления всегда видима
            addSubTaskButton.setVisibility(View.VISIBLE); // Кнопка добавления подзадачи всегда видима
            Log.d("TaskAdapter", "Задача: " + task.getDescription() + ", hasSubtasks: " + hasSubtasks + ", expandButton видима: " + (expandButton.getVisibility() == View.VISIBLE));
        }

        // Обновление состояния раскрытия подзадач
        private void updateExpandedState(Task task) {
            boolean hasSubtasks = subTasks != null && !subTasks.isEmpty();
            if (hasSubtasks && task.isExpanded()) {
                subTaskRecyclerView.setVisibility(View.VISIBLE); // Показ подзадач
                expandButton.setImageResource(android.R.drawable.arrow_up_float); // Иконка "свернуть"
            } else {
                subTaskRecyclerView.setVisibility(View.GONE); // Скрытие подзадач
                expandButton.setImageResource(android.R.drawable.arrow_down_float); // Иконка "раскрыть"
            }
            Log.d("TaskAdapter", "Задача: " + task.getDescription() + ", isExpanded: " + task.isExpanded() + ", subTaskRecyclerView видима: " + (subTaskRecyclerView.getVisibility() == View.VISIBLE));
        }

        // Показ диалога редактирования задачи
        private void showEditTaskDialog(Task task) {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_task, null); // Надувание макета диалога
            builder.setView(dialogView);

            // Привязка элементов диалога
            EditText taskDescription = dialogView.findViewById(R.id.taskDescription);
            Button pickDateButton = dialogView.findViewById(R.id.pickDateButton);
            Button pickTimeButton = dialogView.findViewById(R.id.pickTimeButton);
            Button addTaskButton = dialogView.findViewById(R.id.addTaskButton);

            addTaskButton.setText("Сохранить"); // Изменение текста кнопки
            taskDescription.setText(task.getDescription()); // Установка текущего описания
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(task.getDateTime()); // Установка текущей даты и времени
            pickDateButton.setText(dateFormat.format(task.getDateTime()).split(" ")[0]); // Установка даты
            pickTimeButton.setText(dateFormat.format(task.getDateTime()).split(" ")[1]); // Установка времени

            // Слушатель для выбора даты
            pickDateButton.setOnClickListener(v -> {
                new DatePickerDialog(context, (view, year, month, day) -> {
                    calendar.set(year, month, day); // Установка новой даты
                    pickDateButton.setText(day + "/" + (month + 1) + "/" + year); // Обновление текста кнопки
                }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
            });

            // Слушатель для выбора времени
            pickTimeButton.setOnClickListener(v -> {
                new TimePickerDialog(context, (view, hour, minute) -> {
                    calendar.set(Calendar.HOUR_OF_DAY, hour); // Установка часов
                    calendar.set(Calendar.MINUTE, minute); // Установка минут
                    pickTimeButton.setText(hour + ":" + minute); // Обновление текста кнопки
                }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show();
            });

            AlertDialog dialog = builder.create();
            // Слушатель для сохранения изменений
            addTaskButton.setOnClickListener(v -> {
                String newDesc = taskDescription.getText().toString().trim();
                if (newDesc.isEmpty()) return; // Проверка на пустое описание
                Date newDate = calendar.getTime(); // Получение новой даты
                // Обновление задачи в Firestore
                db.collection("tasks").document(task.getId())
                        .update("description", newDesc, "dateTime", newDate)
                        .addOnSuccessListener(aVoid -> {
                            task.setDescription(newDesc); // Локальное обновление описания
                            task.setDateTime(newDate); // Локальное обновление даты
                            notifyDataSetChanged(); // Уведомление об изменении данных
                            dialog.dismiss(); // Закрытие диалога
                        })
                        .addOnFailureListener(e -> Toast.makeText(context, "Ошибка редактирования", Toast.LENGTH_SHORT).show());
            });
            dialog.show(); // Показ диалога
        }

        // Показ диалога добавления подзадачи
        private void showAddSubTaskDialog(Task task) {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_task, null); // Надувание макета диалога
            builder.setView(dialogView);

            // Привязка элементов диалога
            EditText taskDescription = dialogView.findViewById(R.id.taskDescription);
            Button pickDateButton = dialogView.findViewById(R.id.pickDateButton);
            Button pickTimeButton = dialogView.findViewById(R.id.pickTimeButton);
            Button addTaskButton = dialogView.findViewById(R.id.addTaskButton);

            pickDateButton.setVisibility(View.GONE); // Скрытие кнопки выбора даты
            pickTimeButton.setVisibility(View.GONE); // Скрытие кнопки выбора времени
            addTaskButton.setText("Добавить подзадачу"); // Изменение текста кнопки

            AlertDialog dialog = builder.create();
            // Слушатель для добавления подзадачи
            addTaskButton.setOnClickListener(v -> {
                String description = taskDescription.getText().toString().trim();
                if (description.isEmpty()) return; // Проверка на пустое описание
                // Создание объекта подзадачи
                Map<String, Object> subTask = new HashMap<>();
                subTask.put("description", description); // Установка описания
                subTask.put("isCompleted", false); // Установка начального статуса

                // Добавление подзадачи в Firestore
                db.collection("tasks").document(task.getId()).collection("subtasks")
                        .add(subTask)
                        .addOnSuccessListener(doc -> {
                            task.setExpanded(true); // Автоматическое раскрытие задачи
                            dialog.dismiss(); // Закрытие диалога
                            notifyItemChanged(getAdapterPosition()); // Уведомление об изменении элемента
                        })
                        .addOnFailureListener(e -> Toast.makeText(context, "Ошибка добавления подзадачи", Toast.LENGTH_SHORT).show());
            });
            dialog.show(); // Показ диалога
        }
    }
}