package com.example.milestonemk_4.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.milestonemk_4.R;
import com.example.milestonemk_4.model.Collaborator;

import java.util.List;

public class CollaboratorAdapter extends RecyclerView.Adapter<CollaboratorAdapter.CollaboratorViewHolder> {

    private final List<Collaborator> collaboratorList;
    private final Context context;
    private final OnCollaboratorListener onCollaboratorListener;

    public interface OnCollaboratorListener {
        void onCollaboratorRemove(int position);
    }

    public CollaboratorAdapter(Context context, List<Collaborator> collaboratorList, OnCollaboratorListener onCollaboratorListener) {
        this.context = context;
        this.collaboratorList = collaboratorList;
        this.onCollaboratorListener = onCollaboratorListener;
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
    }

    @Override
    public int getItemCount() {
        return collaboratorList.size();
    }

    public static class CollaboratorViewHolder extends RecyclerView.ViewHolder {
        TextView nameTextView;
        TextView emailTextView;
        ImageButton removeButton;

        public CollaboratorViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.collaboratorName);
            emailTextView = itemView.findViewById(R.id.collaboratorEmail);
            removeButton = itemView.findViewById(R.id.removeCollaboratorBtn);
        }
    }
}