package com.example.testapptradeup.viewmodels;

import android.app.Application;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.Transformations;

import com.example.testapptradeup.models.Review;
import com.example.testapptradeup.models.User;
import com.example.testapptradeup.repositories.CloudinaryRepository;
import com.example.testapptradeup.repositories.UserRepository;
import com.example.testapptradeup.utils.Result;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.functions.FirebaseFunctions;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ProfileViewModel extends AndroidViewModel {
    private final UserRepository userRepository;
    private final CloudinaryRepository cloudinaryRepository;
    @Nullable
    private final String userId;
    private final FirebaseFunctions functions = FirebaseFunctions.getInstance();

    private final MutableLiveData<String> userIdTriggerForReviews = new MutableLiveData<>();
    private final LiveData<List<Review>> userReviewsData;
    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);

    public ProfileViewModel(@NonNull Application application) { // <-- SỬA: Cập nhật Constructor
        super(application);
        this.userRepository = new UserRepository();
        this.cloudinaryRepository = new CloudinaryRepository(); // <-- THÊM MỚI
        this.userId = FirebaseAuth.getInstance().getUid();

        userReviewsData = Transformations.switchMap(userIdTriggerForReviews, id -> {
            if (id == null || id.isEmpty()) {
                return new MutableLiveData<>(new ArrayList<>());
            }
            return userRepository.getUserReviews(id);
        });
    }

    public void loadUserReviews(String userId) {
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

    public LiveData<Boolean> isLoading() { return _isLoading; }
    public LiveData<Boolean> deactivateAccount() {
        // Trạng thái "paused" sẽ được dùng để tạm dừng tài khoản
        return userRepository.updateAccountStatus(userId, "paused");
    }

    private final MutableLiveData<Result<String>> _updateImageResult = new MutableLiveData<>();
    public LiveData<Result<String>> getUpdateImageResult() {
        return _updateImageResult;
    }

    /**
     * Bắt đầu quá trình thay đổi ảnh đại diện:
     * 1. Tải ảnh lên Cloudinary.
     * 2. Lấy URL trả về.
     * 3. Cập nhật URL mới vào document User trên Firestore.
     * @param imageUri Uri của ảnh mới được chọn từ gallery.
     * @param mainViewModel ViewModel chính để cập nhật UI trên toàn app.
     */
    public void changeProfilePicture(Uri imageUri, MainViewModel mainViewModel) {
        if (userId == null) {
            // Gửi lỗi qua LiveData mới
            _updateImageResult.setValue(Result.error(new Exception("Người dùng không hợp lệ.")));
            return;
        }
        _isLoading.setValue(true);

        LiveData<Result<String>> uploadResult = cloudinaryRepository.uploadProfileImage(imageUri, getApplication());

        uploadResult.observeForever(new Observer<>() {
            @Override
            public void onChanged(Result<String> result) {
                uploadResult.removeObserver(this); // Gỡ observer ngay để tránh leak

                if (result.isSuccess()) {
                    String newUrl = result.getData();
                    // Bước 2: Khi upload thành công, LƯU URL vào Firestore
                    updateUserImageUrl(userId, newUrl, mainViewModel);
                } else {
                    _isLoading.setValue(false);
                    _errorMessage.setValue(Objects.requireNonNull(result.getError()).getMessage());
                    // Gửi lỗi qua LiveData mới
                    _updateImageResult.setValue(Result.error(result.getError()));
                }
            }
        });
    }

    private void updateUserImageUrl(String userId, String newUrl, MainViewModel mainViewModel) {
        LiveData<Result<String>> updateDbResult = userRepository.updateProfileImageUrl(userId, newUrl);

        updateDbResult.observeForever(new Observer<>() {
            @Override
            public void onChanged(Result<String> result) {
                updateDbResult.removeObserver(this);
                _isLoading.setValue(false);

                if (result.isSuccess()) {
                    // Bước 3: Cập nhật MainViewModel để thay đổi có hiệu lực trên toàn app
                    User currentUser = mainViewModel.getCurrentUser().getValue();
                    if (currentUser != null) {
                        currentUser.setProfileImageUrl(newUrl);
                        mainViewModel.setCurrentUser(currentUser);
                    }
                }
                // Bước 4: Gửi kết quả cuối cùng (thành công hoặc thất bại) về cho Fragment
                _updateImageResult.setValue(result);
            }
        });
    }

    public LiveData<Boolean> deleteAccountPermanently() {
        MutableLiveData<Boolean> deleteStatus = new MutableLiveData<>();
        if (userId == null) {
            deleteStatus.setValue(false);
            return deleteStatus;
        }

        // Gọi Cloud Function 'permanentlyDeleteUserAccount' đã được định nghĩa trong index.js
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