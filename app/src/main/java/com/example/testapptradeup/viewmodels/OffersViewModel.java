package com.example.testapptradeup.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.testapptradeup.models.Listing;
import com.example.testapptradeup.models.Offer;
import com.example.testapptradeup.repositories.OfferRepository;

import java.util.List;

public class OffersViewModel extends ViewModel {
    private final OfferRepository offerRepository;
    private LiveData<List<Offer>> offers;
    private final MutableLiveData<Boolean> offerActionStatus = new MutableLiveData<>();

    public OffersViewModel() {
        this.offerRepository = new OfferRepository();
    }

    public LiveData<List<Offer>> getOffers(String listingId) {
        if (offers == null) {
            offers = offerRepository.getOffersForListing(listingId);
        }
        return offers;
    }

    public LiveData<Boolean> getOfferActionStatus() {
        return offerActionStatus;
    }

    public void acceptOffer(Offer offer, Listing listing) {
        offerRepository.acceptOffer(offer, listing).observeForever(offerActionStatus::setValue);
    }
}