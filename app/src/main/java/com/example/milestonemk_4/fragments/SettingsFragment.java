package com.example.milestonemk_4.fragments;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.milestonemk_4.R;
import com.example.milestonemk_4.utils.NotificationDebugger;
import com.example.milestonemk_4.utils.NotificationReceiver;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Objects;

public class SettingsFragment extends Fragment {

    private TextView timeTextView;
    private SwitchCompat notificationSwitch;
    private SharedPreferences preferences;
    private Calendar notificationTime;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Set title
        Objects.requireNonNull(((AppCompatActivity) requireActivity()).getSupportActionBar()).setTitle("Settings");

        // Initialize views
        timeTextView = view.findViewById(R.id.notification_time_text);
        notificationSwitch = view.findViewById(R.id.notification_switch);
        Button setTimeButton = view.findViewById(R.id.set_time_button);

        // Debug buttons
        Button testNotificationButton = view.findViewById(R.id.test_notification_button);
        Button checkStatusButton = view.findViewById(R.id.check_status_button);
        Button testAlarmButton = view.findViewById(R.id.test_alarm_button);

        // Initialize SharedPreferences
        preferences = requireActivity().getSharedPreferences("NotificationPrefs", Context.MODE_PRIVATE);

        // Initialize notification time
        notificationTime = Calendar.getInstance();

        // Load saved preferences
        loadSavedPreferences();

        // Set click listeners
        setTimeButton.setOnClickListener(v -> showTimePickerDialog());

        // Set notification switch listener
        notificationSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            saveNotificationState(isChecked);
            if (isChecked) {
                scheduleNotification();
                Toast.makeText(requireContext(), "Daily notifications enabled", Toast.LENGTH_SHORT).show();
            } else {
                cancelNotification();
                Toast.makeText(requireContext(), "Daily notifications disabled", Toast.LENGTH_SHORT).show();
            }
        });

        // Debug button listeners
        testNotificationButton.setOnClickListener(v ->
                NotificationDebugger.sendTestNotification(requireContext()));

        checkStatusButton.setOnClickListener(v -> {
            NotificationDebugger.checkAlarmStatus(requireContext());
            NotificationDebugger.checkNotificationSettings(requireContext());
        });

        testAlarmButton.setOnClickListener(v ->
                NotificationDebugger.setTestAlarm(requireContext()));
    }

    private void loadSavedPreferences() {
        boolean notificationsEnabled = preferences.getBoolean("notifications_enabled", false);
        int hour = preferences.getInt("notification_hour", 8);
        int minute = preferences.getInt("notification_minute", 0);

        // Set the notification time
        notificationTime.set(Calendar.HOUR_OF_DAY, hour);
        notificationTime.set(Calendar.MINUTE, minute);

        // Update UI
        notificationSwitch.setChecked(notificationsEnabled);
        updateTimeDisplay();
    }

    private void saveNotificationState(boolean enabled) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("notifications_enabled", enabled);
        editor.apply();
    }

    private void saveNotificationTime(int hourOfDay, int minute) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("notification_hour", hourOfDay);
        editor.putInt("notification_minute", minute);
        editor.apply();

        // Update the notification time
        notificationTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
        notificationTime.set(Calendar.MINUTE, minute);
    }

    private void updateTimeDisplay() {
        SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        String formattedTime = timeFormat.format(notificationTime.getTime());
        timeTextView.setText(formattedTime);
    }

    private void showTimePickerDialog() {
        int hour = notificationTime.get(Calendar.HOUR_OF_DAY);
        int minute = notificationTime.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(
                requireContext(),
                (view, hourOfDay, selectedMinute) -> {
                    // Save the selected time
                    saveNotificationTime(hourOfDay, selectedMinute);

                    // Update the time display
                    updateTimeDisplay();

                    // If notifications are enabled, reschedule with the new time
                    if (notificationSwitch.isChecked()) {
                        scheduleNotification();
                        Toast.makeText(requireContext(), "Notification time updated", Toast.LENGTH_SHORT).show();
                    }
                },
                hour,
                minute,
                false
        );

        timePickerDialog.show();
    }

    private void scheduleNotification() {
        AlarmManager alarmManager = (AlarmManager) requireActivity().getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(requireContext(), NotificationReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                requireContext(),
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Set the time for the notification
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, notificationTime.get(Calendar.HOUR_OF_DAY));
        calendar.set(Calendar.MINUTE, notificationTime.get(Calendar.MINUTE));
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

    private void cancelNotification() {
        AlarmManager alarmManager = (AlarmManager) requireActivity().getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(requireContext(), NotificationReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                requireContext(),
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Cancel the alarm
        alarmManager.cancel(pendingIntent);
    }
}