package com.example.milestonemk_4.activitiesUI;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.milestonemk_4.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class sign_up_page extends AppCompatActivity {
    private EditText usernameInput, emailInput, passwordInput, confirmPasswordInput;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    // Array of potential background colors (in hex format)
    private static final String[] PROFILE_COLORS = {
            "#4285F4", // Blue
            "#EA4335", // Red
            "#FBBC05", // Yellow
            "#34A853", // Green
            "#9C27B0", // Purple
            "#FF9800", // Orange
            "#00BCD4", // Cyan
            "#795548", // Brown
            "#607D8B", // Blue Grey
            "#E91E63"  // Pink
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up_page);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        usernameInput = findViewById(R.id.Sign_usernameInput);
        emailInput = findViewById(R.id.Sign_EmailInput);
        passwordInput = findViewById(R.id.Sign_PasswordInput);
        confirmPasswordInput = findViewById(R.id.Sign_ConfirmPasswordInput);

        Button signUpBtn = findViewById(R.id.SignUpBtn);
        Button goToLogin = findViewById(R.id.GotoLogIn);

        signUpBtn.setOnClickListener(v -> registerUser());

        goToLogin.setOnClickListener(v -> startActivity(new Intent(sign_up_page.this, log_in_page.class)));
    }

    private void registerUser() {
        String username = usernameInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        String confirmPassword = confirmPasswordInput.getText().toString().trim();

        if (TextUtils.isEmpty(username)) {
            usernameInput.setError("Username is required");
            return;
        }
        if (TextUtils.isEmpty(email)) {
            emailInput.setError("Email is required");
            return;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.setError("Enter a valid email");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            passwordInput.setError("Password is required");
            return;
        }
        if (password.length() < 6) {
            passwordInput.setError("Password must be at least 6 characters");
            return;
        }
        if (!password.equals(confirmPassword)) {
            confirmPasswordInput.setError("Passwords do not match");
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                FirebaseUser user = mAuth.getCurrentUser();
                if (user != null) {
                    user.sendEmailVerification();
                    saveUserData(user.getUid(), username, email);
                }
            } else {
                Toast.makeText(sign_up_page.this, "Registration failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveUserData(String userId, String username, String email) {
        // Generate random profile avatar number (1-5)
        int avatarNumber = (int) (Math.random() * 10) + 1;

        // Select a random background color
        String bgColor = getRandomBackgroundColor();

        Map<String, Object> user = new HashMap<>();
        user.put("username", username);
        user.put("email", email);
        user.put("avatarId", avatarNumber);
        user.put("profileBgColor", bgColor);

        db.collection("users").document(userId)
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(sign_up_page.this, "Account created! Verify your email before logging in.", Toast.LENGTH_LONG).show();
                    startActivity(new Intent(sign_up_page.this, log_in_page.class));
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(sign_up_page.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private String getRandomBackgroundColor() {
        Random random = new Random();
        return PROFILE_COLORS[random.nextInt(PROFILE_COLORS.length)];
    }
}