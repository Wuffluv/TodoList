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

public class SubTaskAdapter extends RecyclerView.Adapter<SubTaskAdapter.SubTaskViewHolder> {

    private List<SubTask> subTaskList;
    private final Runnable updateParentProgressCallback;
    private final Context context;
    private final FirebaseFirestore db;
    private final String taskId;

    public SubTaskAdapter(List<SubTask> subTaskList, Runnable updateParentProgressCallback, Context context, String taskId) {
        this.subTaskList = subTaskList != null ? subTaskList : new ArrayList<>();
        this.updateParentProgressCallback = updateParentProgressCallback;
        this.context = context;
        this.db = FirebaseFirestore.getInstance();
        this.taskId = taskId;
    }

    public void setSubTasks(List<SubTask> subTasks) {
        this.subTaskList = subTasks != null ? subTasks : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SubTaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.subtask_item, parent, false);
        return new SubTaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SubTaskViewHolder holder, int position) {
        SubTask subTask = subTaskList.get(position);
        holder.bind(subTask);
    }

    @Override
    public int getItemCount() {
        return subTaskList.size();
    }

    private void removeSubTask(int position) {
        if (position < 0 || position >= subTaskList.size()) return;
        SubTask st = subTaskList.get(position);
        db.collection("tasks").document(taskId).collection("subtasks").document(st.getSubTaskId()).delete()
                .addOnSuccessListener(aVoid -> {
                    subTaskList.remove(position);
                    notifyItemRemoved(position);
                    updateParentProgressCallback.run();
                    Log.d("Firestore", "Подзадача удалена: " + st.getDescription());
                })
                .addOnFailureListener(e -> {
                    Log.e("FirestoreError", "Ошибка удаления подзадачи: " + e.getMessage());
                    Toast.makeText(context, "Ошибка удаления подзадачи", Toast.LENGTH_SHORT).show();
                });
    }

    private void updateSubTaskCompletion(SubTask subTask, boolean isCompleted) {
        subTask.setCompleted(isCompleted);
        db.collection("tasks").document(taskId).collection("subtasks").document(subTask.getSubTaskId())
                .update("isCompleted", isCompleted)
                .addOnSuccessListener(aVoid -> {
                    notifyDataSetChanged();
                    updateParentProgressCallback.run();
                    Log.d("Firestore", "Статус подзадачи обновлен: " + subTask.getDescription());
                })
                .addOnFailureListener(e -> {
                    Log.e("FirestoreError", "Ошибка обновления статуса подзадачи: " + e.getMessage());
                    Toast.makeText(context, "Ошибка обновления статуса", Toast.LENGTH_SHORT).show();
                });
    }

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
                if (pos != RecyclerView.NO_POSITION) removeSubTask(pos);
            });
        }

        public void bind(SubTask subTask) {
            checkBox.setOnCheckedChangeListener(null);
            checkBox.setChecked(subTask.isCompleted());
            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> updateSubTaskCompletion(subTask, isChecked));
            descriptionTextView.setText(subTask.getDescription());
        }
    }
}