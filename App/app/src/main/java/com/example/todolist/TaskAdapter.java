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

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.lang.ref.WeakReference;
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
import java.util.concurrent.Executors;

public class TaskAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final String TAG = "TaskAdapter";
    private List<Object> items = new ArrayList<>();
    private List<Task> taskList;
    private final Runnable progressUpdateCallback;
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
    private static final SimpleDateFormat headerDateFormat = new SimpleDateFormat("d MMMM", Locale.getDefault());
    private final WeakReference<Context> contextRef;
    private final FirebaseFirestore db;
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private boolean isGroupedView = false;

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;

    public TaskAdapter(List<Task> tasks, Runnable progressUpdateCallback, Context context) {
        this.taskList = tasks != null ? new ArrayList<>(tasks) : new ArrayList<>();
        this.items = new ArrayList<>(this.taskList);
        this.progressUpdateCallback = progressUpdateCallback;
        this.contextRef = new WeakReference<>(context);
        this.db = FirebaseFirestore.getInstance();
        Collections.sort(taskList, (task1, task2) -> {
            if (task1.getDateTime() == null && task2.getDateTime() == null) return 0;
            if (task1.getDateTime() == null) return -1;
            if (task2.getDateTime() == null) return 1;
            return task1.getDateTime().compareTo(task2.getDateTime());
        });
    }

    public boolean isGroupedView() {
        return isGroupedView;
    }

    public void setTasks(List<Task> tasks) {
        try {
            Log.d(TAG, "setTasks called with " + (tasks != null ? tasks.size() : 0) + " tasks");
            List<Task> oldList = new ArrayList<>(this.taskList);
            this.taskList = tasks != null ? new ArrayList<>(tasks) : new ArrayList<>();
            this.items = new ArrayList<>(this.taskList);
            Collections.sort(taskList, (task1, task2) -> {
                if (task1.getDateTime() == null && task2.getDateTime() == null) return 0;
                if (task1.getDateTime() == null) return -1;
                if (task2.getDateTime() == null) return 1;
                return task1.getDateTime().compareTo(task2.getDateTime());
            });
            DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new TaskDiffCallback(oldList, taskList));
            diffResult.dispatchUpdatesTo(this);
            isGroupedView = false;
            if (progressUpdateCallback != null) {
                progressUpdateCallback.run();
            }
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при установке задач: " + e.getMessage(), e);
            showToast("Ошибка загрузки задач");
        }
    }

    public void setGroupedTasks(List<Task> tasks) {
        try {
            Log.d(TAG, "setGroupedTasks called with " + (tasks != null ? tasks.size() : 0) + " tasks");
            this.taskList = tasks != null ? new ArrayList<>(tasks) : new ArrayList<>();
            Collections.sort(taskList, (task1, task2) -> {
                if (task1.getDateTime() == null && task2.getDateTime() == null) return 0;
                if (task1.getDateTime() == null) return -1;
                if (task2.getDateTime() == null) return 1;
                return task1.getDateTime().compareTo(task2.getDateTime());
            });

            Map<String, List<Task>> groupedTasks = new HashMap<>();
            for (Task task : taskList) {
                if (task.getDateTime() == null) {
                    Log.e(TAG, "Task dateTime is null for task: " + task.getDescription());
                    continue;
                }
                String dateKey = headerDateFormat.format(task.getDateTime());
                groupedTasks.computeIfAbsent(dateKey, k -> new ArrayList<>()).add(task);
            }

            List<Object> newItems = new ArrayList<>();
            for (Map.Entry<String, List<Task>> entry : groupedTasks.entrySet()) {
                String date = entry.getKey();
                newItems.add("Задачи на " + date);
                newItems.addAll(entry.getValue());
            }

            this.items = newItems;
            isGroupedView = true;
            notifyDataSetChanged();
            if (progressUpdateCallback != null) {
                progressUpdateCallback.run();
            }
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при установке сгруппированных задач: " + e.getMessage(), e);
            showToast("Ошибка загрузки задач");
        }
    }

    public int getCompletedTaskCount() {
        int count = 0;
        for (Task task : taskList) {
            if (task != null && task.isCompleted()) {
                count++;
            }
        }
        return count;
    }

    private void removeTask(int position) {
        try {
            Object item = items.get(position);
            if (!(item instanceof Task)) return;

            Task task = (Task) item;
            int taskIndex = taskList.indexOf(task);
            if (taskIndex < 0 || taskIndex >= taskList.size()) {
                Log.e(TAG, "Invalid task index: " + taskIndex);
                return;
            }
            if (task == null || task.getId() == null) {
                Log.e(TAG, "Задача или её ID равны null на позиции: " + position);
                return;
            }

            Executors.newSingleThreadExecutor().execute(() -> {
                try {
                    db.collection("tasks").document(task.getId()).delete()
                            .addOnSuccessListener(aVoid -> uiHandler.post(() -> {
                                try {
                                    taskList.remove(taskIndex);
                                    if (isGroupedView) {
                                        setGroupedTasks(taskList);
                                    } else {
                                        setTasks(taskList);
                                    }
                                    if (progressUpdateCallback != null) {
                                        progressUpdateCallback.run();
                                    }
                                } catch (Exception e) {
                                    Log.e(TAG, "Ошибка при удалении задачи из списка: " + e.getMessage(), e);
                                    showToast("Ошибка удаления задачи");
                                }
                            }))
                            .addOnFailureListener(e -> uiHandler.post(() ->
                                    showToast("Ошибка удаления задачи: " + e.getMessage())));
                } catch (Exception e) {
                    Log.e(TAG, "Ошибка удаления задачи: " + e.getMessage(), e);
                    uiHandler.post(() -> showToast("Ошибка удаления задачи"));
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при удалении задачи на позиции " + position + ": " + e.getMessage(), e);
            showToast("Ошибка удаления задачи");
        }
    }

    private void updateTaskCompletion(Task task, boolean isCompleted) {
        try {
            if (task == null || task.getId() == null) {
                Log.e(TAG, "Задача или её ID равны null при обновлении статуса");
                return;
            }
            task.setCompleted(isCompleted);
            Executors.newSingleThreadExecutor().execute(() -> {
                try {
                    db.collection("tasks").document(task.getId())
                            .update("isCompleted", isCompleted)
                            .addOnSuccessListener(aVoid -> uiHandler.post(() -> {
                                try {
                                    if (isGroupedView) {
                                        setGroupedTasks(taskList);
                                    } else {
                                        notifyDataSetChanged();
                                    }
                                    if (progressUpdateCallback != null) {
                                        progressUpdateCallback.run();
                                    }
                                } catch (Exception e) {
                                    Log.e(TAG, "Ошибка при обновлении UI после изменения статуса: " + e.getMessage(), e);
                                    showToast("Ошибка обновления статуса");
                                }
                            }))
                            .addOnFailureListener(e -> uiHandler.post(() ->
                                    showToast("Ошибка обновления статуса: " + e.getMessage())));
                } catch (Exception e) {
                    Log.e(TAG, "Ошибка обновления статуса задачи: " + e.getMessage(), e);
                    uiHandler.post(() -> showToast("Ошибка обновления статуса"));
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при обновлении статуса задачи: " + e.getMessage(), e);
            showToast("Ошибка обновления статуса");
        }
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position) instanceof String ? TYPE_HEADER : TYPE_ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        try {
            if (viewType == TYPE_HEADER) {
                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.date_header, parent, false);
                return new HeaderViewHolder(v);
            } else {
                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.task_item, parent, false);
                return new TaskViewHolder(v);
            }
        } catch (Exception e) {
            Log.e(TAG, "Ошибка создания ViewHolder: " + e.getMessage(), e);
            throw new RuntimeException("Не удалось создать ViewHolder", e);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        try {
            Object item = items.get(position);
            if (holder instanceof HeaderViewHolder) {
                ((HeaderViewHolder) holder).bind((String) item);
            } else {
                ((TaskViewHolder) holder).bind((Task) item);
            }
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при привязке данных на позиции " + position + ": " + e.getMessage(), e);
            showToast("Ошибка отображения задачи");
        }
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

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
            try {
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
                    if (position != RecyclerView.NO_POSITION) {
                        try {
                            Object item = items.get(position);
                            if (item instanceof Task) {
                                Task task = (Task) item;
                                EditTaskDialogFragment dialog = EditTaskDialogFragment.newInstance(task, position, TaskAdapter.this);
                                dialog.show(((AppCompatActivity) contextRef.get()).getSupportFragmentManager(), "EditTaskDialog");
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Ошибка открытия диалога редактирования: " + e.getMessage(), e);
                            showToast("Ошибка открытия диалога редактирования");
                        }
                    }
                });

                expandButton.setOnClickListener(v -> {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        Object item = items.get(position);
                        if (item instanceof Task) {
                            Task task = (Task) item;
                            if (subTasks != null && !subTasks.isEmpty()) {
                                task.setExpanded(!task.isExpanded());
                                updateExpandedState(task);
                                Executors.newSingleThreadExecutor().execute(() -> {
                                    try {
                                        db.collection("tasks").document(task.getId())
                                                .update("isExpanded", task.isExpanded());
                                    } catch (Exception e) {
                                        Log.e(TAG, "Ошибка обновления isExpanded: " + e.getMessage(), e);
                                    }
                                });
                            } else {
                                showToast("Нет подзадач для отображения");
                            }
                        }
                    }
                });

                addSubTaskButton.setOnClickListener(v -> {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        try {
                            Object item = items.get(position);
                            if (item instanceof Task) {
                                Task task = (Task) item;
                                AddSubTaskDialogFragment dialog = AddSubTaskDialogFragment.newInstance(task, position, TaskAdapter.this);
                                dialog.show(((AppCompatActivity) contextRef.get()).getSupportFragmentManager(), "AddSubTaskDialog");
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Ошибка открытия диалога добавления подзадачи: " + e.getMessage(), e);
                            showToast("Ошибка открытия диалога добавления подзадачи");
                        }
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Ошибка инициализации ViewHolder: " + e.getMessage(), e);
                throw new RuntimeException("Не удалось инициализировать ViewHolder", e);
            }
        }

        public void bind(Task task) {
            try {
                taskCheckBox.setOnCheckedChangeListener(null);
                taskCheckBox.setChecked(task.isCompleted());
                taskCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> updateTaskCompletion(task, isChecked));

                if (task.getDateTime() == null) {
                    Log.e(TAG, "Task dateTime is null for task: " + task.getDescription());
                    taskTextView.setText(task.getDescription() + " (Дата не указана)");
                } else {
                    String dateTimeStr = dateFormat.format(task.getDateTime());
                    taskTextView.setText(task.getDescription() + " (" + dateTimeStr + ")");
                }
                taskTextView.setPaintFlags(task.isCompleted() ? taskTextView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG : taskTextView.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));

                subTasks = new ArrayList<>();
                subTaskAdapter = new SubTaskAdapter(subTasks, progressUpdateCallback, contextRef.get(), task.getId());
                subTaskRecyclerView.setLayoutManager(new LinearLayoutManager(contextRef.get()));
                subTaskRecyclerView.setAdapter(subTaskAdapter);

                if (!task.isSubTasksLoaded() && task.isExpanded()) {
                    loadSubTasks(task);
                } else {
                    updateButtonVisibility(task);
                    updateExpandedState(task);
                }
            } catch (Exception e) {
                Log.e(TAG, "Ошибка привязки данных задачи: " + e.getMessage(), e);
                showToast("Ошибка отображения задачи");
            }
        }

        private void loadSubTasks(Task task) {
            Executors.newSingleThreadExecutor().execute(() -> {
                try {
                    db.collection("tasks").document(task.getId()).collection("subtasks")
                            .get()
                            .addOnSuccessListener(querySnapshot -> {
                                List<SubTask> loadedSubTasks = new ArrayList<>();
                                for (QueryDocumentSnapshot doc : querySnapshot) {
                                    SubTask st = doc.toObject(SubTask.class);
                                    st.setSubTaskId(doc.getId());
                                    loadedSubTasks.add(st);
                                }
                                uiHandler.post(() -> {
                                    try {
                                        subTasks.clear();
                                        subTasks.addAll(loadedSubTasks);
                                        subTaskAdapter.setSubTasks(subTasks);
                                        task.setSubTasksLoaded(true);
                                        updateButtonVisibility(task);
                                        updateExpandedState(task);
                                    } catch (Exception e) {
                                        Log.e(TAG, "Ошибка обновления подзадач: " + e.getMessage(), e);
                                        showToast("Ошибка обновления подзадач");
                                    }
                                });
                            })
                            .addOnFailureListener(e -> uiHandler.post(() ->
                                    showToast("Ошибка загрузки подзадач: " + e.getMessage())));
                } catch (Exception e) {
                    Log.e(TAG, "Ошибка загрузки подзадач: " + e.getMessage(), e);
                    uiHandler.post(() -> showToast("Ошибка загрузки подзадач"));
                }
            });
        }

        private void updateButtonVisibility(Task task) {
            try {
                boolean hasSubtasks = subTasks != null && !subTasks.isEmpty();
                expandButton.setVisibility(hasSubtasks ? View.VISIBLE : View.GONE);
                editButton.setVisibility(View.VISIBLE);
                deleteButton.setVisibility(View.VISIBLE);
                addSubTaskButton.setVisibility(View.VISIBLE);
            } catch (Exception e) {
                Log.e(TAG, "Ошибка обновления видимости кнопок: " + e.getMessage(), e);
            }
        }

        private void updateExpandedState(Task task) {
            try {
                boolean hasSubtasks = subTasks != null && !subTasks.isEmpty();
                if (hasSubtasks && task.isExpanded()) {
                    subTaskRecyclerView.setVisibility(View.VISIBLE);
                    expandButton.setImageResource(android.R.drawable.arrow_up_float);
                } else {
                    subTaskRecyclerView.setVisibility(View.GONE);
                    expandButton.setImageResource(android.R.drawable.arrow_down_float);
                }
            } catch (Exception e) {
                Log.e(TAG, "Ошибка обновления состояния раскрытия: " + e.getMessage(), e);
            }
        }
    }

    public static class EditTaskDialogFragment extends DialogFragment {
        private Task task;
        private int position;
        private TaskAdapter adapter;

        public static EditTaskDialogFragment newInstance(Task task, int position, TaskAdapter adapter) {
            EditTaskDialogFragment fragment = new EditTaskDialogFragment();
            fragment.task = task;
            fragment.position = position;
            fragment.adapter = adapter;
            return fragment;
        }

        @NonNull
        @Override
        public android.app.Dialog onCreateDialog(@NonNull Bundle savedInstanceState) {
            try {
                androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(requireActivity());
                View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_task, null);
                builder.setView(dialogView);

                EditText taskDescription = dialogView.findViewById(R.id.taskDescription);
                Button pickDateButton = dialogView.findViewById(R.id.pickDateButton);
                Button pickTimeButton = dialogView.findViewById(R.id.pickTimeButton);
                Button addTaskButton = dialogView.findViewById(R.id.addTaskButton);

                addTaskButton.setText("Сохранить");
                taskDescription.setText(task.getDescription());
                Calendar calendar = Calendar.getInstance();
                if (task.getDateTime() != null) {
                    calendar.setTime(task.getDateTime());
                }
                String dateTimeString = dateFormat.format(calendar.getTime());
                pickDateButton.setText(dateTimeString.split(" ")[0]);
                pickTimeButton.setText(dateTimeString.split(" ")[1]);

                pickDateButton.setOnClickListener(v -> new DatePickerDialog(getContext(),
                        (view, year, month, day) -> {
                            calendar.set(year, month, day);
                            pickDateButton.setText(day + "/" + (month + 1) + "/" + year);
                        },
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH)).show());

                pickTimeButton.setOnClickListener(v -> new TimePickerDialog(getContext(),
                        (view, hour, minute) -> {
                            calendar.set(Calendar.HOUR_OF_DAY, hour);
                            calendar.set(Calendar.MINUTE, minute);
                            pickTimeButton.setText(String.format(Locale.getDefault(), "%02d:%02d", hour, minute));
                        },
                        calendar.get(Calendar.HOUR_OF_DAY),
                        calendar.get(Calendar.MINUTE),
                        true).show());

                addTaskButton.setOnClickListener(v -> {
                    try {
                        String newDesc = taskDescription.getText().toString().trim();
                        if (newDesc.isEmpty()) {
                            Toast.makeText(getContext(), "Введите описание задачи", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        Date newDate = calendar.getTime();
                        FirebaseFirestore db = FirebaseFirestore.getInstance();
                        Executors.newSingleThreadExecutor().execute(() -> {
                            try {
                                db.collection("tasks").document(task.getId())
                                        .update("description", newDesc, "dateTime", newDate)
                                        .addOnSuccessListener(aVoid -> new Handler(Looper.getMainLooper()).post(() -> {
                                            try {
                                                task.setDescription(newDesc);
                                                task.setDateTime(newDate);
                                                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                                    adapter.notifyItemChanged(position);
                                                    if (adapter.progressUpdateCallback != null) {
                                                        adapter.progressUpdateCallback.run();
                                                    }
                                                }, 100);
                                                dismiss();
                                            } catch (Exception e) {
                                                Log.e(TAG, "Ошибка обновления задачи в списке: " + e.getMessage(), e);
                                                Toast.makeText(getContext(), "Ошибка редактирования", Toast.LENGTH_SHORT).show();
                                            }
                                        }))
                                        .addOnFailureListener(e -> new Handler(Looper.getMainLooper()).post(() ->
                                                Toast.makeText(getContext(), "Ошибка редактирования: " + e.getMessage(), Toast.LENGTH_SHORT).show()));
                            } catch (Exception e) {
                                Log.e(TAG, "Ошибка редактирования задачи: " + e.getMessage(), e);
                                new Handler(Looper.getMainLooper()).post(() ->
                                        Toast.makeText(getContext(), "Ошибка редактирования", Toast.LENGTH_SHORT).show());
                            }
                        });
                    } catch (Exception e) {
                        Log.e(TAG, "Ошибка в обработчике кнопки редактирования: " + e.getMessage(), e);
                        Toast.makeText(getContext(), "Ошибка редактирования", Toast.LENGTH_SHORT).show();
                    }
                });

                return builder.create();
            } catch (Exception e) {
                Log.e(TAG, "Ошибка создания диалога редактирования: " + e.getMessage(), e);
                Toast.makeText(getContext(), "Ошибка открытия диалога", Toast.LENGTH_SHORT).show();
                return new androidx.appcompat.app.AlertDialog.Builder(requireActivity()).create();
            }
        }
    }

    public static class AddSubTaskDialogFragment extends DialogFragment {
        private Task task;
        private int position;
        private TaskAdapter adapter;

        public static AddSubTaskDialogFragment newInstance(Task task, int position, TaskAdapter adapter) {
            AddSubTaskDialogFragment fragment = new AddSubTaskDialogFragment();
            fragment.task = task;
            fragment.position = position;
            fragment.adapter = adapter;
            return fragment;
        }

        @NonNull
        @Override
        public android.app.Dialog onCreateDialog(@NonNull Bundle savedInstanceState) {
            try {
                androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(requireActivity());
                View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_task, null);
                builder.setView(dialogView);

                EditText taskDescription = dialogView.findViewById(R.id.taskDescription);
                Button pickDateButton = dialogView.findViewById(R.id.pickDateButton);
                Button pickTimeButton = dialogView.findViewById(R.id.pickTimeButton);
                Button addTaskButton = dialogView.findViewById(R.id.addTaskButton);

                pickDateButton.setVisibility(View.GONE);
                pickTimeButton.setVisibility(View.GONE);
                addTaskButton.setText("Добавить подзадачу");

                addTaskButton.setOnClickListener(v -> {
                    try {
                        String description = taskDescription.getText().toString().trim();
                        if (description.isEmpty()) {
                            Toast.makeText(getContext(), "Введите описание подзадачи", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        Map<String, Object> subTask = new HashMap<>();
                        subTask.put("description", description);
                        subTask.put("isCompleted", false);

                        FirebaseFirestore db = FirebaseFirestore.getInstance();
                        Executors.newSingleThreadExecutor().execute(() -> {
                            try {
                                db.collection("tasks").document(task.getId()).collection("subtasks")
                                        .add(subTask)
                                        .addOnSuccessListener(doc -> new Handler(Looper.getMainLooper()).post(() -> {
                                            try {
                                                task.setExpanded(true);
                                                task.setSubTasksLoaded(false);
                                                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                                    adapter.notifyItemChanged(position);
                                                    if (adapter.progressUpdateCallback != null) {
                                                        adapter.progressUpdateCallback.run();
                                                    }
                                                }, 100);
                                                dismiss();
                                            } catch (Exception e) {
                                                Log.e(TAG, "Ошибка обновления после добавления подзадачи: " + e.getMessage(), e);
                                                Toast.makeText(getContext(), "Ошибка добавления подзадачи", Toast.LENGTH_SHORT).show();
                                            }
                                        }))
                                        .addOnFailureListener(e -> new Handler(Looper.getMainLooper()).post(() ->
                                                Toast.makeText(getContext(), "Ошибка добавления подзадачи: " + e.getMessage(), Toast.LENGTH_SHORT).show()));
                            } catch (Exception e) {
                                Log.e(TAG, "Ошибка добавления подзадачи: " + e.getMessage(), e);
                                new Handler(Looper.getMainLooper()).post(() ->
                                        Toast.makeText(getContext(), "Ошибка добавления подзадачи", Toast.LENGTH_SHORT).show());
                            }
                        });
                    } catch (Exception e) {
                        Log.e(TAG, "Ошибка в обработчике кнопки добавления подзадачи: " + e.getMessage(), e);
                        Toast.makeText(getContext(), "Ошибка добавления подзадачи", Toast.LENGTH_SHORT).show();
                    }
                });

                return builder.create();
            } catch (Exception e) {
                Log.e(TAG, "Ошибка создания диалога добавления подзадачи: " + e.getMessage(), e);
                Toast.makeText(getContext(), "Ошибка открытия диалога", Toast.LENGTH_SHORT).show();
                return new androidx.appcompat.app.AlertDialog.Builder(requireActivity()).create();
            }
        }
    }

    public static class AddTaskDialogFragment extends DialogFragment {
        private TaskAdapter adapter;

        public static AddTaskDialogFragment newInstance(TaskAdapter adapter) {
            AddTaskDialogFragment fragment = new AddTaskDialogFragment();
            fragment.adapter = adapter;
            return fragment;
        }

        @NonNull
        @Override
        public android.app.Dialog onCreateDialog(@NonNull Bundle savedInstanceState) {
            try {
                androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(requireActivity());
                View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_task, null);
                builder.setView(dialogView);

                EditText taskDescription = dialogView.findViewById(R.id.taskDescription);
                Button pickDateButton = dialogView.findViewById(R.id.pickDateButton);
                Button pickTimeButton = dialogView.findViewById(R.id.pickTimeButton);
                Button addTaskButton = dialogView.findViewById(R.id.addTaskButton);

                addTaskButton.setText("Добавить задачу");
                Calendar calendar = Calendar.getInstance();
                String dateTimeString = dateFormat.format(calendar.getTime());
                pickDateButton.setText(dateTimeString.split(" ")[0]);
                pickTimeButton.setText(dateTimeString.split(" ")[1]);

                pickDateButton.setOnClickListener(v -> new DatePickerDialog(getContext(),
                        (view, year, month, day) -> {
                            calendar.set(year, month, day);
                            pickDateButton.setText(day + "/" + (month + 1) + "/" + year);
                        },
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH)).show());

                pickTimeButton.setOnClickListener(v -> new TimePickerDialog(getContext(),
                        (view, hour, minute) -> {
                            calendar.set(Calendar.HOUR_OF_DAY, hour);
                            calendar.set(Calendar.MINUTE, minute);
                            pickTimeButton.setText(String.format(Locale.getDefault(), "%02d:%02d", hour, minute));
                        },
                        calendar.get(Calendar.HOUR_OF_DAY),
                        calendar.get(Calendar.MINUTE),
                        true).show());

                addTaskButton.setOnClickListener(v -> {
                    try {
                        String description = taskDescription.getText().toString().trim();
                        if (description.isEmpty()) {
                            Toast.makeText(getContext(), "Введите описание задачи", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        Date dateTime = calendar.getTime();

                        Task newTask = new Task();
                        newTask.setDescription(description);
                        newTask.setDateTime(dateTime);
                        newTask.setCompleted(false);
                        newTask.setExpanded(false);
                        newTask.setSubTasksLoaded(false);

                        FirebaseFirestore db = FirebaseFirestore.getInstance();
                        Executors.newSingleThreadExecutor().execute(() -> {
                            try {
                                Map<String, Object> taskData = new HashMap<>();
                                taskData.put("description", description);
                                taskData.put("dateTime", dateTime);
                                taskData.put("isCompleted", false);
                                taskData.put("isExpanded", false);

                                db.collection("tasks")
                                        .add(taskData)
                                        .addOnSuccessListener(docRef -> new Handler(Looper.getMainLooper()).post(() -> {
                                            try {
                                                newTask.setId(docRef.getId());
                                                adapter.taskList.add(newTask);
                                                Collections.sort(adapter.taskList, Comparator.comparing(Task::getDateTime));
                                                int newPosition = adapter.taskList.indexOf(newTask);
                                                if (newPosition >= 0 && newPosition < adapter.taskList.size()) {
                                                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                                        try {
                                                            adapter.notifyItemInserted(newPosition);
                                                            if (adapter.progressUpdateCallback != null) {
                                                                adapter.progressUpdateCallback.run();
                                                            }
                                                        } catch (Exception e) {
                                                            Log.e(TAG, "Ошибка при уведомлении адаптера: " + e.getMessage(), e);
                                                            Toast.makeText(getContext(), "Ошибка добавления задачи", Toast.LENGTH_SHORT).show();
                                                        }
                                                    }, 100);
                                                } else {
                                                    Log.e(TAG, "Неверная позиция для новой задачи: " + newPosition);
                                                }
                                                dismiss();
                                            } catch (Exception e) {
                                                Log.e(TAG, "Ошибка добавления задачи в список: " + e.getMessage(), e);
                                                Toast.makeText(getContext(), "Ошибка добавления задачи", Toast.LENGTH_SHORT).show();
                                            }
                                        }))
                                        .addOnFailureListener(e -> new Handler(Looper.getMainLooper()).post(() ->
                                                Toast.makeText(getContext(), "Ошибка добавления задачи: " + e.getMessage(), Toast.LENGTH_SHORT).show()));
                            } catch (Exception e) {
                                Log.e(TAG, "Ошибка добавления задачи в Firestore: " + e.getMessage(), e);
                                new Handler(Looper.getMainLooper()).post(() ->
                                        Toast.makeText(getContext(), "Ошибка добавления задачи", Toast.LENGTH_SHORT).show());
                            }
                        });
                    } catch (Exception e) {
                        Log.e(TAG, "Ошибка в обработчике кнопки добавления задачи: " + e.getMessage(), e);
                        Toast.makeText(getContext(), "Ошибка добавления задачи", Toast.LENGTH_SHORT).show();
                    }
                });

                return builder.create();
            } catch (Exception e) {
                Log.e(TAG, "Ошибка создания диалога добавления задачи: " + e.getMessage(), e);
                Toast.makeText(getContext(), "Ошибка открытия диалога", Toast.LENGTH_SHORT).show();
                return new androidx.appcompat.app.AlertDialog.Builder(requireActivity()).create();
            }
        }
    }

    private static class TaskDiffCallback extends DiffUtil.Callback {
        private final List<Task> oldList;
        private final List<Task> newList;

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

    private void showToast(String message) {
        Context context = contextRef.get();
        if (context != null) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        }
    }
}