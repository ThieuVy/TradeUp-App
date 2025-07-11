package com.example.testapptradeup.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.testapptradeup.models.Listing;
import com.example.testapptradeup.models.User;

public class MainViewModel extends ViewModel {

    private final MutableLiveData<Listing> _newListingPosted = new MutableLiveData<>();
    // ========== SỬA LỖI: Thêm LiveData cho người dùng hiện tại ==========
    private final MutableLiveData<User> _currentUser = new MutableLiveData<>();

    public LiveData<Listing> getNewListingPosted() {
        return _newListingPosted;
    }

    // ========== SỬA LỖI: Thêm getter cho currentUser ==========
    public LiveData<User> getCurrentUser() {
        return _currentUser;
    }

    public void onNewListingPosted(Listing newListing) {
        _newListingPosted.setValue(newListing);
    }

    public void onNewListingEventHandled() {
        _newListingPosted.setValue(null);
    }

    // ========== SỬA LỖI: Thêm setter cho currentUser ==========
    public void setCurrentUser(User user) {
        _currentUser.setValue(user);
    }
}