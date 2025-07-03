package com.example.testapptradeup.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import com.example.testapptradeup.models.User;
import com.example.testapptradeup.repositories.UserRepository;

public class EditProfileViewModel extends ViewModel {
    private final UserRepository userRepository;

    public EditProfileViewModel() {
        this.userRepository = new UserRepository();
    }

    // Lấy hồ sơ để điền vào form
    public LiveData<User> getUserProfile(String userId) {
        return userRepository.getUserProfile(userId);
    }

    // Lưu hồ sơ đã chỉnh sửa
    public LiveData<Boolean> saveUserProfile(User user) {
        return userRepository.updateUserProfile(user);
    }
}