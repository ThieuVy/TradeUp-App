package com.example.testapptradeup.viewmodels;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.testapptradeup.models.Listing;
import com.example.testapptradeup.repositories.ListingRepository;
import java.util.List;

public class ProductListViewModel extends ViewModel {
    private final ListingRepository listingRepository;
    public LiveData<List<Listing>> productList;
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();

    public ProductListViewModel() {
        this.listingRepository = new ListingRepository();
    }

    public void loadProducts(String filterType, @Nullable String categoryId) {
        isLoading.setValue(true);
        productList = listingRepository.getListingsByFilter(filterType, categoryId);
        // Lắng nghe kết quả để tắt loading
        productList.observeForever(listings -> isLoading.setValue(false));
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }
}