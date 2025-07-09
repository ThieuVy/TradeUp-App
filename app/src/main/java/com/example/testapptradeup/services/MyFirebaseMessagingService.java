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

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        Log.d(TAG, "From: " + remoteMessage.getFrom());

        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Message Notification Title: " + remoteMessage.getNotification().getTitle());
            Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
            sendNotification(remoteMessage.getNotification().getTitle(), remoteMessage.getNotification().getBody());
        }
    }

    @Override
    public void onNewToken(@NonNull String token) {
        Log.d(TAG, "Refreshed token: " + token);
        sendTokenToServer(token);
    }

    private void sendTokenToServer(String token) {
        String userId = FirebaseAuth.getInstance().getUid();
        if (userId != null) {
            new UserRepository().updateFcmToken(userId, token);
        }
    }

    private void sendNotification(String title, String messageBody) {
        // SỬA LỖI 1: Không cần this.getActivity(), dùng this vì Service là một Context
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE); // Sửa flag

        createNotificationChannel();

        // SỬA LỖI 2: Dùng `this` làm Context
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_chat)
                        .setContentTitle(title)
                        .setContentText(messageBody)
                        .setAutoCancel(true)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setContentIntent(pendingIntent);

        // SỬA LỖI 3: Dùng `this` làm Context
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        // SỬA LỖI 4: Kiểm tra quyền trước khi hiển thị thông báo
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            // Trên Android 13+, nếu không có quyền, thông báo sẽ không hiển thị.
            // Việc yêu cầu quyền cần được xử lý trong UI (Activity/Fragment).
            // Ở đây, chúng ta chỉ log lại để debug.
            Log.w(TAG, "POST_NOTIFICATIONS permission not granted. Notification will not be shown on Android 13+.");
            // Bạn có thể không hiển thị gì cả, hoặc chỉ hiển thị nếu có quyền
            return; // Không hiển thị nếu không có quyền
        }

        notificationManager.notify((int) System.currentTimeMillis(), notificationBuilder.build());
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // SỬA LỖI 5: Dùng getApplicationContext() để getString
            CharSequence name = getApplicationContext().getString(R.string.fcm_channel_name);
            String description = getApplicationContext().getString(R.string.fcm_channel_description);
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            // SỬA LỖI 6: Dùng getApplicationContext() để getSystemService
            NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
}