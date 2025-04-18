package com.example.milestonemk_4.activitiesUI;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.os.Bundle;
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
import com.example.milestonemk_4.utils.NetworkUtils;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

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

        addCurrentUserAsCollaborator();
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
                finish();
            } else {
                db.collection("projects")
                        .add(projectData)
                        .addOnSuccessListener(documentReference -> {
                            Toast.makeText(this, "Project added!", Toast.LENGTH_SHORT).show();
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
}