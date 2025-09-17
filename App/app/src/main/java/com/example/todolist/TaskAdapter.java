package com.example.todolist;

// Импорты необходимых библиотек и классов для работы с Android, Firebase, UI и данными
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
import com.google.firebase.firestore.MetadataChanges;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TaskAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    // Константы для определения типов элементов в RecyclerView
    private static final int VIEW_TYPE_TASK = 1; // Тип для задач
    private static final int VIEW_TYPE_DATE_HEADER = 2; // Тип для заголовков дат

    // Объявление переменных экземпляра
    private List<Object> displayItems; // Список отображаемых элементов (задачи или заголовки дат)
    private final Runnable progressUpdateCallback; // Callback для обновления прогресса
    private final SimpleDateFormat taskDateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()); // Форматтер для отображения даты и времени задач
    private final Context context; // Контекст приложения
    private final FirebaseFirestore db; // Экземпляр Firestore для работы с базой данных
    private final Map<String, List<SubTask>> subTasksCache = new HashMap<>(); // Кэш для хранения подзадач по ID задачи
    private int totalTaskCount = 0; // Общее количество задач
    private int completedTaskCount = 0; // Количество выполненных задач
    private int currentProgressPercentage = 0; // Текущий процент прогресса

    // Конструктор адаптера
    public TaskAdapter(List<Task> initialTasks, Runnable progressUpdateCallback, Context context) {
        this.displayItems = new ArrayList<>(initialTasks); // Инициализация списка задач
        this.progressUpdateCallback = progressUpdateCallback; // Установка callback для прогресса
        this.context = context; // Сохранение контекста
        this.db = FirebaseFirestore.getInstance(); // Инициализация Firestore
    }

    // --- Методы установки данных ---
    // Установка задач для отображения в режиме календаря
    public void setCalendarTasks(List<Task> tasks) {
        this.displayItems = new ArrayList<>(tasks != null ? tasks : new ArrayList<>()); // Обновление списка задач
        calculateProgress(); // Пересчет прогресса
        notifyDataSetChanged(); // Уведомление адаптера об изменении данных
    }

    // Установка всех задач для отображения в режиме списка
    public void setAllTasksList(List<Object> items) {
        this.displayItems = new ArrayList<>(items != null ? items : new ArrayList<>()); // Обновление списка элементов
        calculateProgress(); // Пересчет прогресса
        notifyDataSetChanged(); // Уведомление адаптера об изменении данных
    }

    // --- Расчет и получение прогресса ---
    // Пересчет прогресса выполнения задач
    private void calculateProgress() {
        totalTaskCount = 0; // Сброс счетчика общего количества задач
        completedTaskCount = 0; // Сброс счетчика выполненных задач
        if (displayItems != null) { // Проверка наличия элементов
            for (Object item : displayItems) { // Перебор всех элементов
                if (item instanceof Task) { // Если элемент — задача
                    totalTaskCount++; // Увеличение общего счетчика
                    if (((Task) item).isCompleted()) { // Проверка, выполнена ли задача
                        completedTaskCount++; // Увеличение счетчика выполненных
                    }
                }
            }
        }
        // Расчет процента прогресса
        currentProgressPercentage = totalTaskCount > 0 ? (completedTaskCount * 100 / totalTaskCount) : 0;
        Log.d("AdapterProgress", "Расчет: Всего=" + totalTaskCount + ", Выполнено=" + completedTaskCount + ", Прогресс=" + currentProgressPercentage); // Логирование
        if (progressUpdateCallback != null) { // Проверка наличия callback
            progressUpdateCallback.run(); // Вызов callback для обновления UI
        }
    }

    // Геттеры для данных прогресса
    public int getTotalTaskCount() { return totalTaskCount; } // Возвращает общее количество задач
    public int getCompletedTaskCount() { return completedTaskCount; } // Возвращает количество выполненных задач
    public int getCurrentProgressPercentage() { return currentProgressPercentage; } // Возвращает процент прогресса

    // --- Переопределенные методы RecyclerView.Adapter ---
    // Определение типа элемента по позиции
    @Override
    public int getItemViewType(int position) {
        if (displayItems != null && position >= 0 && position < displayItems.size()) { // Проверка корректности позиции
            Object item = displayItems.get(position); // Получение элемента
            if (item instanceof Task) { // Если элемент — задача
                return VIEW_TYPE_TASK;
            } else if (item instanceof String) { // Если элемент — заголовок даты
                return VIEW_TYPE_DATE_HEADER;
            }
        }
        return RecyclerView.INVALID_TYPE; // Возврат недопустимого типа для ошибочных случаев
    }

    // Создание ViewHolder для элемента
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext()); // Получение inflater для создания View
        if (viewType == VIEW_TYPE_TASK) { // Если тип — задача
            View v = inflater.inflate(R.layout.task_item, parent, false); // Загрузка макета задачи
            return new TaskViewHolder(v); // Возврат ViewHolder для задачи
        } else if (viewType == VIEW_TYPE_DATE_HEADER) { // Если тип — заголовок даты
            View v = inflater.inflate(R.layout.date_header_item, parent, false); // Загрузка макета заголовка
            return new DateHeaderViewHolder(v); // Возврат ViewHolder для заголовка
        }
        Log.e("TaskAdapter", "onCreateViewHolder called with invalid viewType: " + viewType); // Логирование ошибки
        return new RecyclerView.ViewHolder(new View(parent.getContext())) {}; // Возврат пустого ViewHolder
    }

    // Привязка данных к ViewHolder
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (displayItems == null || position < 0 || position >= displayItems.size()) { // Проверка корректности данных
            Log.e("TaskAdapter", "onBindViewHolder called with invalid position: " + position); // Логирование ошибки
            return;
        }
        Object item = displayItems.get(position); // Получение элемента
        int viewType = getItemViewType(position); // Получение типа элемента

        if (viewType == VIEW_TYPE_TASK && item instanceof Task) { // Если элемент — задача
            ((TaskViewHolder) holder).bind((Task) item); // Привязка данных задачи
        } else if (viewType == VIEW_TYPE_DATE_HEADER && item instanceof String) { // Если элемент — заголовок
            ((DateHeaderViewHolder) holder).bind((String) item); // Привязка данных заголовка
        } else {
            Log.e("TaskAdapter", "onBindViewHolder: Mismatch between viewType (" + viewType + ") and item type or invalid position."); // Логирование ошибки
            holder.itemView.setVisibility(View.GONE); // Скрытие элемента при ошибке
        }
    }

    // Возврат количества элементов
    @Override
    public int getItemCount() {
        return displayItems != null ? displayItems.size() : 0; // Возврат размера списка или 0
    }

    // --- ViewHolder для заголовка даты ---
    static class DateHeaderViewHolder extends RecyclerView.ViewHolder {
        TextView dateHeaderTextView; // Текстовое поле для заголовка даты
        public DateHeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            dateHeaderTextView = itemView.findViewById(R.id.dateHeaderTextView); // Связывание с TextView
        }
        // Привязка текста заголовка
        void bind(String dateText) {
            if (dateHeaderTextView != null) { // Проверка инициализации
                dateHeaderTextView.setText(dateText); // Установка текста
                itemView.setVisibility(View.VISIBLE); // Показ элемента
            }
        }
    }

    // --- ViewHolder для задачи ---
    class TaskViewHolder extends RecyclerView.ViewHolder {
        // Элементы UI задачи
        private final TextView taskTextView; // Текст описания задачи
        private final CheckBox taskCheckBox; // Чекбокс для отметки выполнения
        private final ImageButton deleteButton; // Кнопка удаления задачи
        private final ImageButton editButton; // Кнопка редактирования задачи
        private final ImageButton expandButton; // Кнопка раскрытия подзадач
        private final ImageButton addSubTaskButton; // Кнопка добавления подзадачи
        private final RecyclerView subTaskRecyclerView; // Список подзадач
        private SubTaskAdapter subTaskAdapter; // Адаптер для подзадач
        private Task currentTask; // Текущая привязанная задача

        // Конструктор ViewHolder
        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            // Связывание UI элементов с соответствующими ID
            taskTextView = itemView.findViewById(R.id.taskTextView);
            taskCheckBox = itemView.findViewById(R.id.taskCheckBox);
            deleteButton = itemView.findViewById(R.id.deleteButton);
            editButton = itemView.findViewById(R.id.editButton);
            expandButton = itemView.findViewById(R.id.expandButton);
            addSubTaskButton = itemView.findViewById(R.id.addSubTaskButton);
            subTaskRecyclerView = itemView.findViewById(R.id.subTaskRecyclerView);

            // Проверка инициализации всех элементов UI
            if (taskTextView == null || taskCheckBox == null || deleteButton == null ||
                    editButton == null || expandButton == null || addSubTaskButton == null ||
                    subTaskRecyclerView == null) {
                Log.e("TaskViewHolder", "One or more views not found in task_item layout!"); // Логирование ошибки
                return;
            }

            // Инициализация адаптера для подзадач
            subTaskAdapter = new SubTaskAdapter(new ArrayList<>(), TaskAdapter.this::calculateProgress, context, null, () -> {
                if (currentTask != null) { // Проверка, что задача существует
                    List<SubTask> updatedSubTasks = subTasksCache.getOrDefault(currentTask.getId(), new ArrayList<>());
                    updateButtonVisibility(currentTask, updatedSubTasks); // Обновление видимости кнопок
                    updateExpandedState(currentTask, updatedSubTasks); // Обновление состояния раскрытия
                    TaskAdapter.this.calculateProgress(); // Пересчет прогресса
                }
            });

            // Настройка RecyclerView для подзадач
            subTaskRecyclerView.setLayoutManager(new LinearLayoutManager(context)); // Установка линейного менеджера
            subTaskRecyclerView.setAdapter(subTaskAdapter); // Установка адаптера

            // Обработчик удаления задачи
            deleteButton.setOnClickListener(v -> {
                int position = getBindingAdapterPosition(); // Получение текущей позиции
                if (position != RecyclerView.NO_POSITION && displayItems.get(position) instanceof Task) {
                    removeTaskWithConfirmation((Task) displayItems.get(position)); // Запуск удаления с подтверждением
                }
            });

            // Обработчик редактирования задачи
            editButton.setOnClickListener(v -> {
                int position = getBindingAdapterPosition(); // Получение текущей позиции
                if (position != RecyclerView.NO_POSITION && displayItems.get(position) instanceof Task) {
                    showEditTaskDialog((Task) displayItems.get(position)); // Отображение диалога редактирования
                }
            });

            // Обработчик добавления подзадачи
            addSubTaskButton.setOnClickListener(v -> {
                int position = getBindingAdapterPosition(); // Получение текущей позиции
                if (position != RecyclerView.NO_POSITION && displayItems.get(position) instanceof Task) {
                    showAddSubTaskDialog((Task) displayItems.get(position)); // Отображение диалога добавления подзадачи
                }
            });

            // Обработчик раскрытия/сворачивания подзадач
            expandButton.setOnClickListener(v -> {
                int position = getBindingAdapterPosition(); // Получение текущей позиции
                if (position != RecyclerView.NO_POSITION && displayItems.get(position) instanceof Task) {
                    toggleExpandState((Task) displayItems.get(position)); // Переключение состояния раскрытия
                }
            });
        }

        // Привязка данных задачи к ViewHolder
        public void bind(Task task) {
            if (task == null) { // Проверка, что задача не null
                Log.e("TaskViewHolder", "bind called with null task!"); // Логирование ошибки
                itemView.setVisibility(View.GONE); // Скрытие элемента
                return;
            }
            itemView.setVisibility(View.VISIBLE); // Показ элемента
            this.currentTask = task; // Сохранение текущей задачи
            Log.d("TaskBind", "Binding task: " + task.getDescription() + " ID: " + task.getId()); // Логирование привязки

            // Установка ID задачи в адаптер подзадач
            subTaskAdapter.setTaskId(task.getId());
            Log.d("TaskBind", "Set SubTaskAdapter TaskID to: " + task.getId());

            // Загрузка подзадач из кэша или Firestore
            loadSubTasksOrUseCache(task);

            // Сброс слушателя чекбокса перед установкой значения
            taskCheckBox.setOnCheckedChangeListener(null);
            taskCheckBox.setChecked(task.isCompleted()); // Установка состояния чекбокса

            // Формирование текста задачи с датой
            String dateTimeStr = task.getDateTime() != null ? taskDateFormat.format(task.getDateTime()) : "Нет даты";
            taskTextView.setText(task.getDescription() + " (" + dateTimeStr + ")"); // Установка текста
            // Применение зачеркивания для выполненных задач
            taskTextView.setPaintFlags(task.isCompleted()
                    ? taskTextView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG
                    : taskTextView.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));

            // Установка слушателя изменения состояния чекбокса
            taskCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                int currentPosition = getBindingAdapterPosition(); // Получение текущей позиции
                if (currentPosition != RecyclerView.NO_POSITION &&
                        displayItems.get(currentPosition) instanceof Task &&
                        ((Task)displayItems.get(currentPosition)).getId().equals(task.getId())) { // Проверка актуальности
                    List<SubTask> currentSubTasks = subTasksCache.getOrDefault(task.getId(), new ArrayList<>()); // Получение подзадач
                    if (isChecked) { // Если задача отмечена как выполненная
                        boolean allSubTasksCompleted = currentSubTasks.stream().allMatch(SubTask::isCompleted); // Проверка подзадач
                        if (!allSubTasksCompleted) { // Если есть невыполненные подзадачи
                            Toast.makeText(context, "Сначала выполните все подзадачи!", Toast.LENGTH_SHORT).show(); // Уведомление
                            buttonView.setChecked(false); // Отмена отметки
                            return;
                        }
                    }
                    updateTaskCompletion(task, isChecked, currentSubTasks); // Обновление статуса задачи
                } else {
                    Log.w("TaskViewHolder", "Checkbox listener triggered for outdated ViewHolder or non-Task item."); // Логирование ошибки
                }
            });
        }

        // Переключение состояния раскрытия подзадач
        private void toggleExpandState(Task task) {
            List<SubTask> currentSubTasks = subTasksCache.getOrDefault(task.getId(), new ArrayList<>()); // Получение подзадач
            if (!currentSubTasks.isEmpty()) { // Если есть подзадачи
                task.setExpanded(!task.isExpanded()); // Инверсия состояния раскрытия
                Log.d("Expand", "Task " + task.getDescription() + " expanded: " + task.isExpanded()); // Логирование
                updateExpandedState(task, currentSubTasks); // Обновление UI

                // Обновление состояния в Firestore
                db.collection("tasks").document(task.getId())
                        .update("isExpanded", task.isExpanded())
                        .addOnSuccessListener(aVoid -> Log.d("Firestore", "isExpanded обновлено: " + task.isExpanded())) // Логирование успеха
                        .addOnFailureListener(e -> Log.e("FirestoreError", "Ошибка обновления isExpanded: " + e.getMessage())); // Логирование ошибки
            } else {
                task.setExpanded(false); // Сброс состояния, если подзадач нет
                updateExpandedState(task, currentSubTasks); // Обновление UI
            }
        }

        // Загрузка подзадач из кэша или Firestore
        private void loadSubTasksOrUseCache(Task task) {
            if (task == null || task.getId() == null) { // Проверка валидности задачи
                updateUIForSubtasks(task, new ArrayList<>()); // Обновление UI с пустым списком
                return;
            }
            List<SubTask> cachedSubTasks = subTasksCache.get(task.getId()); // Получение подзадач из кэша
            if (cachedSubTasks != null) { // Если кэш существует
                Log.d("SubTaskLoad", "Using cache for task: " + task.getId() + ", count: " + cachedSubTasks.size()); // Логирование
                updateUIForSubtasks(task, cachedSubTasks); // Обновление UI
            } else { // Если кэша нет
                Log.d("SubTaskLoad", "No cache, loading from Firestore for task: " + task.getId()); // Логирование
                loadSubTasksFromFirestore(task); // Загрузка из Firestore
            }
        }

        // Загрузка подзадач из Firestore
        private void loadSubTasksFromFirestore(Task task) {
            if (task == null || task.getId() == null || task.getId().isEmpty()) { // Проверка валидности задачи
                Log.e("FirestoreError", "Cannot load subtasks: invalid task ID."); // Логирование ошибки
                updateUIForSubtasks(task, new ArrayList<>()); // Обновление UI с пустым списком
                return;
            }
            String taskId = task.getId(); // Получение ID задачи

            // Запрос подзадач из Firestore с поддержкой SnapshotListener
            db.collection("tasks").document(taskId).collection("subtasks")
                    .addSnapshotListener(MetadataChanges.INCLUDE, (querySnapshot, error) -> {
                        if (currentTask == null || !taskId.equals(currentTask.getId())) { // Проверка актуальности ViewHolder
                            Log.w("SubTaskLoad", "ViewHolder reused, ignoring Firestore result for task: " + taskId); // Логирование
                            return;
                        }
                        if (error != null) { // Проверка на ошибки
                            Log.e("FirestoreError", "Error loading subtasks for task " + taskId + ": " + error.getMessage()); // Логирование ошибки
                            updateUIForSubtasks(currentTask, new ArrayList<>()); // Обновление UI с пустым списком
                            return;
                        }

                        List<SubTask> loadedSubTasks = new ArrayList<>(); // Список для хранения подзадач
                        if (querySnapshot != null) { // Проверка наличия данных
                            boolean fromCache = querySnapshot.getMetadata().isFromCache(); // Проверка, из кэша ли данные
                            for (QueryDocumentSnapshot doc : querySnapshot) { // Обработка документов
                                try {
                                    SubTask st = doc.toObject(SubTask.class); // Преобразование в объект SubTask
                                    st.setSubTaskId(doc.getId()); // Установка ID подзадачи
                                    loadedSubTasks.add(st); // Добавление в список
                                } catch (Exception e) {
                                    Log.e("SubTaskLoad", "Error converting subtask document " + doc.getId() + " for task " + taskId + ": " + e.getMessage()); // Логирование ошибки
                                }
                            }
                            subTasksCache.put(taskId, new ArrayList<>(loadedSubTasks)); // Обновление кэша
                            Log.d("SubTaskLoad", "Firestore snapshot update for task: " + taskId + ", count: " + loadedSubTasks.size() + ", fromCache=" + fromCache); // Логирование
                            updateUIForSubtasks(currentTask, loadedSubTasks); // Обновление UI
                        } else {
                            subTasksCache.put(taskId, new ArrayList<>()); // Установка пустого кэша
                            updateUIForSubtasks(currentTask, new ArrayList<>()); // Обновление UI
                        }
                    });
        }

        // Обновление UI для подзадач
        private void updateUIForSubtasks(Task task, List<SubTask> subTasks) {
            if (task == null) return; // Проверка валидности задачи
            List<SubTask> subTasksCopy = new ArrayList<>(subTasks != null ? subTasks : new ArrayList<>()); // Копия списка подзадач
            subTaskAdapter.setSubTasks(subTasksCopy); // Установка подзадач в адаптер
            updateButtonVisibility(task, subTasksCopy); // Обновление видимости кнопок
            updateExpandedState(task, subTasksCopy); // Обновление состояния раскрытия
        }

        // Обновление видимости кнопок
        private void updateButtonVisibility(Task task, List<SubTask> subTasks) {
            if (task == null || expandButton == null || editButton == null || deleteButton == null || addSubTaskButton == null) return; // Проверка инициализации
            boolean hasSubtasks = subTasks != null && !subTasks.isEmpty(); // Проверка наличия подзадач
            expandButton.setVisibility(hasSubtasks ? View.VISIBLE : View.GONE); // Показ/скрытие кнопки раскрытия
            editButton.setVisibility(View.VISIBLE); // Показ кнопки редактирования
            deleteButton.setVisibility(View.VISIBLE); // Показ кнопки удаления
            addSubTaskButton.setVisibility(View.VISIBLE); // Показ кнопки добавления подзадачи
        }

        // Обновление состояния раскрытия подзадач
        private void updateExpandedState(Task task, List<SubTask> subTasks) {
            if (task == null || subTaskRecyclerView == null || expandButton == null) return; // Проверка инициализации
            boolean hasSubtasks = subTasks != null && !subTasks.isEmpty(); // Проверка наличия подзадач
            boolean shouldBeVisible = hasSubtasks && task.isExpanded(); // Проверка, нужно ли показывать подзадачи
            subTaskRecyclerView.setVisibility(shouldBeVisible ? View.VISIBLE : View.GONE); // Показ/скрытие списка подзадач
            // Установка иконки кнопки раскрытия
            expandButton.setImageResource(task.isExpanded()
                    ? android.R.drawable.arrow_up_float
                    : android.R.drawable.arrow_down_float);
        }

        // Показ диалога подтверждения удаления задачи
        private void removeTaskWithConfirmation(Task task) {
            if (task == null || task.getId() == null) return; // Проверка валидности задачи
            String taskId = task.getId(); // Получение ID задачи
            // Создание диалога подтверждения
            new AlertDialog.Builder(context)
                    .setTitle("Удалить задачу?") // Установка заголовка
                    .setMessage("Удалить \"" + task.getDescription() + "\" и все её подзадачи?") // Установка сообщения
                    .setPositiveButton("Удалить", (dialog, which) -> deleteTaskAndSubtasks(taskId)) // Обработчик удаления
                    .setNegativeButton("Отмена", null) // Обработчик отмены
                    .show(); // Отображение диалога
        }

        // Удаление задачи и её подзадач
        private void deleteTaskAndSubtasks(String taskId) {
            if (taskId == null || taskId.isEmpty()) return; // Проверка валидности ID
            Log.d("FirestoreDelete", "Starting deletion for task ID: " + taskId); // Логирование начала удаления
            WriteBatch batch = db.batch(); // Создание пакета операций
            batch.delete(db.collection("tasks").document(taskId)); // Добавление операции удаления задачи

            // Получение подзадач для удаления
            db.collection("tasks").document(taskId).collection("subtasks").get()
                    .addOnCompleteListener(subtaskQueryTask -> {
                        if (subtaskQueryTask.isSuccessful()) { // Проверка успешности запроса
                            for (QueryDocumentSnapshot doc : subtaskQueryTask.getResult()) { // Перебор подзадач
                                batch.delete(doc.getReference()); // Добавление операции удаления подзадачи
                                Log.d("FirestoreDelete", "Added subtask deletion to batch: " + doc.getId()); // Логирование
                            }
                        } else {
                            Log.e("FirestoreDelete", "Failed to get subtasks for deletion: ", subtaskQueryTask.getException()); // Логирование ошибки
                        }
                        // Выполнение пакета операций
                        batch.commit().addOnSuccessListener(aVoid -> {
                            Log.d("FirestoreDelete", "Batch delete successful for task ID: " + taskId); // Логирование успеха
                            handleSuccessfulDeletionUI(taskId); // Обновление UI
                        }).addOnFailureListener(e -> {
                            Log.e("FirestoreDelete", "Batch delete failed for task ID: " + taskId, e); // Логирование ошибки
                            Toast.makeText(context, "Ошибка удаления задачи", Toast.LENGTH_SHORT).show(); // Уведомление
                        });
                    });
        }

        // Обработка успешного удаления в UI
        private void handleSuccessfulDeletionUI(String taskId) {
            subTasksCache.remove(taskId); // Удаление подзадач из кэша
            Log.d("AdapterRemove", "Task deleted from Firestore, ID: " + taskId + ". SnapshotListener will update UI."); // Логирование
            calculateProgress(); // Пересчет прогресса
        }

        // Обновление статуса выполнения задачи
        private void updateTaskCompletion(Task task, boolean isCompleted, List<SubTask> subTasks) {
            if (task == null || task.getId() == null) return; // Проверка валидности задачи
            String taskId = task.getId(); // Получение ID задачи
            task.setCompleted(isCompleted); // Обновление статуса задачи

            WriteBatch batch = db.batch(); // Создание пакета операций
            batch.update(db.collection("tasks").document(taskId), "isCompleted", isCompleted); // Обновление статуса в Firestore

            List<SubTask> subTasksToUpdateLocally = new ArrayList<>(); // Список подзадач для локального обновления
            if (isCompleted && subTasks != null && !subTasks.isEmpty()) { // Если задача завершена и есть подзадачи
                for (SubTask subTask : subTasks) { // Перебор подзадач
                    if (!subTask.isCompleted()) { // Если подзадача не завершена
                        subTasksToUpdateLocally.add(subTask); // Добавление в список
                        if (subTask.getSubTaskId() != null && !subTask.getSubTaskId().isEmpty()) {
                            // Обновление статуса подзадачи в Firestore
                            batch.update(db.collection("tasks").document(taskId).collection("subtasks").document(subTask.getSubTaskId()), "isCompleted", true);
                        }
                    }
                }
            }

            // Выполнение пакета операций
            batch.commit().addOnSuccessListener(aVoid -> {
                Log.d("FirestoreUpdate", "Task status updated: " + task.getDescription() + " -> " + isCompleted); // Логирование успеха
                if (isCompleted && !subTasksToUpdateLocally.isEmpty()) { // Если есть подзадачи для обновления
                    for (SubTask subTask : subTasksToUpdateLocally) { // Локальное обновление подзадач
                        subTask.setCompleted(true);
                    }
                    subTasksCache.put(taskId, new ArrayList<>(subTasks)); // Обновление кэша
                    subTaskAdapter.setSubTasks(new ArrayList<>(subTasks)); // Обновление адаптера
                }

                int position = findTaskPosition(taskId); // Поиск позиции задачи
                if (position != RecyclerView.NO_POSITION) { // Если позиция найдена
                    notifyItemChanged(position); // Уведомление об изменении
                }
                calculateProgress(); // Пересчет прогресса
            }).addOnFailureListener(e -> {
                Log.e("FirestoreUpdate", "Failed to update task status: " + e.getMessage()); // Логирование ошибки
                Toast.makeText(context, "Ошибка обновления статуса", Toast.LENGTH_SHORT).show(); // Уведомление
                task.setCompleted(!isCompleted); // Откат статуса
                int position = findTaskPosition(taskId); // Поиск позиции
                if (position != RecyclerView.NO_POSITION) { // Если позиция найдена
                    notifyItemChanged(position); // Уведомление об изменении
                }
                calculateProgress(); // Пересчет прогресса
            });
        }

        // Показ диалога редактирования задачи
        private void showEditTaskDialog(Task task) {
            if (task == null || task.getId() == null) return; // Проверка валидности задачи
            AlertDialog.Builder builder = new AlertDialog.Builder(context); // Создание конструктора диалога
            View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_task, null); // Загрузка макета
            builder.setView(dialogView); // Установка макета

            // Инициализация элементов диалога
            EditText taskDescription = dialogView.findViewById(R.id.taskDescription); // Поле описания
            Button pickDateButton = dialogView.findViewById(R.id.pickDateButton); // Кнопка выбора даты
            Button pickTimeButton = dialogView.findViewById(R.id.pickTimeButton); // Кнопка выбора времени
            Button saveTaskButton = dialogView.findViewById(R.id.addTaskButton); // Кнопка сохранения

            saveTaskButton.setText("Сохранить"); // Изменение текста кнопки
            taskDescription.setText(task.getDescription()); // Установка текущего описания
            Calendar calendar = Calendar.getInstance(); // Инициализация календаря
            if (task.getDateTime() != null) calendar.setTime(task.getDateTime()); // Установка даты задачи

            // Форматтеры для даты и времени
            SimpleDateFormat editDateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            SimpleDateFormat editTimeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            pickDateButton.setText(editDateFormat.format(calendar.getTime())); // Установка даты
            pickTimeButton.setText(editTimeFormat.format(calendar.getTime())); // Установка времени

            // Обработчик выбора даты
            pickDateButton.setOnClickListener(v -> {
                DatePickerDialog datePickerDialog = new DatePickerDialog(context, (view, year, month, dayOfMonth) -> {
                    calendar.set(year, month, dayOfMonth); // Обновление даты
                    pickDateButton.setText(editDateFormat.format(calendar.getTime())); // Обновление текста
                }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
                datePickerDialog.show(); // Отображение диалога
            });

            // Обработчик выбора времени
            pickTimeButton.setOnClickListener(v -> {
                TimePickerDialog timePickerDialog = new TimePickerDialog(context, (view, hourOfDay, minute) -> {
                    calendar.set(Calendar.HOUR_OF_DAY, hourOfDay); // Обновление времени
                    calendar.set(Calendar.MINUTE, minute);
                    pickTimeButton.setText(editTimeFormat.format(calendar.getTime())); // Обновление текста
                }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true);
                timePickerDialog.show(); // Отображение диалога
            });

            AlertDialog dialog = builder.create(); // Создание диалога
            // Обработчик сохранения изменений
            saveTaskButton.setOnClickListener(v -> {
                String newDesc = taskDescription.getText().toString().trim(); // Получение нового описания
                if (newDesc.isEmpty()) { // Проверка, что описание не пустое
                    Toast.makeText(context, "Описание пусто", Toast.LENGTH_SHORT).show(); // Уведомление
                    return;
                }
                Date newDate = calendar.getTime(); // Получение новой даты

                // Обновление задачи в Firestore
                db.collection("tasks").document(task.getId())
                        .update("description", newDesc, "dateTime", newDate)
                        .addOnSuccessListener(aVoid -> {
                            Log.d("Firestore", "Task edited: " + task.getId()); // Логирование успеха
                            if (context instanceof MainMenuActivity) { // Проверка контекста
                                ((MainMenuActivity) context).updateView(); // Обновление UI
                            } else {
                                task.setDescription(newDesc); // Локальное обновление
                                task.setDateTime(newDate);
                                int pos = findTaskPosition(task.getId()); // Поиск позиции
                                if (pos != -1) notifyItemChanged(pos); // Уведомление об изменении
                                calculateProgress(); // Пересчет прогресса
                            }
                            dialog.dismiss(); // Закрытие диалога
                            Toast.makeText(context, "Задача сохранена", Toast.LENGTH_SHORT).show(); // Уведомление
                        })
                        .addOnFailureListener(e -> {
                            Log.e("FirestoreError", "Error editing task: " + e.getMessage()); // Логирование ошибки
                            Toast.makeText(context, "Ошибка редактирования", Toast.LENGTH_SHORT).show(); // Уведомление
                        });
            });
            dialog.show(); // Отображение диалога
        }

        // Показ диалога добавления подзадачи
        private void showAddSubTaskDialog(Task task) {
            if (task == null || task.getId() == null) return; // Проверка валидности задачи
            AlertDialog.Builder builder = new AlertDialog.Builder(context); // Создание конструктора диалога
            View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_subtask, null); // Загрузка макета
            builder.setView(dialogView); // Установка макета
            builder.setTitle("Добавить подзадачу"); // Установка заголовка

            // Инициализация элементов диалога
            EditText subTaskDescriptionEditText = dialogView.findViewById(R.id.subTaskDescriptionEditText); // Поле описания
            Button addSubTaskConfirmButton = dialogView.findViewById(R.id.addSubTaskConfirmButton); // Кнопка подтверждения
            Button cancelSubTaskButton = dialogView.findViewById(R.id.cancelSubTaskButton); // Кнопка отмены

            AlertDialog dialog = builder.create(); // Создание диалога
            // Обработчик подтверждения добавления подзадачи
            addSubTaskConfirmButton.setOnClickListener(v -> {
                String description = subTaskDescriptionEditText.getText().toString().trim(); // Получение описания
                if (description.isEmpty()) { // Проверка, что описание не пустое
                    Toast.makeText(context, "Введите описание", Toast.LENGTH_SHORT).show(); // Уведомление
                    return;
                }

                // Создание данных подзадачи
                Map<String, Object> subTaskData = new HashMap<>();
                subTaskData.put("description", description); // Описание подзадачи
                subTaskData.put("isCompleted", false); // Статус выполнения

                // Добавление подзадачи в Firestore
                db.collection("tasks").document(task.getId()).collection("subtasks")
                        .add(subTaskData)
                        .addOnSuccessListener(docRef -> {
                            Log.d("Firestore", "Subtask added: " + docRef.getId() + " to task " + task.getId()); // Логирование успеха
                            SubTask newSubTask = new SubTask(description, false); // Создание новой подзадачи
                            newSubTask.setSubTaskId(docRef.getId()); // Установка ID

                            // Обновление кэша
                            List<SubTask> currentSubTasks = subTasksCache.computeIfAbsent(task.getId(), k -> new ArrayList<>());
                            currentSubTasks.add(newSubTask);
                            subTasksCache.put(task.getId(), currentSubTasks);

                            if (!task.isExpanded()) { // Если задача не раскрыта
                                task.setExpanded(true); // Раскрытие задачи
                                db.collection("tasks").document(task.getId()).update("isExpanded", true); // Обновление в Firestore
                            }

                            calculateProgress(); // Пересчет прогресса
                            dialog.dismiss(); // Закрытие диалога
                            Toast.makeText(context, "Подзадача добавлена", Toast.LENGTH_SHORT).show(); // Уведомление
                        })
                        .addOnFailureListener(e -> {
                            Log.e("FirestoreError", "Error adding subtask: " + e.getMessage()); // Логирование ошибки
                            Toast.makeText(context, "Ошибка добавления подзадачи", Toast.LENGTH_SHORT).show(); // Уведомление
                        });
            });
            cancelSubTaskButton.setOnClickListener(v -> dialog.dismiss()); // Обработчик отмены
            dialog.show(); // Отображение диалога
        }

        // Поиск позиции задачи по ID
        private int findTaskPosition(String taskId) {
            if (taskId == null || displayItems == null) return -1; // Проверка валидности
            for (int i = 0; i < displayItems.size(); i++) { // Перебор элементов
                Object item = displayItems.get(i);
                if (item instanceof Task && taskId.equals(((Task) item).getId())) { // Проверка совпадения ID
                    return i; // Возврат позиции
                }
            }
            return -1; // Позиция не найдена
        }
    }
}