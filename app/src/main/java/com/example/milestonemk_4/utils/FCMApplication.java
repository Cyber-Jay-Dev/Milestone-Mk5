package com.example.milestonemk_4.utils;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.HashMap;
import java.util.Map;


public class FCMApplication extends Application {
    private static final String TAG = "FCMApplication";

    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize Firebase if not already done
        FirebaseApp.initializeApp(this);

        // Request and update FCM token on app start
        fetchAndUpdateFCMToken();
    }

    private void fetchAndUpdateFCMToken() {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(new OnCompleteListener<String>() {
                    @Override
                    public void onComplete(@NonNull Task<String> task) {
                        if (!task.isSuccessful()) {
                            Log.w(TAG, "Fetching FCM registration token failed", task.getException());
                            return;
                        }

                        // Get the new token
                        String token = task.getResult();

                        // Save token locally
                        getSharedPreferences("FCM_PREFS", Context.MODE_PRIVATE)
                                .edit()
                                .putString("fcm_token", token)
                                .apply();

                        Log.d(TAG, "FCM Token on app start: " + token);

                        // Update token in Firestore if user is logged in
                        updateTokenInFirestore(token);
                    }
                });
    }


    private void updateTokenInFirestore(String token) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            Map<String, Object> updates = new HashMap<>();
            updates.put("fcmToken", token);
            updates.put("lastTokenUpdate", System.currentTimeMillis());

            FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(currentUser.getUid())
                    .set(updates, SetOptions.merge())
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "FCM Token stored in Firestore"))
                    .addOnFailureListener(e -> Log.e(TAG, "Error storing FCM token", e));
        } else {
            // Store token for later use when user logs in
            getSharedPreferences("FCM_PREFS", Context.MODE_PRIVATE)
                    .edit()
                    .putString("pending_fcm_token", token)
                    .apply();

            Log.d(TAG, "Saved token for later use after login");
        }
    }
}