package com.example.milestonemk_4.fragments;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.milestonemk_4.Adapter.ProjectAdapter;
import com.example.milestonemk_4.R;
import com.example.milestonemk_4.activitiesUI.Add_Project;

import com.example.milestonemk_4.model.Project;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class HomeFragment extends Fragment {

    private ProjectAdapter adapter;
    private List<Project> projectList;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        RecyclerView recyclerView = view.findViewById(R.id.project_recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        projectList = new ArrayList<>();
        adapter = new ProjectAdapter(projectList);
        recyclerView.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        FloatingActionButton fab = view.findViewById(R.id.addProjectBtn);

        fab.setOnClickListener(view1 -> {
            if (getContext() != null){
                startActivity(new Intent(getContext(), Add_Project.class ));
            } else {
                Toast.makeText(getContext(), "error", Toast.LENGTH_SHORT).show();
            }

        });

        loadProjects();
        return view;
    }

    @SuppressLint("NotifyDataSetChanged")
    private void loadProjects() {
        FirebaseUser user = auth.getCurrentUser();

        if (user != null) {
            String uid = user.getUid();

            db.collection("projects")
                    .whereArrayContains("allowedUsers", uid)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            projectList.clear();
                            List<DocumentSnapshot> projectDocs = task.getResult().getDocuments();

                            if (projectDocs.isEmpty()) {
                                adapter.notifyDataSetChanged();
                                return;
                            }

                            for (DocumentSnapshot document : projectDocs) {
                                Project project = document.toObject(Project.class);
                                String projectId = document.getId();
                                assert project != null;
                                project.setId(projectId);

                                db.collection("projects")
                                        .document(projectId)
                                        .collection("tasks")
                                        .get()
                                        .addOnSuccessListener(taskSnapshots -> {
                                            int taskCount = taskSnapshots.size();
                                            project.setTaskCount(taskCount);
                                            projectList.add(project);

                                            // Notify when all projects have been processed
                                            if (projectList.size() == projectDocs.size()) {
                                                adapter.notifyDataSetChanged();
                                            }
                                        })
                                        .addOnFailureListener(e -> {
                                            project.setTaskCount(0);
                                            projectList.add(project);

                                            if (projectList.size() == projectDocs.size()) {
                                                adapter.notifyDataSetChanged();
                                            }
                                        });
                            }
                        } else {
                            Toast.makeText(getContext(), "Failed to load projects", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Set the toolbar title
        Objects.requireNonNull(((AppCompatActivity) requireActivity()).getSupportActionBar()).setTitle("Projects");
    }
}
