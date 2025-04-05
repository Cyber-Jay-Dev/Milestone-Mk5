package com.example.milestonemk_4.utils;

import android.app.Application;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.PersistentCacheSettings;

public class offlineHandle extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Initialize Firestore globally
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // nilalagay yung data sa local disk
        FirebaseFirestoreSettings settings =
                new FirebaseFirestoreSettings.Builder(db.getFirestoreSettings())
                        .setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
                        .build();

        db.setFirestoreSettings(settings);
    }
}
