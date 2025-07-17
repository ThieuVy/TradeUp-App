package com.example.testapptradeup.viewmodels;

import android.util.Log;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;
import com.example.testapptradeup.models.Review;
import com.example.testapptradeup.repositories.UserRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.functions.FirebaseFunctions;
import java.util.ArrayList;
import java.util.List;

public class ProfileViewModel extends ViewModel {
    private final UserRepository userRepository;
    @Nullable
    private final String userId;
    private final FirebaseFunctions functions = FirebaseFunctions.getInstance();

    private final MutableLiveData<String> userIdTriggerForReviews = new MutableLiveData<>();
    private final LiveData<List<Review>> userReviewsData;
    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();

    public ProfileViewModel() {
        this.userRepository = new UserRepository();
        this.userId = FirebaseAuth.getInstance().getUid();

        // Sử dụng switchMap để tự động tải lại review khi userIdTriggerForReviews thay đổi
        userReviewsData = Transformations.switchMap(userIdTriggerForReviews, id -> {
            if (id == null || id.isEmpty()) {
                return new MutableLiveData<>(new ArrayList<>());
            }
            return userRepository.getUserReviews(id);
        });
    }

    /**
     * Kích hoạt việc tải danh sách đánh giá cho một người dùng cụ thể.
     * @param userId ID của người dùng cần tải đánh giá.
     */
    public void loadUserReviews(String userId) {
        // Chỉ kích hoạt nếu userId mới khác với userId hiện tại để tránh tải lại không cần thiết
        if (userId != null && !userId.equals(userIdTriggerForReviews.getValue())) {
            userIdTriggerForReviews.setValue(userId);
        }
    }

    public LiveData<List<Review>> getUserReviewsData() {
        return userReviewsData;
    }

    public LiveData<String> getErrorMessage() {
        return _errorMessage;
    }

    public LiveData<Boolean> deactivateAccount() {
        return userRepository.updateAccountStatus(userId, "paused");
    }

    public LiveData<Boolean> deleteAccountPermanently() {
        MutableLiveData<Boolean> deleteStatus = new MutableLiveData<>();
        if (userId == null) {
            deleteStatus.setValue(false);
            return deleteStatus;
        }

        functions.getHttpsCallable("permanentlyDeleteUserAccount")
                .call()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d("ProfileViewModel", "Cloud Function xóa tài khoản thành công.");
                        deleteStatus.setValue(true);
                    } else {
                        Log.e("ProfileViewModel", "Lỗi khi gọi Cloud Function xóa tài khoản: ", task.getException());
                        deleteStatus.setValue(false);
                    }
                });
        return deleteStatus;
    }
}