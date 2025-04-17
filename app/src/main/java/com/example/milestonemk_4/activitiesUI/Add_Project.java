package com.example.milestonemk_4.activitiesUI;

import android.app.Dialog;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.milestonemk_4.R;
import com.example.milestonemk_4.utils.NetworkUtils;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Add_Project extends AppCompatActivity {
    private EditText projectTitle;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private Dialog dialog;
    private final ArrayList<String> allowedUserUIDs = new ArrayList<>();

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
        });

        addProjectToDb.setOnClickListener(v -> {
            String title = projectTitle.getText().toString().trim();

            if (!title.isEmpty()) {
                addProjectToFirestore(title);
            } else {
                Toast.makeText(this, "Please enter a project title", Toast.LENGTH_SHORT).show();
            }
        });

    }

    private void searchEmail(String emailInput) {
        CollectionReference usersRef = db.collection("users");
        usersRef.whereEqualTo("email", emailInput)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        if (!task.getResult().isEmpty()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                String uid = document.getId();

                                if (!allowedUserUIDs.contains(uid)) {
                                    allowedUserUIDs.add(uid);
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