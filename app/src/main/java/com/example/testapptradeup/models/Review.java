package com.example.testapptradeup.models;

import android.os.Parcel;
import android.os.Parcelable; // <<< THÊM IMPORT NÀY

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

// <<< THÊM "implements Parcelable"
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

    public Review() {}

    // Getters và Setters của bạn vẫn giữ nguyên...
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
    public Date getReviewDate() { return reviewDate; }
    public void setReviewDate(Date reviewDate) { this.reviewDate = reviewDate; }
    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

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
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(reviewId);
        dest.writeString(reviewedUserId);
        dest.writeString(reviewerId);
        dest.writeString(reviewerName);
        dest.writeString(reviewerImageUrl);
        dest.writeFloat(rating);
        dest.writeString(comment);
        dest.writeString(transactionId);
        dest.writeLong(reviewDate != null ? reviewDate.getTime() : -1);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Review> CREATOR = new Creator<Review>() {
        @Override
        public Review createFromParcel(Parcel in) {
            return new Review(in);
        }

        @Override
        public Review[] newArray(int size) {
            return new Review[size];
        }
    };
}