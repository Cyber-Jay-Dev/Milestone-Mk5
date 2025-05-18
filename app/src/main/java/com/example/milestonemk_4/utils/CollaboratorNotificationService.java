package com.example.milestonemk_4.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.example.milestonemk_4.R;
import com.example.milestonemk_4.activitiesUI.MainActivity;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class CollaboratorNotificationService extends FirebaseMessagingService {
    private static final String TAG = "CollabNotificationSvc";
    private static final String CHANNEL_ID = "collaborator_notification_channel";
    private static final int NOTIFICATION_ID = 3001;

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        Log.d(TAG, "FCM message received from: " + remoteMessage.getFrom());

        // Get current user for logging
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String userId = currentUser != null ? currentUser.getUid() : "Not logged in";
        Log.d(TAG, "Message received on device for user: " + userId);

        // Check if message contains data payload
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());

            Map<String, String> data = remoteMessage.getData();
            String projectId = data.get("projectId");
            String projectTitle = data.get("projectTitle");
            String addedBy = data.get("addedBy");

            // Show notification
            if (projectId != null && projectTitle != null) {
                // Always store in Firestore first
                storeNotificationInFirestore(projectId, projectTitle, addedBy);

                // Then display the notification
                String message = "You've been added to \"" + projectTitle + "\"" +
                        (addedBy != null ? " by " + addedBy : "");
                sendNotification(projectTitle, message, projectId);
            }
        }

        // Check if message contains notification payload
        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
            // Use notification payload if data payload wasn't available
            sendNotification(
                    remoteMessage.getNotification().getTitle(),
                    remoteMessage.getNotification().getBody(),
                    null
            );
        }
    }

    @Override
    public void onNewToken(@NonNull String token) {
        Log.d(TAG, "Refreshed FCM token: " + token);
        sendRegistrationToServer(token);
    }

    private void sendRegistrationToServer(String token) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            FirebaseFirestore db = FirebaseFirestore.getInstance();

            // Update user document with FCM token
            Map<String, Object> updates = new HashMap<>();
            updates.put("fcmToken", token);

            db.collection("users").document(currentUser.getUid())
                    .update(updates)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "FCM Token successfully updated!"))
                    .addOnFailureListener(e -> Log.w(TAG, "Error updating FCM token", e));
        } else {
            Log.w(TAG, "Cannot update FCM token, user not logged in");

            // Store token in shared preferences for later use
            // This helps when the token is refreshed but user is not logged in yet
            getApplicationContext().getSharedPreferences("FCM_PREFS", Context.MODE_PRIVATE)
                    .edit()
                    .putString("pending_fcm_token", token)
                    .apply();
        }
    }

    private void storeNotificationInFirestore(String projectId, String projectTitle, String addedBy) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            Map<String, Object> notification = new HashMap<>();
            notification.put("type", "collaboration");
            notification.put("projectId", projectId);
            notification.put("projectTitle", projectTitle);
            notification.put("addedBy", addedBy);
            notification.put("timestamp", System.currentTimeMillis());
            notification.put("read", false);

            FirebaseFirestore.getInstance()
                    .collection("users").document(currentUser.getUid())
                    .collection("notifications")
                    .add(notification)
                    .addOnSuccessListener(documentReference ->
                            Log.d(TAG, "Notification stored in Firestore"))
                    .addOnFailureListener(e ->
                            Log.e(TAG, "Error storing notification in Firestore", e));
        } else {
            Log.w(TAG, "Cannot store notification, user not logged in");
        }
    }

    private void sendNotification(String title, String messageBody, String projectId) {
        Log.d(TAG, "Creating notification: " + title + " - " + messageBody);

        Intent intent = new Intent(this, MainActivity.class);
        if (projectId != null) {
            intent.putExtra("projectId", projectId);
            intent.putExtra("openProject", true);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        // Use unique request code based on projectId or timestamp to avoid PendingIntent collisions
        int requestCode = projectId != null ? projectId.hashCode() : (int) (System.currentTimeMillis() / 1000);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title != null ? title : "New Project Collaboration")
                .setContentText(messageBody != null ? messageBody : "You've been added to a project")
                .setStyle(new NotificationCompat.BigTextStyle().bigText(messageBody))
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setVibrate(new long[]{0, 300, 200, 300})
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Create notification channel for Android O and above
        createNotificationChannel(notificationManager);

        try {
            notificationManager.notify(requestCode, notificationBuilder.build());
            Log.d(TAG, "Notification sent with ID: " + requestCode);
        } catch (Exception e) {
            Log.e(TAG, "Error showing notification", e);
        }
    }

    private void createNotificationChannel(NotificationManager notificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                // Check if channel exists first
                NotificationChannel existingChannel = notificationManager.getNotificationChannel(CHANNEL_ID);
                if (existingChannel == null) {
                    NotificationChannel channel = new NotificationChannel(
                            CHANNEL_ID,
                            "Project Collaboration Notifications",
                            NotificationManager.IMPORTANCE_HIGH
                    );
                    channel.setDescription("Notifications for when you are added to projects");
                    channel.enableVibration(true);
                    channel.setVibrationPattern(new long[]{0, 300, 200, 300});
                    channel.setShowBadge(true);
                    notificationManager.createNotificationChannel(channel);
                    Log.d(TAG, "Notification channel created");
                } else {
                    Log.d(TAG, "Notification channel already exists");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error creating notification channel", e);
            }
        }
    }

    // Helper method for manual notification triggering (used when FCM is not available)
    public static void sendCollaboratorNotification(Context context, String projectId, String projectTitle, String addedByName) {
        Log.d(TAG, "Manual notification trigger for project: " + projectTitle);

        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra("projectId", projectId);
        intent.putExtra("openProject", true);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        // Use unique request code based on projectId or timestamp to avoid PendingIntent collisions
        int requestCode = projectId != null ? projectId.hashCode() : (int) (System.currentTimeMillis() / 1000);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Create the notification channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                NotificationChannel existingChannel = notificationManager.getNotificationChannel(CHANNEL_ID);
                if (existingChannel == null) {
                    NotificationChannel channel = new NotificationChannel(
                            CHANNEL_ID,
                            "Project Collaboration Notifications",
                            NotificationManager.IMPORTANCE_HIGH
                    );
                    channel.setDescription("Notifications for when you are added to projects");
                    channel.enableVibration(true);
                    channel.setVibrationPattern(new long[]{0, 300, 200, 300});
                    channel.setShowBadge(true);
                    notificationManager.createNotificationChannel(channel);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error creating notification channel in static method", e);
            }
        }

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        String message = "You've been added to \"" + projectTitle + "\"" +
                (addedByName != null ? " by " + addedByName : "");

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("New Project Collaboration")
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setVibrate(new long[]{0, 300, 200, 300})
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent);

        try {
            notificationManager.notify(requestCode, notificationBuilder.build());
            Log.d(TAG, "Manual notification sent with ID: " + requestCode);
        } catch (Exception e) {
            Log.e(TAG, "Error showing manual notification", e);
        }
    }
}