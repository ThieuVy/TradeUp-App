package com.example.testapptradeup.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.example.testapptradeup.models.Review;
import com.example.testapptradeup.models.User;
import com.example.testapptradeup.repositories.UserRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class ProfileViewModel extends ViewModel {
    private final UserRepository userRepository;
    private final LiveData<User> userProfileData;
    private final LiveData<List<Review>> userReviewsData; // Mới
    private String userId; // Mới

    public ProfileViewModel() {
        this.userRepository = new UserRepository();
        this.userId = FirebaseAuth.getInstance().getUid();
        this.userProfileData = userRepository.getUserProfile(userId);
        this.userReviewsData = userRepository.getUserReviews(userId); // Mới
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