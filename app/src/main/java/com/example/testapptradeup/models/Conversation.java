package com.example.testapptradeup.models;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;
import java.util.Objects;

public class Conversation {
    private String id; // ID này sẽ được gán thủ công trong Repository
    private String otherUserId;
    private String otherUserName;
    private String otherUserAvatarUrl;
    private String lastMessage;
    @ServerTimestamp
    private Date timestamp;
    private int unreadCount;

    public Conversation() {
        // Constructor rỗng cho Firebase
    }

    // Getters
    // REMOVE @Exclude from here
    public String getId() { return id; }

    // Setters
    // REMOVE @Exclude from here
    public void setId(String id) { this.id = id; }

    public String getOtherUserId() { return otherUserId; }
    public String getOtherUserName() { return otherUserName; }
    public String getOtherUserAvatarUrl() { return otherUserAvatarUrl; }
    public String getLastMessage() { return lastMessage; }
    public Date getTimestamp() { return timestamp; }
    public int getUnreadCount() { return unreadCount; }

    public void setOtherUserId(String otherUserId) { this.otherUserId = otherUserId; }
    public void setOtherUserName(String otherUserName) { this.otherUserName = otherUserName; }
    public void setOtherUserAvatarUrl(String otherUserAvatarUrl) { this.otherUserAvatarUrl = otherUserAvatarUrl; }
    public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }
    public void setUnreadCount(int unreadCount) { this.unreadCount = unreadCount; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Conversation that = (Conversation) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}