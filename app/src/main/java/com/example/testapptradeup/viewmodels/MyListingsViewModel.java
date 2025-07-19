package com.example.testapptradeup.viewmodels;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import com.example.testapptradeup.models.Listing;
import com.example.testapptradeup.models.PagedResult;
import com.example.testapptradeup.repositories.ListingRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class MyListingsViewModel extends ViewModel {
    private final ListingRepository listingRepository;
    @Nullable
    private final String userId;

    private static class LoadParams {
        @NonNull final String userId;
        @Nullable final DocumentSnapshot lastVisible;
        LoadParams(@NonNull String userId, @Nullable DocumentSnapshot lastVisible) {
            this.userId = userId;
            this.lastVisible = lastVisible;
        }
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            LoadParams that = (LoadParams) o;
            return userId.equals(that.userId) && Objects.equals(lastVisible, that.lastVisible);
        }
        @Override
        public int hashCode() {
            return Objects.hash(userId, lastVisible);
        }
    }

    private final MutableLiveData<LoadParams> loadTrigger = new MutableLiveData<>();
    private final LiveData<PagedResult<Listing>> pagedResult;
    private final MediatorLiveData<List<Listing>> allMyListings = new MediatorLiveData<>();
    private final MediatorLiveData<List<Listing>> displayedListings = new MediatorLiveData<>();
    private final MutableLiveData<String> filterStatus = new MutableLiveData<>("all");
    public final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    public final MutableLiveData<Boolean> isLoadingMore = new MutableLiveData<>(false);
    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();

    private DocumentSnapshot lastVisibleDocument = null;
    private boolean isLastPage = false;
    private boolean isCurrentlyLoading = false;

    public boolean isCurrentlyLoading() {
        return isCurrentlyLoading;
    }

    public MyListingsViewModel() {
        this.listingRepository = new ListingRepository();
        this.userId = FirebaseAuth.getInstance().getUid();

        pagedResult = Transformations.switchMap(loadTrigger, params -> {
            if (params == null) {
                MutableLiveData<PagedResult<Listing>> emptyResult = new MutableLiveData<>();
                emptyResult.setValue(new PagedResult<>(Collections.emptyList(), null, null));
                return emptyResult;
            }
            isCurrentlyLoading = true;
            return listingRepository.getMyListings(params.userId, params.lastVisible);
        });

        allMyListings.addSource(pagedResult, result -> {
            if (result == null || !result.isSuccess() || result.getData() == null) {
                _errorMessage.setValue("Không thể tải danh sách tin đăng.");
                resetLoadingStates();
                return;
            }

            boolean isInitialLoad = (loadTrigger.getValue() != null && loadTrigger.getValue().lastVisible == null);

            if (isInitialLoad) {
                allMyListings.setValue(result.getData());
            } else {
                List<Listing> currentList = allMyListings.getValue();
                if (currentList == null) currentList = new ArrayList<>();
                List<Listing> updatedList = new ArrayList<>(currentList);
                updatedList.addAll(result.getData());
                allMyListings.setValue(updatedList);
            }

            lastVisibleDocument = result.getLastVisible();
            isLastPage = result.getData() == null || result.getData().size() < ListingRepository.PAGE_SIZE;
            resetLoadingStates();
        });

        displayedListings.addSource(allMyListings, listings -> {
            String currentFilter = filterStatus.getValue();
            applyFilterAndPost(listings, currentFilter);
        });
        displayedListings.addSource(filterStatus, status -> {
            List<Listing> currentListings = allMyListings.getValue();
            applyFilterAndPost(currentListings, status);
        });

        refreshListings();
    }

    private void resetLoadingStates() {
        isCurrentlyLoading = false;
        isLoading.setValue(false);
        isLoadingMore.setValue(false);
    }

    private void applyFilterAndPost(@Nullable List<Listing> listings, @Nullable String status) {
        if (listings == null) {
            displayedListings.setValue(new ArrayList<>());
            return;
        }
        if (status == null || "all".equals(status)) {
            displayedListings.setValue(listings);
            return;
        }
        List<Listing> filtered = listings.stream()
                .filter(l -> status.equals(l.getStatus()))
                .collect(Collectors.toList());
        displayedListings.setValue(filtered);
    }

    public void loadNextPage() {
        if (userId == null || isCurrentlyLoading || isLastPage) {
            return;
        }
        isLoadingMore.setValue(true);
        loadTrigger.setValue(new LoadParams(userId, lastVisibleDocument));
    }

    public void refreshListings() {
        if (userId == null || isCurrentlyLoading) {
            return;
        }
        isLoading.setValue(true);
        isLastPage = false;
        lastVisibleDocument = null;
        loadTrigger.setValue(new LoadParams(userId, null));
    }

    public LiveData<List<Listing>> getDisplayedListings() { return displayedListings; }
    public LiveData<Boolean> isLoading() { return isLoading; }
    public LiveData<Boolean> isLoadingMore() { return isLoadingMore; }
    public boolean isLastPage() { return isLastPage; }
    public LiveData<String> getErrorMessage() { return _errorMessage; }

    public void setFilter(String status) {
        if (!Objects.equals(filterStatus.getValue(), status)) {
            filterStatus.setValue(status);
        }
    }

    public LiveData<Boolean> deleteListing(String listingId) {
        MutableLiveData<Boolean> deleteResult = new MutableLiveData<>();
        listingRepository.deleteListing(listingId).observeForever(new Observer<>() {
            @Override
            public void onChanged(Boolean success) {
                if (Boolean.TRUE.equals(success)) {
                    // Cập nhật lại danh sách trên UI sau khi xóa thành công
                    List<Listing> currentList = allMyListings.getValue();
                    if (currentList != null) {
                        List<Listing> updatedList = new ArrayList<>(currentList);
                        updatedList.removeIf(l -> l.getId().equals(listingId));
                        allMyListings.setValue(updatedList);
                    }
                }
                deleteResult.setValue(success);
                listingRepository.deleteListing(listingId).removeObserver(this);
            }
        });
        return deleteResult;
    }
}