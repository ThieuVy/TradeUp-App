package com.example.testapptradeup.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.Transformations;
import com.example.testapptradeup.models.Listing;
import com.example.testapptradeup.models.PagedResult;
import com.example.testapptradeup.models.SearchParams;
import com.example.testapptradeup.models.SearchResult;
import com.example.testapptradeup.repositories.ListingRepository;
import com.example.testapptradeup.repositories.UserRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class SearchViewModel extends ViewModel {

    // Trạng thái UI
    public enum UiState {
        IDLE, LOADING, LOADING_MORE, SUCCESS, EMPTY, ERROR
    }

    private final ListingRepository listingRepository;
    private final UserRepository userRepository;
    private final String currentUserId;

    // Dữ liệu và trạng thái
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

        // Lấy danh sách ID yêu thích của người dùng
        if (currentUserId != null) {
            favoriteIds = userRepository.getFavoriteIds(currentUserId);
        } else {
            MutableLiveData<List<String>> emptyFavs = new MutableLiveData<>();
            emptyFavs.setValue(new ArrayList<>());
            favoriteIds = emptyFavs;
        }
    }

    // Getters để Fragment observe
    public LiveData<UiState> getUiState() { return uiState; }
    public LiveData<List<SearchResult>> getSearchResults() { return searchResults; }
    public LiveData<SearchParams> getSearchParams() { return searchParamsLiveData; }

    public void startNewSearch(SearchParams params) {
        searchParamsLiveData.setValue(params);
        isLastPage = false;
        lastVisibleDocument = null;
        searchResults.setValue(new ArrayList<>()); // Xóa kết quả cũ
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

        // Sử dụng Transformations.switchMap để kết hợp kết quả tìm kiếm và danh sách yêu thích
        LiveData<List<SearchResult>> combinedData = Transformations.switchMap(pagedResultLiveData, pagedResult ->
                Transformations.map(favoriteIds, favIds -> {
                    if (pagedResult == null || !pagedResult.isSuccess() || pagedResult.getData() == null) {
                        uiState.postValue(UiState.ERROR);
                        return new ArrayList<>();
                    }

                    lastVisibleDocument = pagedResult.getLastVisible();
                    // PAGE_SIZE cần được định nghĩa trong ListingRepository
                    isLastPage = pagedResult.getData().size() < ListingRepository.PAGE_SIZE;

                    // Chuyển đổi Listing thành SearchResult, kiểm tra isFavorite
                    return pagedResult.getData().stream()
                            .map(listing -> new SearchResult(listing, favIds != null && favIds.contains(listing.getId())))
                            .collect(Collectors.toList());
                })
        );

        // Chỉ observe một lần để xử lý kết quả
        combinedData.observeForever(newResults -> {
            if (newResults != null) {
                List<SearchResult> currentList = new ArrayList<>(searchResults.getValue() != null ? searchResults.getValue() : Collections.emptyList());
                if (isNewSearch) {
                    currentList.clear();
                }
                currentList.addAll(newResults);
                searchResults.postValue(currentList);

                if (currentList.isEmpty() && isNewSearch) {
                    uiState.postValue(UiState.EMPTY);
                } else {
                    uiState.postValue(UiState.SUCCESS);
                }
            }
        });
    }

    public void toggleFavorite(String listingId, boolean isFavorite) {
        if (currentUserId == null) return;

        // Cập nhật trạng thái ngay lập tức trên UI để người dùng thấy phản hồi nhanh
        List<SearchResult> currentResults = searchResults.getValue();
        if (currentResults != null) {
            for (SearchResult result : currentResults) {
                if (result.getId().equals(listingId)) {
                    result.setFavorite(isFavorite);
                    break;
                }
            }
            searchResults.setValue(new ArrayList<>(currentResults)); // Tạo list mới để trigger update
        }

        // ========== SỬA LỖI Ở ĐÂY ==========
        // Đổi tên phương thức được gọi từ 'updateFavoriteStatus' thành 'toggleFavorite'
        userRepository.toggleFavorite(currentUserId, listingId, isFavorite);
        // ===================================
    }

    public boolean isLastPage() {
        return isLastPage;
    }
}