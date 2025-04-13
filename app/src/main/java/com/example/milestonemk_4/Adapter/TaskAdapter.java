package com.example.milestonemk_4.Adapter;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.milestonemk_4.R;
import com.example.milestonemk_4.model.Task;


import java.util.List;
import java.util.Objects;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    private final List<Task> taskList;

    public TaskAdapter(List<Task> taskList) {
        this.taskList = taskList;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.task_items, parent, false);
        return new TaskViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = taskList.get(position);
        holder.taskName.setText(task.getTaskName());
        holder.status.setText(task.getStatus());

        Context context = holder.status.getContext();

        Drawable background = Objects.requireNonNull(ContextCompat.getDrawable(context, R.drawable.rounded_bg)).mutate();

        if (background instanceof GradientDrawable) {
            GradientDrawable gradientDrawable = (GradientDrawable) background;

            String status = task.getStatus().toLowerCase();
            switch (status) {
                case "low":
                    gradientDrawable.setColor(ContextCompat.getColor(context, android.R.color.holo_green_light));
                    break;
                case "medium":
                    gradientDrawable.setColor(ContextCompat.getColor(context, android.R.color.holo_orange_light));
                    break;
                case "high":
                    gradientDrawable.setColor(ContextCompat.getColor(context, android.R.color.holo_red_light));
                    break;
                default:
                    gradientDrawable.setColor(ContextCompat.getColor(context, android.R.color.darker_gray));
                    break;
            }

            holder.status.setBackground(gradientDrawable);
        }
    }
    @Override
    public int getItemCount() {
        return taskList.size();
    }

    public static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView taskName;
        TextView status;
        public TaskViewHolder(View view) {
            super(view);
            taskName = view.findViewById(R.id.TaskNameTV);
            status = view.findViewById(R.id.TaskStatus);
        }
    }
}