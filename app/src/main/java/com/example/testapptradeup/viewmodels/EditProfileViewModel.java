package com.example.testapptradeup.viewmodels;

import android.app.Application;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.example.testapptradeup.models.User;
import com.example.testapptradeup.repositories.CloudinaryRepository;
import com.example.testapptradeup.repositories.UserRepository;
import com.example.testapptradeup.utils.Result;
import com.google.firebase.auth.FirebaseAuth;

import java.util.Objects;

public class EditProfileViewModel extends AndroidViewModel {
    private final UserRepository userRepository;
    private final CloudinaryRepository cloudinaryRepository; // Thêm repository mới

    private final MutableLiveData<Boolean> _saveStatus = new MutableLiveData<>();
    private final MutableLiveData<User> _updatedUser = new MutableLiveData<>();

    // LiveData cho việc cập nhật ảnh đại diện
    // Đổi tên từ _updateResult thành _updateImageResult để rõ ràng hơn
    private final MutableLiveData<Result<String>> _updateImageResult = new MutableLiveData<>();

    // LiveData cho trạng thái loading chung
    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);

    public EditProfileViewModel(@NonNull Application application) {
        super(application);
        this.userRepository = new UserRepository();
        this.cloudinaryRepository = new CloudinaryRepository();
    }

    public void saveUserProfile(User user) {
        _isLoading.setValue(true);
        _saveStatus.setValue(null); // Reset trạng thái trước khi lưu

        // observeForever chỉ nên dùng trong ViewModel và phải được gỡ bỏ
        userRepository.updateUserProfile(user).observeForever(new Observer<>() {
            @Override
            public void onChanged(Boolean success) {
                _isLoading.setValue(false);
                if (Boolean.TRUE.equals(success)) {
                    _updatedUser.setValue(user); // Gửi user đã cập nhật về cho UI
                }
                _saveStatus.setValue(success);
                // Gỡ bỏ observer sau khi nhận được kết quả để tránh memory leak
                userRepository.updateUserProfile(user).removeObserver(this);
            }
        });
    }

    public void uploadAndSaveProfilePicture(Uri imageUri) {
        String currentUserId = FirebaseAuth.getInstance().getUid();
        if (currentUserId == null) {
            _updateImageResult.setValue(Result.error(new Exception("Người dùng chưa đăng nhập.")));
            return;
        }

        _isLoading.setValue(true);

        // Bước 1: Tải ảnh lên Cloudinary
        LiveData<Result<String>> uploadResult = cloudinaryRepository.uploadProfileImage(imageUri, getApplication().getApplicationContext());

        uploadResult.observeForever(new Observer<>() {
            @Override
            public void onChanged(Result<String> result) {
                uploadResult.removeObserver(this);

                if (result.isSuccess()) {
                    String newUrl = result.getData();
                    // Bước 2: Lưu URL mới vào Firestore
                    saveNewUrlToProfile(currentUserId, newUrl);
                } else {
                    _isLoading.setValue(false);
                    _updateImageResult.setValue(Result.error(Objects.requireNonNull(result.getError())));
                }
            }
        });
    }

    private void saveNewUrlToProfile(String userId, String newUrl) {
        LiveData<Result<String>> saveResult = userRepository.updateProfileImageUrl(userId, newUrl);

        saveResult.observeForever(new Observer<>() {
            @Override
            public void onChanged(Result<String> result) {
                saveResult.removeObserver(this);
                _isLoading.setValue(false);
                _updateImageResult.setValue(result); // Gửi kết quả cuối cùng về cho Fragment
            }
        });
    }

    public LiveData<Boolean> getSaveStatus() {
        return _saveStatus;
    }

    public LiveData<User> getUpdatedUser() {
        return _updatedUser;
    }

    public LiveData<Result<String>> getUpdateImageResult() {
        return _updateImageResult;
    }

    public LiveData<Boolean> isLoading() {
        return _isLoading;
    }
}