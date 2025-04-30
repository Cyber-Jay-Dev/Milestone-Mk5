    package com.example.milestonemk_4.utils;

    import android.app.AlarmManager;
    import android.app.PendingIntent;
    import android.content.BroadcastReceiver;
    import android.content.Context;
    import android.content.Intent;
    import android.content.SharedPreferences;

    import java.util.Calendar;

    public class BootReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() != null && intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
                // Get saved notification preferences
                SharedPreferences preferences = context.getSharedPreferences("NotificationPrefs", Context.MODE_PRIVATE);
                boolean notificationsEnabled = preferences.getBoolean("notifications_enabled", false);

                // Only reschedule if notifications were enabled
                if (notificationsEnabled) {
                    int hour = preferences.getInt("notification_hour", 8);
                    int minute = preferences.getInt("notification_minute", 0);

                    // Reschedule the notification
                    scheduleNotification(context, hour, minute);
                }
            }
        }

        private void scheduleNotification(Context context, int hour, int minute) {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(context, NotificationReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            // Set the time for the notification
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, hour);
            calendar.set(Calendar.MINUTE, minute);
            calendar.set(Calendar.SECOND, 0);

            // If the time has already passed today, schedule for tomorrow
            if (calendar.before(Calendar.getInstance())) {
                calendar.add(Calendar.DAY_OF_MONTH, 1);
            }

            // Schedule a repeating alarm
            alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    calendar.getTimeInMillis(),
                    AlarmManager.INTERVAL_DAY,
                    pendingIntent
            );
        }
    }