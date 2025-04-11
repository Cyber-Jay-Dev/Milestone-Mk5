package com.example.milestonemk_4.activitiesUI;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import com.example.milestonemk_4.R;

import java.util.Objects;

public class project_detail extends AppCompatActivity {

    Dialog dialog;

    @SuppressLint("UseCompatLoadingForDrawables")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_project_detail); // Load activity layout

        ImageButton openTasksDialog = findViewById(R.id.openTaskDialog);
        Button backBtn = findViewById(R.id.backBtn);

        // Initialize dialog
        dialog = new Dialog(project_detail.this);
        dialog.setContentView(R.layout.add_task_dialog);
        Objects.requireNonNull(dialog.getWindow()).setLayout(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ActionBar.LayoutParams.WRAP_CONTENT
        );
        dialog.getWindow().setBackgroundDrawable(getDrawable(R.drawable.dialog_bg));
        dialog.setCancelable(true);

        // Set project title
        String title = getIntent().getStringExtra("title");
        TextView titleView = findViewById(R.id.ProjName);
        titleView.setText(title != null ? title : "Project Title");

        // Back Button functionality
        backBtn.setOnClickListener(v -> startActivity(new Intent(project_detail.this, MainActivity.class)));

        // Open task dialog
        openTasksDialog.setOnClickListener(view -> dialog.show());
    }
}
