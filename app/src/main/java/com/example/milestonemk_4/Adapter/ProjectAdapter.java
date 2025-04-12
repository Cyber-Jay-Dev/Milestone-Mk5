package com.example.milestonemk_4.Adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.milestonemk_4.R;
import com.example.milestonemk_4.activitiesUI.project_detail;
import com.example.milestonemk_4.model.Project;

import java.util.List;

public class ProjectAdapter extends RecyclerView.Adapter<ProjectAdapter.ProjectViewHolder> {

    private List<Project> projectList;
    private Context context;

    public ProjectAdapter(List<Project> projectList) {
        this.projectList = projectList;
    }

    @NonNull
    @Override
    public ProjectViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.project_items, parent, false);
        context = parent.getContext();
        return new ProjectViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ProjectViewHolder holder, int position) {
        Project project = projectList.get(position);
        holder.projectTitle.setText(project.getTitle());

        // When an item is clicked, open the project details
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, project_detail.class);
            intent.putExtra("projectId", project.getId()); // Passing projectId to the next activity
            intent.putExtra("title", project.getTitle()); // Optional: Passing project title
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return projectList.size();
    }

    public static class ProjectViewHolder extends RecyclerView.ViewHolder {

        TextView projectTitle;

        public ProjectViewHolder(View itemView) {
            super(itemView);
            projectTitle = itemView.findViewById(R.id.project_title);
        }
    }
}
