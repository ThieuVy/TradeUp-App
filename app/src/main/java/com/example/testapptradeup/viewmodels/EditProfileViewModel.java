package com.example.testapptradeup.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.testapptradeup.models.User;
import com.example.testapptradeup.repositories.UserRepository;

public class EditProfileViewModel extends ViewModel {
    private final UserRepository userRepository;
    private final MutableLiveData<Boolean> _saveStatus = new MutableLiveData<>();
    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<User> _updatedUser = new MutableLiveData<>();

    public EditProfileViewModel() {
        this.userRepository = new UserRepository();
    }

    public void saveUserProfile(User user) {
        _isLoading.setValue(true);
        _saveStatus.setValue(null);
        userRepository.updateUserProfile(user).observeForever(success -> {
            _isLoading.setValue(false);
            if(Boolean.TRUE.equals(success)) {
                _updatedUser.setValue(user); // Lưu lại bản đã cập nhật
            }
            _saveStatus.setValue(success);
        });
    }

    public LiveData<Boolean> getSaveStatus() { return _saveStatus; }
    public LiveData<Boolean> isLoading() { return _isLoading; }
    public LiveData<User> getUpdatedUser() { return _updatedUser; } // Getter cho Fragment
}