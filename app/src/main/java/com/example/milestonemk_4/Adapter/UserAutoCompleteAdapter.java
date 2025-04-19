package com.example.milestonemk_4.Adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.milestonemk_4.R;
import com.example.milestonemk_4.model.User;

import java.util.ArrayList;
import java.util.List;

public class UserAutoCompleteAdapter extends ArrayAdapter<User> {
    private final List<User> userList;
    private final List<User> userListFull;
    private final LayoutInflater layoutInflater;
    private final Context context;

    public UserAutoCompleteAdapter(@NonNull Context context, @NonNull List<User> userList) {
        super(context, 0, userList);
        this.context = context;
        this.userList = userList;
        this.userListFull = new ArrayList<>(userList);
        this.layoutInflater = LayoutInflater.from(context);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = layoutInflater.inflate(R.layout.item_user_autocomplete, parent, false);
        }

        ImageView avatarImageView = convertView.findViewById(R.id.user_avatar);
        TextView usernameTextView = convertView.findViewById(R.id.user_name);

        User user = getItem(position);
        if (user != null) {
            usernameTextView.setText(user.getUsername());
            setProfilePicture(avatarImageView, user.getAvatarId(), user.getProfileBgColor());
        }

        return convertView;
    }

    // Method to set profile picture with background color and avatar overlay
    private void setProfilePicture(ImageView imageView, int avatarId, String bgColor) {
        try {
            // Get the avatar drawable
            int avatarResourceId = getAvatarResourceId(avatarId);
            @SuppressLint("UseCompatLoadingForDrawables") Drawable avatarDrawable = context.getResources().getDrawable(avatarResourceId, context.getTheme());

            // Create a colored background
            @SuppressLint("UseCompatLoadingForDrawables") GradientDrawable backgroundDrawable = (GradientDrawable) context.getResources().getDrawable(
                    R.drawable.circle_background, context.getTheme()).mutate();
            backgroundDrawable.setColor(Color.parseColor(bgColor));

            // Create a layer-list programmatically
            LayerDrawable layerDrawable = new LayerDrawable(new Drawable[]{backgroundDrawable, avatarDrawable});

            // Set the layered drawable as the image source
            imageView.setImageDrawable(layerDrawable);
        } catch (Exception e) {
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

    @NonNull
    @Override
    public Filter getFilter() {
        return userFilter;
    }

    private final Filter userFilter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            FilterResults results = new FilterResults();
            List<User> suggestions = new ArrayList<>();

            if (constraint == null || constraint.length() == 0) {
                suggestions.addAll(userListFull);
            } else {
                String filterPattern = constraint.toString().toLowerCase().trim();

                for (User user : userListFull) {
                    if (user.getUsername().toLowerCase().contains(filterPattern) ||
                            user.getEmail().toLowerCase().contains(filterPattern)) {
                        suggestions.add(user);
                    }
                }
            }

            results.values = suggestions;
            results.count = suggestions.size();

            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            userList.clear();
            userList.addAll((List) results.values);
            notifyDataSetChanged();
        }

        @Override
        public CharSequence convertResultToString(Object resultValue) {
            return ((User) resultValue).getUsername();
        }
    };
}