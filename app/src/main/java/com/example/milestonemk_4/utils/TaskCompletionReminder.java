package com.example.milestonemk_4.utils;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.example.milestonemk_4.R;
import com.example.milestonemk_4.activitiesUI.MainActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

public class TaskCompletionReminder extends BroadcastReceiver {

    private static final String CHANNEL_ID = "task_completion_reminder_channel";
    private static final int NOTIFICATION_ID = 2001;
    private static final String TAG = "TaskCompletionReminder";

    @Override
    public void onReceive(Context context, Intent intent) {
        String projectId = intent.getStringExtra("projectId");
        String taskName = intent.getStringExtra("taskName");

        // Check if task is still not completed
        checkTaskCompletion(context, projectId, taskName);
    }

    private void checkTaskCompletion(Context context, String projectId, String taskName) {
        // Get current user
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.e(TAG, "No user logged in");
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("projects")
                .document(projectId)
                .collection("tasks")
                .whereEqualTo("taskName", taskName)
                .whereEqualTo("assignedUserId", currentUser.getUid())
                .whereNotEqualTo("stage", "Completed")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot snapshot = task.getResult();
                        if (snapshot != null && !snapshot.isEmpty()) {
                            // Task exists and is not completed, send reminder
                            sendCompletionReminder(context, taskName);
                        } else {
                            Log.d(TAG, "Task is already completed or not found");
                        }
                    } else {
                        Log.e(TAG, "Error checking task completion status", task.getException());
                    }
                });
    }

    private void sendCompletionReminder(Context context, String taskName) {
        createNotificationChannel(context);

        Intent openAppIntent = new Intent(context, MainActivity.class);
        openAppIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                NOTIFICATION_ID, // Use different request code from regular notifications
                openAppIntent,
                PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(" You’re Almost There!")
                .setContentText("Finish your task and keep the momentum going.")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Task Completion Reminders";
            String description = "Channel for task completion reminder notifications";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    // Helper method to schedule the reminder
    public static void scheduleTaskReminder(Context context, String projectId, String taskName) {
        // Create intent for the reminder
        Intent intent = new Intent(context, TaskCompletionReminder.class);
        intent.putExtra("projectId", projectId);
        intent.putExtra("taskName", taskName);

        // Create a unique request code based on the task name
        int requestCode = taskName.hashCode();

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Get alarm manager
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        // Set alarm to fire in 3 hours
        long currentTimeMillis = System.currentTimeMillis();
//        long threeHoursLater = currentTimeMillis + (30 * 1000);
        long threeHoursLater = currentTimeMillis + (3 * 60 * 60 * 1000);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    threeHoursLater,
                    pendingIntent
            );
        } else {
            alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    threeHoursLater,
                    pendingIntent
            );
        }

        Log.d(TAG, "Task reminder scheduled for task: " + taskName + " in 3 hours");
    }

    // Helper method to cancel the reminder if task is completed early
    public static void cancelTaskReminder(Context context, String taskName) {
        Intent intent = new Intent(context, TaskCompletionReminder.class);
        int requestCode = taskName.hashCode();

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(pendingIntent);

        Log.d(TAG, "Task reminder cancelled for task: " + taskName);
    }
}