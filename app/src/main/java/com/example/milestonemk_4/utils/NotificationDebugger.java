package com.example.milestonemk_4.utils;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.milestonemk_4.R;
import com.example.milestonemk_4.activitiesUI.MainActivity;

public class NotificationDebugger {

    private static final String TAG = "NotificationDebugger";
    private static final String CHANNEL_ID = "daily_notification_channel";
    private static final int NOTIFICATION_ID = 1001;
    private static final int TEST_NOTIFICATION_ID = 2001;

    /**
     * Sends an immediate test notification to verify basic notification functionality
     */
    public static void sendTestNotification(Context context) {
        Log.d(TAG, "Sending test notification");

        // Create notification channel for Android O and above
        createNotificationChannel(context);

        // Create an intent to open the app when notification is clicked
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE
        );

        // Build the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Test Notification")
                .setContentText("This is a test notification to verify functionality")
                .setPriority(NotificationCompat.PRIORITY_HIGH) // Using HIGH priority for test
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        // Show the notification
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

        try {
            notificationManager.notify(TEST_NOTIFICATION_ID, builder.build());
            Toast.makeText(context, "Test notification sent", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Test notification sent successfully");
        } catch (SecurityException e) {
            Log.e(TAG, "Permission denied: " + e.getMessage());
            Toast.makeText(context, "Notification permission denied", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Log.e(TAG, "Failed to send test notification: " + e.getMessage());
            Toast.makeText(context, "Failed to send notification: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Verifies and logs alarm status
     */
    public static void checkAlarmStatus(Context context) {
        SharedPreferences preferences = context.getSharedPreferences("NotificationPrefs", Context.MODE_PRIVATE);
        boolean notificationsEnabled = preferences.getBoolean("notifications_enabled", false);
        int hour = preferences.getInt("notification_hour", 8);
        int minute = preferences.getInt("notification_minute", 0);

        Log.d(TAG, "Notifications enabled: " + notificationsEnabled);
        Log.d(TAG, "Notification time: " + hour + ":" + minute);

        // Check if pending intent exists (alarm is set)
        Intent intent = new Intent(context, NotificationReceiver.class);
        boolean alarmExists = (PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE) != null);

        Log.d(TAG, "Alarm is set: " + alarmExists);

        Toast.makeText(context,
                "Notification status: " + (notificationsEnabled ? "Enabled" : "Disabled") +
                        "\nAlarm set: " + (alarmExists ? "Yes" : "No") +
                        "\nTime: " + hour + ":" + minute,
                Toast.LENGTH_LONG).show();
    }

    /**
     * Sets an alarm for immediate testing (30 seconds from now)
     */
    public static void setTestAlarm(Context context) {
        Log.d(TAG, "Setting test alarm for 30 seconds from now");

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, NotificationReceiver.class);
        // Add test flag to distinguish it
        intent.putExtra("isTestAlarm", true);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                100, // Different request code for test alarm
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Set alarm for 30 seconds from now
        long triggerTime = System.currentTimeMillis() + 30 * 1000;

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // Use setExactAndAllowWhileIdle for newer devices to ensure delivery
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                );
            } else {
                // Use setExact for older devices
                alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                );
            }

            Toast.makeText(context, "Test alarm set for 30 seconds from now", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Test alarm set successfully for time: " + triggerTime);
        } catch (Exception e) {
            Log.e(TAG, "Failed to set test alarm: " + e.getMessage());
            Toast.makeText(context, "Failed to set test alarm: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Creates notification channel for Android O and above
     */
    private static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.d(TAG, "Creating notification channel");

            CharSequence name = "Daily Notifications";
            String description = "Channel for daily app reminder notifications";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            // Register the channel with the system
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);

            Log.d(TAG, "Notification channel created successfully");
        }
    }

    /**
     * Check device-specific notification settings and permission status
     */
    public static void checkNotificationSettings(Context context) {
        StringBuilder status = new StringBuilder();

        // Check if notification channel exists (for Android O+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            NotificationChannel channel = notificationManager.getNotificationChannel(CHANNEL_ID);

            if (channel == null) {
                status.append("• Notification channel not created\n");
            } else {
                boolean channelEnabled = channel.getImportance() != NotificationManager.IMPORTANCE_NONE;
                status.append("• Notification channel: ").append(channelEnabled ? "Enabled" : "Disabled").append("\n");
            }
        }

        // Check notification permission
        boolean hasPermission = NotificationManagerCompat.from(context).areNotificationsEnabled();
        status.append("• Notification permission: ").append(hasPermission ? "Granted" : "Denied").append("\n");

        // Check battery optimization (general info)
        status.append("• Check battery optimization settings for your app\n");

        // Print all info
        Log.d(TAG, "Notification settings:\n" + status.toString());
        Toast.makeText(context, status.toString(), Toast.LENGTH_LONG).show();
    }
}