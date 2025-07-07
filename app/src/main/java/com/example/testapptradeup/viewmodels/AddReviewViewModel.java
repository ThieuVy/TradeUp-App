package com.example.testapptradeup.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.testapptradeup.models.Review;
import com.example.testapptradeup.models.User;
import com.example.testapptradeup.repositories.ReviewRepository;
import com.example.testapptradeup.utils.SharedPrefsHelper;
import com.google.firebase.auth.FirebaseAuth;

public class AddReviewViewModel extends AndroidViewModel {

    // Lớp nội bộ để quản lý trạng thái kết quả
    public static class PostResult {
        private final boolean success;
        private final String errorMessage;

        private PostResult(boolean success, String errorMessage) {
            this.success = success;
            this.errorMessage = errorMessage;
        }

        public static PostResult success() { return new PostResult(true, null); }
        public static PostResult error(String message) { return new PostResult(false, message); }

        public boolean isSuccess() { return success; }
        public String getErrorMessage() { return errorMessage; }
    }

    private final ReviewRepository reviewRepository;
    private final String currentUserId;
    private final User currentUser;
    private final MutableLiveData<PostResult> postStatus = new MutableLiveData<>();

    public AddReviewViewModel(@NonNull Application application) {
        super(application);
        reviewRepository = new ReviewRepository();
        currentUserId = FirebaseAuth.getInstance().getUid();
        // Lấy thông tin người dùng từ SharedPreferences
        SharedPrefsHelper prefsHelper = new SharedPrefsHelper(application);
        currentUser = prefsHelper.getCurrentUser();
    }

    public LiveData<PostResult> getPostStatus() {
        return postStatus;
    }

    public void postReview(String transactionId, String reviewedUserId, float rating, String comment) {
        if (currentUserId == null || currentUser == null) {
            postStatus.setValue(PostResult.error("Phiên đăng nhập không hợp lệ."));
            return;
        }

        Review review = new Review();
        review.setTransactionId(transactionId);
        review.setReviewedUserId(reviewedUserId);
        review.setRating(rating);
        review.setComment(comment);

        // Gán thông tin người đánh giá (là người dùng hiện tại)
        review.setReviewerId(currentUserId);
        review.setReviewerName(currentUser.getName());
        review.setReviewerImageUrl(currentUser.getProfileImageUrl());

        // Gọi repository và quan sát kết quả
        reviewRepository.postReview(review).observeForever(success -> {
            if (Boolean.TRUE.equals(success)) {
                postStatus.setValue(PostResult.success());
            } else {
                postStatus.setValue(PostResult.error("Không thể kết nối đến máy chủ."));
            }
        });
    }
}