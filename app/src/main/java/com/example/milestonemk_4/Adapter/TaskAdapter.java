package com.example.milestonemk_4.Adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.milestonemk_4.R;
import com.example.milestonemk_4.model.Task;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {
    private final List<Task> taskList;
    private OnItemLongClickListener longClickListener;
    private final FirebaseFirestore db;

    public interface OnItemLongClickListener {
        void onItemLongClick(Task task, View view);
    }

    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        this.longClickListener = listener;
    }

    public TaskAdapter(List<Task> taskList) {
        this.taskList = taskList;
        this.db = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.task_items, parent, false);
        return new TaskViewHolder(itemView);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = taskList.get(position);
        holder.taskName.setText(task.getTaskName());
        holder.status.setText(task.getStatus());

        Context context = holder.status.getContext();
        Drawable background = ContextCompat.getDrawable(context, R.drawable.rounded_bg);
        if (background instanceof GradientDrawable) {
            GradientDrawable gradientDrawable = (GradientDrawable) background;
            String status = task.getStatus().toLowerCase();
            switch (status) {
                case "low":
                    gradientDrawable.setColor(ContextCompat.getColor(context, android.R.color.holo_green_light));
                    break;
                case "medium":
                    gradientDrawable.setColor(ContextCompat.getColor(context, android.R.color.holo_orange_light));
                    break;
                case "high":
                    gradientDrawable.setColor(ContextCompat.getColor(context, android.R.color.holo_red_light));
                    break;
                default:
                    gradientDrawable.setColor(ContextCompat.getColor(context, android.R.color.darker_gray));
                    break;
            }
            holder.status.setBackground(gradientDrawable);
        }

        // Get assignment information from Firebase
        // Assuming the Task model has a field for assignedTo that contains the user ID
        if (task.getAssignedUserId() != null && !task.getAssignedUserId().isEmpty()) {
            loadUserData(task.getAssignedUserId(), holder.assignedAvatar, holder.assignedUsername);
        } else {
            // If no user is assigned, hide or set default values
            holder.assignedUsername.setText("Unassigned");
            holder.assignedAvatar.setImageResource(R.drawable.default_profile);
        }

        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onItemLongClick(task, holder.itemView);
            }
            return true;
        });
    }

    @SuppressLint("SetTextI18n")
    private void loadUserData(String userId, ImageView avatarImageView, TextView usernameTextView) {
        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Get username
                        String username = documentSnapshot.getString("username");
                        if (username != null) {
                            usernameTextView.setText(username);
                        } else {
                            usernameTextView.setText("User");
                        }

                        // Get avatar data
                        Long avatarId = documentSnapshot.getLong("avatarId");
                        String bgColor = documentSnapshot.getString("profileBgColor");

                        if (avatarId != null && bgColor != null) {
                            setProfilePicture(avatarImageView, avatarId.intValue(), bgColor);
                        } else {
                            // Set default avatar
                            avatarImageView.setImageResource(R.drawable.default_profile);
                        }
                    } else {
                        // Set defaults if user document doesn't exist
                        usernameTextView.setText("Unknown");
                        avatarImageView.setImageResource(R.drawable.default_profile);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("TaskAdapter", "Error loading user data", e);
                    usernameTextView.setText("Error");
                    avatarImageView.setImageResource(R.drawable.default_profile);
                });
    }

    // Method to set profile picture with background color and avatar overlay
    private void setProfilePicture(ImageView imageView, int avatarId, String bgColor) {
        try {
            Context context = imageView.getContext();
            // Get the avatar drawable
            int avatarResourceId = getAvatarResourceId(avatarId);
            @SuppressLint("UseCompatLoadingForDrawables") Drawable avatarDrawable = context.getResources().getDrawable(avatarResourceId, context.getTheme());

            // Create a colored background
            @SuppressLint("UseCompatLoadingForDrawables") GradientDrawable backgroundDrawable = (GradientDrawable) context.getResources().getDrawable(R.drawable.circle_background, context.getTheme()).mutate();
            backgroundDrawable.setColor(Color.parseColor(bgColor));

            // Create a layer-list programmatically
            LayerDrawable layerDrawable = new LayerDrawable(new Drawable[]{backgroundDrawable, avatarDrawable});

            // Set the layered drawable as the image source
            imageView.setImageDrawable(layerDrawable);
        } catch (Exception e) {
            Log.e("TaskAdapter", "Error setting profile picture", e);
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
    public int getItemCount() {
        return taskList.size();
    }

    public static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView taskName;
        TextView status;
        ImageView assignedAvatar;
        TextView assignedUsername;

        public TaskViewHolder(View view) {
            super(view);
            taskName = view.findViewById(R.id.TaskNameTV);
            status = view.findViewById(R.id.TaskStatus);
            assignedAvatar = view.findViewById(R.id.assignedAvatar);
            assignedUsername = view.findViewById(R.id.assignedUsername);
        }
    }
}