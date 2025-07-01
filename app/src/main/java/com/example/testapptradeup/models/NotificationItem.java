package com.example.testapptradeup.models;

import java.util.Date;

public class NotificationItem {
    private String id; // Document ID from Firestore
    private String userId; // ID of the user this notification belongs to
    private String title;
    private String content;
    private Date timestamp;
    private NotificationType type; // ĐÃ SỬA: Thay đổi kiểu dữ liệu từ String sang Enum
    private String category; // e.g., "Trading", "Promotions", "System"
    private boolean isRead;
    private String actionUrl; // Optional URL or ID for deep linking

    // Định nghĩa Enum cho các loại thông báo
    public enum NotificationType {
        TRADE, PRICE_ALERT, NEWS, SYSTEM, MESSAGE, PROMOTION, LISTING, OTHER
    }

    public NotificationItem() {
        // Constructor rỗng cần thiết cho Firestore
    }

    // Constructor đã cập nhật để sử dụng NotificationType
    public NotificationItem(String id, String userId, String title, String content, Date timestamp,
                            NotificationType type, String category, boolean isRead, String actionUrl) {
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.content = content;
        this.timestamp = timestamp;
        this.type = type;
        this.category = category;
        this.isRead = isRead;
        this.actionUrl = actionUrl;
    }

    // Constructor phụ cho LocalNotificationManager (nếu bạn vẫn muốn dùng)
    // Lưu ý: Constructor này không bao gồm userId, timestamp, category, isRead
    // Nếu bạn đang chuyển hoàn toàn sang Firestore, hãy xem xét loại bỏ constructor này
    public NotificationItem(String id, String title, String content, NotificationType type) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.type = type;
        this.timestamp = new Date(); // Mặc định thời gian hiện tại
        this.isRead = false; // Mặc định chưa đọc
        this.category = "OTHER"; // Mặc định category
        this.userId = ""; // Mặc định userId
        this.actionUrl = ""; // Mặc định actionUrl
    }

    // Getters
    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public Date getTimestamp() { return timestamp; }

    // ĐÃ SỬA: Getter trả về NotificationType
    public NotificationType getType() { return type; }
    // Getter trả về String của type cho Firestore (nếu cần)
    public String getTypeString() { return type != null ? type.name() : null; }

    public String getCategory() { return category; }
    public boolean isRead() { return isRead; }
    public String getActionUrl() { return actionUrl; }

    // Setters (Firestore cần setters để ánh xạ dữ liệu)
    public void setId(String id) { this.id = id; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setTitle(String title) { this.title = title; }
    public void setContent(String content) { this.content = content; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }

    // ĐÃ SỬA: Setter nhận NotificationType
    public void setType(NotificationType type) { this.type = type; }
    // Setter nhận String và chuyển đổi sang NotificationType cho Firestore (nếu cần)
    public void setTypeString(String typeString) {
        try {
            this.type = NotificationType.valueOf(typeString);
        } catch (IllegalArgumentException | NullPointerException e) {
            this.type = NotificationType.OTHER; // Mặc định nếu không khớp
        }
    }

    public void setCategory(String category) { this.category = category; }
    public void setRead(boolean read) { isRead = read; }
    public void setActionUrl(String actionUrl) { this.actionUrl = actionUrl; }
}
