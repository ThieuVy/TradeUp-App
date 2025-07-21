package com.example.testapptradeup.models;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class ChatMessage extends ChatItem {
    private String messageId;
    private String chatId;
    private String senderId;
    private String text;
    private String imageUrl;
    @ServerTimestamp
    private Date timestamp;
    private String messageType = "TEXT"; // Mặc định là TEXT
    private double offerPrice;

    public ChatMessage() {}

    public ChatMessage(String senderId, String text, String imageUrl) {
        this.senderId = senderId;
        this.text = text;
        this.imageUrl = imageUrl;
    }

    // Getters and Setters (giữ nguyên)
    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }
    public String getChatId() { return chatId; }
    public void setChatId(String chatId) { this.chatId = chatId; }
    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    @Override
    public Date getTimestamp() { return timestamp; }

    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }

    @Override
    public String getItemId() {
        // Đảm bảo messageId không null để tránh crash
        return messageId != null ? messageId : "";
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public double getOfferPrice() {
        return offerPrice;
    }

    public void setOfferPrice(double offerPrice) {
        this.offerPrice = offerPrice;
    }
}