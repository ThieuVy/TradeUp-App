package com.example.testapptradeup.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.testapptradeup.models.Listing;
import com.example.testapptradeup.models.User;

public class MainViewModel extends ViewModel {

    private final MutableLiveData<Listing> _newListingPosted = new MutableLiveData<>();
    // LiveData chứa thông tin người dùng hiện tại
    private final MutableLiveData<User> _currentUser = new MutableLiveData<>();

    public LiveData<Listing> getNewListingPosted() {
        return _newListingPosted;
    }

    public LiveData<User> getCurrentUser() {
        return _currentUser;
    }

    public void onNewListingPosted(Listing newListing) {
        _newListingPosted.setValue(newListing);
    }

    public void onNewListingEventHandled() {
        _newListingPosted.setValue(null);
    }

    // Phương thức để cập nhật người dùng, được gọi từ Login hoặc EditProfile
    public void setCurrentUser(User user) {
        _currentUser.setValue(user);
    }
}