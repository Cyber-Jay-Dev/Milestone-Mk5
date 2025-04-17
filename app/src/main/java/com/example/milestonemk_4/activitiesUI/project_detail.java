package com.example.milestonemk_4.activitiesUI;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Dialog;
import android.content.ClipData;
import android.os.Bundle;
import android.view.DragEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
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
    String projectId;

    RecyclerView toDoRecyclerView, inProgressRecyclerView, completedRecyclerView;
    TaskAdapter toDoAdapter, inProgressAdapter, completedAdapter;
    List<Task> toDoList, inProgressList, completedList;

    private Task draggedTask;

    @SuppressLint("UseCompatLoadingForDrawables")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_project_detail);

        db = FirebaseFirestore.getInstance();

        ImageButton openTasksDialog = findViewById(R.id.openTaskDialog);
        Button backBtn = findViewById(R.id.backBtn);
        toDoRecyclerView = findViewById(R.id.taskRecyclerview);
        inProgressRecyclerView = findViewById(R.id.inProgressRecyclerView);
        completedRecyclerView = findViewById(R.id.completedRecyclerView);

        projectId = getIntent().getStringExtra("projectId");
        String title = getIntent().getStringExtra("title");

        if (projectId == null || title == null) {
            Toast.makeText(this, "Project details are missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        TextView titleView = findViewById(R.id.ProjName);
        titleView.setText(title);

        dialog = new Dialog(this);
        dialog.setContentView(R.layout.add_task_dialog);
        Objects.requireNonNull(dialog.getWindow()).setLayout(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ActionBar.LayoutParams.WRAP_CONTENT
        );
        dialog.getWindow().setBackgroundDrawable(getDrawable(R.drawable.dialog_bg));
        dialog.setCancelable(true);

        TextInputEditText taskNameInput = dialog.findViewById(R.id.taskNameInput);
        MaterialAutoCompleteTextView statusInput = dialog.findViewById(R.id.statusInput);
        Button submitBtn = dialog.findViewById(R.id.btn);

        backBtn.setOnClickListener(v -> finish());
        openTasksDialog.setOnClickListener(view -> dialog.show());

        toDoList = new ArrayList<>();
        inProgressList = new ArrayList<>();
        completedList = new ArrayList<>();

        toDoAdapter = new TaskAdapter(toDoList);
        inProgressAdapter = new TaskAdapter(inProgressList);
        completedAdapter = new TaskAdapter(completedList);

        toDoRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        inProgressRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        completedRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        toDoRecyclerView.setAdapter(toDoAdapter);
        inProgressRecyclerView.setAdapter(inProgressAdapter);
        completedRecyclerView.setAdapter(completedAdapter);

        // Set long click listeners to start drag
        toDoAdapter.setOnItemLongClickListener((task, view) -> startDrag(view, task));
        inProgressAdapter.setOnItemLongClickListener((task, view) -> startDrag(view, task));
        completedAdapter.setOnItemLongClickListener((task, view) -> startDrag(view, task));

        // Set drag listeners
        toDoRecyclerView.setOnDragListener((v, event) -> handleDrag(event, "To Do", toDoList, toDoAdapter));
        inProgressRecyclerView.setOnDragListener((v, event) -> handleDrag(event, "In Progress", inProgressList, inProgressAdapter));
        completedRecyclerView.setOnDragListener((v, event) -> handleDrag(event, "Completed", completedList, completedAdapter));

        submitBtn.setOnClickListener(v -> {
            String taskName = Objects.requireNonNull(taskNameInput.getText()).toString().trim();
            String status = Objects.requireNonNull(statusInput.getText()).toString().trim();

            if (taskName.isEmpty() || status.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, Object> taskData = new HashMap<>();
            taskData.put("taskName", taskName);
            taskData.put("status", status);
            taskData.put("stage", "To Do");

            db.collection("projects")
                    .document(projectId)
                    .collection("tasks")
                    .add(taskData)
                    .addOnSuccessListener(docRef -> {
                        Toast.makeText(this, "Task added successfully!", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        taskNameInput.setText("");
                        statusInput.setText("");
                        fetchTasks();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        });
    }

    private void startDrag(View view, Task task) {
        if (view.getParent() != null) {
            draggedTask = task;
            View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(view);
            view.startDragAndDrop(ClipData.newPlainText("", ""), shadowBuilder, null, 0);
        }
    }


    @SuppressLint("NotifyDataSetChanged")
    private boolean handleDrag(DragEvent event, String targetStage, List<Task> targetList, TaskAdapter targetAdapter) {
        switch (event.getAction()) {
            case DragEvent.ACTION_DRAG_STARTED:
                return true;
            case DragEvent.ACTION_DROP:
                if (draggedTask != null) {
                    toDoList.remove(draggedTask);
                    inProgressList.remove(draggedTask);
                    completedList.remove(draggedTask);

                    draggedTask.setStage(targetStage);
                    targetList.add(draggedTask);

                    updateTaskStageInFirestore(draggedTask);

                    toDoAdapter.notifyDataSetChanged();
                    inProgressAdapter.notifyDataSetChanged();
                    completedAdapter.notifyDataSetChanged();

                    draggedTask = null;
                }
                return true;
            case DragEvent.ACTION_DRAG_ENDED:
                draggedTask = null;
                return true;
        }
        return false;
    }

    private void updateTaskStageInFirestore(Task task) {
        db.collection("projects")
                .document(projectId)
                .collection("tasks")
                .whereEqualTo("taskName", task.getTaskName())
                .get()
                .addOnSuccessListener(snapshot -> {
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        db.collection("projects")
                                .document(projectId)
                                .collection("tasks")
                                .document(doc.getId())
                                .update("stage", task.getStage());
                    }
                });
    }

    @SuppressLint("NotifyDataSetChanged")
    private void fetchTasks() {
        db.collection("projects")
                .document(projectId)
                .collection("tasks")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    toDoList.clear();
                    inProgressList.clear();
                    completedList.clear();

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        String taskName = doc.getString("taskName");
                        String status = doc.getString("status");
                        String stage = doc.getString("stage");

                        Task task = new Task(taskName, status, stage);
                        if (stage == null || stage.equals("To Do")) {
                            toDoList.add(task);
                        } else if (stage.equals("In Progress")) {
                            inProgressList.add(task);
                        } else if (stage.equals("Completed")) {
                            completedList.add(task);
                        }
                    }

                    toDoAdapter.notifyDataSetChanged();
                    inProgressAdapter.notifyDataSetChanged();
                    completedAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    @Override
    protected void onStart() {
        super.onStart();
        fetchTasks();
    }
}