package com.example.testapptradeup.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.testapptradeup.models.OfferWithListing;
import com.example.testapptradeup.repositories.OfferRepository;
import com.google.firebase.auth.FirebaseAuth;

import java.util.List;

public class MyOffersViewModel extends ViewModel {

    private final OfferRepository offerRepository;
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private final LiveData<List<OfferWithListing>> myOffers;

    public MyOffersViewModel() {
        offerRepository = new OfferRepository();
        String userId = FirebaseAuth.getInstance().getUid();

        isLoading.setValue(true);
        myOffers = offerRepository.getOffersSentByUserWithListingInfo(userId);
        myOffers.observeForever(offers -> isLoading.setValue(false)); // Tắt loading khi có kết quả
    }

    public LiveData<List<OfferWithListing>> getMyOffers() {
        return myOffers;
    }

    public LiveData<Boolean> isLoading() {
        return isLoading;
    }
}