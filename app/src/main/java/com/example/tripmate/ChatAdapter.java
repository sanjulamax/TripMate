package com.example.tripmate;




import android.app.AlertDialog;
import android.content.Context;
import android.view.*;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.tripmate.R;
import com.example.tripmate.ChatMessage;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.*;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {
    private List<ChatMessage> messages;
    private String currentUserId;
    private String tripId;
    private Context context;

    public ChatAdapter(Context context, List<ChatMessage> messages, String currentUserId, String tripId) {
        this.context = context;
        this.messages = messages;
        this.currentUserId = currentUserId;
        this.tripId = tripId;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_message_with_delete, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatMessage msg = messages.get(position);
        holder.tvSender.setText(msg.senderName);
        holder.tvMessage.setText(msg.text);

        // Show timestamp
        holder.tvTimestamp.setText(
                msg.timestamp != null
                        ? new SimpleDateFormat("HH:mm", Locale.getDefault()).format(msg.timestamp)
                        : ""
        );

        // Show delete icon only for my messages
        if (msg.senderId != null && msg.senderId.equals(currentUserId)) {
            holder.ivDelete.setVisibility(View.VISIBLE);
            holder.ivDelete.setOnClickListener(v -> {
                // Confirm and delete
                new AlertDialog.Builder(context)
                        .setTitle("Delete Message")
                        .setMessage("Are you sure you want to delete this message?")
                        .setPositiveButton("Delete", (dialog, which) -> {
                            // Remove from Firestore
                            FirebaseFirestore.getInstance()
                                    .collection("trips")
                                    .document(tripId)
                                    .collection("chat")
                                    .document(msg.id) // msg.id must be set to doc.getId()
                                    .delete();
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        } else {
            holder.ivDelete.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        TextView tvSender, tvMessage, tvTimestamp;
        ImageView ivDelete;
        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSender = itemView.findViewById(R.id.tvSender);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
            ivDelete = itemView.findViewById(R.id.ivDeleteMessage);
        }
    }
}