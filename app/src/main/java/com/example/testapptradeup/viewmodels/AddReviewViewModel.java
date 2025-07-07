package com.example.testapptradeup.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.testapptradeup.models.Review;
import com.example.testapptradeup.repositories.ReviewRepository;
import com.google.firebase.auth.FirebaseAuth;

public class AddReviewViewModel extends ViewModel {
    private final ReviewRepository reviewRepository;
    private final String currentUserId;
    private final MutableLiveData<Boolean> postStatus = new MutableLiveData<>();

    public AddReviewViewModel() {
        reviewRepository = new ReviewRepository();
        currentUserId = FirebaseAuth.getInstance().getUid();
    }

    public LiveData<Boolean> getPostStatus() {
        return postStatus;
    }

    public void postReview(String transactionId, String reviewedUserId, float rating, String comment) {
        if (currentUserId == null) {
            postStatus.setValue(false);
            return;
        }

        Review review = new Review();
        review.setTransactionId(transactionId);
        review.setReviewedUserId(reviewedUserId);
        review.setReviewerId(currentUserId);
        review.setRating(rating);
        review.setComment(comment);
        // Các thông tin khác như reviewerName, reviewerAvatarUrl cần được lấy từ dữ liệu người dùng hiện tại

        reviewRepository.postReview(review, reviewedUserId, currentUserId).observeForever(postStatus::setValue);
    }
}