package com.example.testapptradeup.adapters;

import android.annotation.SuppressLint;
// Thêm import Color
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.testapptradeup.R;
import com.example.testapptradeup.models.NotificationItem; // Đảm bảo import NotificationItem

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

    private List<NotificationItem> notifications = new ArrayList<>();
    private OnNotificationClickListener listener;

    public interface OnNotificationClickListener {
        void onNotificationClick(NotificationItem notification, int position);
    }

    public void setOnNotificationClickListener(OnNotificationClickListener listener) {
        this.listener = listener;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setNotifications(List<NotificationItem> notifications) {
        this.notifications = notifications;
        notifyDataSetChanged();
    }

    // This method is for client-side UI update after marking as read
    public void markAsRead(int position) {
        if (position >= 0 && position < notifications.size()) {
            notifications.get(position).setRead(true);
            notifyItemChanged(position);
        }
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        NotificationItem notification = notifications.get(position);
        holder.bind(notification, listener);
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    public static class NotificationViewHolder extends RecyclerView.ViewHolder {
        private final ImageView iconNotification;
        private final TextView textNotificationTitle;
        private final TextView textNotificationTime;
        private final TextView textNotificationContent;
        private final View indicatorUnread;
        private final TextView textNotificationCategory;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            iconNotification = itemView.findViewById(R.id.icon_notification);
            textNotificationTitle = itemView.findViewById(R.id.text_notification_title);
            textNotificationTime = itemView.findViewById(R.id.text_notification_time);
            textNotificationContent = itemView.findViewById(R.id.text_notification_content);
            indicatorUnread = itemView.findViewById(R.id.indicator_unread);
            textNotificationCategory = itemView.findViewById(R.id.text_notification_category);
        }

        @SuppressLint("SetTextI18n")
        public void bind(NotificationItem notification, OnNotificationClickListener listener) {
            textNotificationTitle.setText(notification.getTitle());
            textNotificationContent.setText(notification.getContent());

            // Định dạng thời gian
            if (notification.getTimestamp() != null) {
                long diffMillis = System.currentTimeMillis() - notification.getTimestamp().getTime();
                long seconds = diffMillis / 1000;
                long minutes = seconds / 60;
                long hours = minutes / 60;
                long days = hours / 24;

                if (seconds < 60) {
                    textNotificationTime.setText("Vừa xong");
                } else if (minutes < 60) {
                    textNotificationTime.setText(minutes + " phút trước");
                } else if (hours < 24) {
                    textNotificationTime.setText(hours + " giờ trước");
                } else if (days == 1) {
                    textNotificationTime.setText("Hôm qua");
                } else if (days < 7) {
                    textNotificationTime.setText(days + " ngày trước");
                } else {
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    textNotificationTime.setText(sdf.format(notification.getTimestamp()));
                }
            } else {
                textNotificationTime.setText("Không rõ");
            }


            // Hiển thị icon và màu nền dựa trên loại thông báo
            int iconResId = R.drawable.ic_notification_default; // Default icon
            int backgroundColor = R.color.primary_color; // Default background color for icon circle

            if (notification.getType() != null) { // SỬA: Dùng getType() trả về NotificationType
                switch (notification.getType()) { // SỬA: Dùng NotificationType enum
                    case TRADE:
                        iconResId = R.drawable.ic_trade; // Example: assuming you have ic_trade
                        backgroundColor = R.color.trade_notification_bg; // Example color
                        break;
                    case PRICE_ALERT:
                        iconResId = R.drawable.ic_price_alert; // Example
                        backgroundColor = R.color.price_alert_notification_bg; // Example color
                        break;
                    case NEWS:
                        iconResId = R.drawable.ic_news; // Example
                        backgroundColor = R.color.news_notification_bg; // Example color
                        break;
                    case SYSTEM:
                        iconResId = R.drawable.ic_system; // Example
                        backgroundColor = R.color.system_notification_bg; // Example color
                        break;
                    case PROMOTION:
                        iconResId = R.drawable.ic_promotion;
                        backgroundColor = R.color.promotion_notification_bg;
                        break;
                    case LISTING:
                        iconResId = R.drawable.ic_listing;
                        backgroundColor = R.color.listing_notification_bg;
                        break;
                    case MESSAGE:
                        iconResId = R.drawable.ic_chat;
                        backgroundColor = R.color.message_notification_bg;
                        break;
                    case OTHER:
                    default:
                        iconResId = R.drawable.ic_notification_default;
                        backgroundColor = R.color.primary_color;
                        break;
                }
            }

            iconNotification.setImageResource(iconResId);
            // iconNotification.setBackgroundColor(itemView.getContext().getColor(backgroundColor)); // Fix: use setBackgroundTintList
            // Thay đổi cách đặt màu nền cho ImageView (backgroundTint)
            iconNotification.setBackgroundTintList(ContextCompat.getColorStateList(itemView.getContext(), backgroundColor));


            // Hiển thị/ẩn chỉ báo chưa đọc
            indicatorUnread.setVisibility(notification.isRead() ? View.GONE : View.VISIBLE);

            // Set category tag text and visibility
            if (notification.getCategory() != null && !notification.getCategory().isEmpty()) {
                textNotificationCategory.setText(notification.getCategory().toUpperCase(Locale.getDefault()));
                textNotificationCategory.setVisibility(View.VISIBLE);
                // You might want to change category tag background color based on category too
            } else {
                textNotificationCategory.setVisibility(View.GONE);
            }

            // Xử lý sự kiện click vào toàn bộ item
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onNotificationClick(notification, getBindingAdapterPosition());
                }
            });

            // Thay đổi màu nền của CardView nếu chưa đọc
            if (!notification.isRead()) {
                ((androidx.cardview.widget.CardView) itemView).setCardBackgroundColor(
                        ContextCompat.getColor(itemView.getContext(), R.color.unread_notification_background));
            } else {
                ((androidx.cardview.widget.CardView) itemView).setCardBackgroundColor(
                        ContextCompat.getColor(itemView.getContext(), android.R.color.white));
            }
        }
    }
}
