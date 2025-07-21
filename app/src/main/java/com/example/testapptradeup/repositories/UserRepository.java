package com.example.testapptradeup.repositories;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.testapptradeup.models.Review;
import com.example.testapptradeup.models.User;
import com.example.testapptradeup.models.UserStatus;
import com.example.testapptradeup.utils.Result;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserRepository {
    private static final String TAG = "UserRepository";
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth mAuth = FirebaseAuth.getInstance();

    // <<< KHAI BÁO CÁC BIẾN CHO REALTIME DATABASE >>>
    private final DatabaseReference presenceRef;
    private final DatabaseReference connectedRef;
    private ValueEventListener presenceListener;

    public UserRepository() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid != null) {
            presenceRef = FirebaseDatabase.getInstance().getReference("/status/" + uid);
            connectedRef = FirebaseDatabase.getInstance().getReference(".info/connected");
            setupPresenceDetection();
        } else {
            presenceRef = null;
            connectedRef = null;
        }
    }

    private void setupPresenceDetection() {
        if (connectedRef == null || presenceRef == null) return;
        connectedRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean connected = Boolean.TRUE.equals(snapshot.getValue(Boolean.class));
                if (connected) {
                    // Sử dụng constructor đã được định nghĩa
                    presenceRef.setValue(new UserStatus(true));
                    presenceRef.onDisconnect().setValue(new UserStatus(false));
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w(TAG, "Listener was cancelled at .info/connected");
            }
        });
    }

    /**
     * Lắng nghe trạng thái online/offline của một người dùng khác.
     * @param otherUserId ID của người dùng cần theo dõi.
     * @return LiveData chứa đối tượng UserStatus.
     */
    public LiveData<UserStatus> getUserStatus(String otherUserId) {
        MutableLiveData<UserStatus> statusData = new MutableLiveData<>();
        DatabaseReference userStatusRef = FirebaseDatabase.getInstance().getReference("/status/" + otherUserId);
        userStatusRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    statusData.setValue(snapshot.getValue(UserStatus.class));
                } else {
                    statusData.setValue(new UserStatus(false)); // Mặc định là offline
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w(TAG, "getUserStatus listener cancelled for user: " + otherUserId);
            }
        });
        return statusData;
    }

    // =====================================================================
    // CÁC HÀM CŨ CHO FIRESTORE (GIỮ NGUYÊN)
    // =====================================================================

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
                            userLiveData.setValue(Result.success(user));
                        } else {
                            userLiveData.setValue(Result.error(new Exception("Failed to parse user data.")));
                        }
                    } else {
                        userLiveData.setValue(Result.error(new Exception("User not found.")));
                    }
                }).addOnFailureListener(e -> userLiveData.setValue(Result.error(e)));
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
            favoriteIdsData.setValue(new ArrayList<>());
            return favoriteIdsData;
        }
        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Object favsObject = documentSnapshot.get("favoriteListingIds");
                        if (favsObject instanceof List) {
                            favoriteIdsData.setValue((List<String>) favsObject);
                        } else {
                            favoriteIdsData.setValue(new ArrayList<>());
                        }
                    } else {
                        favoriteIdsData.setValue(new ArrayList<>());
                    }
                }).addOnFailureListener(e -> favoriteIdsData.setValue(new ArrayList<>()));
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
                .whereEqualTo("moderationStatus", "approved")
                .orderBy("reviewDate", Query.Direction.DESCENDING)
                .limit(5)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if(queryDocumentSnapshots != null) {
                        reviewsData.setValue(queryDocumentSnapshots.toObjects(Review.class));
                    } else {
                        reviewsData.setValue(new ArrayList<>());
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("UserRepository", "Error getting user reviews", e);
                    reviewsData.setValue(new ArrayList<>());
                });
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

    public void toggleFavorite(String userId, String listingId, boolean isFavorite) {
        if (userId == null) {
            Log.e(TAG, "Cannot toggle favorite, userId is null");
            return;
        }
        DocumentReference userRef = db.collection("users").document(userId);
        FieldValue updateValue = isFavorite ? FieldValue.arrayUnion(listingId) : FieldValue.arrayRemove(listingId);
        userRef.update("favoriteListingIds", updateValue)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Favorite status updated for user: " + userId))
                .addOnFailureListener(e -> Log.e(TAG, "Error updating favorite status: ", e));
    }

    public void updateFcmToken(String userId, String token) {
        if (userId == null || token == null) return;
        db.collection("users").document(userId)
                .update("fcmToken", token)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "FCM token updated successfully for user: " + userId))
                .addOnFailureListener(e -> Log.e(TAG, "Error updating FCM token", e));
    }

    public LiveData<Result<String>> updateProfileImageUrl(String userId, String newUrl) {
        MutableLiveData<Result<String>> result = new MutableLiveData<>();
        if (userId == null || newUrl == null) {
            result.setValue(Result.error(new IllegalArgumentException("User ID hoặc URL không hợp lệ.")));
            return result;
        }
        db.collection("users").document(userId)
                .update("profileImageUrl", newUrl)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Cập nhật URL ảnh đại diện thành công.");
                    result.setValue(Result.success(newUrl));
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi cập nhật URL ảnh đại diện: ", e);
                    result.setValue(Result.error(e));
                });
        return result;
    }

    /**
     * Lấy danh sách ID các tin đăng yêu thích của người dùng.
     * Dùng addSnapshotListener để tự động cập nhật UI khi danh sách thay đổi.
     *
     * @param userId ID của người dùng.
     * @return LiveData chứa danh sách các ID.
     */
    public LiveData<List<String>> getFavoriteListingIds(String userId) {
        MutableLiveData<List<String>> favoriteIdsData = new MutableLiveData<>();
        if (userId == null || userId.isEmpty()) {
            favoriteIdsData.setValue(new ArrayList<>());
            return favoriteIdsData;
        }

        db.collection("users").document(userId)
                .addSnapshotListener((documentSnapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Lỗi lắng nghe danh sách yêu thích: ", error);
                        favoriteIdsData.postValue(new ArrayList<>());
                        return;
                    }

                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        // SỬ DỤNG TÊN TRƯỜNG ĐÃ THỐNG NHẤT: "favoriteListingIds"
                        Object favsObject = documentSnapshot.get("favoriteListingIds");
                        if (favsObject instanceof List) {
                            favoriteIdsData.postValue((List<String>) favsObject);
                        } else {
                            favoriteIdsData.postValue(new ArrayList<>());
                        }
                    } else {
                        // Nếu user document không tồn tại, trả về danh sách rỗng
                        favoriteIdsData.postValue(new ArrayList<>());
                    }
                });
        return favoriteIdsData;
    }


    /**
     * Cập nhật trạng thái yêu thích (thêm hoặc xóa).
     * Hàm này an toàn, sẽ tự tạo document người dùng nếu chưa có.
     *
     * @param userId ID của người dùng.
     * @param listingId ID của tin đăng.
     * @param isFavorite true để thêm vào yêu thích, false để xóa.
     * @return Task để theo dõi việc hoàn thành.
     */
    public Task<Void> updateFavoriteStatus(String userId, String listingId, boolean isFavorite) {
        if (userId == null || userId.isEmpty()) {
            // Trả về một task đã thất bại nếu không có userId
            return Tasks.forException(new IllegalArgumentException("User ID không hợp lệ."));
        }

        DocumentReference userRef = db.collection("users").document(userId);
        // SỬ DỤNG TÊN TRƯỜNG ĐÃ THỐNG NHẤT: "favoriteListingIds"
        FieldValue updateValue = isFavorite ?
                FieldValue.arrayUnion(listingId) :
                FieldValue.arrayRemove(listingId);

        Map<String, Object> data = new HashMap<>();
        data.put("favoriteListingIds", updateValue);

        // Sử dụng .set với merge=true để cập nhật an toàn và tránh lỗi "NOT_FOUND"
        return userRef.set(data, SetOptions.merge());
    }

    public void logUserView(String userId, String listingId, String categoryId) {
        if (userId == null || listingId == null) return;

        DocumentReference viewRef = db.collection("users").document(userId)
                .collection("viewHistory").document(listingId);

        Map<String, Object> viewData = new HashMap<>();
        viewData.put("viewedAt", FieldValue.serverTimestamp());
        viewData.put("categoryId", categoryId);

        // Dùng set với merge để ghi đè timestamp nếu xem lại, không tạo document mới
        viewRef.set(viewData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Ghi lại lịch sử xem thành công cho listing: " + listingId))
                .addOnFailureListener(e -> Log.e(TAG, "Lỗi ghi lịch sử xem", e));
    }
}