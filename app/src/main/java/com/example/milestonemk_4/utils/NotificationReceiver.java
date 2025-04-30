package com.example.milestonemk_4.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.milestonemk_4.R;
import com.example.milestonemk_4.activitiesUI.MainActivity;

public class NotificationReceiver extends BroadcastReceiver {

    private static final String TAG = "NotificationReceiver";
    private static final String CHANNEL_ID = "daily_notification_channel";
    private static final int NOTIFICATION_ID = 1001;

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "NotificationReceiver triggered. Intent: " + intent.toString());

        try {
            // Create notification channel for Android O and above
            createNotificationChannel(context);

            boolean isTest = intent.getBooleanExtra("isTestAlarm", false);

            // Create an intent to open the app when notification is clicked
            Intent openAppIntent = new Intent(context, MainActivity.class);
            openAppIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    openAppIntent,
                    PendingIntent.FLAG_IMMUTABLE
            );

            // Build the notification
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle(isTest ? "Test Reminder" : "Daily Reminder")
                    .setContentText("Don't forget to check your tasks for today!")
                    .setPriority(NotificationCompat.PRIORITY_HIGH) // Using HIGH priority for better visibility
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true);

            // Show the notification
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

            try {
                int notificationId = isTest ? 2001 : NOTIFICATION_ID;
                notificationManager.notify(notificationId, builder.build());
                Log.d(TAG, "Notification sent successfully with ID: " + notificationId);
            } catch (SecurityException e) {
                Log.e(TAG, "Permission denied: " + e.getMessage());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error showing notification: " + e.getMessage(), e);
        }
    }

    private void createNotificationChannel(Context context) {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.d(TAG, "Creating notification channel");

            CharSequence name = "Daily Notifications";
            String description = "Channel for daily app reminder notifications";
            int importance = NotificationManager.IMPORTANCE_HIGH; // Using HIGH importance for better visibility
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            channel.enableVibration(true);
            channel.enableLights(true);

            // Register the channel with the system
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);

            Log.d(TAG, "Notification channel created successfully");
        }
    }
}