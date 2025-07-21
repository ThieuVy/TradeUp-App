package com.example.testapptradeup.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.testapptradeup.R;
import com.example.testapptradeup.models.ChatMessage;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatMessageAdapter extends ListAdapter<ChatMessage, RecyclerView.ViewHolder> {
    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;
    private final String currentUserId;

    public ChatMessageAdapter() {
        super(DIFF_CALLBACK);
        this.currentUserId = FirebaseAuth.getInstance().getUid();
    }

    @Override
    public int getItemViewType(int position) {
        if (getItem(position).getSenderId().equals(currentUserId)) {
            return VIEW_TYPE_SENT;
        } else {
            return VIEW_TYPE_RECEIVED;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_TYPE_SENT) {
            View view = inflater.inflate(R.layout.item_chat_message_sent, parent, false);
            return new SentMessageViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_chat_message_received, parent, false);
            return new ReceivedMessageViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage message = getItem(position);
        if (holder.getItemViewType() == VIEW_TYPE_SENT) {
            ((SentMessageViewHolder) holder).bind(message);
        } else {
            ((ReceivedMessageViewHolder) holder).bind(message);
        }
    }

    // ViewHolder cho tin nhắn gửi đi
    static class SentMessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageBody, messageTime;
        ImageView imageBody; // Thêm ImageView

        SentMessageViewHolder(View itemView) {
            super(itemView);
            messageBody = itemView.findViewById(R.id.text_message_body);
            messageTime = itemView.findViewById(R.id.text_message_time);
            imageBody = itemView.findViewById(R.id.image_message_body); // Ánh xạ ImageView
        }

        void bind(ChatMessage message) {
            // === LOGIC QUYẾT ĐỊNH HIỂN THỊ ===
            if (message.getImageUrl() != null && !message.getImageUrl().isEmpty()) {
                // Nếu có URL ảnh -> Hiển thị ảnh, ẩn text
                messageBody.setVisibility(View.GONE);
                imageBody.setVisibility(View.VISIBLE);
                Glide.with(itemView.getContext())
                        .load(message.getImageUrl())
                        .placeholder(R.color.background_light) // Màu placeholder
                        .into(imageBody);
            } else {
                // Nếu không có URL ảnh -> Hiển thị text, ẩn ảnh
                messageBody.setVisibility(View.VISIBLE);
                imageBody.setVisibility(View.GONE);
                messageBody.setText(message.getText());
            }

            if (message.getTimestamp() != null) {
                messageTime.setText(new SimpleDateFormat("HH:mm", Locale.getDefault()).format(message.getTimestamp()));
            }
        }
    }

    // ViewHolder cho tin nhắn nhận được
    static class ReceivedMessageViewHolder extends RecyclerView.ViewHolder {
        CircleImageView senderAvatar;
        TextView messageBody, messageTime;
        ImageView imageBody; // Thêm ImageView

        ReceivedMessageViewHolder(View itemView) {
            super(itemView);
            senderAvatar = itemView.findViewById(R.id.image_sender_avatar);
            messageBody = itemView.findViewById(R.id.text_message_body);
            messageTime = itemView.findViewById(R.id.text_message_time);
            imageBody = itemView.findViewById(R.id.image_message_body); // Ánh xạ ImageView
        }

        void bind(ChatMessage message) {
            // === LOGIC QUYẾT ĐỊNH HIỂN THỊ (Tương tự như trên) ===
            if (message.getImageUrl() != null && !message.getImageUrl().isEmpty()) {
                messageBody.setVisibility(View.GONE);
                imageBody.setVisibility(View.VISIBLE);
                Glide.with(itemView.getContext())
                        .load(message.getImageUrl())
                        .placeholder(R.color.background_light)
                        .into(imageBody);
            } else {
                messageBody.setVisibility(View.VISIBLE);
                imageBody.setVisibility(View.GONE);
                messageBody.setText(message.getText());
            }

            if (message.getTimestamp() != null) {
                messageTime.setText(new SimpleDateFormat("HH:mm", Locale.getDefault()).format(message.getTimestamp()));
            }
            // Lấy và hiển thị avatar người gửi
            FirebaseDatabase.getInstance().getReference("Users").child(message.getSenderId()).child("avatarUrl")
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            String avatarUrl = snapshot.getValue(String.class);
                            if (avatarUrl != null && !avatarUrl.isEmpty()) {
                                Glide.with(itemView.getContext())
                                        .load(avatarUrl)
                                        .placeholder(R.drawable.ic_default_avatar) // Ảnh placeholder
                                        .into(senderAvatar);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) { }
                    });
        }
    }

    private static final DiffUtil.ItemCallback<ChatMessage> DIFF_CALLBACK = new DiffUtil.ItemCallback<>() {
        @Override
        public boolean areItemsTheSame(@NonNull ChatMessage oldItem, @NonNull ChatMessage newItem) {
            return oldItem.getMessageId().equals(newItem.getMessageId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull ChatMessage oldItem, @NonNull ChatMessage newItem) {
            return oldItem.getText().equals(newItem.getText());
        }
    };
}