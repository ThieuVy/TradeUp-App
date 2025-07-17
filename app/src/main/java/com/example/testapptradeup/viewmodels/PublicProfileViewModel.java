package com.example.testapptradeup.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import com.example.testapptradeup.models.Listing;
import com.example.testapptradeup.models.Review;
import com.example.testapptradeup.models.User;
import com.example.testapptradeup.repositories.ListingRepository;
import com.example.testapptradeup.repositories.UserRepository;
import com.example.testapptradeup.utils.Result;

import java.util.Collections;
import java.util.List;

public class PublicProfileViewModel extends ViewModel {

    private final UserRepository userRepository;
    private final ListingRepository listingRepository;

    private final MutableLiveData<String> userIdTrigger = new MutableLiveData<>();
    private final LiveData<User> userProfile;
    private final LiveData<List<Listing>> userListings;
    private final LiveData<List<Review>> userReviews;
    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>();
    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();

    public PublicProfileViewModel() {
        userRepository = new UserRepository();
        listingRepository = new ListingRepository();

        LiveData<Result<User>> userProfileResult = Transformations.switchMap(userIdTrigger, id -> {
            if (id == null || id.isEmpty()) {
                MutableLiveData<Result<User>> errorResult = new MutableLiveData<>();
                errorResult.setValue(Result.error(new IllegalArgumentException("User ID is invalid.")));
                return errorResult;
            }
            return userRepository.getUserProfile(id);
        });

        userProfile = Transformations.map(userProfileResult, result -> {
            _isLoading.setValue(false);
            if (result.isSuccess()) {
                _errorMessage.setValue(null);
                return result.getData();
            } else {
                _errorMessage.setValue(result.getError() != null ? result.getError().getMessage() : "Lỗi không xác định");
                return null;
            }
        });

        userListings = Transformations.switchMap(userIdTrigger, id -> {
            if (id == null || id.isEmpty()) return new MutableLiveData<>(Collections.emptyList());
            return listingRepository.getActiveListingsByUser(id, 10);
        });

        userReviews = Transformations.switchMap(userIdTrigger, id -> {
            if (id == null || id.isEmpty()) return new MutableLiveData<>(Collections.emptyList());
            return userRepository.getUserReviews(id);
        });
    }

    public void loadProfileData(String newUserId) {
        if (newUserId != null && !newUserId.equals(userIdTrigger.getValue())) {
            _isLoading.setValue(true);
            _errorMessage.setValue(null);
            userIdTrigger.setValue(newUserId);
        }
    }

    public LiveData<User> getUserProfile() { return userProfile; }
    public LiveData<List<Listing>> getUserListings() { return userListings; }
    public LiveData<List<Review>> getUserReviews() { return userReviews; }
    public LiveData<Boolean> isLoading() { return _isLoading; }
    public LiveData<String> getErrorMessage() { return _errorMessage; }
}