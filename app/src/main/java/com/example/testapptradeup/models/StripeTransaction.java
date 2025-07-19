package com.example.testapptradeup.models;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class StripeTransaction {
    private String id;
    private String description;
    private long amount;
    private String currency;
    private String status;
    private long created; // Unix timestamp

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public long getAmount() { return amount; }
    public void setAmount(long amount) { this.amount = amount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public long getCreated() { return created; }
    public void setCreated(long created) { this.created = created; }

    // Helper methods
    public String getFormattedAmount() {
        // Stripe trả về amount ở đơn vị nhỏ nhất, nhưng với VND là 1.
        NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        return format.format(amount);
    }

    public String getFormattedDate() {
        if (created == 0) return "";
        Date date = new Date(created * 1000L); // Convert Unix timestamp to milliseconds
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getDefault());
        return sdf.format(date);
    }
}