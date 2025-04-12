package com.example.milestonemk_4.activitiesUI;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Dialog;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.milestonemk_4.Adapter.TaskAdapter;
import com.example.milestonemk_4.R;
import com.example.milestonemk_4.model.Task;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class project_detail extends AppCompatActivity {

    Dialog dialog;
    FirebaseFirestore db;
    RecyclerView taskRecyclerView;
    TaskAdapter taskAdapter;
    List<Task> taskList;
    String projectId;

    @SuppressLint("UseCompatLoadingForDrawables")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_project_detail);

        db = FirebaseFirestore.getInstance();

        ImageButton openTasksDialog = findViewById(R.id.openTaskDialog);
        Button backBtn = findViewById(R.id.backBtn);
        taskRecyclerView = findViewById(R.id.taskRecyclerview);
        taskList = new ArrayList<>();
        taskAdapter = new TaskAdapter(taskList);
        taskRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        taskRecyclerView.setAdapter(taskAdapter);

        // Initialize dialog
        dialog = new Dialog(project_detail.this);
        dialog.setContentView(R.layout.add_task_dialog);
        Objects.requireNonNull(dialog.getWindow()).setLayout(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ActionBar.LayoutParams.WRAP_CONTENT
        );
        dialog.getWindow().setBackgroundDrawable(getDrawable(R.drawable.dialog_bg));
        dialog.setCancelable(true);

        // Get views from the dialog
        TextInputEditText taskNameInput = dialog.findViewById(R.id.taskNameInput);
        MaterialAutoCompleteTextView statusInput = dialog.findViewById(R.id.statusInput);
        Button submitBtn = dialog.findViewById(R.id.btn);

        projectId = getIntent().getStringExtra("projectId");
        String title = getIntent().getStringExtra("title");

        // Validate projectId and title
        if (projectId == null || title == null) {
            Toast.makeText(this, "Project details are missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Set project title
        TextView titleView = findViewById(R.id.ProjName);
        titleView.setText(title);

        // Back Button functionality
        backBtn.setOnClickListener(v -> finish());

        // Open task dialog
        openTasksDialog.setOnClickListener(view -> dialog.show());

        // Handle task submission
        submitBtn.setOnClickListener(v -> {
            String taskName = Objects.requireNonNull(taskNameInput.getText()).toString().trim();
            String status = Objects.requireNonNull(statusInput.getText()).toString().trim();

            if (taskName.isEmpty() || status.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            // Create task data map
            Map<String, Object> taskData = new HashMap<>();
            taskData.put("taskName", taskName);
            taskData.put("status", status);

            // Save task as a subcollection of the current project
            db.collection("projects")
                    .document(projectId)
                    .collection("tasks")
                    .add(taskData)
                    .addOnSuccessListener(docRef -> {
                        Toast.makeText(this, "Task added successfully!", Toast.LENGTH_SHORT).show();
                        dialog.dismiss(); // Close dialog
                        taskNameInput.setText("");
                        statusInput.setText("");
                        // Reload tasks after adding a new one
                        fetchTasks();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });
    }

    // Fetch tasks from Firestore and update the RecyclerView
    @SuppressLint("NotifyDataSetChanged")
    private void fetchTasks() {
        db.collection("projects")
                .document(projectId)
                .collection("tasks")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    taskList.clear(); // Clear existing tasks
                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        String taskName = document.getString("taskName");
                        String status = document.getString("status");

                        Task task = new Task(taskName, status);
                        taskList.add(task); // Add task to the list
                    }
                    taskAdapter.notifyDataSetChanged(); // Notify adapter to update the RecyclerView
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error fetching tasks: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    protected void onStart() {
        super.onStart();
        fetchTasks();
    }
}
