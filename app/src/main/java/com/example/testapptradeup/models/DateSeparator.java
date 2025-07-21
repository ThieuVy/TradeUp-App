package com.example.testapptradeup.models;

import java.util.Date;

/**
 * Model đại diện cho một item dấu phân cách ngày tháng trong danh sách chat.
 */
public class DateSeparator extends ChatItem {
    private final Date date;

    public DateSeparator(Date date) {
        this.date = date;
    }

    @Override
    public String getItemId() {
        // Sử dụng giá trị long của timestamp làm ID duy nhất.
        return String.valueOf(date.getTime());
    }

    @Override
    public Date getTimestamp() {
        return date;
    }
}