package com.example.testapptradeup.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import com.example.testapptradeup.models.Listing;
import com.example.testapptradeup.repositories.ListingRepository;

public class ProductDetailViewModel extends ViewModel {
    private final ListingRepository listingRepository;
    public LiveData<Listing> listingDetail;

    public ProductDetailViewModel() {
        this.listingRepository = new ListingRepository();
    }

    public void loadListingDetail(String listingId) {
        listingDetail = listingRepository.getListingById(listingId);
    }
}