package com.example.testapptradeup.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import com.example.testapptradeup.models.User;
import com.example.testapptradeup.repositories.UserRepository;
import com.example.testapptradeup.utils.Result; // Đảm bảo rằng lớp Result được import

import java.util.Objects;

public class EditProfileViewModel extends ViewModel {
    private final UserRepository userRepository;

    private final MutableLiveData<String> userIdTrigger = new MutableLiveData<>();
    private final LiveData<User> userProfile;
    private final MutableLiveData<Boolean> _saveStatus = new MutableLiveData<>();
    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>();
    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();

    public EditProfileViewModel() {
        this.userRepository = new UserRepository();

        // ========== PHẦN SỬA ĐỔI BẮT ĐẦU Ở ĐÂY ==========

        // 1. switchMap bây giờ chịu trách nhiệm gọi repository và trả về LiveData<Result<User>>
        LiveData<Result<User>> userProfileResult = Transformations.switchMap(userIdTrigger, id -> {
            if (id == null || id.isEmpty()) {
                MutableLiveData<Result<User>> errorResult = new MutableLiveData<>();
                errorResult.setValue(Result.error(new IllegalArgumentException("ID người dùng không hợp lệ.")));
                return errorResult;
            }
            return userRepository.getUserProfile(id);
        });

        // 2. map được dùng để trích xuất dữ liệu thành công hoặc xử lý lỗi từ userProfileResult
        userProfile = Transformations.map(userProfileResult, result -> {
            _isLoading.setValue(false); // Luôn tắt trạng thái loading khi có kết quả
            if (result.isSuccess()) {
                _errorMessage.setValue(null); // Xóa lỗi cũ
                return result.getData(); // Trả về đối tượng User
            } else {
                // Nếu có lỗi thì cập nhật thông báo lỗi và trả về null cho userProfile
                _errorMessage.setValue(Objects.requireNonNull(result.getError()).getMessage());
                return null;
            }
        });
        // ========== PHẦN SỬA ĐỔI KẾT THÚC Ở ĐÂY ==========
    }

    public void loadUserProfile(String userId) {
        if (userId != null && !userId.equals(userIdTrigger.getValue())) {
            _isLoading.setValue(true);
            userIdTrigger.setValue(userId);
        }
    }

    public void saveUserProfile(User user) {
        _saveStatus.setValue(null);
        _isLoading.setValue(true);
        userRepository.updateUserProfile(user).observeForever(success -> {
            _isLoading.setValue(false);
            _saveStatus.setValue(success);
            if (Boolean.FALSE.equals(success)) {
                _errorMessage.setValue("Không thể lưu các thay đổi.");
            }
        });
    }

    // --- CÁC HÀM GETTER CHO FRAGMENT ĐỂ QUAN SÁT ---
    public LiveData<User> getUserProfile() { return userProfile; }
    public LiveData<Boolean> getSaveStatus() { return _saveStatus; }
    public LiveData<Boolean> isLoading() { return _isLoading; }
    public LiveData<String> getErrorMessage() { return _errorMessage; }
}
