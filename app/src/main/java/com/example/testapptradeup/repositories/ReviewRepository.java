package com.example.testapptradeup.repositories;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.testapptradeup.models.Review;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

public class ReviewRepository {
    private static final String TAG = "ReviewRepository";
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public LiveData<Boolean> postReview(Review review) {
        MutableLiveData<Boolean> success = new MutableLiveData<>();

        // Sử dụng một Firestore Transaction để đảm bảo tất cả các thao tác đọc-ghi diễn ra một cách nguyên tử
        db.runTransaction((com.google.firebase.firestore.Transaction.Function<Void>) transaction -> {
            DocumentReference transactionRef = db.collection("transactions").document(review.getTransactionId());
            DocumentReference reviewedUserRef = db.collection("users").document(review.getReviewedUserId());
            DocumentReference reviewRef = db.collection("reviews").document(); // Document cho review mới

            // 1. Đọc thông tin của transaction và user được đánh giá
            com.example.testapptradeup.models.Transaction transactionData = transaction.get(transactionRef).toObject(com.example.testapptradeup.models.Transaction.class);
            com.example.testapptradeup.models.User userData = transaction.get(reviewedUserRef).toObject(com.example.testapptradeup.models.User.class);

            if (transactionData == null) {
                throw new FirebaseFirestoreException("Không tìm thấy giao dịch với ID: " + review.getTransactionId(), FirebaseFirestoreException.Code.ABORTED);
            }
            if (userData == null) {
                throw new FirebaseFirestoreException("Không tìm thấy người dùng với ID: " + review.getReviewedUserId(), FirebaseFirestoreException.Code.ABORTED);
            }

            // 2. Thực hiện các thao tác ghi

            // 2.1. Ghi document review mới
            review.setReviewId(reviewRef.getId());
            transaction.set(reviewRef, review);

            // 2.2. Cập nhật document transaction
            // Logic xác định field cần cập nhật giờ đã đúng vì có `transactionData`
            String fieldToUpdate = review.getReviewerId().equals(transactionData.getBuyerId()) ? "buyerReviewed" : "sellerReviewed";
            transaction.update(transactionRef, fieldToUpdate, true);

            // 2.3. Cập nhật document của người được đánh giá
            int oldReviewCount = userData.getReviewCount();
            double oldRating = userData.getRating();
            double newAverageRating = ((oldRating * oldReviewCount) + review.getRating()) / (oldReviewCount + 1);

            transaction.update(reviewedUserRef, "reviewCount", FieldValue.increment(1));
            transaction.update(reviewedUserRef, "rating", newAverageRating);

            return null; // Bắt buộc phải trả về null khi thành công
        }).addOnSuccessListener(aVoid -> {
            Log.d(TAG, "Gửi đánh giá và cập nhật tất cả các document liên quan thành công!");
            success.setValue(true);
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Lỗi khi thực thi Firestore Transaction: ", e);
            success.setValue(false);
        });


        return success;
    }
}