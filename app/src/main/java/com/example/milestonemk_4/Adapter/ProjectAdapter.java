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

    public ProjectAdapter(List<Project> projectList) {
        this.projectList = projectList;
    }

    @NonNull
    @Override
    public ProjectViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.project_items, parent, false);
        return new ProjectViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProjectViewHolder holder, int position) {
        Project project = projectList.get(position);
        holder.projectTitle.setText(project.getTitle());

//        test
        holder.itemView.setOnClickListener(v -> {
            // Get the context of the clicked item
            Context context = v.getContext();

            // Create an Intent to open the DetailActivity
            Intent intent = new Intent(context, project_detail.class);

            // Pass the project data (title and userId) to the DetailActivity
            intent.putExtra("title", project.getTitle());
            intent.putExtra("userId", project.getUserId());

            // Start the activity
            context.startActivity(intent);
        });
//        end test
    }

    @Override
    public int getItemCount() {
        return projectList.size();
    }

    public static class ProjectViewHolder extends RecyclerView.ViewHolder {
        TextView projectTitle;

        public ProjectViewHolder(@NonNull View itemView) {
            super(itemView);
            projectTitle = itemView.findViewById(R.id.project_title);
        }
    }
}
