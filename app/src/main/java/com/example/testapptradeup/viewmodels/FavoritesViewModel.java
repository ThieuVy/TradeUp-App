package com.example.testapptradeup.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.Transformations;
import com.example.testapptradeup.models.Listing;
import com.example.testapptradeup.repositories.ListingRepository;
import com.example.testapptradeup.repositories.UserRepository;
import com.google.firebase.auth.FirebaseAuth;
import java.util.List;

public class FavoritesViewModel extends ViewModel {
    private final UserRepository userRepository;
    private final ListingRepository listingRepository;
    private final LiveData<List<Listing>> favoriteListingsData;

    public FavoritesViewModel() {
        this.userRepository = new UserRepository();
        this.listingRepository = new ListingRepository();
        String userId = FirebaseAuth.getInstance().getUid();

        // Chain LiveData: Lấy IDs -> Dùng IDs để lấy List<Listing>
        LiveData<List<String>> favoriteIdsData = userRepository.getFavoriteIds(userId);
        favoriteListingsData = Transformations.switchMap(favoriteIdsData, ids ->
                listingRepository.getListingsByIds(ids)
        );
    }

    public LiveData<List<Listing>> getFavoriteListings() {
        return favoriteListingsData;
    }
}