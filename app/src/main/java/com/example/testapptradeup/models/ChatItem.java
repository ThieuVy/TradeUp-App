package com.example.testapptradeup.models;

import java.util.Date;

/**
 * Lớp cơ sở trừu tượng cho các item trong danh sách chat.
 * Giúp RecyclerView có thể hiển thị nhiều loại view (tin nhắn, dấu ngày tháng).
 */
public abstract class ChatItem {
    // ID duy nhất cho item, cần cho DiffUtil để hoạt động hiệu quả.
    public abstract String getItemId();

    // Timestamp của item, dùng để so sánh và quyết định chèn dấu ngày tháng.
    public abstract Date getTimestamp();
}