package com.example.milestonemk_4.Adapter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.milestonemk_4.R;
import com.example.milestonemk_4.model.Collaborator;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class CollaboratorAdapter extends RecyclerView.Adapter<CollaboratorAdapter.CollaboratorViewHolder> {
    private final List<Collaborator> collaboratorList;
    private final Context context;
    private final OnCollaboratorListener onCollaboratorListener;
    private final FirebaseFirestore db;

    public interface OnCollaboratorListener {
        void onCollaboratorRemove(int position);
    }

    public CollaboratorAdapter(Context context, List<Collaborator> collaboratorList, OnCollaboratorListener onCollaboratorListener) {
        this.context = context;
        this.collaboratorList = collaboratorList;
        this.onCollaboratorListener = onCollaboratorListener;
        this.db = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public CollaboratorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_collaborator, parent, false);
        return new CollaboratorViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CollaboratorViewHolder holder, int position) {
        Collaborator collaborator = collaboratorList.get(position);
        holder.nameTextView.setText(collaborator.getUsername() != null ? collaborator.getUsername() : "User");
        holder.emailTextView.setText(collaborator.getEmail());
        holder.removeButton.setOnClickListener(v -> {
            if (onCollaboratorListener != null) {
                onCollaboratorListener.onCollaboratorRemove(position);
            }
        });

        // Load avatar from Firestore
        loadUserAvatar(collaborator.getUid(), holder.avatarImageView);
    }

    private void loadUserAvatar(String uid, ImageView imageView) {
        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Long avatarId = documentSnapshot.getLong("avatarId");
                        String bgColor = documentSnapshot.getString("profileBgColor");

                        if (avatarId != null && bgColor != null) {
                            setProfilePicture(imageView, avatarId.intValue(), bgColor);
                        } else {
                            // Set default avatar if data is missing
                            imageView.setImageResource(R.drawable.default_profile);
                        }
                    } else {
                        // Set default avatar if document doesn't exist
                        imageView.setImageResource(R.drawable.default_profile);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("CollaboratorAdapter", "Error loading avatar", e);
                    imageView.setImageResource(R.drawable.default_profile);
                });
    }

    // Method to set profile picture with background color and avatar overlay
    private void setProfilePicture(ImageView imageView, int avatarId, String bgColor) {
        try {
            // Get the avatar drawable
            int avatarResourceId = getAvatarResourceId(avatarId);
            Drawable avatarDrawable = context.getResources().getDrawable(avatarResourceId, context.getTheme());

            // Create a colored background
            GradientDrawable backgroundDrawable = (GradientDrawable) context.getResources().getDrawable(R.drawable.circle_background, context.getTheme()).mutate();
            backgroundDrawable.setColor(Color.parseColor(bgColor));

            // Create a layer-list programmatically
            LayerDrawable layerDrawable = new LayerDrawable(new Drawable[]{backgroundDrawable, avatarDrawable});

            // Set the layered drawable as the image source
            imageView.setImageDrawable(layerDrawable);
        } catch (Exception e) {
            Log.e("CollaboratorAdapter", "Error setting profile picture", e);
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
        return collaboratorList.size();
    }

    public static class CollaboratorViewHolder extends RecyclerView.ViewHolder {
        TextView nameTextView;
        TextView emailTextView;
        ImageButton removeButton;
        ImageView avatarImageView;

        public CollaboratorViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.collaboratorName);
            emailTextView = itemView.findViewById(R.id.collaboratorEmail);
            removeButton = itemView.findViewById(R.id.removeCollaboratorBtn);
            avatarImageView = itemView.findViewById(R.id.collab_avatar);
        }
    }
}