package com.example.testapptradeup.viewmodels;

import android.location.Location;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

import com.example.testapptradeup.models.Listing;
import com.example.testapptradeup.models.PagedResult;
import com.example.testapptradeup.models.SearchParams;
import com.example.testapptradeup.models.SearchResult;
import com.example.testapptradeup.repositories.ListingRepository;
import com.example.testapptradeup.repositories.UserRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class SearchViewModel extends ViewModel {

    public enum UiState {
        IDLE, LOADING, LOADING_MORE, SUCCESS, EMPTY, ERROR
    }

    private final ListingRepository listingRepository;
    private final UserRepository userRepository;
    private final String currentUserId;

    private final MutableLiveData<SearchParams> searchParamsLiveData = new MutableLiveData<>(new SearchParams());
    private final MutableLiveData<UiState> uiState = new MutableLiveData<>(UiState.IDLE);
    private final MutableLiveData<List<SearchResult>> searchResults = new MutableLiveData<>(new ArrayList<>());
    private final LiveData<List<String>> favoriteIds;

    private DocumentSnapshot lastVisibleDocument = null;
    private boolean isLastPage = false;

    public SearchViewModel() {
        listingRepository = new ListingRepository();
        userRepository = new UserRepository();
        currentUserId = FirebaseAuth.getInstance().getUid();

        if (currentUserId != null) {
            favoriteIds = userRepository.getFavoriteIds(currentUserId);
        } else {
            MutableLiveData<List<String>> emptyFavs = new MutableLiveData<>();
            emptyFavs.setValue(new ArrayList<>());
            favoriteIds = emptyFavs;
        }
    }

    public LiveData<UiState> getUiState() { return uiState; }
    public LiveData<List<SearchResult>> getSearchResults() { return searchResults; }
    public LiveData<SearchParams> getSearchParams() { return searchParamsLiveData; }

    public void startNewSearch(SearchParams params) {
        searchParamsLiveData.setValue(params);
        isLastPage = false;
        lastVisibleDocument = null;
        searchResults.setValue(new ArrayList<>());
        performSearch(true);
    }

    public void loadMore() {
        if (uiState.getValue() == UiState.LOADING || uiState.getValue() == UiState.LOADING_MORE || isLastPage) {
            return;
        }
        performSearch(false);
    }

    private void performSearch(boolean isNewSearch) {
        if (isNewSearch) {
            uiState.setValue(UiState.LOADING);
        } else {
            uiState.setValue(UiState.LOADING_MORE);
        }

        SearchParams params = searchParamsLiveData.getValue();
        if (params == null) {
            uiState.setValue(UiState.ERROR);
            return;
        }

        LiveData<PagedResult<Listing>> pagedResultLiveData = listingRepository.searchListings(params, lastVisibleDocument);

        pagedResultLiveData.observeForever(new Observer<>() {
            @Override
            public void onChanged(PagedResult<Listing> pagedResult) {
                pagedResultLiveData.removeObserver(this);

                if (pagedResult == null || !pagedResult.isSuccess()) {
                    Log.e("SearchViewModel", "Search failed", pagedResult != null ? pagedResult.getError() : new Exception("PagedResult is null"));
                    uiState.postValue(UiState.ERROR);
                    return;
                }

                // SỬA LỖI: Thêm logic lọc theo khoảng cách ở client-side
                List<Listing> distanceFilteredListings = new ArrayList<>();
                if (params.getUserLocation() != null && params.getMaxDistance() > 0) {
                    for (Listing listing : pagedResult.getData()) {
                        if (listing.getLatitude() != 0 && listing.getLongitude() != 0) {
                            float[] results = new float[1];
                            Location.distanceBetween(
                                    params.getUserLocation().getLatitude(),
                                    params.getUserLocation().getLongitude(),
                                    listing.getLatitude(),
                                    listing.getLongitude(),
                                    results);
                            float distanceInMeters = results[0];
                            if (distanceInMeters <= params.getMaxDistance() * 1000) {
                                distanceFilteredListings.add(listing);
                            }
                        }
                    }
                } else {
                    distanceFilteredListings.addAll(pagedResult.getData());
                }

                lastVisibleDocument = pagedResult.getLastVisible();
                isLastPage = pagedResult.getData() == null || pagedResult.getData().size() < ListingRepository.PAGE_SIZE;

                List<String> favIds = favoriteIds.getValue();
                List<SearchResult> newResults = distanceFilteredListings.stream()
                        .map(listing -> new SearchResult(listing, favIds != null && favIds.contains(listing.getId())))
                        .collect(Collectors.toList());

                List<SearchResult> currentList = new ArrayList<>(Objects.requireNonNull(searchResults.getValue()));
                if (isNewSearch) {
                    currentList.clear();
                }
                currentList.addAll(newResults);
                searchResults.postValue(currentList);

                if (currentList.isEmpty()) {
                    uiState.postValue(UiState.EMPTY);
                } else {
                    uiState.postValue(UiState.SUCCESS);
                }
            }
        });
    }

    public void toggleFavorite(String listingId, boolean isFavorite) {
        if (currentUserId == null) return;

        List<SearchResult> currentResults = searchResults.getValue();
        if (currentResults != null) {
            for (SearchResult result : currentResults) {
                if (result.getId().equals(listingId)) {
                    result.setFavorite(isFavorite);
                    break;
                }
            }
            searchResults.setValue(new ArrayList<>(currentResults));
        }

        userRepository.toggleFavorite(currentUserId, listingId, isFavorite);
    }

    public boolean isLastPage() {
        return isLastPage;
    }
}