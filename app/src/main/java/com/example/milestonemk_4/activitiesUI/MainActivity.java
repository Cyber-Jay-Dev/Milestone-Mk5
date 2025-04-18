package com.example.milestonemk_4.activitiesUI;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.milestonemk_4.R;
import com.example.milestonemk_4.fragments.AboutFragment;
import com.example.milestonemk_4.fragments.HomeFragment;
import com.example.milestonemk_4.fragments.SettingsFragment;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    private DrawerLayout drawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        SharedPreferences prefs = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        boolean isLoggedIn = prefs.getBoolean("isLoggedIn", false);

        Log.d("MainActivityCheck", "isLoggedIn: " + isLoggedIn);

        if (!isLoggedIn) {
            startActivity(new Intent(MainActivity.this, log_in_page.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        updateNavHeader(navigationView);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar, R.string.open_nav, R.string.close_nav);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new HomeFragment())
                    .commit();
            navigationView.setCheckedItem(R.id.nav_home);
        }
    }

    @SuppressLint("SetTextI18n")
    private void updateNavHeader(NavigationView navigationView) {
        View headerView = navigationView.getHeaderView(0);
        TextView usernameTextView = headerView.findViewById(R.id.username);
        TextView emailTextView = headerView.findViewById(R.id.email);
        ImageView profileImageView = headerView.findViewById(R.id.profile_image);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            emailTextView.setText(user.getEmail());

            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("users").document(user.getUid()).get().addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult().exists()) {
                    String username = task.getResult().getString("username");
                    usernameTextView.setText("Hi, " + username + "!");

                    // Get the avatar ID and background color
                    Long avatarId = task.getResult().getLong("avatarId");
                    String bgColor = task.getResult().getString("profileBgColor");

                    if (avatarId != null && bgColor != null) {
                        // Load the corresponding avatar with background color
                        setProfilePicture(profileImageView, avatarId.intValue(), bgColor);
                    }
                } else {
                    usernameTextView.setText("Hi, User!");
                }
            }).addOnFailureListener(e -> Log.e("FirestoreError", "Error fetching user data", e));
        }
    }

    // Method to set profile picture with background color and avatar overlay
    private void setProfilePicture(ImageView imageView, int avatarId, String bgColor) {
        try {
            // Get the avatar drawable
            int avatarResourceId = getAvatarResourceId(avatarId);
            @SuppressLint("UseCompatLoadingForDrawables") Drawable avatarDrawable = getResources().getDrawable(avatarResourceId, getTheme());

            // Create a colored background
            @SuppressLint("UseCompatLoadingForDrawables") GradientDrawable backgroundDrawable = (GradientDrawable) getResources().getDrawable(R.drawable.circle_background, getTheme()).mutate();
            backgroundDrawable.setColor(Color.parseColor(bgColor));

            // Create a layer-list programmatically
            LayerDrawable layerDrawable = new LayerDrawable(new Drawable[]{backgroundDrawable, avatarDrawable});

            // Set the layered drawable as the image source
            imageView.setImageDrawable(layerDrawable);
        } catch (Exception e) {
            Log.e("ProfilePicture", "Error setting profile picture", e);
            // Fallback to default if there's an error
            imageView.setImageResource(R.drawable.default_profile);
        }
    }

    // Helper method to get the avatar resource ID
    private int getAvatarResourceId(int profilePicId) {
        switch (profilePicId) {
            case 1:
                return R.drawable.profile_1;
            case 2:
                return R.drawable.profile_2;
            case 3:
                return R.drawable.profile_3;
            case 4:
                return R.drawable.profile_4;
            case 5:
                return R.drawable.profile_5;
            case 6:
                return R.drawable.profile_6;
            case 7:
                return R.drawable.profile_7;
            case 8:
                return R.drawable.profile_8;
            case 9:
                return R.drawable.profile_9;
            case 10:
                return R.drawable.profile_10;
            default:
                return R.drawable.default_profile;
        }
    }
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        Fragment selectedFragment = null;
        int itemId = item.getItemId();

        if (itemId == R.id.nav_home) {
            selectedFragment = new HomeFragment();
        } else if (itemId == R.id.nav_aboutUs) {
            selectedFragment = new AboutFragment();
        } else if (itemId == R.id.nav_settings) {
            selectedFragment = new SettingsFragment();
        } else if (itemId == R.id.nav_logOut) {
            logout();
            return true;
        }

        if (selectedFragment != null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, selectedFragment)
                    .commit();
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void logout() {
        SharedPreferences prefs = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        prefs.edit().putBoolean("isLoggedIn", false).apply();

        startActivity(new Intent(MainActivity.this, log_in_page.class));
        finish();

        Toast.makeText(MainActivity.this, "Account has been logged out", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}