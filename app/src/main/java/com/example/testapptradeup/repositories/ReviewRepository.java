package com.example.testapptradeup.repositories;

import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.testapptradeup.models.Review;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Transaction;

public class ReviewRepository {
    private static final String TAG = "ReviewRepository";
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public LiveData<Boolean> postReview(Review review) {
        MutableLiveData<Boolean> success = new MutableLiveData<>();

        // === SỬA ĐỔI: Đảm bảo trạng thái là "pending" khi gửi đi ===
        review.setModerationStatus("pending");

        db.runTransaction((Transaction.Function<Void>) transaction -> {
            DocumentReference transactionRef = db.collection("transactions").document(review.getTransactionId());
            DocumentReference reviewedUserRef = db.collection("users").document(review.getReviewedUserId());
            DocumentReference reviewRef = db.collection("reviews").document();

            com.example.testapptradeup.models.Transaction transactionData = transaction.get(transactionRef).toObject(com.example.testapptradeup.models.Transaction.class);
            com.example.testapptradeup.models.User userData = transaction.get(reviewedUserRef).toObject(com.example.testapptradeup.models.User.class);

            if (transactionData == null) {
                throw new FirebaseFirestoreException("Không tìm thấy giao dịch với ID: " + review.getTransactionId(), FirebaseFirestoreException.Code.ABORTED);
            }
            if (userData == null) {
                throw new FirebaseFirestoreException("Không tìm thấy người dùng với ID: " + review.getReviewedUserId(), FirebaseFirestoreException.Code.ABORTED);
            }

            review.setReviewId(reviewRef.getId());
            transaction.set(reviewRef, review);

            String fieldToUpdate = review.getReviewerId().equals(transactionData.getBuyerId()) ? "buyerReviewed" : "sellerReviewed";
            transaction.update(transactionRef, fieldToUpdate, true);

            int oldReviewCount = userData.getReviewCount();
            double oldRating = userData.getRating();
            double newAverageRating = ((oldRating * oldReviewCount) + review.getRating()) / (oldReviewCount + 1);

            transaction.update(reviewedUserRef, "reviewCount", FieldValue.increment(1));
            transaction.update(reviewedUserRef, "rating", newAverageRating);

            return null;
        }).addOnSuccessListener(aVoid -> {
            Log.d(TAG, "Gửi đánh giá (chờ duyệt) và cập nhật các document liên quan thành công!");
            success.setValue(true);
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Lỗi khi thực thi Firestore Transaction: ", e);
            success.setValue(false);
        });

        return success;
    }
}