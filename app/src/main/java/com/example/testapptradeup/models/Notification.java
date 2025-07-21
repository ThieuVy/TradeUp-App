package com.example.testapptradeup.models;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.PropertyName; // <-- THÊM IMPORT NÀY
import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class Notification {

    public enum NotificationType {
        MESSAGE, OFFER, LISTING, PROMOTION, SYSTEM, OTHER, TRADE, PRICE_ALERT, NEWS
    }

    @DocumentId
    private String id;
    private String title;
    private String content;
    private NotificationType type;

    // =======================================================
    // SỬA LỖI CUSTOM CLASS MAPPER
    // Tên trường trong Firestore là "read", nhưng getter là "isRead()"
    // Dùng @PropertyName để chỉ cho Firestore biết điều này.
    // =======================================================
    @PropertyName("read")
    private boolean isRead;

    private String relatedId;
    private String senderId;
    private String userId;
    private String category;
    private String actionUrl;

    @ServerTimestamp
    private Date timestamp;

    public Notification() {}

    // --- GETTERS AND SETTERS ---

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public NotificationType getType() { return type; }
    public void setType(NotificationType type) { this.type = type; }

    // Setter và Getter cho isRead
    @PropertyName("read")
    public boolean isRead() { return isRead; }
    @PropertyName("read")
    public void setRead(boolean read) { isRead = read; }

    public String getRelatedId() { return relatedId; }
    public void setRelatedId(String relatedId) { this.relatedId = relatedId; }
    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getActionUrl() { return actionUrl; }
    public void setActionUrl(String actionUrl) { this.actionUrl = actionUrl; }
    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }
}