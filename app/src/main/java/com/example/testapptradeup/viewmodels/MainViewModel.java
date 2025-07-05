package com.example.testapptradeup.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.testapptradeup.models.Listing;

public class MainViewModel extends ViewModel {

    private final MutableLiveData<Listing> _newListingPosted = new MutableLiveData<>();
    public LiveData<Listing> getNewListingPosted() {
        return _newListingPosted;
    }

    public void onNewListingPosted(Listing newListing) {
        _newListingPosted.setValue(newListing);
    }

    public void onNewListingEventHandled() {
        _newListingPosted.setValue(null);
    }
}