package com.example.testapptradeup.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.testapptradeup.models.Listing;
import com.example.testapptradeup.models.Offer;
import com.example.testapptradeup.repositories.ListingRepository;
import com.example.testapptradeup.repositories.OfferRepository;
import com.example.testapptradeup.repositories.UserRepository;
import com.google.firebase.auth.FirebaseAuth;

public class ProductDetailViewModel extends ViewModel {
    private final ListingRepository listingRepository;
    public LiveData<Listing> listingDetail;
    private final OfferRepository offerRepository;
    private final UserRepository userRepository;
    private final String currentUserId;

    public ProductDetailViewModel() {
        this.listingRepository = new ListingRepository();
        this.offerRepository = new OfferRepository();
        this.userRepository = new UserRepository();
        this.currentUserId = FirebaseAuth.getInstance().getUid();
    }

    public void loadListingDetail(String listingId) {
        listingDetail = listingRepository.getListingById(listingId);
    }

    public LiveData<Listing> getListingDetail() {
        return listingDetail;
    }

    public LiveData<Boolean> makeOffer(String listingId, String sellerId, double price, String message) {
        if (currentUserId == null) {
            MutableLiveData<Boolean> failure = new MutableLiveData<>();
            failure.setValue(false);
            return failure;
        }

        Offer offer = new Offer();
        offer.setListingId(listingId);
        offer.setSellerId(sellerId);
        offer.setBuyerId(currentUserId);
        // Lấy thông tin người mua (tên, avatar)
        // Trong một app thực tế, bạn nên lấy từ SharedPreferences hoặc ViewModel chung
        offer.setBuyerName("Current User Name");
        offer.setBuyerAvatarUrl("url_to_avatar");
        offer.setOfferPrice(price);
        offer.setMessage(message);
        offer.setStatus("pending");

        return offerRepository.createOffer(offer);
    }
}