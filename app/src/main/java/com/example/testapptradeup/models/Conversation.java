package com.example.testapptradeup.models;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;
import java.util.List; // <<< THÊM MỚI
import java.util.Objects;

public class Conversation {
    private String id;
    private String otherUserId;
    private String otherUserName;
    private String otherUserAvatarUrl;
    private String lastMessage;
    private String lastMessageSenderId;
    private List<String> members; // Danh sách ID của 2 người dùng trong cuộc trò chuyện

    @ServerTimestamp
    private Date timestamp;
    private int unreadCount;

    public Conversation() {
        // Constructor rỗng cho Firebase
    }

    // Getters
    public String getId() { return id; }
    public String getOtherUserId() { return otherUserId; }
    public String getOtherUserName() { return otherUserName; }
    public String getOtherUserAvatarUrl() { return otherUserAvatarUrl; }
    public String getLastMessage() { return lastMessage; }
    public String getLastMessageSenderId() { return lastMessageSenderId; }
    public Date getTimestamp() { return timestamp; }
    public int getUnreadCount() { return unreadCount; }

    public List<String> getMembers() { return members; }
    public void setMembers(List<String> members) { this.members = members; }

    // Setters
    public void setId(String id) { this.id = id; }
    public void setOtherUserId(String otherUserId) { this.otherUserId = otherUserId; }
    public void setOtherUserName(String otherUserName) { this.otherUserName = otherUserName; }
    public void setOtherUserAvatarUrl(String otherUserAvatarUrl) { this.otherUserAvatarUrl = otherUserAvatarUrl; }
    public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; }
    public void setLastMessageSenderId(String lastMessageSenderId) { this.lastMessageSenderId = lastMessageSenderId; }
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