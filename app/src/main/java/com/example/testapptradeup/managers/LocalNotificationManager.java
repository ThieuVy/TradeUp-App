package com.example.testapptradeup.managers;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.testapptradeup.models.NotificationItem;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Date; // Thêm import Date
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * Manager class for handling notification operations
 * This class provides methods to save/load notifications from SharedPreferences
 * and manage notification states
 * NOTE: This class requires Gson library. Add to your app's build.gradle:
 * implementation 'com.google.code.gson:gson:2.10.1'
 */
public class LocalNotificationManager {

    private static final String PREFS_NAME = "notifications_prefs";
    private static final String KEY_NOTIFICATIONS = "notifications_list";

    private final SharedPreferences sharedPreferences;
    private final Gson gson;

    // Singleton pattern
    private static LocalNotificationManager instance;

    private LocalNotificationManager(Context context) {
        context.getApplicationContext();
        this.sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.gson = new Gson();
    }

    public static synchronized LocalNotificationManager getInstance(Context context) {
        if (instance == null) {
            instance = new LocalNotificationManager(context);
        }
        return instance;
    }

    /**
     * Save notifications to SharedPreferences
     */
    public void saveNotifications(List<NotificationItem> notifications) {
        try {
            String json = gson.toJson(notifications);
            sharedPreferences.edit()
                    .putString(KEY_NOTIFICATIONS, json)
                    .apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Load notifications from SharedPreferences
     */
    public List<NotificationItem> loadNotifications() {
        try {
            String json = sharedPreferences.getString(KEY_NOTIFICATIONS, null);
            if (json != null && !json.isEmpty()) {
                Type listType = new TypeToken<List<NotificationItem>>(){}.getType();
                List<NotificationItem> notifications = gson.fromJson(json, listType);
                // Đảm bảo type enum được khôi phục đúng cách nếu lưu dưới dạng String
                if (notifications != null) {
                    for (NotificationItem item : notifications) {
                        // Gson sẽ tự động ánh xạ nếu trường `type` là NotificationType.
                        // Nếu bạn lưu dưới dạng String và có setter setTypeString(String), thì cần gọi:
                        // item.setTypeString(item.getTypeString()); // Để gọi setter chuyển đổi
                    }
                    return notifications;
                }
                return new ArrayList<>();
            }
        } catch (Exception e) {
            e.printStackTrace();
            // If there's an error loading, clear corrupted data
            clearAllNotifications();
        }
        return new ArrayList<>();
    }

    /**
     * Add a new notification
     */
    public void addNotification(NotificationItem notification) {
        if (notification == null) return;

        List<NotificationItem> notifications = loadNotifications();
        notifications.add(0, notification); // Add to beginning
        saveNotifications(notifications);
    }

    /**
     * Mark notification as read
     */
    public void markAsRead(String notificationId) {
        if (notificationId == null || notificationId.isEmpty()) return;

        List<NotificationItem> notifications = loadNotifications();
        boolean found = false;
        for (NotificationItem item : notifications) {
            if (notificationId.equals(item.getId())) {
                item.setRead(true);
                found = true;
                break;
            }
        }
        if (found) {
            saveNotifications(notifications);
        }
    }

    /**
     * Mark all notifications as read
     */
    public void markAllAsRead() {
        List<NotificationItem> notifications = loadNotifications();
        boolean hasChanges = false;
        for (NotificationItem item : notifications) {
            if (!item.isRead()) {
                item.setRead(true);
                hasChanges = true;
            }
        }
        if (hasChanges) {
            saveNotifications(notifications);
        }
    }

    /**
     * Delete a notification
     */
    public void deleteNotification(String notificationId) {
        if (notificationId == null || notificationId.isEmpty()) return;

        List<NotificationItem> notifications = loadNotifications();
        boolean removed = false;

        // Use iterator to safely remove items while iterating
        Iterator<NotificationItem> iterator = notifications.iterator();
        while (iterator.hasNext()) {
            NotificationItem item = iterator.next();
            if (notificationId.equals(item.getId())) {
                iterator.remove();
                removed = true;
                break; // Assuming IDs are unique, we can break after first match
            }
        }

        if (removed) {
            saveNotifications(notifications);
        }
    }

    /**
     * Clear all notifications
     */
    public void clearAllNotifications() {
        sharedPreferences.edit()
                .remove(KEY_NOTIFICATIONS)
                .apply();
    }

    /**
     * Get unread notifications count
     */
    public int getUnreadCount() {
        List<NotificationItem> notifications = loadNotifications();
        int count = 0;
        for (NotificationItem item : notifications) {
            if (!item.isRead()) {
                count++;
            }
        }
        return count;
    }

    /**
     * Get total notifications count
     */
    public int getTotalCount() {
        return loadNotifications().size();
    }

    /**
     * Create and add a trade notification
     */
    public void createTradeNotification(String userId, String title, String content) {
        if (userId == null || title == null || content == null) return;

        NotificationItem notification = new NotificationItem(
                UUID.randomUUID().toString(),
                userId,
                title,
                content,
                new Date(), // timestamp hiện tại
                NotificationItem.NotificationType.TRADE,
                "Trading", // Category
                false, // isRead
                null
        );
        addNotification(notification);
    }

    /**
     * Create and add a price alert notification
     */
    public void createPriceAlertNotification(String userId, String symbol, double price, String content) {
        if (userId == null || symbol == null || content == null) return;

        String title = "Price Alert: " + symbol;
        NotificationItem notification = new NotificationItem(
                UUID.randomUUID().toString(),
                userId,
                title,
                content,
                new Date(),
                NotificationItem.NotificationType.PRICE_ALERT,
                "Price Alerts", // Category
                false,
                null
        );
        addNotification(notification);
    }

    /**
     * Create and add a news notification
     */
    public void createNewsNotification(String userId, String title, String content, String actionUrl) {
        if (userId == null || title == null || content == null) return;

        NotificationItem notification = new NotificationItem(
                UUID.randomUUID().toString(),
                userId,
                title,
                content,
                new Date(),
                NotificationItem.NotificationType.NEWS,
                "News", // Category
                false,
                actionUrl
        );
        addNotification(notification);
    }

    /**
     * Create and add a system notification
     */
    public void createSystemNotification(String userId, String title, String content) {
        if (userId == null || title == null || content == null) return;

        NotificationItem notification = new NotificationItem(
                UUID.randomUUID().toString(),
                userId,
                title,
                content,
                new Date(),
                NotificationItem.NotificationType.SYSTEM,
                "System", // Category
                false,
                null
        );
        addNotification(notification);
    }

    /**
     * Create and add a message notification
     */
    public void createMessageNotification(String userId, String title, String content, String actionUrl) {
        if (userId == null || title == null || content == null) return;

        NotificationItem notification = new NotificationItem(
                UUID.randomUUID().toString(),
                userId,
                title,
                content,
                new Date(),
                NotificationItem.NotificationType.MESSAGE,
                "Tin nhắn", // Category (Sử dụng tên tiếng Việt cho phù hợp với Tab Layout)
                false,
                actionUrl
        );
        addNotification(notification);
    }

    /**
     * Create and add a promotion notification
     */
    public void createPromotionNotification(String userId, String title, String content, String actionUrl) {
        if (userId == null || title == null || content == null) return;

        NotificationItem notification = new NotificationItem(
                UUID.randomUUID().toString(),
                userId,
                title,
                content,
                new Date(),
                NotificationItem.NotificationType.PROMOTION,
                "Ưu đãi", // Category
                false,
                actionUrl
        );
        addNotification(notification);
    }

    /**
     * Create and add a listing notification (e.g., listing accepted/rejected, expired)
     */
    public void createListingNotification(String userId, String title, String content, String actionUrl) {
        if (userId == null || title == null || content == null) return;

        NotificationItem notification = new NotificationItem(
                UUID.randomUUID().toString(),
                userId,
                title,
                content,
                new Date(),
                NotificationItem.NotificationType.LISTING,
                "Danh sách", // Category
                false,
                actionUrl
        );
        addNotification(notification);
    }


    /**
     * Get notifications by type
     */
    public List<NotificationItem> getNotificationsByType(NotificationItem.NotificationType type) {
        if (type == null) return new ArrayList<>();

        List<NotificationItem> allNotifications = loadNotifications();
        List<NotificationItem> filteredNotifications = new ArrayList<>();

        for (NotificationItem item : allNotifications) {
            if (item.getType() != null && type.equals(item.getType())) { // So sánh với Enum
                filteredNotifications.add(item);
            }
        }

        return filteredNotifications;
    }

    /**
     * Get recent notifications (last 7 days)
     */
    public List<NotificationItem> getRecentNotifications() {
        List<NotificationItem> allNotifications = loadNotifications();
        List<NotificationItem> recentNotifications = new ArrayList<>();

        long sevenDaysAgo = System.currentTimeMillis() - (7L * 24 * 60 * 60 * 1000);

        for (NotificationItem item : allNotifications) {
            if (item.getTimestamp() != null && item.getTimestamp().getTime() > sevenDaysAgo) {
                recentNotifications.add(item);
            }
        }

        return recentNotifications;
    }

    /**
     * Clean old notifications (older than 30 days)
     */
    public void cleanOldNotifications() {
        List<NotificationItem> allNotifications = loadNotifications();
        List<NotificationItem> validNotifications = new ArrayList<>();

        long thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000);

        for (NotificationItem item : allNotifications) {
            if (item.getTimestamp() != null && item.getTimestamp().getTime() > thirtyDaysAgo) {
                validNotifications.add(item);
            }
        }

        // Only save if there were changes
        if (validNotifications.size() != allNotifications.size()) {
            saveNotifications(validNotifications);
        }
    }

    /**
     * Check if notification exists
     */
    public boolean notificationExists(String notificationId) {
        if (notificationId == null || notificationId.isEmpty()) return false;

        List<NotificationItem> notifications = loadNotifications();
        for (NotificationItem item : notifications) {
            if (notificationId.equals(item.getId())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get notification by ID
     */
    public NotificationItem getNotificationById(String notificationId) {
        if (notificationId == null || notificationId.isEmpty()) return null;

        List<NotificationItem> notifications = loadNotifications();
        for (NotificationItem item : notifications) {
            if (notificationId.equals(item.getId())) {
                return item;
            }
        }
        return null;
    }
}
