package com.example.testapptradeup.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;
import com.example.testapptradeup.models.Listing;
import com.example.testapptradeup.repositories.ListingRepository;

public class EditPostViewModel extends ViewModel {
    private final ListingRepository repository;
    private final MutableLiveData<String> listingIdTrigger = new MutableLiveData<>();

    // <<< SỬA LỖI 1: Thay đổi từ public field sang private và thêm getter >>>
    private final LiveData<Listing> listingData;

    private final MutableLiveData<Boolean> _updateStatus = new MutableLiveData<>();
    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>();

    public EditPostViewModel() {
        this.repository = new ListingRepository();
        listingData = Transformations.switchMap(listingIdTrigger, repository::getListingById);
    }

    // <<< SỬA LỖI 1: Thêm phương thức getter này >>>
    public LiveData<Listing> getListingData() {
        return listingData;
    }

    public LiveData<Boolean> getUpdateStatus() {
        return _updateStatus;
    }

    public LiveData<Boolean> isLoading() {
        return _isLoading;
    }

    public void loadListingData(String listingId) {
        if (listingId != null && !listingId.equals(listingIdTrigger.getValue())) {
            listingIdTrigger.setValue(listingId);
        }
    }

    // Phương thức này không thay đổi, nhưng giờ nó sẽ được gọi đúng cách từ Fragment
    public void saveChanges(Listing updatedListing, MainViewModel mainViewModel) {
        _isLoading.setValue(true);
        LiveData<Boolean> result = repository.updateListing(updatedListing);
        result.observeForever(new Observer<>() {
            @Override
            public void onChanged(Boolean success) {
                _isLoading.setValue(false);
                _updateStatus.setValue(success);
                if (Boolean.TRUE.equals(success)) {
                    mainViewModel.postListingUpdateEvent();
                }
                result.removeObserver(this);
            }
        });
    }
}