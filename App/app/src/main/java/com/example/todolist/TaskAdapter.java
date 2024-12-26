package com.example.todolist;

import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    private final List<Task> taskList = new ArrayList<>();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

    public void addTask(Task task) {
        taskList.add(task);
        Collections.sort(taskList, Comparator.comparing(Task::getDateTime));
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.task_item, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = taskList.get(position);
        holder.bind(task);
    }

    @Override
    public int getItemCount() {
        return taskList.size();
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        private final TextView taskTextView;
        private final CheckBox taskCheckBox;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            taskTextView = itemView.findViewById(R.id.taskTextView);
            taskCheckBox = itemView.findViewById(R.id.taskCheckBox);
        }

        public void bind(Task task) {
            taskTextView.setText(task.getDescription() + " (" + new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(task.getDateTime()) + ")");
            taskTextView.setPaintFlags(task.isCompleted() ? taskTextView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG : taskTextView.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            taskCheckBox.setChecked(task.isCompleted());

            taskCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                task.setCompleted(isChecked);
                taskTextView.setPaintFlags(isChecked ? taskTextView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG : taskTextView.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            });
        }
    }
}
