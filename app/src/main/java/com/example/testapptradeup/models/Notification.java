package com.example.testapptradeup.models;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

// Lớp POJO để ánh xạ dữ liệu từ Firestore
public class Notification {
    private String title;
    private String content;
    private String type; // "message", "offer", "listing", "promotion"
    private boolean read;
    @ServerTimestamp
    private Date timestamp;

    // Cần có một constructor rỗng cho Firestore
    public Notification() {}

    public Notification(String title, String content, String type, boolean read) {
        this.title = title;
        this.content = content;
        this.type = type;
        this.read = read;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }
}