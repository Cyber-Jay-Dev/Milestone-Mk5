package com.example.milestonemk_4.activitiesUI;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.milestonemk_4.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;

public class log_in_page extends AppCompatActivity {
    private EditText emailInput, passwordInput;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_in_page);

        mAuth = FirebaseAuth.getInstance();

        emailInput = findViewById(R.id.EmailInput);
        passwordInput = findViewById(R.id.PasswordInput);

        Button loginBtn = findViewById(R.id.logInBtn);
        TextView goToSignUp = findViewById(R.id.GotoSignUp);

        loginBtn.setOnClickListener(v -> loginUser());

        goToSignUp.setOnClickListener(v -> startActivity(new Intent(log_in_page.this, sign_up_page.class)));

    }
    private void loginUser() {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

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
        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                FirebaseUser user = mAuth.getCurrentUser();
                if (user != null && user.isEmailVerified()) {

                    SharedPreferences prefs = getSharedPreferences("MyPrefs", MODE_PRIVATE);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putBoolean("isLoggedIn", true);
                    editor.apply();

                    boolean check = prefs.getBoolean("isLoggedIn", false);
                    Log.d("LoginStatus", "Saved isLoggedIn: " + check);


                    Toast.makeText(log_in_page.this, "Login Successful!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(log_in_page.this, MainActivity.class));
                    finish();
                } else {
                    Toast.makeText(log_in_page.this, "Please verify your email before logging in", Toast.LENGTH_SHORT).show();
                }
            } else {
                if (task.getException() instanceof FirebaseAuthInvalidUserException) {
                    emailInput.setError("No account found with this email");
                } else if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                    passwordInput.setError("Incorrect password");
                } else {
                    Toast.makeText(log_in_page.this, "Authentication failed", Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

}