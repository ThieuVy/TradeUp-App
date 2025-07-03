package com.example.testapptradeup.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.testapptradeup.models.Listing;
import com.example.testapptradeup.repositories.ListingRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MyListingsViewModel extends ViewModel {
    private final ListingRepository listingRepository;
    private final String userId;

    // State variables
    private final MutableLiveData<String> filterStatus = new MutableLiveData<>("all");
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoadingMore = new MutableLiveData<>(false);
    private final MutableLiveData<List<Listing>> allMyListings = new MutableLiveData<>(new ArrayList<>());
    private DocumentSnapshot lastVisibleDocument = null;
    private boolean isLastPage = false;

    // LiveData cho UI observe
    private final MediatorLiveData<List<Listing>> displayedListings = new MediatorLiveData<>();

    public MyListingsViewModel() {
        this.listingRepository = new ListingRepository();
        this.userId = FirebaseAuth.getInstance().getUid();

        displayedListings.addSource(allMyListings, this::applyFilterAndPost);
        displayedListings.addSource(filterStatus, status -> applyFilterAndPost(allMyListings.getValue()));

        loadInitialListings();
    }

    private void applyFilterAndPost(List<Listing> listings) {
        if (listings == null) {
            displayedListings.setValue(new ArrayList<>());
            return;
        }
        String status = filterStatus.getValue();
        if (status == null || "all".equals(status)) {
            displayedListings.setValue(listings);
            return;
        }
        List<Listing> filtered = listings.stream()
                .filter(l -> status.equals(l.getStatus()))
                .collect(Collectors.toList());
        displayedListings.setValue(filtered);
    }

    private void loadInitialListings() {
        if (userId == null) return;
        isLoading.setValue(true);
        isLastPage = false;
        lastVisibleDocument = null;

        listingRepository.getMyListings(userId, null).observeForever(result -> {
            isLoading.setValue(false);
            if (result != null && result.isSuccess()) {
                allMyListings.setValue(result.getData());
                lastVisibleDocument = result.getLastVisible();
                if (result.getData() == null || result.getData().size() < ListingRepository.PAGE_SIZE) {
                    isLastPage = true;
                }
            }
        });
    }

    public void loadNextPage() {
        if (userId == null || Boolean.TRUE.equals(isLoadingMore.getValue()) || isLastPage) {
            return;
        }
        isLoadingMore.setValue(true);

        listingRepository.getMyListings(userId, lastVisibleDocument).observeForever(result -> {
            isLoadingMore.setValue(false);
            if (result != null && result.isSuccess() && result.getData() != null) {
                if (result.getData().isEmpty()) {
                    isLastPage = true;
                    return;
                }

                List<Listing> currentList = new ArrayList<>(allMyListings.getValue());
                currentList.addAll(result.getData());
                allMyListings.setValue(currentList);

                lastVisibleDocument = result.getLastVisible();
                if (result.getData().size() < ListingRepository.PAGE_SIZE) {
                    isLastPage = true;
                }
            }
        });
    }

    public void refreshListings() {
        loadInitialListings();
    }

    // Getters cho Fragment
    public LiveData<List<Listing>> getDisplayedListings() { return displayedListings; }
    public LiveData<Boolean> isLoading() { return isLoading; }
    public LiveData<Boolean> isLoadingMore() { return isLoadingMore; }
    public boolean isLastPage() { return isLastPage; }

    public void setFilter(String status) {
        filterStatus.setValue(status);
    }

    public LiveData<Boolean> deleteListing(String listingId) {
        // Sau khi xóa, gọi refresh để tải lại toàn bộ danh sách
        refreshListings();
        return listingRepository.deleteListing(listingId);
    }
}