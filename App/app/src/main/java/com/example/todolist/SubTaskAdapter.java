package com.example.todolist;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * Адаптер для списка подзадач (SubTask)
 */
public class SubTaskAdapter extends RecyclerView.Adapter<SubTaskAdapter.SubTaskViewHolder> {

    private List<SubTask> subTaskList;
    private DatabaseHelper dbHelper;
    private final Runnable updateParentProgressCallback;

    public SubTaskAdapter(List<SubTask> subTaskList,
                          DatabaseHelper dbHelper,
                          Runnable updateParentProgressCallback) {
        this.subTaskList = subTaskList;
        this.dbHelper = dbHelper;
        this.updateParentProgressCallback = updateParentProgressCallback;
    }

    @NonNull
    @Override
    public SubTaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.subtask_item, parent, false);
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

    public void addSubTask(SubTask subTask) {
        subTaskList.add(subTask);
        notifyDataSetChanged();
        updateParentProgressCallback.run();
    }

    public void removeSubTask(int position) {
        if (position >= 0 && position < subTaskList.size()) {
            SubTask st = subTaskList.get(position);
            dbHelper.deleteSubTask(st.getSubTaskId());
            subTaskList.remove(position);
            notifyItemRemoved(position);
            updateParentProgressCallback.run();
        }
    }

    public void updateSubTaskCompletion(SubTask subTask, boolean isCompleted) {
        subTask.setCompleted(isCompleted);
        dbHelper.updateSubTaskCompletion(subTask.getSubTaskId(), isCompleted);
        // Можно точечно: notifyItemChanged(...) если хотим только 1 элемент
        notifyDataSetChanged();
        updateParentProgressCallback.run();
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

            // Удаление
            deleteButton.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    removeSubTask(pos);
                }
            });
        }

        public void bind(SubTask subTask) {
            // 1) Отключаем временно листенер
            checkBox.setOnCheckedChangeListener(null);

            // 2) Устанавливаем состояние
            checkBox.setChecked(subTask.isCompleted());

            // 3) Возвращаем листенер
            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    SubTask st = subTaskList.get(pos);
                    updateSubTaskCompletion(st, isChecked);
                }
            });

            descriptionTextView.setText(subTask.getDescription());
        }
    }
}
