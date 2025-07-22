package com.example.testapptradeup.adapters;

import android.annotation.SuppressLint;
import android.text.format.DateUtils;
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
import com.example.testapptradeup.models.ChatItem;
import com.example.testapptradeup.models.ChatMessage;
import com.example.testapptradeup.models.DateSeparator;
import com.google.firebase.auth.FirebaseAuth;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatMessageAdapter extends ListAdapter<ChatItem, RecyclerView.ViewHolder> {
    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;
    private static final int VIEW_TYPE_DATE_SEPARATOR = 3;
    private static final int VIEW_TYPE_OFFER = 4;
    private final String currentUserId;

    public ChatMessageAdapter() {
        super(DIFF_CALLBACK);
        this.currentUserId = FirebaseAuth.getInstance().getUid();
    }

    @Override
    public int getItemViewType(int position) {
        ChatItem item = getItem(position);
        if (item instanceof ChatMessage) {
            ChatMessage message = (ChatMessage) item;

            if ("OFFER".equals(message.getMessageType())) {
                return VIEW_TYPE_OFFER;
            }

            if (message.getSenderId() != null && message.getSenderId().equals(currentUserId)) {
                return VIEW_TYPE_SENT;
            } else {
                return VIEW_TYPE_RECEIVED;
            }
        } else {
            return VIEW_TYPE_DATE_SEPARATOR;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        switch (viewType) {
            case VIEW_TYPE_SENT:
                View sentView = inflater.inflate(R.layout.item_chat_message_sent, parent, false);
                return new SentMessageViewHolder(sentView);
            case VIEW_TYPE_RECEIVED:
                View receivedView = inflater.inflate(R.layout.item_chat_message_received, parent, false);
                return new ReceivedMessageViewHolder(receivedView);
            case VIEW_TYPE_DATE_SEPARATOR:
                View separatorView = inflater.inflate(R.layout.item_chat_date_separator, parent, false);
                return new DateSeparatorViewHolder(separatorView);
            case VIEW_TYPE_OFFER:
                View offerView = inflater.inflate(R.layout.item_chat_message_offer, parent, false);
                return new OfferMessageViewHolder(offerView);
            default:
                throw new IllegalArgumentException("Invalid view type");
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatItem item = getItem(position);
        switch (holder.getItemViewType()) {
            case VIEW_TYPE_SENT:
                ((SentMessageViewHolder) holder).bind((ChatMessage) item);
                break;
            case VIEW_TYPE_RECEIVED:
                ((ReceivedMessageViewHolder) holder).bind((ChatMessage) item);
                break;
            case VIEW_TYPE_DATE_SEPARATOR:
                ((DateSeparatorViewHolder) holder).bind((DateSeparator) item);
                break;
            case VIEW_TYPE_OFFER:
                ((OfferMessageViewHolder) holder).bind((ChatMessage) item);
                break;

        }
    }

    // Lớp ViewHolder cho tin nhắn gửi đi
    static class SentMessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageBody, messageTime;
        ImageView imageBody;

        SentMessageViewHolder(View itemView) {
            super(itemView);
            messageBody = itemView.findViewById(R.id.text_message_body);
            messageTime = itemView.findViewById(R.id.text_message_time);
            imageBody = itemView.findViewById(R.id.image_message_body);
        }

        // ===============================================
        // === THAY ĐỔI QUAN TRỌNG NHẤT NẰM Ở ĐÂY (SENT) ===
        // ===============================================
        void bind(ChatMessage message) {
            // Kiểm tra xem tin nhắn có URL hình ảnh không
            if (message.getImageUrl() != null && !message.getImageUrl().isEmpty()) {
                // Nếu có, đây là tin nhắn hình ảnh
                messageBody.setVisibility(View.GONE); // Ẩn TextView
                imageBody.setVisibility(View.VISIBLE); // Hiện ImageView
                // Dùng Glide để tải ảnh từ URL vào ImageView
                Glide.with(itemView.getContext())
                        .load(message.getImageUrl())
                        .placeholder(R.color.background_light) // Ảnh tạm trong khi tải
                        .into(imageBody);
            } else {
                // Nếu không, đây là tin nhắn văn bản
                messageBody.setVisibility(View.VISIBLE); // Hiện TextView
                imageBody.setVisibility(View.GONE); // Ẩn ImageView
                messageBody.setText(message.getText());
            }

            // Hiển thị thời gian (giữ nguyên)
            if (message.getTimestamp() != null) {
                messageTime.setText(new SimpleDateFormat("HH:mm", Locale.getDefault()).format(message.getTimestamp()));
            }
        }
    }

    // Lớp ViewHolder cho tin nhắn nhận được
    static class ReceivedMessageViewHolder extends RecyclerView.ViewHolder {
        CircleImageView senderAvatar;
        TextView messageBody, messageTime;
        ImageView imageBody;

        ReceivedMessageViewHolder(View itemView) {
            super(itemView);
            senderAvatar = itemView.findViewById(R.id.image_sender_avatar);
            messageBody = itemView.findViewById(R.id.text_message_body);
            messageTime = itemView.findViewById(R.id.text_message_time);
            imageBody = itemView.findViewById(R.id.image_message_body);
        }

        // ===================================================
        // === THAY ĐỔI QUAN TRỌNG NHẤT NẰM Ở ĐÂY (RECEIVED) ===
        // ===================================================
        void bind(ChatMessage message) {
            // Logic tương tự như tin nhắn gửi đi
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

            // TODO: Cập nhật logic để tải avatar của người gửi nếu cần
            // Glide.with(itemView.getContext()).load(senderAvatarUrl)...
        }
    }

    // ViewHolder mới cho dấu phân cách ngày tháng
    static class DateSeparatorViewHolder extends RecyclerView.ViewHolder {
        private final TextView dateTextView;

        DateSeparatorViewHolder(View itemView) {
            super(itemView);
            dateTextView = (TextView) itemView;
        }

        void bind(DateSeparator separator) {
            dateTextView.setText(formatDateSeparator(separator.getTimestamp().getTime()));
        }

        private String formatDateSeparator(long timestamp) {
            if (DateUtils.isToday(timestamp)) {
                return "Hôm nay";
            } else if (isYesterday(timestamp)) {
                return "Hôm qua";
            } else {
                SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM, yyyy", new Locale("vi", "VN"));
                return sdf.format(timestamp);
            }
        }

        private boolean isYesterday(long timestamp) {
            Calendar now = Calendar.getInstance();
            Calendar timeToCheck = Calendar.getInstance();
            timeToCheck.setTimeInMillis(timestamp);
            now.add(Calendar.DAY_OF_YEAR, -1);
            return now.get(Calendar.YEAR) == timeToCheck.get(Calendar.YEAR)
                    && now.get(Calendar.DAY_OF_YEAR) == timeToCheck.get(Calendar.DAY_OF_YEAR);
        }
    }

    static class OfferMessageViewHolder extends RecyclerView.ViewHolder {
        TextView offerPriceText, offerTimeText;

        OfferMessageViewHolder(View itemView) {
            super(itemView);
            offerPriceText = itemView.findViewById(R.id.text_offer_price_in_chat);
            offerTimeText = itemView.findViewById(R.id.text_offer_time_in_chat);
        }

        @SuppressLint("SetTextI18n")
        void bind(ChatMessage message) {
            // Định dạng giá
            NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
            offerPriceText.setText(format.format(message.getOfferPrice()));

            // Định dạng thời gian
            if (message.getTimestamp() != null) {
                offerTimeText.setText("Vào lúc " + new SimpleDateFormat("HH:mm, dd/MM/yyyy", Locale.getDefault()).format(message.getTimestamp()));
            } else {
                offerTimeText.setVisibility(View.GONE);
            }
        }
    }

    // Cập nhật DiffUtil để làm việc với ChatItem
    private static final DiffUtil.ItemCallback<ChatItem> DIFF_CALLBACK = new DiffUtil.ItemCallback<>() {
        @Override
        public boolean areItemsTheSame(@NonNull ChatItem oldItem, @NonNull ChatItem newItem) {
            return oldItem.getItemId().equals(newItem.getItemId());
        }

        @SuppressLint("DiffUtilEquals")
        @Override
        public boolean areContentsTheSame(@NonNull ChatItem oldItem, @NonNull ChatItem newItem) {
            return oldItem.equals(newItem);
        }
    };
}