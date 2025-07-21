package com.example.testapptradeup.adapters;

import android.annotation.SuppressLint;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.example.testapptradeup.R;
import com.example.testapptradeup.models.Notification; // <-- SỬA: DÙNG MODEL NOTIFICATION
import java.text.SimpleDateFormat;
import java.util.Locale;

public class NotificationAdapter extends ListAdapter<Notification, NotificationAdapter.NotificationViewHolder> {

    private final OnNotificationClickListener listener;

    public interface OnNotificationClickListener {
        void onNotificationClick(Notification notification);
    }

    public NotificationAdapter(OnNotificationClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        Notification notification = getItem(position);
        holder.bind(notification, listener);
    }

    static class NotificationViewHolder extends RecyclerView.ViewHolder {
        private final ImageView iconNotification;
        private final TextView textNotificationTitle;
        private final TextView textNotificationTime;
        private final TextView textNotificationContent;
        private final View indicatorUnread;
        private final CardView cardView;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = (CardView) itemView;
            iconNotification = itemView.findViewById(R.id.icon_notification);
            textNotificationTitle = itemView.findViewById(R.id.text_notification_title);
            textNotificationTime = itemView.findViewById(R.id.text_notification_time);
            textNotificationContent = itemView.findViewById(R.id.text_notification_content);
            indicatorUnread = itemView.findViewById(R.id.indicator_unread);
        }

        @SuppressLint("SetTextI18n")
        public void bind(final Notification notification, final OnNotificationClickListener listener) {
            textNotificationTitle.setText(notification.getTitle());
            textNotificationContent.setText(notification.getContent());

            // Định dạng thời gian (Giữ nguyên logic của bạn)
            if (notification.getTimestamp() != null) {
                long diffMillis = System.currentTimeMillis() - notification.getTimestamp().getTime();
                long minutes = diffMillis / (60 * 1000);
                long hours = minutes / 60;
                long days = hours / 24;

                if (minutes < 1) textNotificationTime.setText("Vừa xong");
                else if (minutes < 60) textNotificationTime.setText(minutes + " phút trước");
                else if (hours < 24) textNotificationTime.setText(hours + " giờ trước");
                else if (days < 7) textNotificationTime.setText(days + " ngày trước");
                else textNotificationTime.setText(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(notification.getTimestamp()));
            } else {
                textNotificationTime.setText("");
            }

            // Hiển thị icon và màu sắc dựa trên Enum
            int iconResId = R.drawable.ic_notification_default;
            int tintColor = R.color.primary_color;

            if (notification.getType() != null) {
                switch (notification.getType()) {
                    case MESSAGE:
                        iconResId = R.drawable.ic_chat; // Đổi thành icon chat của bạn
                        tintColor = R.color.message_notification_bg;
                        break;
                    case OFFER:
                        iconResId = R.drawable.ic_trade; // Đổi thành icon offer của bạn
                        tintColor = R.color.trade_notification_bg;
                        break;
                    case LISTING:
                        iconResId = R.drawable.ic_listing; // Đổi thành icon listing của bạn
                        tintColor = R.color.listing_notification_bg;
                        break;
                    case PROMOTION:
                        iconResId = R.drawable.ic_promotion;
                        tintColor = R.color.promotion_notification_bg;
                        break;
                    case SYSTEM:
                        iconResId = R.drawable.ic_system; // Đổi thành icon system của bạn
                        tintColor = R.color.system_notification_bg;
                        break;
                }
            }
            iconNotification.setImageResource(iconResId);
            iconNotification.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(itemView.getContext(), R.color.white)));
            iconNotification.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(itemView.getContext(), tintColor)));


            // Hiển thị/ẩn chỉ báo và thay đổi màu nền
            indicatorUnread.setVisibility(notification.isRead() ? View.GONE : View.VISIBLE);
            int cardBackgroundColor = notification.isRead()
                    ? ContextCompat.getColor(itemView.getContext(), android.R.color.white)
                    : ContextCompat.getColor(itemView.getContext(), R.color.unread_notification_background);
            cardView.setCardBackgroundColor(cardBackgroundColor);

            // Bắt sự kiện click
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onNotificationClick(notification);
                }
            });
        }
    }

    // DIFF_CALLBACK để ListAdapter hoạt động hiệu quả
    private static final DiffUtil.ItemCallback<Notification> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<Notification>() {
                @Override
                public boolean areItemsTheSame(@NonNull Notification oldItem, @NonNull Notification newItem) {
                    return oldItem.getId().equals(newItem.getId());
                }

                @Override
                public boolean areContentsTheSame(@NonNull Notification oldItem, @NonNull Notification newItem) {
                    return oldItem.isRead() == newItem.isRead() &&
                            oldItem.getTitle().equals(newItem.getTitle());
                }
            };
}