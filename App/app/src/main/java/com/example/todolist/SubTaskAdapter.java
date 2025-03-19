package com.example.todolist;

import android.content.Context;
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

// Адаптер для отображения списка подзадач в RecyclerView
public class SubTaskAdapter extends RecyclerView.Adapter<SubTaskAdapter.SubTaskViewHolder> {

    // Список подзадач
    private List<SubTask> subTaskList;
    // Callback для обновления прогресса родительской задачи
    private final Runnable updateParentProgressCallback;
    // Контекст активности
    private final Context context;
    // Экземпляр Firestore для работы с базой данных
    private final FirebaseFirestore db;
    // ID родительской задачи
    private final String taskId;

    // Конструктор адаптера
    public SubTaskAdapter(List<SubTask> subTaskList, Runnable updateParentProgressCallback, Context context, String taskId) {
        // Инициализация списка подзадач, с проверкой на null
        this.subTaskList = subTaskList != null ? subTaskList : new ArrayList<>();
        this.updateParentProgressCallback = updateParentProgressCallback; // Callback для обновления прогресса
        this.context = context; // Контекст для показа Toast
        this.db = FirebaseFirestore.getInstance(); // Инициализация Firestore
        this.taskId = taskId; // ID связанной задачи
    }

    // Метод для обновления списка подзадач
    public void setSubTasks(List<SubTask> subTasks) {
        this.subTaskList = subTasks != null ? subTasks : new ArrayList<>(); // Установка нового списка с проверкой на null
        notifyDataSetChanged(); // Уведомление адаптера об изменении данных
    }

    // Создание нового ViewHolder для подзадачи
    @NonNull
    @Override
    public SubTaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Надувание макета элемента подзадачи
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.subtask_item, parent, false);
        return new SubTaskViewHolder(view); // Возврат нового ViewHolder
    }

    // Привязка данных подзадачи к ViewHolder
    @Override
    public void onBindViewHolder(@NonNull SubTaskViewHolder holder, int position) {
        SubTask subTask = subTaskList.get(position); // Получение подзадачи по позиции
        holder.bind(subTask); // Привязка данных к ViewHolder
    }

    // Возвращает общее количество подзадач
    @Override
    public int getItemCount() {
        return subTaskList.size();
    }

    // Метод для удаления подзадачи
    private void removeSubTask(int position) {
        // Проверка валидности позиции
        if (position < 0 || position >= subTaskList.size()) return;
        SubTask st = subTaskList.get(position); // Получение подзадачи для удаления
        // Удаление подзадачи из Firestore
        db.collection("tasks").document(taskId).collection("subtasks").document(st.getSubTaskId()).delete()
                .addOnSuccessListener(aVoid -> {
                    subTaskList.remove(position); // Удаление из локального списка
                    notifyItemRemoved(position); // Уведомление об удалении элемента
                    updateParentProgressCallback.run(); // Обновление прогресса родительской задачи
                    Log.d("Firestore", "Подзадача удалена: " + st.getDescription());
                })
                .addOnFailureListener(e -> {
                    // Логирование ошибки удаления
                    Log.e("FirestoreError", "Ошибка удаления подзадачи: " + e.getMessage());
                    Toast.makeText(context, "Ошибка удаления подзадачи", Toast.LENGTH_SHORT).show();
                });
    }

    // Метод для обновления статуса выполнения подзадачи
    private void updateSubTaskCompletion(SubTask subTask, boolean isCompleted) {
        subTask.setCompleted(isCompleted); // Локальное обновление статуса
        // Обновление статуса в Firestore
        db.collection("tasks").document(taskId).collection("subtasks").document(subTask.getSubTaskId())
                .update("isCompleted", isCompleted)
                .addOnSuccessListener(aVoid -> {
                    notifyDataSetChanged(); // Уведомление об изменении данных
                    updateParentProgressCallback.run(); // Обновление прогресса родительской задачи
                    Log.d("Firestore", "Статус подзадачи обновлен: " + subTask.getDescription());
                })
                .addOnFailureListener(e -> {
                    // Логирование ошибки обновления
                    Log.e("FirestoreError", "Ошибка обновления статуса подзадачи: " + e.getMessage());
                    Toast.makeText(context, "Ошибка обновления статуса", Toast.LENGTH_SHORT).show();
                });
    }

    // Внутренний класс ViewHolder для элементов подзадачи
    class SubTaskViewHolder extends RecyclerView.ViewHolder {
        private CheckBox checkBox; // Чекбокс для отметки выполнения
        private TextView descriptionTextView; // Текст описания подзадачи
        private ImageButton deleteButton; // Кнопка удаления подзадачи

        // Конструктор ViewHolder
        public SubTaskViewHolder(@NonNull View itemView) {
            super(itemView);
            // Привязка элементов интерфейса к переменным
            checkBox = itemView.findViewById(R.id.subTaskCheckBox);
            descriptionTextView = itemView.findViewById(R.id.subTaskTextView);
            deleteButton = itemView.findViewById(R.id.deleteSubTaskButton);

            // Установка слушателя для кнопки удаления
            deleteButton.setOnClickListener(v -> {
                int pos = getAdapterPosition(); // Получение позиции элемента
                if (pos != RecyclerView.NO_POSITION) removeSubTask(pos); // Удаление подзадачи, если позиция валидна
            });
        }

        // Метод привязки данных подзадачи к элементам интерфейса
        public void bind(SubTask subTask) {
            checkBox.setOnCheckedChangeListener(null); // Сброс слушателя для предотвращения нежелательных вызовов
            checkBox.setChecked(subTask.isCompleted()); // Установка текущего статуса чекбокса
            // Установка нового слушателя для обновления статуса
            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> updateSubTaskCompletion(subTask, isChecked));
            descriptionTextView.setText(subTask.getDescription()); // Установка текста описания
        }
    }
}