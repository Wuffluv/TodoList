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

public class SubTaskAdapter extends RecyclerView.Adapter<SubTaskAdapter.SubTaskViewHolder> {
    private List<SubTask> subTaskList;
    private final Runnable updateParentProgressCallback;
    private final Context context;
    private final FirebaseFirestore db;
    private final String taskId;

    public SubTaskAdapter(List<SubTask> subTaskList, Runnable updateParentProgressCallback, Context context, String taskId) {
        this.subTaskList = subTaskList != null ? new ArrayList<>(subTaskList) : new ArrayList<>();
        this.updateParentProgressCallback = updateParentProgressCallback;
        this.context = context;
        this.db = FirebaseFirestore.getInstance();
        this.taskId = taskId;
    }

    public void setSubTasks(List<SubTask> subTasks) {
        List<SubTask> oldList = new ArrayList<>(this.subTaskList);
        this.subTaskList = subTasks != null ? new ArrayList<>(subTasks) : new ArrayList<>();
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new SubTaskDiffCallback(oldList, this.subTaskList));
        diffResult.dispatchUpdatesTo(this);
        updateParentProgressCallback.run();
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
        Executors.newSingleThreadExecutor().execute(() -> {
            db.collection("tasks").document(taskId).collection("subtasks").document(st.getSubTaskId()).delete()
                    .addOnSuccessListener(aVoid -> new Handler(Looper.getMainLooper()).post(() -> {
                        subTaskList.remove(position);
                        notifyItemRemoved(position);
                        updateParentProgressCallback.run();
                    }))
                    .addOnFailureListener(e -> new Handler(Looper.getMainLooper()).post(() ->
                            Toast.makeText(context, "Ошибка удаления подзадачи", Toast.LENGTH_SHORT).show()));
        });
    }

    private void updateSubTaskCompletion(SubTask subTask, boolean isCompleted) {
        subTask.setCompleted(isCompleted);
        Executors.newSingleThreadExecutor().execute(() -> {
            db.collection("tasks").document(taskId).collection("subtasks").document(subTask.getSubTaskId())
                    .update("isCompleted", isCompleted)
                    .addOnSuccessListener(aVoid -> new Handler(Looper.getMainLooper()).post(() -> {
                        notifyDataSetChanged();
                        updateParentProgressCallback.run();
                    }))
                    .addOnFailureListener(e -> new Handler(Looper.getMainLooper()).post(() ->
                            Toast.makeText(context, "Ошибка обновления статуса", Toast.LENGTH_SHORT).show()));
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
            return oldList.get(oldItemPosition).getSubTaskId().equals(newList.get(newItemPosition).getSubTaskId());
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            SubTask oldSubTask = oldList.get(oldItemPosition);
            SubTask newSubTask = newList.get(newItemPosition);
            return oldSubTask.isCompleted() == newSubTask.isCompleted() &&
                    oldSubTask.getDescription().equals(newSubTask.getDescription());
        }
    }
}