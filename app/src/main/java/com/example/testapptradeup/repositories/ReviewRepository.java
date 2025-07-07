package com.example.testapptradeup.repositories;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.testapptradeup.models.Review;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

public class ReviewRepository {
    private static final String TAG = "ReviewRepository";
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public LiveData<Boolean> postReview(Review review, String reviewedUserId, String currentUserId) {
        MutableLiveData<Boolean> success = new MutableLiveData<>();
        WriteBatch batch = db.batch();

        // 1. Tạo document review mới
        DocumentReference reviewRef = db.collection("reviews").document();
        review.setReviewId(reviewRef.getId());
        batch.set(reviewRef, review);

        // 2. Cập nhật user được đánh giá
        DocumentReference userRef = db.collection("users").document(reviewedUserId);
        batch.update(userRef, "reviewCount", FieldValue.increment(1));
        // Việc tính lại rating trung bình phức tạp hơn, thường được xử lý tốt nhất bằng Cloud Function
        // để đảm bảo tính nhất quán. Tạm thời, chúng ta có thể làm ở client.
        // Để làm đúng, bạn cần đọc user document, tính toán rating mới rồi cập nhật.
        // Dùng `FieldValue.increment` cho rating sẽ cộng dồn, không phải tính trung bình.

        // 3. Cập nhật transaction để ghi nhận đã đánh giá
        DocumentReference transactionRef = db.collection("transactions").document(review.getTransactionId());
        String fieldToUpdate = review.getReviewerId().equals(currentUserId) ? "buyerReviewed" : "sellerReviewed";
        batch.update(transactionRef, fieldToUpdate, true);

        batch.commit().addOnSuccessListener(aVoid -> success.setValue(true))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi khi đăng đánh giá: ", e);
                    success.setValue(false);
                });

        return success;
    }
}