package com.example.testapptradeup.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import com.example.testapptradeup.models.User;
import com.example.testapptradeup.repositories.UserRepository;

public class EditProfileViewModel extends ViewModel {
    private final UserRepository userRepository;

    // ----- PHẦN TẢI DỮ LIỆU NGƯỜI DÙNG -----

    // INPUT: Một LiveData private để nhận ID người dùng cần tải.
    // Fragment sẽ gọi một hàm để đặt giá trị cho trigger này.
    private final MutableLiveData<String> userIdTrigger = new MutableLiveData<>();

    // OUTPUT: Một LiveData public mà Fragment sẽ observe.
    // Nó được tạo bằng `switchMap`, sẽ tự động gọi repository khi `userIdTrigger` thay đổi.
    private final LiveData<User> userProfile;


    // ----- PHẦN LƯU DỮ LIỆU -----

    // OUTPUT: Một LiveData private để giữ trạng thái của việc lưu (thành công/thất bại).
    private final MutableLiveData<Boolean> _saveStatus = new MutableLiveData<>();
    // LiveData public để Fragment observe. Dùng _ để tuân thủ quy ước cho MutableLiveData.
    public LiveData<Boolean> getSaveStatus() {
        return _saveStatus;
    }


    public EditProfileViewModel() {
        this.userRepository = new UserRepository();

        // Cấu hình `switchMap`: Khi `userIdTrigger` có giá trị mới (id),
        // nó sẽ tự động gọi `userRepository.getUserProfile(id)` và trả về kết quả.
        userProfile = Transformations.switchMap(userIdTrigger, id -> {
            if (id == null || id.isEmpty()) {
                // Nếu không có ID, trả về một LiveData rỗng.
                MutableLiveData<User> emptyUser = new MutableLiveData<>();
                emptyUser.setValue(null);
                return emptyUser;
            }
            // Gọi repository để lấy dữ liệu thực tế.
            return userRepository.getUserProfile(id);
        });
    }

    // ----- CÁC HÀM ACTION MÀ FRAGMENT SẼ GỌI -----

    /**
     * ACTION: Kích hoạt việc tải dữ liệu hồ sơ người dùng.
     * @param userId ID của người dùng cần tải.
     */
    public void loadUserProfile(String userId) {
        // Chỉ cần đặt giá trị cho trigger, `switchMap` sẽ tự lo phần còn lại.
        if (userId != null && !userId.equals(userIdTrigger.getValue())) {
            userIdTrigger.setValue(userId);
        }
    }

    /**
     * ACTION: Lưu hồ sơ đã chỉnh sửa.
     * @param user Đối tượng User chứa thông tin đã được cập nhật từ UI.
     */
    public void saveUserProfile(User user) {
        // Reset trạng thái trước khi lưu để tránh nhận lại kết quả cũ.
        _saveStatus.setValue(null);
        showLoading(true); // Giả sử có một LiveData cho trạng thái loading

        userRepository.updateUserProfile(user).observeForever(success -> {
            _saveStatus.setValue(success);
            showLoading(false);
        });
    }

    // Getter cho Fragment để observe dữ liệu người dùng.
    public LiveData<User> getUserProfile() {
        return userProfile;
    }

    // (Tùy chọn) Thêm LiveData cho trạng thái loading
    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>();
    public LiveData<Boolean> isLoading() {
        return _isLoading;
    }
    private void showLoading(boolean loading) {
        _isLoading.setValue(loading);
    }
}