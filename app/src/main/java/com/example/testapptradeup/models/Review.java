package com.example.testapptradeup.models;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;
import java.util.Objects;

public class Review implements Parcelable {
    private String reviewId;
    private String reviewedUserId;
    private String reviewerId;
    private String reviewerName;
    private String reviewerImageUrl;
    private float rating;
    private String comment;
    private String transactionId;
    @ServerTimestamp
    private Date reviewDate;
    // BƯỚC 1.1: THÊM TRƯỜNG moderationStatus
    private String moderationStatus; // "pending", "approved", "rejected"

    public Review() {
        // BƯỚC 1.2: ĐẶT GIÁ TRỊ MẶC ĐỊNH LÀ "pending"
        this.moderationStatus = "pending";
    }

    // Getters and Setters
    public String getReviewId() { return reviewId; }
    public void setReviewId(String reviewId) { this.reviewId = reviewId; }
    public String getReviewedUserId() { return reviewedUserId; }
    public void setReviewedUserId(String reviewedUserId) { this.reviewedUserId = reviewedUserId; }
    public String getReviewerId() { return reviewerId; }
    public void setReviewerId(String reviewerId) { this.reviewerId = reviewerId; }
    public String getReviewerName() { return reviewerName; }
    public void setReviewerName(String reviewerName) { this.reviewerName = reviewerName; }
    public String getReviewerImageUrl() { return reviewerImageUrl; }
    public void setReviewerImageUrl(String reviewerImageUrl) { this.reviewerImageUrl = reviewerImageUrl; }
    public float getRating() { return rating; }
    public void setRating(float rating) { this.rating = rating; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    public Date getReviewDate() { return reviewDate; }
    public void setReviewDate(Date reviewDate) { this.reviewDate = reviewDate; }
    public String getModerationStatus() { return moderationStatus; }
    public void setModerationStatus(String moderationStatus) { this.moderationStatus = moderationStatus; }


    // BƯỚC 1.3: CẬP NHẬT PARCELABLE
    protected Review(Parcel in) {
        reviewId = in.readString();
        reviewedUserId = in.readString();
        reviewerId = in.readString();
        reviewerName = in.readString();
        reviewerImageUrl = in.readString();
        rating = in.readFloat();
        comment = in.readString();
        transactionId = in.readString();
        long tmpDate = in.readLong();
        reviewDate = tmpDate == -1 ? null : new Date(tmpDate);
        moderationStatus = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(reviewId != null ? reviewId : "");
        dest.writeString(reviewedUserId != null ? reviewedUserId : "");
        dest.writeString(reviewerId != null ? reviewerId : "");
        dest.writeString(reviewerName != null ? reviewerName : "");
        dest.writeString(reviewerImageUrl != null ? reviewerImageUrl : "");
        dest.writeFloat(rating);
        dest.writeString(comment != null ? comment : "");
        dest.writeString(transactionId != null ? transactionId : "");
        dest.writeLong(reviewDate != null ? reviewDate.getTime() : -1);
        dest.writeString(moderationStatus != null ? moderationStatus : "");
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Review> CREATOR = new Creator<>() {
        @Override
        public Review createFromParcel(Parcel in) {
            return new Review(in);
        }

        @Override
        public Review[] newArray(int size) {
            return new Review[size];
        }
    };

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Review review = (Review) o;
        return Float.compare(review.rating, rating) == 0 &&
                Objects.equals(reviewId, review.reviewId) &&
                Objects.equals(reviewedUserId, review.reviewedUserId) &&
                Objects.equals(reviewerId, review.reviewerId) &&
                Objects.equals(reviewerName, review.reviewerName) &&
                Objects.equals(reviewerImageUrl, review.reviewerImageUrl) &&
                Objects.equals(comment, review.comment) &&
                Objects.equals(transactionId, review.transactionId) &&
                Objects.equals(reviewDate, review.reviewDate) &&
                Objects.equals(moderationStatus, review.moderationStatus); // Thêm vào equals
    }

    @Override
    public int hashCode() {
        return Objects.hash(reviewId, reviewedUserId, reviewerId, reviewerName, reviewerImageUrl, rating, comment, transactionId, reviewDate, moderationStatus); // Thêm vào hashCode
    }
}