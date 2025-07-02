package com.example.testapptradeup.models;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class Review {
    private String reviewId;
    private String reviewedUserId; // ID của người được đánh giá
    private String reviewerId;     // ID của người đánh giá
    private String reviewerName;
    private String reviewerImageUrl;
    private float rating;          // Số sao (1.0 -> 5.0)
    private String comment;
    @ServerTimestamp
    private Date reviewDate;

    // Constructor rỗng cần cho Firebase
    public Review() {}

    // Getters
    public String getReviewId() { return reviewId; }
    public String getReviewedUserId() { return reviewedUserId; }
    public String getReviewerId() { return reviewerId; }
    public String getReviewerName() { return reviewerName; }
    public String getReviewerImageUrl() { return reviewerImageUrl; }
    public float getRating() { return rating; }
    public String getComment() { return comment; }
    public Date getReviewDate() { return reviewDate; }

    // Setters
    public void setReviewId(String reviewId) { this.reviewId = reviewId; }
    public void setReviewedUserId(String reviewedUserId) { this.reviewedUserId = reviewedUserId; }
    public void setReviewerId(String reviewerId) { this.reviewerId = reviewerId; }
    public void setReviewerName(String reviewerName) { this.reviewerName = reviewerName; }
    public void setReviewerImageUrl(String reviewerImageUrl) { this.reviewerImageUrl = reviewerImageUrl; }
    public void setRating(float rating) { this.rating = rating; }
    public void setComment(String comment) { this.comment = comment; }
    public void setReviewDate(Date reviewDate) { this.reviewDate = reviewDate; }
}