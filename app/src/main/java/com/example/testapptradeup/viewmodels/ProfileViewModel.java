package com.example.testapptradeup.viewmodels;

import android.util.Log;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;
import com.example.testapptradeup.models.Review;
import com.example.testapptradeup.models.User;
import com.example.testapptradeup.repositories.UserRepository;
import com.example.testapptradeup.utils.Result;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.functions.FirebaseFunctions;
import java.util.List;

public class ProfileViewModel extends ViewModel {
    private final UserRepository userRepository;
    @Nullable
    private final String userId;
    private final FirebaseFunctions functions = FirebaseFunctions.getInstance();

    private final MutableLiveData<String> userIdTrigger = new MutableLiveData<>();
    private final LiveData<User> userProfileData;
    private final LiveData<List<Review>> userReviewsData;
    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();

    public ProfileViewModel() {
        this.userRepository = new UserRepository();
        this.userId = FirebaseAuth.getInstance().getUid();

        LiveData<Result<User>> userProfileResult = Transformations.switchMap(userIdTrigger, id -> {
            if (id == null || id.isEmpty()) {
                MutableLiveData<Result<User>> emptyResult = new MutableLiveData<>();
                emptyResult.setValue(Result.error(new Exception("Invalid user ID.")));
                return emptyResult;
            }
            return userRepository.getUserProfile(id);
        });

        // Tách dữ liệu thành công và lỗi ra hai LiveData riêng biệt
        userProfileData = Transformations.map(userProfileResult, result -> {
            if (result.isSuccess()) {
                _errorMessage.setValue(null); // Xóa lỗi cũ
                return result.getData();
            } else {
                _errorMessage.setValue(result.getError().getMessage());
                return null; // Trả về null khi có lỗi
            }
        });

        userReviewsData = Transformations.switchMap(userIdTrigger, id -> {
            if (id == null || id.isEmpty()) return new MutableLiveData<>(new java.util.ArrayList<>());
            return userRepository.getUserReviews(id);
        });

        refreshUserProfile();
    }

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
    public LiveData<String> getErrorMessage() {
        return _errorMessage;
    }

    /**
     * Yêu cầu repository cập nhật trạng thái tài khoản thành "paused".
     * @return LiveData<Boolean> cho biết thao tác có thành công không.
     */
    public LiveData<Boolean> deactivateAccount() {
        return userRepository.updateAccountStatus(userId, "paused");
    }

    /**
     * Gọi Cloud Function 'permanentlyDeleteUserAccount' để xóa vĩnh viễn tài khoản.
     * @return LiveData<Boolean> cho biết thao tác có thành công không.
     */
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