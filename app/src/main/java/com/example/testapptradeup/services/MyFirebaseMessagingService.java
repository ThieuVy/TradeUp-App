package com.example.testapptradeup.services;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.testapptradeup.R;
import com.example.testapptradeup.activities.MainActivity;
import com.example.testapptradeup.repositories.UserRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "MyFirebaseMsgService";
    private static final String CHANNEL_ID = "tradeup_chat_channel";

    /**
     * Được gọi khi có tin nhắn mới.
     * @param remoteMessage Đối tượng chứa thông tin tin nhắn.
     */
    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        Log.d(TAG, "From: " + remoteMessage.getFrom());

        // Kiểm tra xem tin nhắn có chứa payload notification không.
        if (remoteMessage.getNotification() != null) {
            String title = remoteMessage.getNotification().getTitle();
            String body = remoteMessage.getNotification().getBody();
            Log.d(TAG, "Message Notification Title: " + title);
            Log.d(TAG, "Message Notification Body: " + body);

            sendNotification(title, body);
        }

        // Kiểm tra xem tin nhắn có chứa payload data không.
        if (!remoteMessage.getData().isEmpty()) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());
            // TODO: Xử lý payload data ở đây nếu cần (ví dụ: cập nhật UI trong ứng dụng)
        }
    }

    /**
     * Được gọi khi FCM cấp một token mới hoặc token hiện tại được làm mới.
     * @param token Token mới.
     */
    @Override
    public void onNewToken(@NonNull String token) {
        Log.d(TAG, "Refreshed token: " + token);
        sendTokenToServer(token);
    }

    /**
     * Gửi token lên server để lưu vào Firestore.
     * @param token Token cần gửi.
     */
    private void sendTokenToServer(String token) {
        String userId = FirebaseAuth.getInstance().getUid();
        if (userId != null) {
            new UserRepository().updateFcmToken(userId, token);
        }
    }

    /**
     * Tạo và hiển thị một thông báo đơn giản.
     * @param title Tiêu đề thông báo.
     * @param messageBody Nội dung thông báo.
     */
    private void sendNotification(String title, String messageBody) {
        // Tạo Intent để mở MainActivity khi người dùng nhấn vào thông báo
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

        createNotificationChannel();

        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_chat) // Icon nhỏ trên status bar
                        .setContentTitle(title)
                        .setContentText(messageBody)
                        .setAutoCancel(true) // Tự động xóa thông báo khi nhấn vào
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setContentIntent(pendingIntent);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        // Kiểm tra quyền POST_NOTIFICATIONS trước khi hiển thị (bắt buộc cho Android 13+)
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Quyền POST_NOTIFICATIONS chưa được cấp. Thông báo sẽ không hiển thị trên Android 13+.");
            // Trên thực tế, bạn cần yêu cầu quyền này trong Activity/Fragment.
            return;
        }

        notificationManager.notify((int) System.currentTimeMillis(), notificationBuilder.build());
    }

    /**
     * Tạo Notification Channel, cần thiết cho Android 8.0 (API 26) trở lên.
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.fcm_channel_name);
            String description = getString(R.string.fcm_channel_description);
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}