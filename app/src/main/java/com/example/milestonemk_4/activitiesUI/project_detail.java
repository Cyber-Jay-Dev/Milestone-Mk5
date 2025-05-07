package com.example.milestonemk_4.activitiesUI;

import android.annotation.SuppressLint;

import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.View;
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
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.milestonemk_4.Adapter.AttachmentAdapter;
import com.example.milestonemk_4.Adapter.FilePreviewAdapter;
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class project_detail extends AppCompatActivity {

    private static final String TAG = "ProjectDetail";
    FirebaseFirestore db;
    String projectId;

    RecyclerView toDoRecyclerView, inProgressRecyclerView, completedRecyclerView;
    TaskAdapter toDoAdapter, inProgressAdapter, completedAdapter;
    List<Task> toDoList, inProgressList, completedList;
    List<User> userList;

    private Task draggedTask;
    private Task currentCompletedTask;
    private ArrayList<Uri> selectedFiles = new ArrayList<>();

    private ActivityResultLauncher<String[]> filePickerLauncher;

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

    @SuppressLint("UseCompatLoadingForDrawables")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_project_detail);

        initializeVariables();
        setupFilePickerLauncher();
        setupAutoScrollHandling();
        setupClickListeners();
        initializeTaskLists();
        validateAndSetProjectDetails();
        setupRecyclerViews();
        setupDragListeners();
    }

    private void initializeVariables() {
        db = FirebaseFirestore.getInstance();
        userList = new ArrayList<>();
        screenWidth = getResources().getDisplayMetrics().widthPixels;
        selectedFiles = new ArrayList<>();
    }

    private void setupFilePickerLauncher() {
        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenMultipleDocuments(),
                uris -> {
                    if (uris != null && !uris.isEmpty()) {
                        selectedFiles.clear();
                        selectedFiles.addAll(uris);
                        showFilePreviewDialog(selectedFiles);
                    }
                }
        );
    }
    private void setupAutoScrollHandling() {
        autoScrollHandler = new Handler(Looper.getMainLooper());
        horizontalScrollView = findViewById(R.id.horizontalScrollView);
    }
    private void setupClickListeners() {
        ImageButton openTasksDialog = findViewById(R.id.openTaskDialog);
        Button backBtn = findViewById(R.id.backBtn);

        backBtn.setOnClickListener(v -> finish());
        openTasksDialog.setOnClickListener(view -> showAddTaskDialog(projectId));
    }
    private void validateAndSetProjectDetails() {
        projectId = getIntent().getStringExtra("projectId");
        String title = getIntent().getStringExtra("title");

        if (projectId == null || title == null) {
            Toast.makeText(this, "Project details are missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        TextView titleView = findViewById(R.id.ProjName);
        titleView.setText(title);
    }
    private void initializeTaskLists() {
        toDoList = new ArrayList<>();
        inProgressList = new ArrayList<>();
        completedList = new ArrayList<>();

        toDoAdapter = new TaskAdapter(toDoList);
        inProgressAdapter = new TaskAdapter(inProgressList);
        completedAdapter = new TaskAdapter(completedList);
    }

    private void setupDragInteractions() {
        toDoAdapter.setOnItemLongClickListener((task, view) -> startDrag(view, task));
        inProgressAdapter.setOnItemLongClickListener((task, view) -> startDrag(view, task));
        completedAdapter.setOnItemLongClickListener((task, view) -> startDrag(view, task));
    }

    private void showSendFilesDialog(Task task) {
        // Store current task being completed
        currentCompletedTask = task;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.send_files_dialog, null);
        builder.setView(dialogView);

        Button btnSkip = dialogView.findViewById(R.id.btn_skip);
        Button btnSendFiles = dialogView.findViewById(R.id.btn_send_files);

        AlertDialog sendFilesDialog = builder.create();
        sendFilesDialog.setCancelable(false); // Force user to make a choice

        btnSkip.setOnClickListener(v -> {
            // Just close the dialog
            sendFilesDialog.dismiss();
            Toast.makeText(this, "Task marked complete!", Toast.LENGTH_SHORT).show();
        });

        btnSendFiles.setOnClickListener(v -> {
            // Close dialog and launch file picker
            sendFilesDialog.dismiss();
            launchFilePicker();
        });

        sendFilesDialog.show();
    }

    // Method to launch the file picker
    private void launchFilePicker() {
        try {
            // Launch file picker to select multiple files
            filePickerLauncher.launch(new String[]{"*/*"});
        } catch (Exception e) {
            Log.e(TAG, "Error launching file picker: " + e.getMessage());
            Toast.makeText(this, "Error opening file picker", Toast.LENGTH_SHORT).show();
        }
    }

    // Method to show preview of selected files
    private void showFilePreviewDialog(ArrayList<Uri> files) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.file_preview_dialog, null);
        builder.setView(dialogView);

        RecyclerView fileRecyclerView = dialogView.findViewById(R.id.fileRecyclerView);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel);
        Button btnUpload = dialogView.findViewById(R.id.btn_upload);

        // Setup recycler view with file names
        fileRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        FilePreviewAdapter fileAdapter = new FilePreviewAdapter(this, files);
        fileRecyclerView.setAdapter(fileAdapter);

        AlertDialog previewDialog = builder.create();

        btnCancel.setOnClickListener(v -> {
            selectedFiles.clear();
            previewDialog.dismiss();
        });

        btnUpload.setOnClickListener(v -> {
            previewDialog.dismiss();
            // Upload files to Google Drive
            uploadFilesToDrive(files);
        });

        previewDialog.show();
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
                return true;

            case DragEvent.ACTION_DRAG_ENTERED:
                // Visual feedback could be added here
                return true;

            case DragEvent.ACTION_DRAG_EXITED:
                // Reset visual feedback if any
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

                    // Show the send files dialog if the task is moved to Completed
                    // and it belongs to the current user
                    if (targetStage.equals("Completed")) {
                        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                        if (currentUser != null && currentUser.getUid().equals(draggedTask.getAssignedUserId())) {
                            // Show dialog to send files
                            showSendFilesDialog(draggedTask);
                        }
                    }

                    // Notify all adapters that data has changed
                    toDoAdapter.notifyDataSetChanged();
                    inProgressAdapter.notifyDataSetChanged();
                    completedAdapter.notifyDataSetChanged();

                    draggedTask = null;
                    isDragging = false;
                }

                // Stop auto-scrolling after drop
                stopAutoScroll();
                return true;

            case DragEvent.ACTION_DRAG_ENDED:
                draggedTask = null;
                isDragging = false;
                // Stop auto-scrolling after drag ends
                stopAutoScroll();
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

    private void setupRecyclerViews() {
        toDoRecyclerView = findViewById(R.id.taskRecyclerview);
        inProgressRecyclerView = findViewById(R.id.inProgressRecyclerView);
        completedRecyclerView = findViewById(R.id.completedRecyclerView);

        toDoRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        inProgressRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        completedRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        toDoRecyclerView.setAdapter(toDoAdapter);
        inProgressRecyclerView.setAdapter(inProgressAdapter);
        completedRecyclerView.setAdapter(completedAdapter);

        setupDragInteractions();
        // Add click listener for completed tasks to view attachments
        completedAdapter.setOnItemClickListener(this::showTaskAttachmentsDialog);
    }
    private void showTaskAttachmentsDialog(Task task) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.task_attachment_dialog, null);
        builder.setView(dialogView);

        TextView dialogTitle = dialogView.findViewById(R.id.dialog_title);
        dialogTitle.setText("Attachments: " + task.getTaskName());

        TextView noAttachmentsText = dialogView.findViewById(R.id.no_attachments_text);
        RecyclerView attachmentsRecyclerView = dialogView.findViewById(R.id.attachments_recycler_view);
        Button btnClose = dialogView.findViewById(R.id.btn_close);

        List<Map<String, String>> attachments = task.getAttachments();

        if (attachments == null || attachments.isEmpty()) {
            noAttachmentsText.setVisibility(View.VISIBLE);
            attachmentsRecyclerView.setVisibility(View.GONE);
        } else {
            noAttachmentsText.setVisibility(View.GONE);
            attachmentsRecyclerView.setVisibility(View.VISIBLE);

            // Set up RecyclerView
            attachmentsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
            AttachmentAdapter attachmentAdapter = new AttachmentAdapter(this, attachments);
            attachmentsRecyclerView.setAdapter(attachmentAdapter);
        }

        AlertDialog dialog = builder.create();

        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }
    private void uploadFilesToDrive(ArrayList<Uri> files) {
        try {
            // Create a share intent with multiple files
            Intent shareIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
            shareIntent.setType("*/*");

            // Try to set Google Drive as target app
            shareIntent.setPackage("com.google.android.apps.docs");

            // Create ArrayList of URIs for attachments
            ArrayList<Uri> contentUris = new ArrayList<>();

            // List to store file information
            List<Map<String, String>> fileAttachments = new ArrayList<>();

            // Add all selected files to the intent
            for (Uri fileUri : files) {
                try {
                    // Create a file in app's cache directory
                    String fileName = getFileNameFromUri(fileUri);
                    File cacheFile = createCacheFile(fileUri, fileName);

                    // Get content URI through FileProvider
                    Uri contentUri = FileProvider.getUriForFile(
                            this,
                            "com.example.milestonemk_4.fileprovider", // Must match the authority in your manifest
                            cacheFile
                    );

                    // Add to list with permission
                    contentUris.add(contentUri);

                    // Save the content URI as string
                    Map<String, String> fileInfo = new HashMap<>();
                    fileInfo.put("name", fileName);

                    // Store the content URI as string - make sure it includes the scheme and authority
                    String uriString = contentUri.toString();
                    fileInfo.put("uri", uriString);

                    Log.d(TAG, "Adding attachment: name=" + fileName + ", uri=" + uriString);
                    fileAttachments.add(fileInfo);

                } catch (Exception e) {
                    Log.e(TAG, "Error preparing file: " + e.getMessage(), e);
                }
            }

            // Put URIs as extra
            shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, contentUris);

            // Add task name as subject
            if (currentCompletedTask != null) {
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Files for task: " + currentCompletedTask.getTaskName());

                // Save file attachments to the task in Firestore
                saveAttachmentsToTask(currentCompletedTask, fileAttachments);
            }

            // Grant read permissions
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            // Try to start the activity
            try {
                startActivity(shareIntent);
            } catch (ActivityNotFoundException e) {
                // If Google Drive app is not found, try a more generic intent
                Intent genericShareIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
                genericShareIntent.setType("*/*");
                genericShareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, contentUris);
                startActivity(Intent.createChooser(genericShareIntent, "Upload files using"));
            }

            Toast.makeText(this, "Preparing files for upload", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Log.e(TAG, "Error uploading files: " + e.getMessage(), e);
            Toast.makeText(this, "Error preparing files for upload", Toast.LENGTH_SHORT).show();
        }
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

                        // Retrieve attachments from Firestore
                        List<Map<String, String>> attachments = (List<Map<String, String>>) doc.get("attachments");

                        Task task = new Task(taskName, status, stage);

                        // Set assigned user information
                        task.setAssignedUserId(assignedUserId);
                        task.setAssignedUsername(assignedUsername);

                        // Set attachments if they exist
                        if (attachments != null && !attachments.isEmpty()) {
                            task.setAttachments(attachments);
                        }

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
    private String getFileNameFromUri(Uri uri) {
        String result = null;
        if (uri.getScheme() != null && uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex >= 0) {
                        result = cursor.getString(nameIndex);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error getting filename from URI: " + e.getMessage(), e);
            }
        }
        if (result == null) {
            result = uri.getLastPathSegment();
        }
        return result != null ? result : "unknown_file";
    }

    private File createCacheFile(Uri sourceUri, String fileName) throws IOException {
        // Create cache directory if it doesn't exist
        File cacheDir = new File(getCacheDir(), "shared_files");
        if (!cacheDir.exists()) {
            cacheDir.mkdirs();
        }

        // Create the file in cache - use a unique name to avoid conflicts
        String uniqueFileName = System.currentTimeMillis() + "_" + fileName;
        File destFile = new File(cacheDir, uniqueFileName);

        // Copy content from uri to the cache file
        try (InputStream inputStream = getContentResolver().openInputStream(sourceUri);
             FileOutputStream outputStream = new FileOutputStream(destFile)) {

            if (inputStream == null) {
                throw new IOException("Failed to open input stream");
            }

            byte[] buffer = new byte[4096];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
            outputStream.flush();

            // Make sure the file is readable by other apps
            destFile.setReadable(true, false);

            Log.d(TAG, "Created cache file: " + destFile.getAbsolutePath());
            return destFile;
        }
    }

    private void saveAttachmentsToTask(Task task, List<Map<String, String>> attachments) {
        if (task == null || attachments == null || attachments.isEmpty()) {
            return;
        }

        Log.d(TAG, "Saving " + attachments.size() + " attachments to task: " + task.getTaskName());

        // Find the task document in Firestore
        db.collection("projects")
                .document(projectId)
                .collection("tasks")
                .whereEqualTo("taskName", task.getTaskName())
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.isEmpty()) {
                        DocumentSnapshot taskDoc = snapshot.getDocuments().get(0);

                        // Get existing attachments if any
                        List<Map<String, String>> existingAttachments =
                                (List<Map<String, String>>) taskDoc.get("attachments");

                        // Combine with new attachments
                        List<Map<String, String>> allAttachments = new ArrayList<>();
                        if (existingAttachments != null) {
                            allAttachments.addAll(existingAttachments);
                        }
                        allAttachments.addAll(attachments);

                        // Log for debugging
                        for (Map<String, String> attachment : allAttachments) {
                            Log.d(TAG, "Attachment being saved: " + attachment.get("name") +
                                    " URI: " + attachment.get("uri"));
                        }

                        // Update the task with all attachments
                        db.collection("projects")
                                .document(projectId)
                                .collection("tasks")
                                .document(taskDoc.getId())
                                .update("attachments", allAttachments)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "Attachments saved successfully", Toast.LENGTH_SHORT).show();
                                    // Update local task object
                                    task.setAttachments(allAttachments);
                                    // Refresh the task list
                                    fetchTasks();
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Error saving attachments: " + e.getMessage(), e);
                                    Toast.makeText(this, "Failed to save attachments", Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        Log.e(TAG, "Task not found in Firestore");
                        Toast.makeText(this, "Task not found, could not save attachments", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error finding task: " + e.getMessage(), e);
                    Toast.makeText(this, "Error finding task", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    protected void onStart() {
        super.onStart();
        fetchTasks();
    }

    @Override
    protected void onPause() {
        super.onPause();
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