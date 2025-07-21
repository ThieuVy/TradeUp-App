package com.example.testapptradeup.viewmodels;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.testapptradeup.models.Notification;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class NotificationsViewModel extends ViewModel {
    private static final String TAG = "NotificationsViewModel";

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final String currentUserId;

    private final MutableLiveData<List<Notification>> notifications = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Integer> unreadCount = new MutableLiveData<>(0);

    public NotificationsViewModel() {
        this.currentUserId = (FirebaseAuth.getInstance().getCurrentUser() != null)
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;
        if (currentUserId != null) {
            loadNotifications("Tất cả");
        } else {
            errorMessage.setValue("Người dùng chưa đăng nhập.");
        }
    }

    // --- Getters cho LiveData ---
    public LiveData<List<Notification>> getNotifications() { return notifications; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<Integer> getUnreadCount() { return unreadCount; }

    /**
     * Tải thông báo từ collection "notifications" cấp cao nhất.
     */
    public void loadNotifications(@NonNull String filterCategory) {
        if (currentUserId == null) {
            errorMessage.setValue("Không thể tải thông báo: Người dùng chưa đăng nhập.");
            notifications.setValue(new ArrayList<>());
            return;
        }

        isLoading.setValue(true);

        // =======================================================
        // SỬA LỖI: THAY ĐỔI CÂU TRUY VẤN
        // Truy vấn vào collection "notifications" cấp cao nhất
        // và lọc theo trường "userId" trong document.
        // =======================================================
        Query query = db.collection("notifications")
                .whereEqualTo("userId", currentUserId) // <-- THAY ĐỔI QUAN TRỌNG
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(50);

        // Áp dụng bộ lọc category (nếu có)
        if (!"Tất cả".equalsIgnoreCase(filterCategory)) {
            try {
                Notification.NotificationType type = Notification.NotificationType.valueOf(filterCategory.toUpperCase());
                query = query.whereEqualTo("type", type);
            } catch (IllegalArgumentException e) {
                // Xử lý nếu category không hợp lệ
                isLoading.setValue(false);
                notifications.setValue(new ArrayList<>());
                return;
            }
        }

        query.addSnapshotListener((value, error) -> {
            isLoading.setValue(false);
            if (error != null) {
                Log.e(TAG, "Lỗi khi lắng nghe thông báo", error);
                errorMessage.setValue("Lỗi tải thông báo.");
                return;
            }

            if (value != null) {
                List<Notification> loadedNotifications = new ArrayList<>();
                int currentUnreadCount = 0;
                for (QueryDocumentSnapshot doc : value) {
                    Notification notification = doc.toObject(Notification.class);
                    loadedNotifications.add(notification);
                    if (!notification.isRead()) {
                        currentUnreadCount++;
                    }
                }
                notifications.setValue(loadedNotifications);
                unreadCount.setValue(currentUnreadCount);
            }
        });
    }

    /**
     * Đánh dấu đã đọc trong collection "notifications" cấp cao nhất.
     */
    public void markAsRead(String notificationId) {
        if (notificationId == null) return;
        // SỬA: Truy cập thẳng vào collection "notifications"
        db.collection("notifications").document(notificationId)
                .update("read", true)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Đánh dấu thông báo " + notificationId + " là đã đọc thành công."))
                .addOnFailureListener(e -> Log.e(TAG, "Lỗi khi đánh dấu đã đọc", e));
    }

    /**
     * Đánh dấu tất cả là đã đọc trong collection "notifications" cấp cao nhất.
     */
    public void markAllAsRead() {
        if (currentUserId == null) return;
        // SỬA: Truy vấn collection "notifications" và lọc theo userId
        db.collection("notifications")
                .whereEqualTo("userId", currentUserId)
                .whereEqualTo("read", false)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        doc.getReference().update("read", true);
                    }
                });
    }

    /**
     * Xóa tất cả thông báo trong collection "notifications" cấp cao nhất.
     */
    public void clearAllNotifications() {
        if (currentUserId == null) return;
        // SỬA: Truy vấn collection "notifications" và lọc theo userId để xóa
        db.collection("notifications")
                .whereEqualTo("userId", currentUserId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        doc.getReference().delete();
                    }
                });
    }

    public void clearErrorMessage() {
        errorMessage.setValue(null);
    }
}