// viewmodels/MainViewModel.java
package com.example.testapptradeup.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.testapptradeup.models.Listing;
import com.example.testapptradeup.models.User; // <<< THÊM IMPORT NÀY

public class MainViewModel extends ViewModel {

    private final MutableLiveData<Listing> _newListingPosted = new MutableLiveData<>();
    // ========== BẮT ĐẦU PHẦN SỬA ĐỔI ==========
    private final MutableLiveData<User> _currentUser = new MutableLiveData<>();
    // ========================================

    public LiveData<Listing> getNewListingPosted() {
        return _newListingPosted;
    }

    // ========== BẮT ĐẦU PHẦN SỬA ĐỔI ==========
    public LiveData<User> getCurrentUser() {
        return _currentUser;
    }
    // ========================================

    public void onNewListingPosted(Listing newListing) {
        _newListingPosted.setValue(newListing);
    }

    public void onNewListingEventHandled() {
        _newListingPosted.setValue(null);
    }

    // ========== BẮT ĐẦU PHẦN SỬA ĐỔI ==========
    public void setCurrentUser(User user) {
        _currentUser.setValue(user);
    }
    // ========================================
}