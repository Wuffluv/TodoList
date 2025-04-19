package com.example.todolist;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Адаптер для отображения списка задач в RecyclerView.
 * Поддерживает группировку задач по датам и работу с подзадачами.
 */
public class TaskAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final String TAG = "TaskAdapter"; // Тег для логов
    private List<Object> items = new ArrayList<>(); // Список элементов (задачи + заголовки)
    private List<Task> taskList = new ArrayList<>(); // Список только задач
    private final Runnable progressCallback; // Колбек для обновления прогресса
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()); // Формат даты+времени
    private static final SimpleDateFormat headerDateFormat = new SimpleDateFormat("d MMMM", Locale.getDefault()); // Формат даты для заголовков
    private final WeakReference<Context> contextRef; // Слабая ссылка на контекст
    private final FirebaseFirestore db; // Ссылка на Firestore
    private final Handler uiHandler = new Handler(Looper.getMainLooper()); // Хендлер для UI операций
    private final ExecutorService executor = Executors.newFixedThreadPool(2); // Пул потоков для фоновых операций
    private boolean isGroupedView = false; // Флаг группировки по датам

    // Типы элементов списка
    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;

    /**
     * Конструктор адаптера
     * @param tasks список задач
     * @param callback колбек для обновления прогресса
     * @param context контекст приложения
     */
    public TaskAdapter(List<Task> tasks, Runnable callback, Context context) {
        this.taskList = tasks != null ? new ArrayList<>(tasks) : new ArrayList<>();
        this.items = new ArrayList<>(taskList);
        this.progressCallback = callback;
        this.contextRef = new WeakReference<>(context);
        this.db = FirebaseFirestore.getInstance();
        sortTasks();
    }

    /**
     * Очистка ресурсов адаптера
     */
    public void cleanup() {
        executor.shutdownNow();
        items.clear();
        taskList.clear();
    }

    /**
     * Проверка, включена ли группировка по датам
     */
    public boolean isGroupedView() {
        return isGroupedView;
    }

    /**
     * Установка списка задач (без группировки)
     * @param tasks новый список задач
     */
    public void setTasks(List<Task> tasks) {
        List<Task> oldList = new ArrayList<>(taskList);
        taskList = tasks != null ? new ArrayList<>(tasks) : new ArrayList<>();
        items = new ArrayList<>(taskList);
        sortTasks();

        // Используем DiffUtil для эффективного обновления RecyclerView
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new TaskDiffCallback(oldList, taskList));
        diffResult.dispatchUpdatesTo(this);
        isGroupedView = false;
        notifyProgress();
    }

    /**
     * Установка списка задач с группировкой по датам
     * @param tasks новый список задач
     */
    public void setGroupedTasks(List<Task> tasks) {
        taskList = tasks != null ? new ArrayList<>(tasks) : new ArrayList<>();
        sortTasks();

        // Группировка задач по датам
        Map<String, List<Task>> groupedTasks = new HashMap<>();
        for (Task task : taskList) {
            String dateKey = task.getDateTime() != null ? headerDateFormat.format(task.getDateTime()) : "Без даты";
            groupedTasks.computeIfAbsent(dateKey, k -> new ArrayList<>()).add(task);
        }

        // Формирование списка элементов (заголовок + задачи)
        List<Object> newItems = new ArrayList<>();
        for (Map.Entry<String, List<Task>> entry : groupedTasks.entrySet()) {
            newItems.add("Задачи на " + entry.getKey());
            newItems.addAll(entry.getValue());
        }

        this.items = newItems;
        isGroupedView = true;
        notifyDataSetChanged();
        notifyProgress();
    }

    /**
     * Получение количества выполненных задач
     */
    public int getCompletedTaskCount() {
        return (int) taskList.stream().filter(Task::isCompleted).count();
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    /**
     * Удаление задачи
     * @param position позиция задачи в списке
     */
    private void removeTask(int position) {
        Log.d(TAG, "Removing task at position: " + position);
        if (position < 0 || position >= items.size()) return;

        Object item = items.get(position);
        if (!(item instanceof Task)) return;

        Task task = (Task) item;
        executor.execute(() -> {
            // Удаление задачи из Firestore
            db.collection("tasks").document(task.getId())
                    .delete()
                    .addOnSuccessListener(aVoid -> uiHandler.post(() -> {
                        Context context = contextRef.get();
                        if (context instanceof AppCompatActivity && !((AppCompatActivity) context).isFinishing() && !((AppCompatActivity) context).isDestroyed()) {
                            Log.d(TAG, "Task removed successfully, updating UI");
                            List<Task> oldList = new ArrayList<>(taskList);
                            taskList.remove(task);
                            if (isGroupedView) {
                                setGroupedTasks(taskList);
                            } else {
                                items = new ArrayList<>(taskList);
                                DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new TaskDiffCallback(oldList, taskList));
                                diffResult.dispatchUpdatesTo(TaskAdapter.this);
                            }
                            notifyProgress();
                            showToast("Задача удалена");
                        } else {
                            Log.w(TAG, "Activity is finishing or destroyed, skipping UI update");
                        }
                    }))
                    .addOnFailureListener(e -> uiHandler.post(() -> {
                        Context context = contextRef.get();
                        if (context instanceof AppCompatActivity && !((AppCompatActivity) context).isFinishing() && !((AppCompatActivity) context).isDestroyed()) {
                            showError("Ошибка удаления задачи: " + e.getMessage());
                        }
                    }));
        });
    }

    /**
     * Обновление статуса выполнения задачи
     * @param task задача для обновления
     * @param isCompleted новый статус выполнения
     */
    private void updateTaskCompletion(Task task, boolean isCompleted) {
        executor.execute(() -> db.collection("tasks").document(task.getId()).collection("subtasks")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    // Проверяем, что все подзадачи выполнены, если задача отмечается как выполненная
                    boolean allSubTasksCompleted = true;
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        SubTask subTask = doc.toObject(SubTask.class);
                        if (!subTask.isCompleted()) {
                            allSubTasksCompleted = false;
                            break;
                        }
                    }
                    if (isCompleted && !allSubTasksCompleted) {
                        uiHandler.post(() -> showToast("Нельзя отметить задачу как выполненную, пока не выполнены все подзадачи"));
                        return;
                    }
                    task.setCompleted(isCompleted);
                    // Обновление статуса в Firestore
                    db.collection("tasks").document(task.getId())
                            .update("isCompleted", isCompleted)
                            .addOnSuccessListener(aVoid -> uiHandler.post(() -> {
                                Context context = contextRef.get();
                                if (context instanceof AppCompatActivity && !((AppCompatActivity) context).isFinishing() && !((AppCompatActivity) context).isDestroyed()) {
                                    if (isGroupedView) setGroupedTasks(taskList);
                                    else notifyDataSetChanged();
                                    notifyProgress();
                                }
                            }))
                            .addOnFailureListener(e -> showError("Ошибка обновления статуса: " + e.getMessage()));
                })
                .addOnFailureListener(e -> showError("Ошибка проверки подзадач: " + e.getMessage())));
    }

    /**
     * Сортировка задач по дате
     */
    private void sortTasks() {
        Collections.sort(taskList, (t1, t2) -> {
            Date d1 = t1.getDateTime(), d2 = t2.getDateTime();
            if (d1 == null && d2 == null) return 0;
            if (d1 == null) return -1;
            if (d2 == null) return 1;
            return d1.compareTo(d2);
        });
    }

    /**
     * Уведомление об изменении прогресса
     */
    private void notifyProgress() {
        if (progressCallback != null) progressCallback.run();
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position) instanceof String ? TYPE_HEADER : TYPE_ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_HEADER) {
            View view = inflater.inflate(R.layout.date_header, parent, false);
            return new HeaderViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.task_item, parent, false);
            return new TaskViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Object item = items.get(position);
        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).bind((String) item);
        } else {
            ((TaskViewHolder) holder).bind((Task) item);
        }
    }

    /**
     * ViewHolder для заголовков дат
     */
    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        private final TextView headerTextView;

        HeaderViewHolder(View itemView) {
            super(itemView);
            headerTextView = itemView.findViewById(R.id.headerText);
        }

        void bind(String header) {
            headerTextView.setText(header);
        }
    }

    /**
     * ViewHolder для задач
     */
    class TaskViewHolder extends RecyclerView.ViewHolder {
        private final TextView taskTextView;
        private final CheckBox taskCheckBox;
        private final ImageButton deleteButton, editButton, expandButton, addSubTaskButton;
        private final RecyclerView subTaskRecyclerView;
        private SubTaskAdapter subTaskAdapter;
        private List<SubTask> subTasks = new ArrayList<>();

        TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            // Инициализация UI элементов
            taskTextView = itemView.findViewById(R.id.taskTextView);
            taskCheckBox = itemView.findViewById(R.id.taskCheckBox);
            deleteButton = itemView.findViewById(R.id.deleteButton);
            editButton = itemView.findViewById(R.id.editButton);
            expandButton = itemView.findViewById(R.id.expandButton);
            addSubTaskButton = itemView.findViewById(R.id.addSubTaskButton);
            subTaskRecyclerView = itemView.findViewById(R.id.subTaskRecyclerView);

            // Установка обработчиков кликов
            deleteButton.setOnClickListener(v -> removeTask(getAdapterPosition()));
            editButton.setOnClickListener(v -> showEditDialog((Task) items.get(getAdapterPosition())));
            expandButton.setOnClickListener(v -> toggleExpand((Task) items.get(getAdapterPosition())));
            addSubTaskButton.setOnClickListener(v -> showAddSubTaskDialog((Task) items.get(getAdapterPosition())));
        }

        /**
         * Привязка данных задачи к ViewHolder
         * @param task задача для отображения
         */
        void bind(Task task) {
            // Настройка CheckBox
            taskCheckBox.setOnCheckedChangeListener(null);
            taskCheckBox.setChecked(task.isCompleted());
            taskCheckBox.setOnCheckedChangeListener((v, isChecked) -> updateTaskCompletion(task, isChecked));

            // Форматирование текста задачи
            String text = task.getDateTime() != null
                    ? task.getDescription() + " (" + dateFormat.format(task.getDateTime()) + ")"
                    : task.getDescription() + " (Дата не указана)";
            taskTextView.setText(text);
            // Зачеркивание текста, если задача выполнена
            taskTextView.setPaintFlags(task.isCompleted() ? taskTextView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG
                    : taskTextView.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));

            // Настройка RecyclerView для подзадач
            subTaskAdapter = new SubTaskAdapter(subTasks, progressCallback, contextRef.get(), task.getId());
            subTaskRecyclerView.setLayoutManager(new LinearLayoutManager(contextRef.get()));
            subTaskRecyclerView.setAdapter(subTaskAdapter);

            // Загрузка подзадач, если они еще не загружены
            if (!task.isSubTasksLoaded()) loadSubTasks(task);
            else updateExpandedState(task);
        }

        /**
         * Загрузка подзадач из Firestore
         * @param task задача, для которой загружаются подзадачи
         */
        private void loadSubTasks(Task task) {
            executor.execute(() -> db.collection("tasks").document(task.getId()).collection("subtasks")
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        subTasks.clear();
                        querySnapshot.forEach(doc -> {
                            SubTask subTask = doc.toObject(SubTask.class);
                            subTask.setSubTaskId(doc.getId());
                            subTasks.add(subTask);
                        });
                        uiHandler.post(() -> {
                            subTaskAdapter.setSubTasks(subTasks);
                            task.setSubTasksLoaded(true);
                            updateExpandedState(task);
                        });
                    })
                    .addOnFailureListener(e -> showError("Ошибка загрузки подзадач: " + e.getMessage())));
        }

        /**
         * Обновление состояния раскрытия/скрытия подзадач
         * @param task задача для обновления
         */
        private void updateExpandedState(Task task) {
            boolean hasSubtasks = !subTasks.isEmpty();
            expandButton.setVisibility(hasSubtasks ? View.VISIBLE : View.GONE);
            subTaskRecyclerView.setVisibility(hasSubtasks && task.isExpanded() ? View.VISIBLE : View.GONE);
            expandButton.setImageResource(task.isExpanded() ? android.R.drawable.arrow_up_float : android.R.drawable.arrow_down_float);
        }

        /**
         * Переключение состояния раскрытия/скрытия подзадач
         * @param task задача для переключения
         */
        private void toggleExpand(Task task) {
            if (!subTasks.isEmpty()) {
                task.setExpanded(!task.isExpanded());
                updateExpandedState(task);
                // Сохранение состояния в Firestore
                executor.execute(() -> db.collection("tasks").document(task.getId())
                        .update("isExpanded", task.isExpanded()));
            } else {
                showToast("Нет подзадач для отображения");
            }
        }

        /**
         * Показать диалог редактирования задачи
         * @param task задача для редактирования
         */
        private void showEditDialog(Task task) {
            EditTaskDialogFragment.newInstance(task, getAdapterPosition(), TaskAdapter.this)
                    .show(((AppCompatActivity) contextRef.get()).getSupportFragmentManager(), "EditTaskDialog");
        }

        /**
         * Показать диалог добавления подзадачи
         * @param task задача, для которой добавляется подзадача
         */
        private void showAddSubTaskDialog(Task task) {
            AddSubTaskDialogFragment.newInstance(task, getAdapterPosition(), TaskAdapter.this)
                    .show(((AppCompatActivity) contextRef.get()).getSupportFragmentManager(), "AddSubTaskDialog");
        }
    }

    /**
     * Базовый класс для диалогов работы с задачами
     */
    abstract static class BaseTaskDialogFragment extends DialogFragment {
        protected TaskAdapter adapter;
        protected Task task;
        protected int position;

        @NonNull
        @Override
        public android.app.Dialog onCreateDialog(@NonNull Bundle savedInstanceState) {
            androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(requireActivity());
            View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_task, null);
            builder.setView(dialogView);

            // Инициализация UI элементов диалога
            EditText taskDescription = dialogView.findViewById(R.id.taskDescription);
            Button pickDateButton = dialogView.findViewById(R.id.pickDateButton);
            Button pickTimeButton = dialogView.findViewById(R.id.pickTimeButton);
            Button actionButton = dialogView.findViewById(R.id.addTaskButton);

            configureDialog(taskDescription, pickDateButton, pickTimeButton, actionButton);
            return builder.create();
        }

        /**
         * Абстрактный метод для настройки диалога
         */
        protected abstract void configureDialog(EditText taskDescription, Button pickDateButton, Button pickTimeButton, Button actionButton);
    }

    /**
     * Диалог для редактирования задачи
     */
    public static class EditTaskDialogFragment extends BaseTaskDialogFragment {
        public static EditTaskDialogFragment newInstance(Task task, int position, TaskAdapter adapter) {
            EditTaskDialogFragment fragment = new EditTaskDialogFragment();
            fragment.task = task;
            fragment.position = position;
            fragment.adapter = adapter;
            return fragment;
        }

        @Override
        protected void configureDialog(EditText taskDescription, Button pickDateButton, Button pickTimeButton, Button actionButton) {
            actionButton.setText("Сохранить");
            taskDescription.setText(task.getDescription());

            Calendar calendar = Calendar.getInstance();
            if (task.getDateTime() != null) calendar.setTime(task.getDateTime());
            adapter.updateDateTimeButtons(pickDateButton, pickTimeButton, calendar);

            // Установка обработчиков
            pickDateButton.setOnClickListener(v -> adapter.showDatePicker(pickDateButton, calendar));
            pickTimeButton.setOnClickListener(v -> adapter.showTimePicker(pickTimeButton, calendar));
            actionButton.setOnClickListener(v -> saveTask(taskDescription, calendar));
        }

        /**
         * Сохранение изменений задачи
         */
        private void saveTask(EditText taskDescription, Calendar calendar) {
            String newDesc = taskDescription.getText().toString().trim();
            if (newDesc.isEmpty()) {
                adapter.showToast("Введите описание задачи");
                return;
            }
            Date newDate = calendar.getTime();
            adapter.executor.execute(() -> adapter.db.collection("tasks").document(task.getId())
                    .update("description", newDesc, "dateTime", newDate)
                    .addOnSuccessListener(aVoid -> adapter.uiHandler.post(() -> {
                        Context context = adapter.contextRef.get();
                        if (context instanceof AppCompatActivity && !((AppCompatActivity) context).isFinishing() && !((AppCompatActivity) context).isDestroyed()) {
                            task.setDescription(newDesc);
                            task.setDateTime(newDate);
                            adapter.notifyItemChanged(position);
                            adapter.notifyProgress();
                            dismiss();
                        }
                    }))
                    .addOnFailureListener(e -> adapter.showError("Ошибка редактирования: " + e.getMessage())));
        }
    }

    /**
     * Диалог для добавления подзадачи
     */
    public static class AddSubTaskDialogFragment extends BaseTaskDialogFragment {
        public static AddSubTaskDialogFragment newInstance(Task task, int position, TaskAdapter adapter) {
            AddSubTaskDialogFragment fragment = new AddSubTaskDialogFragment();
            fragment.task = task;
            fragment.position = position;
            fragment.adapter = adapter;
            return fragment;
        }

        @Override
        protected void configureDialog(EditText taskDescription, Button pickDateButton, Button pickTimeButton, Button actionButton) {
            // Скрываем ненужные элементы для подзадач
            pickDateButton.setVisibility(View.GONE);
            pickTimeButton.setVisibility(View.GONE);
            actionButton.setText("Добавить подзадачу");

            actionButton.setOnClickListener(v -> {
                String description = taskDescription.getText().toString().trim();
                if (description.isEmpty()) {
                    adapter.showToast("Введите описание подзадачи");
                    return;
                }
                Map<String, Object> subTask = new HashMap<>();
                subTask.put("description", description);
                subTask.put("isCompleted", false);

                // Добавление подзадачи в Firestore
                adapter.executor.execute(() -> adapter.db.collection("tasks").document(task.getId()).collection("subtasks")
                        .add(subTask)
                        .addOnSuccessListener(doc -> adapter.uiHandler.post(() -> {
                            Context context = adapter.contextRef.get();
                            if (context instanceof AppCompatActivity && !((AppCompatActivity) context).isFinishing() && !((AppCompatActivity) context).isDestroyed()) {
                                task.setExpanded(true);
                                task.setSubTasksLoaded(false);
                                adapter.notifyItemChanged(position);
                                adapter.notifyProgress();
                                dismiss();
                            }
                        }))
                        .addOnFailureListener(e -> adapter.showError("Ошибка добавления подзадачи: " + e.getMessage())));
            });
        }
    }

    /**
     * Диалог для добавления новой задачи
     */
    public static class AddTaskDialogFragment extends BaseTaskDialogFragment {
        public static AddTaskDialogFragment newInstance(TaskAdapter adapter) {
            AddTaskDialogFragment fragment = new AddTaskDialogFragment();
            fragment.adapter = adapter;
            return fragment;
        }

        @Override
        protected void configureDialog(EditText taskDescription, Button pickDateButton, Button pickTimeButton, Button actionButton) {
            actionButton.setText("Добавить задачу");
            Calendar calendar = Calendar.getInstance();
            adapter.updateDateTimeButtons(pickDateButton, pickTimeButton, calendar);

            // Установка обработчиков
            pickDateButton.setOnClickListener(v -> adapter.showDatePicker(pickDateButton, calendar));
            pickTimeButton.setOnClickListener(v -> adapter.showTimePicker(pickTimeButton, calendar));
            actionButton.setOnClickListener(v -> addTask(taskDescription, calendar));
        }

        /**
         * Добавление новой задачи
         */
        private void addTask(EditText taskDescription, Calendar calendar) {
            String description = taskDescription.getText().toString().trim();
            if (description.isEmpty()) {
                adapter.showToast("Введите описание задачи");
                return;
            }
            Task newTask = new Task();
            newTask.setDescription(description);
            newTask.setDateTime(calendar.getTime());
            newTask.setCompleted(false);
            newTask.setExpanded(false);
            newTask.setSubTasksLoaded(false);

            adapter.executor.execute(() -> {
                Map<String, Object> taskData = new HashMap<>();
                taskData.put("description", description);
                taskData.put("dateTime", newTask.getDateTime());
                taskData.put("isCompleted", false);
                taskData.put("isExpanded", false);
                taskData.put("userId", FirebaseAuth.getInstance().getCurrentUser().getUid());

                // Добавление задачи в Firestore
                adapter.db.collection("tasks").add(taskData)
                        .addOnSuccessListener(docRef -> adapter.uiHandler.post(() -> {
                            Context context = adapter.contextRef.get();
                            if (context instanceof AppCompatActivity && !((AppCompatActivity) context).isFinishing() && !((AppCompatActivity) context).isDestroyed()) {
                                newTask.setId(docRef.getId());
                                adapter.taskList.add(newTask);
                                adapter.sortTasks();
                                adapter.notifyItemInserted(adapter.taskList.indexOf(newTask));
                                adapter.notifyProgress();
                                dismiss();
                            }
                        }))
                        .addOnFailureListener(e -> adapter.showError("Ошибка добавления задачи: " + e.getMessage()));
            });
        }
    }

    /**
     * Показать DatePicker для выбора даты
     */
    void showDatePicker(Button pickDateButton, Calendar calendar) {
        Context context = contextRef.get();
        if (context != null) {
            new DatePickerDialog(context,
                    (view, year, month, day) -> {
                        calendar.set(year, month, day);
                        pickDateButton.setText(day + "/" + (month + 1) + "/" + year);
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)).show();
        }
    }

    /**
     * Показать TimePicker для выбора времени
     */
    void showTimePicker(Button pickTimeButton, Calendar calendar) {
        Context context = contextRef.get();
        if (context != null) {
            new TimePickerDialog(context,
                    (view, hour, minute) -> {
                        calendar.set(Calendar.HOUR_OF_DAY, hour);
                        calendar.set(Calendar.MINUTE, minute);
                        pickTimeButton.setText(String.format(Locale.getDefault(), "%02d:%02d", hour, minute));
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    true).show();
        }
    }

    /**
     * Обновление кнопок даты и времени в диалоге
     */
    void updateDateTimeButtons(Button pickDateButton, Button pickTimeButton, Calendar calendar) {
        String dateTimeString = dateFormat.format(calendar.getTime());
        pickDateButton.setText(dateTimeString.split(" ")[0]);
        pickTimeButton.setText(dateTimeString.split(" ")[1]);
    }

    /**
     * Показать сообщение об ошибке
     */
    void showError(String message) {
        Log.e(TAG, message);
        showToast(message);
    }

    /**
     * Показать Toast сообщение
     */
    void showToast(String message) {
        Context context = contextRef.get();
        if (context != null) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Callback для DiffUtil для эффективного обновления RecyclerView
     */
    private static class TaskDiffCallback extends DiffUtil.Callback {
        private final List<Task> oldList, newList;

        TaskDiffCallback(List<Task> oldList, List<Task> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }

        @Override
        public int getOldListSize() {
            return oldList.size();
        }

        @Override
        public int getNewListSize() {
            return newList.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            return oldList.get(oldItemPosition).getId().equals(newList.get(newItemPosition).getId());
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            Task oldTask = oldList.get(oldItemPosition);
            Task newTask = newList.get(newItemPosition);
            return oldTask.isCompleted() == newTask.isCompleted() &&
                    oldTask.getDescription().equals(newTask.getDescription()) &&
                    (oldTask.getDateTime() == null && newTask.getDateTime() == null ||
                            (oldTask.getDateTime() != null && newTask.getDateTime() != null && oldTask.getDateTime().equals(newTask.getDateTime())));
        }
    }
}