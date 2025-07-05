package com.example.testapptradeup.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.testapptradeup.models.Listing;
import com.example.testapptradeup.models.Review;
import com.example.testapptradeup.models.User;
import com.example.testapptradeup.repositories.ListingRepository;
import com.example.testapptradeup.repositories.UserRepository;

import java.util.List;

public class PublicProfileViewModel extends ViewModel {

    private final UserRepository userRepository;
    private final ListingRepository listingRepository;

    private final MutableLiveData<String> userId = new MutableLiveData<>();
    private LiveData<User> userProfile;
    // Kiểu dữ liệu mong muốn là LiveData<List<Listing>>
    private LiveData<List<Listing>> userListings;
    private LiveData<List<Review>> userReviews;

    public PublicProfileViewModel() {
        userRepository = new UserRepository();
        listingRepository = new ListingRepository();
    }

    public void loadProfileData(String newUserId) {
        if (newUserId != null && !newUserId.equals(userId.getValue())) {
            userId.setValue(newUserId);
            userProfile = userRepository.getUserProfile(newUserId);

            // Lời gọi này bây giờ sẽ hợp lệ vì phương thức trong Repository đã được sửa
            userListings = listingRepository.getActiveListingsByUser(newUserId, 10);

            userReviews = userRepository.getUserReviews(newUserId);
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