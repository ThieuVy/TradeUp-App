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

import java.util.ArrayList;
import java.util.List;

public class PublicProfileViewModel extends ViewModel {

    private final UserRepository userRepository;
    private final ListingRepository listingRepository;
    private final MutableLiveData<String> userIdTrigger = new MutableLiveData<>();

    private final MutableLiveData<String> userId = new MutableLiveData<>();
    private final LiveData<User> userProfile;
    private final LiveData<List<Listing>> userListings;
    private final LiveData<List<Review>> userReviews;

    public PublicProfileViewModel() {
        userRepository = new UserRepository();
        listingRepository = new ListingRepository();

        // 3. Sử dụng switchMap để tự động gọi repository khi userIdTrigger thay đổi
        userProfile = Transformations.switchMap(userIdTrigger, id -> {
            if (id == null || id.isEmpty()) {
                return new MutableLiveData<>(null); // Trả về LiveData rỗng nếu không có id
            }
            return userRepository.getUserProfile(id);
        });

        userListings = Transformations.switchMap(userIdTrigger, id -> {
            if (id == null || id.isEmpty()) {
                return new MutableLiveData<>(new ArrayList<>());
            }
            return listingRepository.getActiveListingsByUser(id, 10);
        });

        userReviews = Transformations.switchMap(userIdTrigger, id -> {
            if (id == null || id.isEmpty()) {
                return new MutableLiveData<>(new ArrayList<>());
            }
            return userRepository.getUserReviews(id);
        });
    }

    public void loadProfileData(String newUserId) {
        if (newUserId != null && !newUserId.equals(userIdTrigger.getValue())) {
            userIdTrigger.setValue(newUserId);
        }
    }

    // Getters cho Fragment observe
    public LiveData<User> getUserProfile() {
        return userProfile;
    }

    public LiveData<List<Listing>> getUserListings() {
        return userListings;
    }

    public LiveData<List<Review>> getUserReviews() {
        return userReviews;
    }
}