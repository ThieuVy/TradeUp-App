// File: app/src/main/java/com/example/testapptradeup/models/Report.java
package com.example.testapptradeup.models;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;
import java.util.Objects;

public class Report {
    @DocumentId
    private String id;
    private String type; // "listing", "profile", "conversation"
    private String reporterId;
    private String reportedUserId;
    private String reportedListingId; // Có thể null
    private String chatId; // Có thể null
    private String reason;
    private String status; // "pending", "resolved"
    @ServerTimestamp
    private Date timestamp;

    public Report() {
        // Constructor rỗng cho Firebase
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getReporterId() { return reporterId; }
    public void setReporterId(String reporterId) { this.reporterId = reporterId; }
    public String getReportedUserId() { return reportedUserId; }
    public void setReportedUserId(String reportedUserId) { this.reportedUserId = reportedUserId; }
    public String getReportedListingId() { return reportedListingId; }
    public void setReportedListingId(String reportedListingId) { this.reportedListingId = reportedListingId; }
    public String getChatId() { return chatId; }
    public void setChatId(String chatId) { this.chatId = chatId; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Report report = (Report) o;
        return Objects.equals(id, report.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}