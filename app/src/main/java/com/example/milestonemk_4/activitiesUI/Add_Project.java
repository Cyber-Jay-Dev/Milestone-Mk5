package com.example.milestonemk_4.activitiesUI;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.os.Bundle;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.milestonemk_4.R;
import com.example.milestonemk_4.Adapter.CollaboratorAdapter;
import com.example.milestonemk_4.model.Collaborator;
import com.example.milestonemk_4.utils.CollaboratorNotificationService;
import com.example.milestonemk_4.utils.NetworkUtils;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.functions.FirebaseFunctions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Add_Project extends AppCompatActivity implements CollaboratorAdapter.OnCollaboratorListener {
    private EditText projectTitle;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private Dialog dialog;
    private final ArrayList<String> allowedUserUIDs = new ArrayList<>();
    private final ArrayList<Collaborator> collaboratorList = new ArrayList<>();
    private CollaboratorAdapter collaboratorAdapter;
    private String currentUserName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_project);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        projectTitle = findViewById(R.id.projectTitleInput);
        Button addProjectToDb = findViewById(R.id.addprojectItem);
        ImageButton backButton = findViewById(R.id.topbar_backBtn);
        ImageButton openDialog = findViewById(R.id.openAddDialog);

        // Initialize RecyclerView
        RecyclerView recyclerView = findViewById(R.id.UserProfiles);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        collaboratorAdapter = new CollaboratorAdapter(this, collaboratorList, this);
        recyclerView.setAdapter(collaboratorAdapter);

        backButton.setOnClickListener(view1 -> finish());

        dialog = new Dialog(this);
        dialog.setContentView(R.layout.add_collab_dialog);
        dialog.setCancelable(false);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            dialog.getWindow().setBackgroundDrawable(
                    ContextCompat.getDrawable(this, R.drawable.custom_dialog_bg));
        }

        //dialog stuff
        ImageButton closedialog = dialog.findViewById(R.id.close_dialog);
        EditText dialogEmailInput = dialog.findViewById(R.id.collabEmailInput);
        Button addCollabBtn = dialog.findViewById(R.id.addCollabBtn);
        closedialog.setOnClickListener(view -> dialog.dismiss());
        openDialog.setOnClickListener(view -> dialog.show());

        addCollabBtn.setOnClickListener(view -> {
            String emailCollab = dialogEmailInput.getText().toString().trim();

            if (emailCollab.isEmpty()) {
                Toast.makeText(this, "Please enter an email", Toast.LENGTH_SHORT).show();
                return;
            }

            searchEmail(emailCollab);
            dialogEmailInput.setText(""); // Clear input field after search
        });

        addProjectToDb.setOnClickListener(v -> {
            String title = projectTitle.getText().toString().trim();

            if (!title.isEmpty()) {
                addProjectToFirestore(title);
            } else {
                Toast.makeText(this, "Please enter a project title", Toast.LENGTH_SHORT).show();
            }
        });

        getCurrentUserInfo();
        addCurrentUserAsCollaborator();
    }

    private void getCurrentUserInfo() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            String uid = currentUser.getUid();
            db.collection("users").document(uid).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            currentUserName = documentSnapshot.getString("username");
                        }
                    });
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private void addCurrentUserAsCollaborator() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            String uid = currentUser.getUid();
            String email = currentUser.getEmail();

            // Add to allowed UIDs
            if (!allowedUserUIDs.contains(uid)) {
                allowedUserUIDs.add(uid);
            }

            // Get user info and add to display list
            db.collection("users").document(uid).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        String name = documentSnapshot.exists() ?
                                documentSnapshot.getString("username") :
                                "You (Owner)";

                        Collaborator currentUserCollab = new Collaborator(uid, email, name);
                        collaboratorList.add(currentUserCollab);
                        collaboratorAdapter.notifyDataSetChanged();
                    });
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private void searchEmail(String emailInput) {
        CollectionReference usersRef = db.collection("users");
        usersRef.whereEqualTo("email", emailInput)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        if (!task.getResult().isEmpty()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                String uid = document.getId();
                                String email = document.getString("email");
                                String name = document.getString("username");

                                if (!allowedUserUIDs.contains(uid)) {
                                    allowedUserUIDs.add(uid);

                                    // Add to display list
                                    Collaborator collaborator = new Collaborator(uid, email, name);
                                    collaboratorList.add(collaborator);
                                    collaboratorAdapter.notifyDataSetChanged();

                                    dialog.dismiss();
                                    Toast.makeText(this, "Collaborator added!", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(this, "Collaborator already added", Toast.LENGTH_SHORT).show();
                                }
                            }
                        } else {
                            Toast.makeText(this, "No user found with this email.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "Error searching for user.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onCollaboratorRemove(int position) {
        // Prevent removing yourself (first collaborator)
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null && position == 0 &&
                collaboratorList.get(0).getUid().equals(currentUser.getUid())) {
            Toast.makeText(this, "Cannot remove yourself as collaborator", Toast.LENGTH_SHORT).show();
            return;
        }

        // Remove from both lists
        String removedUid = collaboratorList.get(position).getUid();
        allowedUserUIDs.remove(removedUid);
        collaboratorList.remove(position);
        collaboratorAdapter.notifyItemRemoved(position);

        Toast.makeText(this, "Collaborator removed", Toast.LENGTH_SHORT).show();
    }

    private void addProjectToFirestore(String title) {
        FirebaseUser user = auth.getCurrentUser();

        if (user != null) {
            String uid = user.getUid();

            if (!allowedUserUIDs.contains(uid)) {
                allowedUserUIDs.add(uid);
            }

            Map<String, Object> projectData = new HashMap<>();
            projectData.put("title", title);
            projectData.put("userId", uid);
            projectData.put("allowedUsers", allowedUserUIDs);

            if (!NetworkUtils.isNetworkAvailable(this)) {
                Toast.makeText(this, "You are offline. Project will sync when online.", Toast.LENGTH_LONG).show();
                setResult(RESULT_OK);
                finish();
            } else {
                db.collection("projects")
                        .add(projectData)
                        .addOnSuccessListener(documentReference -> {
                            Toast.makeText(this, "Project added!", Toast.LENGTH_SHORT).show();

                            // Send notifications to all collaborators except the current user
                            String projectId = documentReference.getId();
                            notifyCollaborators(projectId, title);

                            setResult(RESULT_OK);
                            finish();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(this, "Error adding project", Toast.LENGTH_SHORT).show();
                        });
            }
        } else {
            Toast.makeText(this, "User not logged in!", Toast.LENGTH_SHORT).show();
        }
    }

    private void notifyCollaborators(String projectId, String projectTitle) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) return;

        String currentUserId = currentUser.getUid();
        String addedByName = currentUserName != null ? currentUserName : "Project Owner";

        Log.d("Add_Project", "Starting notification for " + (collaboratorList.size() - 1) + " collaborators");

        // For each collaborator (skip the current user)
        for (Collaborator collaborator : collaboratorList) {
            String collaboratorId = collaborator.getUid();
            if (!collaboratorId.equals(currentUserId)) {
                Log.d("Add_Project", "Attempting to notify collaborator: " + collaborator.getUsername() + " (ID: " + collaboratorId + ")");

                // 1. Store notification in Firestore first (most reliable)
                storeNotificationInFirestore(collaboratorId, projectId, projectTitle, addedByName);

                // 2. Try to send via Cloud Messaging if possible
                sendCloudMessage(collaboratorId, projectId, projectTitle, addedByName);
            }
        }
    }

    // New method to ensure notifications are stored in Firestore
    private void storeNotificationInFirestore(String userId, String projectId, String projectTitle, String addedByName) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("type", "collaboration");
        notification.put("projectId", projectId);
        notification.put("projectTitle", projectTitle);
        notification.put("addedBy", addedByName);
        notification.put("timestamp", System.currentTimeMillis());
        notification.put("read", false);

        db.collection("users").document(userId)
                .collection("notifications")
                .add(notification)
                .addOnSuccessListener(documentReference ->
                        Log.d("Add_Project", "Notification stored in Firestore for user: " + userId))
                .addOnFailureListener(e ->
                        Log.e("Add_Project", "Failed to store notification in Firestore for user: " + userId, e));
    }

    // Updated Cloud Messaging method
    private void sendCloudMessage(String userId, String projectId, String projectTitle, String addedByName) {
        // Get the user's FCM token
        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String fcmToken = documentSnapshot.getString("fcmToken");

                        Log.d("Add_Project", "User " + userId + " has FCM token: " + (fcmToken != null ? "Yes" : "No"));

                        if (fcmToken != null && !fcmToken.isEmpty()) {
                            // Use Firebase Cloud Functions to send message
                            Map<String, Object> data = new HashMap<>();
                            data.put("token", fcmToken);
                            data.put("projectId", projectId);
                            data.put("projectTitle", projectTitle);
                            data.put("addedBy", addedByName);

                            FirebaseFunctions.getInstance()
                                    .getHttpsCallable("sendCollaborationNotification")
                                    .call(data)
                                    .addOnSuccessListener(result -> {
                                        Log.d("Add_Project", "Cloud function succeeded for user: " + userId);
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e("Add_Project", "Cloud function failed for user: " + userId, e);
                                        // Try fallback local notification
                                        sendLocalNotification(userId, projectId, projectTitle, addedByName);
                                    });
                        } else {
                            Log.d("Add_Project", "No FCM token for user: " + userId + ", using local notification");
                            // No token, try local notification
                            sendLocalNotification(userId, projectId, projectTitle, addedByName);
                        }
                    } else {
                        Log.e("Add_Project", "User document doesn't exist for ID: " + userId);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("Add_Project", "Failed to get user data for ID: " + userId, e);
                    // Fallback to local notification
                    sendLocalNotification(userId, projectId, projectTitle, addedByName);
                });
    }

    // Local notification method as fallback
    private void sendLocalNotification(String userId, String projectId, String projectTitle, String addedByName) {
        Log.d("Add_Project", "Attempting to send local notification for user: " + userId);

        // Try to send a local notification if we're on the same device
        try {
            CollaboratorNotificationService.sendCollaboratorNotification(
                    this, projectId, projectTitle, addedByName);
            Log.d("Add_Project", "Local notification triggered for project: " + projectTitle);
        } catch (Exception e) {
            Log.e("Add_Project", "Failed to send local notification", e);
        }
    }
}