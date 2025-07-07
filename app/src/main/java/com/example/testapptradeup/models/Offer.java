package com.example.testapptradeup.models;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class Offer {
    private String id;
    private String listingId;
    private String buyerId;
    private String buyerName;
    private String buyerAvatarUrl;
    private String sellerId;
    private double offerPrice;
    private String message;
    private String status; // "pending", "accepted", "rejected", "countered"
    @ServerTimestamp
    private Date timestamp;

    public Offer() {}

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getListingId() { return listingId; }
    public void setListingId(String listingId) { this.listingId = listingId; }
    public String getBuyerId() { return buyerId; }
    public void setBuyerId(String buyerId) { this.buyerId = buyerId; }
    public String getBuyerName() { return buyerName; }
    public void setBuyerName(String buyerName) { this.buyerName = buyerName; }
    public String getBuyerAvatarUrl() { return buyerAvatarUrl; }
    public void setBuyerAvatarUrl(String buyerAvatarUrl) { this.buyerAvatarUrl = buyerAvatarUrl; }
    public String getSellerId() { return sellerId; }
    public void setSellerId(String sellerId) { this.sellerId = sellerId; }
    public double getOfferPrice() { return offerPrice; }
    public void setOfferPrice(double offerPrice) { this.offerPrice = offerPrice; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }
}