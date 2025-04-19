package com.example.todolist;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

/**
 * Адаптер для отображения подзадач в RecyclerView.
 */
public class SubTaskAdapter extends RecyclerView.Adapter<SubTaskAdapter.SubTaskViewHolder> {
    // Список подзадач
    private List<SubTask> subTaskList;
    // Callback для обновления прогресса родительской задачи
    private final Runnable updateParentProgressCallback;
    // Контекст приложения
    private final Context context;
    // Объект Firebase для работы с базой данных
    private final FirebaseFirestore db;
    // Идентификатор родительской задачи
    private final String taskId;

    /**
     * Конструктор адаптера.
     * @param subTaskList Список подзадач.
     * @param updateParentProgressCallback Callback для обновления прогресса.
     * @param context Контекст приложения.
     * @param taskId Идентификатор родительской задачи.
     */
    public SubTaskAdapter(List<SubTask> subTaskList, Runnable updateParentProgressCallback, Context context, String taskId) {
        // Инициализация списка подзадач, копируя входной список или создавая пустой
        this.subTaskList = subTaskList != null ? new ArrayList<>(subTaskList) : new ArrayList<>();
        this.updateParentProgressCallback = updateParentProgressCallback;
        this.context = context;
        this.db = FirebaseFirestore.getInstance();
        this.taskId = taskId;
    }

    /**
     * Обновляет список подзадач с использованием DiffUtil для эффективного обновления UI.
     * @param subTasks Новый список подзадач.
     */
    public void setSubTasks(List<SubTask> subTasks) {
        // Копирование старого списка для сравнения
        List<SubTask> oldList = new ArrayList<>(this.subTaskList);
        // Обновление текущего списка
        this.subTaskList = subTasks != null ? new ArrayList<>(subTasks) : new ArrayList<>();
        // Вычисление различий между старым и новым списками
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new SubTaskDiffCallback(oldList, this.subTaskList));
        // Применение изменений к адаптеру
        diffResult.dispatchUpdatesTo(this);
        // Вызов callback для обновления прогресса
        updateParentProgressCallback.run();
    }

    @NonNull
    @Override
    public SubTaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Надувание layout для элемента подзадачи
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.subtask_item, parent, false);
        return new SubTaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SubTaskViewHolder holder, int position) {
        // Привязка данных подзадачи к ViewHolder
        SubTask subTask = subTaskList.get(position);
        holder.bind(subTask);
    }

    @Override
    public int getItemCount() {
        // Возвращает количество подзадач
        return subTaskList.size();
    }

    /**
     * Удаляет подзадачу из Firestore и списка.
     * @param position Позиция подзадачи в списке.
     */
    private void removeSubTask(int position) {
        // Проверка корректности позиции
        if (position < 0 || position >= subTaskList.size()) return;
        SubTask subTask = subTaskList.get(position);
        // Выполнение операции удаления в отдельном потоке
        Executors.newSingleThreadExecutor().execute(() -> {
            db.collection("tasks").document(taskId).collection("subtasks").document(subTask.getSubTaskId()).delete()
                    .addOnSuccessListener(aVoid -> new Handler(Looper.getMainLooper()).post(() -> {
                        // Удаление подзадачи из списка и уведомление адаптера
                        subTaskList.remove(position);
                        notifyItemRemoved(position);
                        // Обновление прогресса родительской задачи
                        updateParentProgressCallback.run();
                    }))
                    .addOnFailureListener(e -> new Handler(Looper.getMainLooper()).post(() ->
                            // Показ сообщения об ошибке
                            Toast.makeText(context, "Ошибка удаления подзадачи", Toast.LENGTH_SHORT).show()));
        });
    }

    /**
     * Обновляет статус завершения подзадачи в Firestore.
     * @param subTask Подзадача.
     * @param isCompleted Новый статус завершения.
     */
    private void updateSubTaskCompletion(SubTask subTask, boolean isCompleted) {
        // Обновление локального статуса подзадачи
        subTask.setCompleted(isCompleted);
        // Выполнение обновления в Firestore в отдельном потоке
        Executors.newSingleThreadExecutor().execute(() -> {
            db.collection("tasks").document(taskId).collection("subtasks").document(subTask.getSubTaskId())
                    .update("isCompleted", isCompleted)
                    .addOnSuccessListener(aVoid -> new Handler(Looper.getMainLooper()).post(() -> {
                        // Уведомление адаптера об изменении элемента
                        int position = subTaskList.indexOf(subTask);
                        if (position >= 0) {
                            notifyItemChanged(position);
                            // Обновление прогресса родительской задачи
                            updateParentProgressCallback.run();
                        }
                    }))
                    .addOnFailureListener(e -> new Handler(Looper.getMainLooper()).post(() ->
                            // Показ сообщения об ошибке
                            Toast.makeText(context, "Ошибка обновления статуса", Toast.LENGTH_SHORT).show()));
        });
    }

    /**
     * ViewHolder для элемента подзадачи.
     */
    class SubTaskViewHolder extends RecyclerView.ViewHolder {
        private CheckBox checkBox;
        private TextView descriptionTextView;
        private ImageButton deleteButton;

        public SubTaskViewHolder(@NonNull View itemView) {
            super(itemView);
            // Инициализация элементов интерфейса
            checkBox = itemView.findViewById(R.id.subTaskCheckBox);
            descriptionTextView = itemView.findViewById(R.id.subTaskTextView);
            deleteButton = itemView.findViewById(R.id.deleteSubTaskButton);

            // Обработчик нажатия на кнопку удаления
            deleteButton.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    removeSubTask(pos);
                }
            });
        }

        /**
         * Привязывает данные подзадачи к элементам интерфейса.
         * @param subTask Подзадача.
         */
        public void bind(SubTask subTask) {
            // Отключение слушателя для предотвращения нежелательных вызовов
            checkBox.setOnCheckedChangeListener(null);
            // Установка статуса завершения
            checkBox.setChecked(subTask.isCompleted());
            // Установка слушателя для обновления статуса завершения
            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> updateSubTaskCompletion(subTask, isChecked));
            // Установка описания подзадачи
            descriptionTextView.setText(subTask.getDescription());
        }
    }

    /**
     * Callback для DiffUtil, используемый для сравнения списков подзадач.
     */
    private static class SubTaskDiffCallback extends DiffUtil.Callback {
        private final List<SubTask> oldList;
        private final List<SubTask> newList;

        SubTaskDiffCallback(List<SubTask> oldList, List<SubTask> newList) {
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
            // Сравнение подзадач по их идентификатору
            return oldList.get(oldItemPosition).getSubTaskId().equals(newList.get(newItemPosition).getSubTaskId());
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            // Сравнение содержимого подзадач
            SubTask oldSubTask = oldList.get(oldItemPosition);
            SubTask newSubTask = newList.get(newItemPosition);
            return oldSubTask.isCompleted() == newSubTask.isCompleted() &&
                    oldSubTask.getDescription().equals(newSubTask.getDescription());
        }
    }
}