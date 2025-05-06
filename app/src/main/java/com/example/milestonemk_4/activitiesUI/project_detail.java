package com.example.milestonemk_4.activitiesUI;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Dialog;
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.DragEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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
import com.example.milestonemk_4.service.DropboxService;
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

    // Auto-scroll related variables
    private HorizontalScrollView horizontalScrollView;
    private Handler autoScrollHandler;
    private Runnable autoScrollRunnable;
    private boolean isAutoScrolling = false;

    // Improved scroll threshold and speed settings
    private static final int SCROLL_THRESHOLD = 200;
    private static final int SCROLL_SPEED_SLOW = 8;
    private static final int SCROLL_SPEED_MEDIUM = 15;
    private static final int SCROLL_SPEED_FAST = 25;
    private static final int SCROLL_ACCELERATE_THRESHOLD = 100;
    private static final int SCROLL_FAST_THRESHOLD = 50;
    private static final int SCROLL_DELAY = 10;

    private int screenWidth;
    private float lastTouchX;

    private boolean isDragging = false;

    private DropboxService dropboxService;
    private Uri selectedFileUri;
    private Task taskForUpload;
    private ActivityResultLauncher<String> filePickerLauncher;

    @SuppressLint("UseCompatLoadingForDrawables")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_project_detail);

        db = FirebaseFirestore.getInstance();
        userList = new ArrayList<>();

        // Set up auto-scroll handling
        autoScrollHandler = new Handler(Looper.getMainLooper());
        horizontalScrollView = findViewById(R.id.horizontalScrollView);

        // Get screen width for scroll calculations
        screenWidth = getResources().getDisplayMetrics().widthPixels;

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

        // Set drag listeners with improved implementation
        setupDragListeners();

        dropboxService = new DropboxService(this);

        // Initialize the file picker launcher
        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        selectedFileUri = uri;
                        if (taskForUpload != null) {
                            showDropboxUploadDialog(taskForUpload);
                        }
                    }
                });
    }
    private void showDropboxAuthPrompt() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Dropbox Upload");
        builder.setMessage("To upload files to Dropbox, you need to connect your Dropbox account first.");
        builder.setPositiveButton("Connect", (dialog, which) -> {
            Intent intent = new Intent(this, DropboxAuthActivity.class);
            startActivity(intent);
        });
        builder.setNegativeButton("Skip", null);
        builder.show();
    }
    private void showDropboxFilePickerDialog(Task task) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Upload to Dropbox");
        builder.setMessage("Task completed! Would you like to upload files related to this task to Dropbox?");
        builder.setPositiveButton("Choose File", (dialog, which) -> {
            taskForUpload = task;
            filePickerLauncher.launch("*/*");
        });
        builder.setNegativeButton("Skip", null);
        builder.show();
    }
    private void showDropboxUploadDialog(Task task) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_dropbox_upload, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);

        TextView fileNameText = dialogView.findViewById(R.id.fileNameText);
        Button pickFileButton = dialogView.findViewById(R.id.pickFileButton);
        Button uploadButton = dialogView.findViewById(R.id.uploadButton);
        Button cancelButton = dialogView.findViewById(R.id.cancelButton);

        // Get filename from Uri
        String fileName = getFileNameFromUri(selectedFileUri);
        fileNameText.setText(fileName);

        AlertDialog dialog = builder.create();
        dialog.show();

        pickFileButton.setOnClickListener(v -> {
            filePickerLauncher.launch("*/*");
            dialog.dismiss();
        });

        uploadButton.setOnClickListener(v -> {
            if (selectedFileUri != null) {
                // Show progress
                dialog.dismiss();
                AlertDialog progressDialog = new AlertDialog.Builder(this)
                        .setTitle("Uploading")
                        .setMessage("Uploading file to Dropbox...")
                        .setCancelable(false)
                        .create();
                progressDialog.show();

                // Upload the file
                dropboxService.uploadFile(selectedFileUri, task.getTaskName(), new DropboxService.UploadCallback() {
                    @Override
                    public void onUploadSuccess(String fileName, String path) {
                        runOnUiThread(() -> {
                            progressDialog.dismiss();
                            Toast.makeText(project_detail.this,
                                    "File uploaded to " + path, Toast.LENGTH_LONG).show();
                        });
                    }

                    @Override
                    public void onUploadFailure(String errorMessage) {
                        runOnUiThread(() -> {
                            progressDialog.dismiss();
                            Toast.makeText(project_detail.this,
                                    "Upload failed: " + errorMessage, Toast.LENGTH_LONG).show();
                        });
                    }
                });
            } else {
                Toast.makeText(this, "Please select a file first", Toast.LENGTH_SHORT).show();
            }
        });

        cancelButton.setOnClickListener(v -> dialog.dismiss());
    }

    // Helper method to get file name from Uri
    private String getFileNameFromUri(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int columnIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                    if (columnIndex != -1) {
                        result = cursor.getString(columnIndex);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Could not get file name", e);
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.project_detail_menu, null);
        menu.add(Menu.NONE, 1, Menu.NONE, "Dropbox Settings");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == 1) {
            // Open Dropbox settings
            Intent intent = new Intent(this, DropboxAuthActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupDragListeners() {
        // Set drag listeners for RecyclerViews
        View.OnDragListener dragListener = (v, event) -> {
            // Handle touch coordinates for auto-scroll during drag operation
            if (isDragging && (event.getAction() == DragEvent.ACTION_DRAG_LOCATION
                    || event.getAction() == DragEvent.ACTION_DRAG_ENTERED)) {
                lastTouchX = event.getX() + ((View) v.getParent()).getLeft();
                handleTouchForAutoScroll();
            }

            // Determine which RecyclerView is involved
            if (v == toDoRecyclerView) {
                return handleDrag(event, "To Do", toDoList, toDoAdapter);
            } else if (v == inProgressRecyclerView) {
                return handleDrag(event, "In Progress", inProgressList, inProgressAdapter);
            } else if (v == completedRecyclerView) {
                return handleDrag(event, "Completed", completedList, completedAdapter);
            }
            return false;
        };

        // Apply the same drag listener to all RecyclerViews
        toDoRecyclerView.setOnDragListener(dragListener);
        inProgressRecyclerView.setOnDragListener(dragListener);
        completedRecyclerView.setOnDragListener(dragListener);

        // Global drag listener for tracking drag state
        View mainContainer = findViewById(R.id.mainContainer);
        mainContainer.setOnDragListener((v, event) -> {
            switch (event.getAction()) {
                case DragEvent.ACTION_DRAG_STARTED:
                    isDragging = true;
                    return true;

                case DragEvent.ACTION_DRAG_LOCATION:
                    if (isDragging) {
                        // Update last touch position for auto-scroll
                        lastTouchX = event.getX();
                        handleTouchForAutoScroll();
                    }
                    return true;

                case DragEvent.ACTION_DRAG_ENDED:
                case DragEvent.ACTION_DROP:
                    isDragging = false;
                    stopAutoScroll();
                    return true;
            }
            return false;  // Don't consume the event
        });
    }

    private void handleTouchForAutoScroll() {
        if (!isDragging) return;

        // Calculate if we're near the edges
        int scrollX = horizontalScrollView.getScrollX();
        int maxScrollX = horizontalScrollView.getChildAt(0).getWidth() - horizontalScrollView.getWidth();

        // Get absolute position within the screen
        int[] location = new int[2];
        horizontalScrollView.getLocationOnScreen(location);
        int absoluteX = (int) lastTouchX + location[0];

        // Calculate appropriate scroll speed based on proximity to edge
        int scrollSpeed = 0;

        // Near left edge logic with improved three-tier speed system
        if (absoluteX - location[0] < SCROLL_THRESHOLD) {
            if (absoluteX - location[0] < SCROLL_FAST_THRESHOLD) {
                // Very close to edge - fast scroll
                scrollSpeed = -SCROLL_SPEED_FAST;
            } else if (absoluteX - location[0] < SCROLL_ACCELERATE_THRESHOLD) {
                // Moderately close - medium scroll
                scrollSpeed = -SCROLL_SPEED_MEDIUM;
            } else {
                // Near edge - slower scroll
                scrollSpeed = -SCROLL_SPEED_SLOW;
            }
        }
        // Near right edge logic with improved three-tier speed system
        else if (absoluteX > location[0] + horizontalScrollView.getWidth() - SCROLL_THRESHOLD) {
            if (absoluteX > location[0] + horizontalScrollView.getWidth() - SCROLL_FAST_THRESHOLD) {
                // Very close to edge - fast scroll
                scrollSpeed = SCROLL_SPEED_FAST;
            } else if (absoluteX > location[0] + horizontalScrollView.getWidth() - SCROLL_ACCELERATE_THRESHOLD) {
                // Moderately close - medium scroll
                scrollSpeed = SCROLL_SPEED_MEDIUM;
            } else {
                // Near edge - slower scroll
                scrollSpeed = SCROLL_SPEED_SLOW;
            }
        }

        // Log scroll logic for debugging
        Log.d("AutoScroll", "Position: " + absoluteX + ", ScrollX: " + scrollX +
                ", MaxScroll: " + maxScrollX + ", Speed: " + scrollSpeed);

        // Apply scroll if needed
        if (scrollSpeed != 0) {
            startAutoScroll(scrollSpeed);
        } else {
            // Not near edges, stop auto-scrolling
            stopAutoScroll();
        }
    }

    private void startAutoScroll(int speed) {
        final int finalSpeed = speed;

        if (isAutoScrolling) {
            // Update the current speed in the runnable
            if (autoScrollRunnable != null) {
                // The runnable will pick up the new position on next iteration
                return;
            }
        }

        isAutoScrolling = true;
        autoScrollRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isAutoScrolling || !isDragging) {
                    stopAutoScroll();
                    return;
                }

                // Calculate current scroll position
                int scrollX = horizontalScrollView.getScrollX();
                int maxScrollX = horizontalScrollView.getChildAt(0).getWidth() - horizontalScrollView.getWidth();

                // Get current speed calculation based on edge proximity
                int currentSpeed = finalSpeed;

                // Check if we can scroll in the desired direction
                if ((currentSpeed > 0 && scrollX < maxScrollX) || (currentSpeed < 0 && scrollX > 0)) {
                    // Scroll by the calculated amount
                    horizontalScrollView.scrollBy(currentSpeed, 0);

                    // Log actual scrolling for debugging
                    Log.d("AutoScroll", "Scrolling with speed: " + currentSpeed +
                            ", ScrollX: " + horizontalScrollView.getScrollX());

                    // Schedule next scroll
                    autoScrollHandler.postDelayed(this, SCROLL_DELAY);
                } else {
                    // Can't scroll further in this direction
                    Log.d("AutoScroll", "Can't scroll further, stopping");
                    stopAutoScroll();
                }
            }
        };

        // Start the scroll runnable
        autoScrollHandler.post(autoScrollRunnable);
        Log.d("AutoScroll", "Started auto-scroll with speed: " + finalSpeed);
    }

    private void stopAutoScroll() {
        isAutoScrolling = false;
        if (autoScrollRunnable != null) {
            autoScrollHandler.removeCallbacks(autoScrollRunnable);
            autoScrollRunnable = null;
        }
    }

    private void startDrag(View view, Task task) {
        if (view.getParent() != null) {
            draggedTask = task;
            isDragging = true;
            View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(view);
            ClipData dragData = ClipData.newPlainText("task", task.getTaskName());

            // Always use global drag flags
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                view.startDragAndDrop(dragData, shadowBuilder, task, View.DRAG_FLAG_GLOBAL);
            } else {
                view.startDrag(dragData, shadowBuilder, task, 0);
            }

            Log.d("DragOperation", "Started drag for task: " + task.getTaskName());
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private boolean handleDrag(DragEvent event, String targetStage, List<Task> targetList, TaskAdapter targetAdapter) {
        switch (event.getAction()) {
            case DragEvent.ACTION_DRAG_STARTED:
                // Return true to indicate we're interested in the drag event
                return true;

            case DragEvent.ACTION_DRAG_ENTERED:
                // Visual feedback when drag enters this view
                Log.d("DragOperation", "Drag entered: " + targetStage);
                return true;

            case DragEvent.ACTION_DRAG_LOCATION:
                // Update coordinates for auto-scroll
                return true;

            case DragEvent.ACTION_DRAG_EXITED:
                // Visual feedback when drag exits this view
                Log.d("DragOperation", "Drag exited: " + targetStage);
                return true;

            case DragEvent.ACTION_DROP:
                if (draggedTask != null) {
                    String oldStage = draggedTask.getStage();

                    // Remove from all lists (to handle any case)
                    toDoList.remove(draggedTask);
                    inProgressList.remove(draggedTask);
                    completedList.remove(draggedTask);

                    // Add to the target list with the new stage
                    draggedTask.setStage(targetStage);
                    targetList.add(draggedTask);

                    // Update the database
                    updateTaskStageInFirestore(draggedTask);

                    // Handle task reminder logic
                    handleTaskReminder(draggedTask, oldStage, targetStage);

                    // Show Dropbox upload dialog when a task is moved to completed
                    if (targetStage.equals("Completed")) {
                        if (dropboxService.isAuthenticated()) {
                            // If authenticated, show file picker
                            showDropboxFilePickerDialog(draggedTask);
                        } else {
                            // If not authenticated, show auth prompt
                            showDropboxAuthPrompt();
                        }
                    }

                    // Notify all adapters that data has changed
                    toDoAdapter.notifyDataSetChanged();
                    inProgressAdapter.notifyDataSetChanged();
                    completedAdapter.notifyDataSetChanged();

                    Log.d("DragOperation", "Task dropped: " + draggedTask.getTaskName() + " to " + targetStage);
                    draggedTask = null;
                    isDragging = false;
                }

                // Stop auto-scrolling after drop
                stopAutoScroll();
                return true;

            case DragEvent.ACTION_DRAG_ENDED:
                // Clean up after drag operation ends, whether or not it resulted in a drop
                Log.d("DragOperation", "Drag ended");
                isDragging = false;
                stopAutoScroll();
                return true;

            default:
                return false;
        }
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
        taskData.put("stage", "To Do");

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

    @Override
    protected void onPause() {
        super.onPause();
        // Make sure to stop auto-scrolling when activity is paused
        stopAutoScroll();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Remove any callbacks to prevent memory leaks
        if (autoScrollHandler != null && autoScrollRunnable != null) {
            autoScrollHandler.removeCallbacks(autoScrollRunnable);
        }
    }
}