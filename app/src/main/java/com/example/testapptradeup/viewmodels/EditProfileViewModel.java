package com.example.testapptradeup.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import com.example.testapptradeup.models.User;
import com.example.testapptradeup.repositories.UserRepository;
import com.example.testapptradeup.utils.Result; // Đảm bảo import lớp Result

public class EditProfileViewModel extends ViewModel {
    private final UserRepository userRepository;

    // --- Triggers và LiveData trung gian ---
    private final MutableLiveData<String> userIdTrigger = new MutableLiveData<>();
    private final LiveData<Result<User>> userProfileResult; // LiveData trung gian giữ Result

    // --- LiveData cuối cùng cho UI ---
    private final LiveData<User> userProfile;
    private final MutableLiveData<Boolean> _saveStatus = new MutableLiveData<>();
    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>();
    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();


    public EditProfileViewModel() {
        this.userRepository = new UserRepository();

        // ========== BẮT ĐẦU PHẦN SỬA ĐỔI ==========

        // 1. switchMap giờ chỉ chịu trách nhiệm gọi repository và trả về LiveData<Result<User>>
        userProfileResult = Transformations.switchMap(userIdTrigger, id -> {
            if (id == null || id.isEmpty()) {
                // Nếu ID không hợp lệ, trả về một LiveData chứa lỗi ngay lập tức.
                MutableLiveData<Result<User>> errorResult = new MutableLiveData<>();
                errorResult.setValue(Result.error(new IllegalArgumentException("User ID is invalid.")));
                return errorResult; // Kiểu trả về là LiveData<Result<User>>, khớp với nhánh else
            }
            // Gọi repository để lấy dữ liệu thực tế.
            return userRepository.getUserProfile(id); // Kiểu trả về là LiveData<Result<User>>
        });

        // 2. Sử dụng map để trích xuất dữ liệu thành công hoặc xử lý lỗi từ userProfileResult
        userProfile = Transformations.map(userProfileResult, result -> {
            _isLoading.setValue(false); // Luôn tắt loading khi có kết quả
            if (result.isSuccess()) {
                _errorMessage.setValue(null); // Xóa lỗi cũ
                return result.getData(); // Trả về đối tượng User
            } else {
                // Nếu có lỗi, cập nhật errorMessage và trả về null cho userProfile
                _errorMessage.setValue(result.getError().getMessage());
                return null;
            }
        });

        // ==========================================
    }

    /**
     * Kích hoạt việc tải/làm mới dữ liệu hồ sơ người dùng.
     * @param userId ID của người dùng cần tải.
     */
    public void loadUserProfile(String userId) {
        if (userId != null && !userId.equals(userIdTrigger.getValue())) {
            _isLoading.setValue(true); // Bật loading trước khi trigger
            userIdTrigger.setValue(userId);
        }
    }

    /**
     * Lưu hồ sơ đã chỉnh sửa.
     * @param user Đối tượng User chứa thông tin đã được cập nhật từ UI.
     */
    public void saveUserProfile(User user) {
        _saveStatus.setValue(null); // Reset trạng thái để tránh nhận lại kết quả cũ
        _isLoading.setValue(true);

        // observeForever ở đây là chấp nhận được vì nó được gỡ bỏ ngay sau khi nhận được kết quả
        userRepository.updateUserProfile(user).observeForever(success -> {
            _isLoading.setValue(false);
            _saveStatus.setValue(success);
            if (Boolean.FALSE.equals(success)) {
                _errorMessage.setValue("Không thể lưu thay đổi.");
            }
        });
    }

    // --- GETTERS CHO FRAGMENT OBSERVE ---

    public LiveData<User> getUserProfile() {
        return userProfile;
    }

    public LiveData<Boolean> getSaveStatus() {
        return _saveStatus;
    }

    public LiveData<Boolean> isLoading() {
        return _isLoading;
    }

    public LiveData<String> getErrorMessage() {
        return _errorMessage;
    }
}