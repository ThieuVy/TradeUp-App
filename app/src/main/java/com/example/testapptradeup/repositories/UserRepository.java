package com.example.testapptradeup.repositories;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.testapptradeup.models.Review;
import com.example.testapptradeup.models.User;
import com.example.testapptradeup.utils.Result;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class UserRepository {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth mAuth = FirebaseAuth.getInstance();

    public LiveData<Result<User>> getUserProfile(String userId) {
        MutableLiveData<Result<User>> userLiveData = new MutableLiveData<>();
        if (userId == null) {
            userLiveData.setValue(Result.error(new IllegalArgumentException("User ID is null")));
            return userLiveData;
        }

        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            user.setId(documentSnapshot.getId());
                            userLiveData.setValue(Result.success(user)); // <<< TRẢ VỀ SUCCESS
                        } else {
                            userLiveData.setValue(Result.error(new Exception("Failed to parse user data.")));
                        }
                    } else {
                        userLiveData.setValue(Result.error(new Exception("User not found.")));
                    }
                }).addOnFailureListener(e -> {
                    // <<< TRẢ VỀ ERROR
                    userLiveData.setValue(Result.error(e));
                });
        return userLiveData;
    }

    public LiveData<Boolean> updateUserProfile(User user) {
        MutableLiveData<Boolean> updateStatus = new MutableLiveData<>();
        db.collection("users").document(user.getId()).set(user)
                .addOnSuccessListener(aVoid -> updateStatus.setValue(true))
                .addOnFailureListener(e -> updateStatus.setValue(false));
        return updateStatus;
    }

    public LiveData<List<String>> getFavoriteIds(String userId) {
        MutableLiveData<List<String>> favoriteIdsData = new MutableLiveData<>();
        if (userId == null) {
            favoriteIdsData.setValue(null);
            return favoriteIdsData;
        }
        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Sử dụng get() thay vì toObject() để tránh lỗi nếu trường không tồn tại
                        Object favsObject = documentSnapshot.get("favoriteListingIds");
                        if (favsObject instanceof List) {
                            List<String> favIds = (List<String>) favsObject;
                            favoriteIdsData.setValue(favIds);
                        } else {
                            // Trường không tồn tại hoặc không phải là List
                            favoriteIdsData.setValue(new ArrayList<>());
                        }
                    } else {
                        favoriteIdsData.setValue(null); // User không tồn tại
                    }
                }).addOnFailureListener(e -> favoriteIdsData.setValue(null));
        return favoriteIdsData;
    }

    public LiveData<List<Review>> getUserReviews(String userId) {
        MutableLiveData<List<Review>> reviewsData = new MutableLiveData<>();
        if (userId == null) {
            reviewsData.setValue(new ArrayList<>());
            return reviewsData;
        }
        db.collection("reviews")
                .whereEqualTo("reviewedUserId", userId)
                // === THÊM MỚI: Chỉ lấy các đánh giá đã được duyệt ===
                .whereEqualTo("moderationStatus", "approved")
                .orderBy("reviewDate", Query.Direction.DESCENDING)
                .limit(5)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> reviewsData.setValue(queryDocumentSnapshots.toObjects(Review.class)))
                .addOnFailureListener(e -> reviewsData.setValue(new ArrayList<>()));
        return reviewsData;
    }

    public LiveData<Boolean> updateAccountStatus(String userId, String status) {
        MutableLiveData<Boolean> statusData = new MutableLiveData<>();
        if (userId == null) {
            statusData.setValue(false);
            return statusData;
        }
        db.collection("users").document(userId)
                .update("accountStatus", status)
                .addOnSuccessListener(aVoid -> statusData.setValue(true))
                .addOnFailureListener(e -> statusData.setValue(false));
        return statusData;
    }

    // Phương thức này đã đúng và sẽ được gọi bởi SearchViewModel
    public void toggleFavorite(String userId, String listingId, boolean isFavorite) {
        MutableLiveData<Boolean> status = new MutableLiveData<>();
        if (userId == null) {
            status.setValue(false);
            return;
        }

        FieldValue updateValue = isFavorite ?
                FieldValue.arrayUnion(listingId) :
                FieldValue.arrayRemove(listingId);

        db.collection("users").document(userId)
                .update("favoriteListingIds", updateValue)
                .addOnSuccessListener(aVoid -> status.setValue(true))
                .addOnFailureListener(e -> status.setValue(false));
    }
    /**
     * Cập nhật hoặc thêm mới FCM token cho một người dùng.
     * Đây là thao tác "fire-and-forget", không cần chờ kết quả trả về.
     * @param userId ID của người dùng.
     * @param token FCM token mới.
     */
    public void updateFcmToken(String userId, String token) {
        if (userId == null || token == null) {
            return;
        }
        db.collection("users").document(userId)
                .update("fcmToken", token)
                .addOnSuccessListener(aVoid -> Log.d("UserRepository", "FCM token updated successfully for user: " + userId))
                .addOnFailureListener(e -> Log.e("UserRepository", "Error updating FCM token", e));
    }
}