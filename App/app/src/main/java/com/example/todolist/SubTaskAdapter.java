package com.example.todolist;

import android.content.Context;
import android.graphics.Paint; // <-- ДОБАВЛЕН ИМПОРТ
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class SubTaskAdapter extends RecyclerView.Adapter<SubTaskAdapter.SubTaskViewHolder> {

    private List<SubTask> subTaskList;
    private final Runnable updateParentProgressCallback;
    private final Runnable updateParentUICallback;
    private final Context context;
    private final FirebaseFirestore db;
    private String taskId; // ID родительской задачи

    public SubTaskAdapter(List<SubTask> subTaskList, Runnable updateParentProgressCallback, Context context, String taskId, Runnable updateParentUICallback) {
        this.subTaskList = subTaskList != null ? new ArrayList<>(subTaskList) : new ArrayList<>();
        this.updateParentProgressCallback = updateParentProgressCallback;
        this.updateParentUICallback = updateParentUICallback;
        this.context = context;
        this.db = FirebaseFirestore.getInstance();
        this.taskId = taskId;
        Log.d("SubTaskAdapter", "Constructor for Task ID: " + taskId + ", initial subtasks: " + this.subTaskList.size());
    }

    public void setTaskId(String taskId) {
        String oldTaskId = this.taskId;
        boolean changed = (taskId == null && oldTaskId != null) || (taskId != null && !taskId.equals(oldTaskId));
        if (changed) {
            Log.d("SubTaskAdapter", "Setting Task ID from " + oldTaskId + " to " + taskId);
            this.taskId = taskId;
            this.subTaskList.clear(); // Очищаем список при смене родителя
            notifyDataSetChanged();
        }
    }

    public void setSubTasks(List<SubTask> subTasks) {
        this.subTaskList = subTasks != null ? new ArrayList<>(subTasks) : new ArrayList<>();
        Log.d("SubTaskAdapter", "Set SubTasks for Task ID: " + taskId + ", new count: " + this.subTaskList.size());
        notifyDataSetChanged();
        // Не вызываем коллбэки здесь, чтобы избежать лишних обновлений
    }

    public List<SubTask> getSubTasks() {
        // Возвращаем копию, чтобы избежать внешних модификаций
        return new ArrayList<>(subTaskList);
    }

    @NonNull
    @Override
    public SubTaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.subtask_item, parent, false);
        return new SubTaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SubTaskViewHolder holder, int position) {
        if (position >= 0 && position < subTaskList.size()) {
            SubTask subTask = subTaskList.get(position);
            holder.bind(subTask);
        } else {
            Log.e("SubTaskAdapter", "Invalid position in onBindViewHolder: " + position + ", size: " + subTaskList.size());
            // Можно скрыть элемент или показать заглушку
            holder.itemView.setVisibility(View.GONE);
        }
    }


    @Override
    public int getItemCount() {
        return subTaskList.size();
    }

    // --- Действия с подзадачами ---
    private void removeSubTask(int position) {
        if (taskId == null || taskId.isEmpty()) {
            Log.e("SubTaskAdapter", "Cannot remove subtask, parent taskId is null or empty."); return;
        }
        if (position < 0 || position >= subTaskList.size()) {
            Log.e("SubTaskAdapter", "Cannot remove subtask, invalid position: " + position); return;
        }

        SubTask subTaskToRemove = subTaskList.get(position);
        String subTaskId = subTaskToRemove.getSubTaskId();

        if (subTaskId == null || subTaskId.isEmpty()) {
            Log.e("SubTaskAdapter", "Cannot remove subtask, subTaskId is null or empty. Removing from list only.");
            // Если нет ID, удаляем только из списка UI
            subTaskList.remove(position);
            notifyItemRemoved(position);
            if (updateParentUICallback != null) updateParentUICallback.run();
            return;
        }

        db.collection("tasks").document(taskId).collection("subtasks").document(subTaskId).delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d("Firestore", "Подзадача удалена из Firestore: " + subTaskToRemove.getDescription());
                    // Проверяем, что элемент все еще на той же позиции
                    if (position < subTaskList.size() && subTaskList.get(position).getSubTaskId().equals(subTaskId)) {
                        subTaskList.remove(position);
                        notifyItemRemoved(position);
                    } else {
                        // Элемент сместился или удален, ищем по ID
                        int currentPosition = findSubTaskPosition(subTaskId);
                        if (currentPosition != -1) {
                            subTaskList.remove(currentPosition);
                            notifyItemRemoved(currentPosition);
                        } else {
                            notifyDataSetChanged(); // Обновляем весь список, если не нашли
                        }
                    }
                    // Уведомляем родителя об изменении UI
                    if (updateParentUICallback != null) updateParentUICallback.run();
                })
                .addOnFailureListener(e -> {
                    Log.e("FirestoreError", "Ошибка удаления подзадачи из Firestore: " + e.getMessage());
                    Toast.makeText(context, "Ошибка удаления подзадачи", Toast.LENGTH_SHORT).show();
                });
    }

    private void updateSubTaskCompletion(SubTask subTask, boolean isCompleted, int position) {
        if (taskId == null || taskId.isEmpty()) {
            Log.e("SubTaskAdapter", "Cannot update subtask, parent taskId is null or empty.");
            notifyItemChanged(position); // Откатываем UI
            return;
        }
        String subTaskId = subTask.getSubTaskId();
        if (subTaskId == null || subTaskId.isEmpty()) {
            Log.e("SubTaskAdapter", "Cannot update subtask, subTaskId is null or empty.");
            notifyItemChanged(position); // Откатываем UI
            return;
        }

        // Оптимистичное обновление UI
        subTask.setCompleted(isCompleted);
        notifyItemChanged(position);

        db.collection("tasks").document(taskId).collection("subtasks").document(subTaskId)
                .update("isCompleted", isCompleted)
                .addOnSuccessListener(aVoid -> {
                    Log.d("Firestore", "Статус подзадачи обновлен в Firestore: " + subTask.getDescription() + " -> " + isCompleted);
                    // Уведомляем родителя об изменении UI
                    if (updateParentUICallback != null) updateParentUICallback.run();
                })
                .addOnFailureListener(e -> {
                    Log.e("FirestoreError", "Ошибка обновления статуса подзадачи в Firestore: " + e.getMessage());
                    Toast.makeText(context, "Ошибка обновления статуса", Toast.LENGTH_SHORT).show();
                    // Откатываем изменение в UI
                    subTask.setCompleted(!isCompleted);
                    notifyItemChanged(position);
                    if (updateParentUICallback != null) updateParentUICallback.run();
                });
    }

    // Вспомогательный метод для поиска подзадачи по ID
    private int findSubTaskPosition(String subTaskId) {
        if (subTaskId == null) return -1;
        for (int i = 0; i < subTaskList.size(); i++) {
            if (subTaskId.equals(subTaskList.get(i).getSubTaskId())) {
                return i;
            }
        }
        return -1;
    }


    // --- ViewHolder для подзадачи ---
    class SubTaskViewHolder extends RecyclerView.ViewHolder {
        private CheckBox checkBox;
        private TextView descriptionTextView;
        private ImageButton deleteButton;

        public SubTaskViewHolder(@NonNull View itemView) {
            super(itemView);
            checkBox = itemView.findViewById(R.id.subTaskCheckBox);
            descriptionTextView = itemView.findViewById(R.id.subTaskTextView);
            deleteButton = itemView.findViewById(R.id.deleteSubTaskButton);

            deleteButton.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    removeSubTask(pos);
                }
            });
        }

        public void bind(SubTask subTask) {
            descriptionTextView.setText(subTask.getDescription());
            // Зачеркивание текста
            descriptionTextView.setPaintFlags(subTask.isCompleted()
                    ? descriptionTextView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG // Используем Paint
                    : descriptionTextView.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG)); // Используем Paint

            // Снимаем слушатель перед установкой состояния
            checkBox.setOnCheckedChangeListener(null);
            checkBox.setChecked(subTask.isCompleted());
            // Устанавливаем слушатель
            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    // Получаем актуальный объект из списка
                    if (position < subTaskList.size()){ // Доп. проверка
                        SubTask currentSubTask = subTaskList.get(position);
                        updateSubTaskCompletion(currentSubTask, isChecked, position);
                    }
                }
            });
        }
    }
}