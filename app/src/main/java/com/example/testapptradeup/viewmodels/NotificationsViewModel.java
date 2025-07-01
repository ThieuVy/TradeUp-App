package com.example.testapptradeup.viewmodels;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.testapptradeup.models.NotificationItem;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query; // For ordering
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class NotificationsViewModel extends ViewModel {

    private static final String TAG = "NotificationsViewModel";

    private final MutableLiveData<List<NotificationItem>> notifications = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Integer> unreadCount = new MutableLiveData<>();

    private final FirebaseFirestore db;
    private String currentUserId;

    // A copy of all fetched notifications (before filtering by tab)
    private List<NotificationItem> allNotificationsCache = new ArrayList<>();

    public NotificationsViewModel() {
        db = FirebaseFirestore.getInstance();
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        isLoading.setValue(false);
        unreadCount.setValue(0);

        // Get current user ID (or observe auth state if your app handles dynamic sign-in/out)
        if (mAuth.getCurrentUser() != null) {
            currentUserId = mAuth.getCurrentUser().getUid();
        } else {
            // Handle case where user is not logged in
            errorMessage.setValue("Người dùng chưa đăng nhập.");
            Log.e(TAG, "NotificationsViewModel initialized without a logged-in user.");
        }
    }

    // Getters for LiveData
    public LiveData<List<NotificationItem>> getNotifications() { return notifications; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<Integer> getUnreadCount() { return unreadCount; }

    /**
     * Loads notifications for the current user from Firestore.
     * @param filterCategory Optional category to filter notifications by. Null or "All" for all.
     */
    public void loadNotifications(String filterCategory) {
        if (currentUserId == null) {
            errorMessage.setValue("Không thể tải thông báo: Người dùng chưa đăng nhập.");
            return;
        }

        isLoading.setValue(true);
        db.collection("notifications")
                .whereEqualTo("userId", currentUserId)
                .orderBy("timestamp", Query.Direction.DESCENDING) // Sắp xếp theo thời gian mới nhất trước
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.w(TAG, "Lỗi khi lắng nghe thông báo: ", error);
                        errorMessage.setValue("Lỗi tải thông báo: " + error.getMessage());
                        isLoading.setValue(false);
                        return;
                    }

                    List<NotificationItem> fetchedNotifications = new ArrayList<>();
                    int currentUnreadCount = 0;
                    if (value != null) {
                        for (QueryDocumentSnapshot doc : value) {
                            NotificationItem notification = doc.toObject(NotificationItem.class);
                            notification.setId(doc.getId()); // Đặt ID tài liệu vào đối tượng
                            fetchedNotifications.add(notification);
                            if (!notification.isRead()) {
                                currentUnreadCount++;
                            }
                        }
                    }
                    allNotificationsCache = fetchedNotifications; // Cache all fetched notifications
                    filterAndSetNotifications(filterCategory); // Apply filter and update LiveData
                    unreadCount.setValue(currentUnreadCount);
                    isLoading.setValue(false);
                });
    }

    /**
     * Filters the cached notifications and updates the LiveData.
     * Called after initial load and when filter category changes.
     */
    private void filterAndSetNotifications(String filterCategory) {
        if (filterCategory == null || filterCategory.equalsIgnoreCase("Tất cả")) {
            notifications.setValue(new ArrayList<>(allNotificationsCache));
        } else {
            List<NotificationItem> filteredList = new ArrayList<>();
            for (NotificationItem item : allNotificationsCache) {
                if (item.getCategory() != null && item.getCategory().equalsIgnoreCase(filterCategory)) {
                    filteredList.add(item);
                }
            }
            notifications.setValue(filteredList);
        }
    }


    /**
     * Marks a notification as read in Firestore.
     * @param notificationId ID of the notification to mark as read.
     */
    public void markAsRead(String notificationId) {
        if (currentUserId == null) {
            errorMessage.setValue("Không thể đánh dấu đã đọc: Người dùng chưa đăng nhập.");
            return;
        }
        db.collection("notifications").document(notificationId)
                .update("read", true) // Assuming 'read' field in Firestore
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Đánh dấu thông báo " + notificationId + " là đã đọc thành công."))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi đánh dấu thông báo đã đọc: " + e.getMessage(), e);
                    errorMessage.setValue("Lỗi đánh dấu thông báo đã đọc: " + e.getMessage());
                });
    }

    /**
     * Marks all unread notifications as read for the current user.
     */
    public void markAllAsRead() {
        if (currentUserId == null) {
            errorMessage.setValue("Không thể đánh dấu tất cả đã đọc: Người dùng chưa đăng nhập.");
            return;
        }

        db.collection("notifications")
                .whereEqualTo("userId", currentUserId)
                .whereEqualTo("read", false)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        doc.getReference().update("read", true)
                                .addOnFailureListener(e -> Log.e(TAG, "Lỗi khi cập nhật thông báo riêng lẻ: " + e.getMessage(), e));
                    }
                    Log.d(TAG, "Đã gửi yêu cầu đánh dấu tất cả thông báo là đã đọc.");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi khi truy vấn thông báo để đánh dấu đã đọc: " + e.getMessage(), e);
                    errorMessage.setValue("Lỗi đánh dấu tất cả thông báo đã đọc: " + e.getMessage());
                });
    }

    /**
     * Deletes a specific notification from Firestore.
     * @param notificationId ID of the notification to delete.
     */
    public void deleteNotification(String notificationId) {
        if (currentUserId == null) {
            errorMessage.setValue("Không thể xóa thông báo: Người dùng chưa đăng nhập.");
            return;
        }

        db.collection("notifications").document(notificationId)
                .delete()
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Xóa thông báo " + notificationId + " thành công."))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi xóa thông báo: " + e.getMessage(), e);
                    errorMessage.setValue("Lỗi xóa thông báo: " + e.getMessage());
                });
    }

    /**
     * Deletes all notifications for the current user.
     */
    public void clearAllNotifications() {
        if (currentUserId == null) {
            errorMessage.setValue("Không thể xóa tất cả thông báo: Người dùng chưa đăng nhập.");
            return;
        }

        db.collection("notifications")
                .whereEqualTo("userId", currentUserId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        doc.getReference().delete()
                                .addOnFailureListener(e -> Log.e(TAG, "Lỗi khi xóa thông báo riêng lẻ: " + e.getMessage(), e));
                    }
                    Log.d(TAG, "Đã gửi yêu cầu xóa tất cả thông báo.");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi khi truy vấn thông báo để xóa: " + e.getMessage(), e);
                    errorMessage.setValue("Lỗi xóa tất cả thông báo: " + e.getMessage());
                });
    }


    /**
     * Clears any current error message.
     */
    public void clearErrorMessage() {
        errorMessage.setValue(null);
    }
}
