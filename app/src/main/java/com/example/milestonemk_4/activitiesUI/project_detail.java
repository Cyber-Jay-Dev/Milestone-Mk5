package com.example.milestonemk_4.activitiesUI;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Dialog;
import android.content.ClipData;
import android.os.Bundle;
import android.util.Log;
import android.view.DragEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.milestonemk_4.Adapter.TaskAdapter;
import com.example.milestonemk_4.Adapter.UserAutoCompleteAdapter;
import com.example.milestonemk_4.R;
import com.example.milestonemk_4.model.Project;
import com.example.milestonemk_4.model.Task;
import com.example.milestonemk_4.model.User;
import com.example.milestonemk_4.utils.TaskCompletionReminder;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class project_detail extends AppCompatActivity {

    private static final String TAG = "ProjectDetail";
    Dialog dialog;
    FirebaseFirestore db;
    String projectId;

    RecyclerView toDoRecyclerView, inProgressRecyclerView, completedRecyclerView;
    TaskAdapter toDoAdapter, inProgressAdapter, completedAdapter;
    List<Task> toDoList, inProgressList, completedList;
    List<User> userList;

    private Task draggedTask;

    @SuppressLint("UseCompatLoadingForDrawables")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_project_detail);

        db = FirebaseFirestore.getInstance();
        userList = new ArrayList<>();

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

        // Initialize dialog but don't show it yet - we'll use the enhanced version
        dialog = new Dialog(this);
        dialog.setContentView(R.layout.add_task_dialog);
        Objects.requireNonNull(dialog.getWindow()).setLayout(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ActionBar.LayoutParams.WRAP_CONTENT
        );
        dialog.getWindow().setBackgroundDrawable(getDrawable(R.drawable.dialog_bg));
        dialog.setCancelable(true);

        backBtn.setOnClickListener(v -> finish());
        openTasksDialog.setOnClickListener(view -> showAddTaskDialog(projectId));

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
                    String oldStage = draggedTask.getStage();
                    toDoList.remove(draggedTask);
                    inProgressList.remove(draggedTask);
                    completedList.remove(draggedTask);

                    draggedTask.setStage(targetStage);
                    targetList.add(draggedTask);

                    updateTaskStageInFirestore(draggedTask);

                    // Handle task reminder logic
                    handleTaskReminder(draggedTask, oldStage, targetStage);

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

    // New method to handle task reminder logic
    private void handleTaskReminder(Task task, String oldStage, String newStage) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            return;
        }

        // Only process reminders for tasks assigned to the current user
        if (!currentUser.getUid().equals(task.getAssignedUserId())) {
            return;
        }

        // For tasks moved to "In Progress", schedule a reminder
        if (newStage.equals("In Progress")) {
            // Schedule a reminder for 3 hours later
            TaskCompletionReminder.scheduleTaskReminder(this, projectId, task.getTaskName());
            Toast.makeText(this, "Reminder set for 3 hours", Toast.LENGTH_SHORT).show();
        }

        // For tasks moved to "Completed", cancel any existing reminder
        else if (newStage.equals("Completed")) {
            // Cancel reminder as task is completed
            TaskCompletionReminder.cancelTaskReminder(this, task.getTaskName());
        }
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

    // Enhanced task dialog with required user assignment
    private void showAddTaskDialog(String projectId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.add_task_dialog, null);
        builder.setView(dialogView);

        EditText taskNameInput = dialogView.findViewById(R.id.taskNameInput);
        MaterialAutoCompleteTextView statusInput = dialogView.findViewById(R.id.statusInput);
        MaterialAutoCompleteTextView assignedUserInput = dialogView.findViewById(R.id.assignedUserInput);
        Button submitBtn = dialogView.findViewById(R.id.btn);

        // Set up status dropdown
        String[] statusOptions = getResources().getStringArray(R.array.option_list);
        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, statusOptions);
        statusInput.setAdapter(statusAdapter);

        // Fetch allowed users for this project
        fetchAllowedUsers(projectId, users -> {
            // Set up user dropdown
            UserAutoCompleteAdapter userAdapter = new UserAutoCompleteAdapter(this, users);
            assignedUserInput.setAdapter(userAdapter);

            // If no users are available, show a message and disable the submit button
            if (users.isEmpty()) {
                Toast.makeText(this, "No users available for assignment. Please add users to the project first.", Toast.LENGTH_LONG).show();
                submitBtn.setEnabled(false);
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();

        submitBtn.setOnClickListener(v -> {
            String taskName = taskNameInput.getText().toString().trim();
            String status = statusInput.getText().toString().trim();
            String assignedUsername = assignedUserInput.getText().toString().trim();

            // Validate inputs
            if (taskName.isEmpty()) {
                taskNameInput.setError("Task name is required");
                return;
            }
            if (status.isEmpty()) {
                statusInput.setError("Status is required");
                return;
            }
            if (assignedUsername.isEmpty()) {
                assignedUserInput.setError("User assignment is required");
                return;
            }

            // Find the selected user
            User selectedUser = null;
            for (User user : userList) {
                if (user.getUsername().equals(assignedUsername)) {
                    selectedUser = user;
                    break;
                }
            }

            // Validate user selection
            if (selectedUser == null) {
                assignedUserInput.setError("Please select a valid user");
                return;
            }

            // Create and save task
            saveTask(taskName, status, selectedUser);
            dialog.dismiss();
        });
    }

    // Save task with user assignment
    private void saveTask(String taskName, String status, User assignedUser) {
        Map<String, Object> taskData = new HashMap<>();
        taskData.put("taskName", taskName);
        taskData.put("status", status);
        taskData.put("stage", "To Do"); // Default stage for new tasks

        // Add required user assignment
        taskData.put("assignedUserId", assignedUser.getUid());
        taskData.put("assignedUsername", assignedUser.getUsername());

        db.collection("projects")
                .document(projectId)
                .collection("tasks")
                .add(taskData)
                .addOnSuccessListener(docRef -> {
                    Toast.makeText(this, "Task added successfully!", Toast.LENGTH_SHORT).show();
                    fetchTasks();

                    // No need to schedule reminder here as new tasks start in "To Do"
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // Method to fetch allowed users for a project
    private void fetchAllowedUsers(String projectId, OnUsersLoadedListener listener) {
        userList.clear();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // First, get the project to retrieve the allowed users list
        db.collection("projects").document(projectId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Project project = documentSnapshot.toObject(Project.class);
                        if (project != null && project.getAllowedUsers() != null && !project.getAllowedUsers().isEmpty()) {
                            // Project has allowed users, fetch their details
                            for (String userId : project.getAllowedUsers()) {
                                db.collection("users").document(userId).get()
                                        .addOnSuccessListener(userDoc -> {
                                            if (userDoc.exists()) {
                                                String uid = userDoc.getId();
                                                String username = userDoc.getString("username");
                                                String email = userDoc.getString("email");
                                                Long avatarIdLong = userDoc.getLong("avatarId");
                                                String bgColor = userDoc.getString("profileBgColor");

                                                int avatarId = avatarIdLong != null ? avatarIdLong.intValue() : 0;

                                                User user = new User(uid, username, email, avatarId, bgColor);
                                                userList.add(user);

                                                // Check if we've loaded all users
                                                if (userList.size() == project.getAllowedUsers().size()) {
                                                    listener.onUsersLoaded(userList);
                                                }
                                            }
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e("FetchUsers", "Error loading user: " + e.getMessage());
                                            // If we fail to load a user, still notify listener to prevent UI from hanging
                                            if (userList.isEmpty()) {
                                                listener.onUsersLoaded(userList);
                                            }
                                        });
                            }
                        } else {
                            // No allowed users, return an empty list
                            listener.onUsersLoaded(userList);
                        }
                    } else {
                        // Project not found, return an empty list
                        listener.onUsersLoaded(userList);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("FetchUsers", "Error loading project: " + e.getMessage());
                    listener.onUsersLoaded(userList);
                });
    }

    // Interface to handle async user loading
    interface OnUsersLoadedListener {
        void onUsersLoaded(List<User> users);
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
                        String assignedUserId = doc.getString("assignedUserId");
                        String assignedUsername = doc.getString("assignedUsername");

                        Task task = new Task(taskName, status, stage);

                        // Set assigned user information
                        task.setAssignedUserId(assignedUserId);
                        task.setAssignedUsername(assignedUsername);

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