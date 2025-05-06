package com.example.milestonemk_4.Adapter;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.milestonemk_4.R;

import java.util.List;
import java.util.Map;

public class AttachmentAdapter extends RecyclerView.Adapter<AttachmentAdapter.AttachmentViewHolder> {
    private Context context;
    private List<Map<String, String>> attachments;

    public AttachmentAdapter(Context context, List<Map<String, String>> attachments) {
        this.context = context;
        this.attachments = attachments;
    }

    @NonNull
    @Override
    public AttachmentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.file_item, parent, false);
        return new AttachmentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AttachmentViewHolder holder, int position) {
        Map<String, String> attachment = attachments.get(position);
        String fileName = attachment.get("name");
        String fileUri = attachment.get("uri");

        holder.fileName.setText(fileName != null ? fileName : "Unnamed File");

        // Set click listener to open the file
        holder.itemView.setOnClickListener(v -> {
            if (fileUri != null && !fileUri.isEmpty()) {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(fileUri));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(context, "Cannot open this file: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(context, "File URI is missing", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return attachments != null ? attachments.size() : 0;
    }

    static class AttachmentViewHolder extends RecyclerView.ViewHolder {
        TextView fileName;

        public AttachmentViewHolder(@NonNull View itemView) {
            super(itemView);
            fileName = itemView.findViewById(R.id.file_name);
        }
    }
}