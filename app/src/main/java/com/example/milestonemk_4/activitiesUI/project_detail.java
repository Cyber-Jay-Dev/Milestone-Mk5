package com.example.milestonemk_4.activitiesUI;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.TextView;

import com.example.milestonemk_4.R;

public class project_detail extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_project_detail);

        String title = getIntent().getStringExtra("title");
        String userId = getIntent().getStringExtra("userId");

        // Display the data in the UI
        TextView titleView = findViewById(R.id.detailTitle);
        TextView userIdView = findViewById(R.id.detailUserId);

        titleView.setText("Project: " + title);
        userIdView.setText("Created by: " + userId);
    }
}