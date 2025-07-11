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
import java.util.List;

public class ProfileViewModel extends ViewModel {
    private final UserRepository userRepository;
    @Nullable
    private final String userId;
    private final FirebaseFunctions functions = FirebaseFunctions.getInstance();

    // SỬA LỖI: Xóa các LiveData và logic liên quan đến việc tải lại User
    // userProfileData, userIdTrigger, và các Transformations liên quan đã bị xóa.

    private final LiveData<List<Review>> userReviewsData;
    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();

    public ProfileViewModel() {
        this.userRepository = new UserRepository();
        this.userId = FirebaseAuth.getInstance().getUid();

        // Logic lấy review vẫn có thể giữ lại, vì nó là một phần của profile.
        // Nó được kích hoạt một lần duy nhất khi ViewModel được tạo.
        MutableLiveData<String> userIdTriggerForReviews = new MutableLiveData<>();
        if(this.userId != null) {
            userIdTriggerForReviews.setValue(this.userId);
        }

        userReviewsData = Transformations.switchMap(userIdTriggerForReviews, id -> {
            if (id == null || id.isEmpty()) return new MutableLiveData<>(new java.util.ArrayList<>());
            return userRepository.getUserReviews(id);
        });
    }

    // SỬA LỖI: Xóa các hàm không còn cần thiết
    // public void refreshUserProfile() { }
    // public LiveData<User> getUserProfileData() { }

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