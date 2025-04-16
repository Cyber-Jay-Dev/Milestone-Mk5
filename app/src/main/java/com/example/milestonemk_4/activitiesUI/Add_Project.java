package com.example.milestonemk_4.activitiesUI;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.milestonemk_4.R;
import com.example.milestonemk_4.utils.NetworkUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class Add_Project extends AppCompatActivity {
    private EditText projectTitle;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_project);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        projectTitle = findViewById(R.id.projectTitleInput);
        Button addProjectToDb = findViewById(R.id.addprojectItem);
        ImageButton backButton = findViewById(R.id.topbar_backBtn);

        backButton.setOnClickListener(view1 -> finish());

        addProjectToDb.setOnClickListener(v -> {
            String title = projectTitle.getText().toString().trim();

            if (!title.isEmpty()) {
                addProjectToFirestore(title);
            } else {
                Toast.makeText(this, "Please enter a project title", Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void addProjectToFirestore(String title) {
        FirebaseUser user = auth.getCurrentUser();

        if (user != null) {
            String uid = user.getUid();

            Map<String, Object> projectData = new HashMap<>();
            projectData.put("title", title);
            projectData.put("userId", uid);

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