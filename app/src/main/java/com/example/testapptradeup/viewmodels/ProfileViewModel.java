package com.example.testapptradeup.viewmodels;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import com.example.testapptradeup.models.Review;
import com.example.testapptradeup.models.User;
import com.example.testapptradeup.repositories.UserRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class ProfileViewModel extends ViewModel {
    private final UserRepository userRepository;
    @Nullable
    private final String userId;

    // LiveData để trigger việc tải/làm mới dữ liệu
    private final MutableLiveData<String> userIdTrigger = new MutableLiveData<>();

    // LiveData cuối cùng mà Fragment sẽ observe
    private final LiveData<User> userProfileData;
    private final LiveData<List<Review>> userReviewsData;

    public ProfileViewModel() {
        this.userRepository = new UserRepository();
        this.userId = FirebaseAuth.getInstance().getUid();

        // ========== PHẦN SỬA LỖI Ở ĐÂY ==========
        // Sử dụng switchMap để tự động tải lại dữ liệu khi userIdTrigger thay đổi
        userProfileData = Transformations.switchMap(userIdTrigger, id -> {
            if (id == null || id.isEmpty()) {
                // Trả về null hoặc một LiveData rỗng nếu không có userId
                return new MutableLiveData<>(null);
            }
            return userRepository.getUserProfile(id);
        });

        // Tương tự cho reviews
        userReviewsData = Transformations.switchMap(userIdTrigger, id -> {
            if (id == null || id.isEmpty()) {
                return new MutableLiveData<>(new java.util.ArrayList<>());
            }
            return userRepository.getUserReviews(id);
        });

        // Kích hoạt tải dữ liệu lần đầu
        refreshUserProfile();
    }

    /**
     * Phương thức này sẽ được gọi từ Fragment để yêu cầu tải lại dữ liệu.
     */
    public void refreshUserProfile() {
        if (userId != null) {
            userIdTrigger.setValue(userId);
        }
    }

    public LiveData<User> getUserProfileData() {
        return userProfileData;
    }
    public LiveData<List<Review>> getUserReviewsData() {
        return userReviewsData;
    }
    public LiveData<Boolean> deactivateAccount() {
        return userRepository.updateAccountStatus(userId, "paused"); // "paused" hoặc "suspended"
    }
    // Mới: Xóa tài khoản là một quá trình phức tạp, đây chỉ là bước đầu
    public void deleteAccount(FirebaseAuth auth, FirebaseFirestore db) {
        // Cần xóa user trong Auth, sau đó xóa document trong Firestore
        // Thường sẽ dùng Cloud Function để đảm bảo tính toàn vẹn
    }

}