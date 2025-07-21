package com.example.testapptradeup.models;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;
import com.google.firebase.database.ServerValue;

@IgnoreExtraProperties
public class UserStatus {

    private boolean isOnline = false;
    private Object last_seen = ServerValue.TIMESTAMP; // BƯỚC 1: Đổi kiểu thành Object

    public UserStatus() {}

    public UserStatus(boolean isOnline) {
        this.isOnline = isOnline;
        this.last_seen = ServerValue.TIMESTAMP;
    }

    // --- GETTERS & SETTERS ---

    public boolean isOnline() {
        return isOnline;
    }

    public void setOnline(boolean online) {
        isOnline = online;
    }

    // Getter cho Firebase (nó sẽ gọi hàm này để lấy giá trị ghi vào DB)
    public Object getLast_seen() {
        return last_seen;
    }

    public void setLast_seen(Object last_seen) {
        this.last_seen = last_seen;
    }

    // BƯỚC 2: Tạo một getter tùy chỉnh để luôn trả về Long
    @Exclude // Annotation này để Firebase không cố gắng ghi/đọc trường ảo này
    public long getLastSeenLong() {
        if (last_seen instanceof Long) {
            return (Long) last_seen;
        }
        // Nếu không phải Long (có thể là Map hoặc null), trả về 0
        return 0;
    }
}