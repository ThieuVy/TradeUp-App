package com.example.testapptradeup.models;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class Transaction {
    private String id;
    private String listingId;
    private String listingTitle;
    private String listingImageUrl;
    private String sellerId;
    private String sellerName;
    private String buyerId;
    private String buyerName;
    private double finalPrice;
    private boolean sellerReviewed;
    private boolean buyerReviewed;
    @ServerTimestamp
    private Date transactionDate;
    private String paymentMethod;
    public Transaction() {}

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getListingId() { return listingId; }
    public void setListingId(String listingId) { this.listingId = listingId; }
    public String getListingTitle() { return listingTitle; }
    public void setListingTitle(String listingTitle) { this.listingTitle = listingTitle; }
    public String getListingImageUrl() { return listingImageUrl; }
    public void setListingImageUrl(String listingImageUrl) { this.listingImageUrl = listingImageUrl; }
    public String getSellerId() { return sellerId; }
    public void setSellerId(String sellerId) { this.sellerId = sellerId; }
    public String getSellerName() { return sellerName; }
    public void setSellerName(String sellerName) { this.sellerName = sellerName; }
    public String getBuyerId() { return buyerId; }
    public void setBuyerId(String buyerId) { this.buyerId = buyerId; }
    public String getBuyerName() { return buyerName; }
    public void setBuyerName(String buyerName) { this.buyerName = buyerName; }
    public double getFinalPrice() { return finalPrice; }
    public void setFinalPrice(double finalPrice) { this.finalPrice = finalPrice; }
    public boolean isSellerReviewed() { return sellerReviewed; }
    public void setSellerReviewed(boolean sellerReviewed) { this.sellerReviewed = sellerReviewed; }
    public boolean isBuyerReviewed() { return buyerReviewed; }
    public void setBuyerReviewed(boolean buyerReviewed) { this.buyerReviewed = buyerReviewed; }
    public Date getTransactionDate() { return transactionDate; }
    public void setTransactionDate(Date transactionDate) { this.transactionDate = transactionDate; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
}