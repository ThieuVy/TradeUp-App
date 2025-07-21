package com.example.testapptradeup.viewmodels;

import android.util.Log;

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
import java.util.List;
import java.util.Objects;

public class MyListingsViewModel extends ViewModel {
    private final ListingRepository listingRepository;
    private final String userId;

    // Lớp nội bộ để đóng gói tham số tải dữ liệu, đã bao gồm 'status'
    private static class LoadParams {
        @NonNull final String userId;
        @Nullable final String status;
        @Nullable final DocumentSnapshot lastVisible;

        public LoadParams(@NonNull String userId, @Nullable String status, @Nullable DocumentSnapshot lastVisible) {
            this.userId = userId;
            this.status = status;
            this.lastVisible = lastVisible;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            LoadParams that = (LoadParams) o;
            return userId.equals(that.userId) &&
                    Objects.equals(status, that.status) &&
                    Objects.equals(lastVisible, that.lastVisible);
        }

        @Override
        public int hashCode() {
            return Objects.hash(userId, status, lastVisible);
        }
    }

    private final MutableLiveData<LoadParams> loadTrigger = new MutableLiveData<>();
    private final LiveData<PagedResult<Listing>> pagedResult;

    // Đây là LiveData chính chứa danh sách hiển thị, được cập nhật trực tiếp từ pagedResult
    private final MediatorLiveData<List<Listing>> displayedListings = new MediatorLiveData<>();
    private final MutableLiveData<String> filterStatus = new MutableLiveData<>("all");

    public final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    public final MutableLiveData<Boolean> isLoadingMore = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    private DocumentSnapshot lastVisibleDocument = null;
    private boolean isLastPage = false;
    private boolean isCurrentlyLoading = false;

    public MyListingsViewModel() {
        this.listingRepository = new ListingRepository();
        this.userId = FirebaseAuth.getInstance().getUid();

        pagedResult = Transformations.switchMap(Transformations.distinctUntilChanged(loadTrigger), params -> {
            if (params == null) {
                return new MutableLiveData<>();
            }
            isCurrentlyLoading = true;
            // Gọi hàm getMyListings đã được sửa đổi với 3 tham số
            return listingRepository.getMyListings(params.userId, params.status, params.lastVisible);
        });

        // Lắng nghe kết quả phân trang và cập nhật danh sách hiển thị
        displayedListings.addSource(pagedResult, result -> {
            if (result == null) {
                errorMessage.setValue("Đã xảy ra lỗi không xác định.");
                resetLoadingStates();
                return;
            }
            if (!result.isSuccess() || result.getData() == null) {
                Exception error = result.getError();
                String message = (error != null) ? error.getMessage() : null;
                errorMessage.setValue(message != null && !message.isEmpty() ? message : "Không thể tải danh sách tin đăng.");
                resetLoadingStates();
                return;
            }

            boolean isInitialLoad = (loadTrigger.getValue() != null && loadTrigger.getValue().lastVisible == null);

            if (isInitialLoad) {
                // Nếu là lần tải đầu (refresh hoặc đổi tab), thay thế hoàn toàn danh sách
                displayedListings.setValue(new ArrayList<>(result.getData()));
            } else {
                // Nếu là tải thêm trang, nối vào danh sách cũ
                List<Listing> currentList = displayedListings.getValue();
                if (currentList == null) currentList = new ArrayList<>();
                List<Listing> updatedList = new ArrayList<>(currentList);
                updatedList.addAll(result.getData());
                displayedListings.setValue(updatedList);
            }

            Log.d("MyListingsCheck", "ViewModel đang tải tin đăng cho User ID: " + this.userId);
            lastVisibleDocument = result.getLastVisible();
            isLastPage = result.getData().size() < ListingRepository.PAGE_SIZE;
            resetLoadingStates();
        });

        // Tải dữ liệu lần đầu với bộ lọc mặc định là "all"
        refreshListings();
    }

    private void resetLoadingStates() {
        isCurrentlyLoading = false;
        isLoading.setValue(false);
        isLoadingMore.setValue(false);
    }

    public void loadNextPage() {
        if (userId == null || isCurrentlyLoading || isLastPage) return;
        isLoadingMore.setValue(true);
        // Truyền cả status hiện tại khi tải thêm trang
        loadTrigger.setValue(new LoadParams(userId, filterStatus.getValue(), lastVisibleDocument));
    }

    public void refreshListings() {
        if (userId == null) return;
        isLoading.setValue(true);
        isLastPage = false;
        lastVisibleDocument = null;
        // Xóa danh sách cũ để UI hiển thị vòng xoay loading chính
        displayedListings.setValue(new ArrayList<>());
        // Kích hoạt trigger để tải lại từ đầu với status hiện tại
        loadTrigger.setValue(new LoadParams(userId, filterStatus.getValue(), null));
    }

    public void setFilter(String status) {
        if (!Objects.equals(filterStatus.getValue(), status)) {
            filterStatus.setValue(status);
            // Khi đổi tab (thay đổi bộ lọc), luôn luôn tải lại danh sách từ đầu
            refreshListings();
        }
    }

    public LiveData<Boolean> deleteListing(String listingId) {
        MutableLiveData<Boolean> deleteResult = new MutableLiveData<>();
        // Sử dụng observeForever an toàn trong ViewModel vì nó sẽ được dọn dẹp trong onCleared()
        // Tuy nhiên, gỡ observer ngay sau khi hoàn thành là một thói quen tốt.
        Observer<Boolean> observer = new Observer<>() {
            @Override
            public void onChanged(Boolean success) {
                if (Boolean.TRUE.equals(success)) {
                    List<Listing> currentList = displayedListings.getValue();
                    if (currentList != null) {
                        List<Listing> updatedList = new ArrayList<>(currentList);
                        updatedList.removeIf(l -> l.getId().equals(listingId));
                        displayedListings.setValue(updatedList);
                    }
                }
                deleteResult.setValue(success);
                // Gỡ bỏ observer để tránh gọi lại nhiều lần
                deleteResult.removeObserver(this);
            }
        };
        listingRepository.deleteListing(listingId).observeForever(observer);
        return deleteResult;
    }

    // --- Getters cho Fragment ---

    // Fragment sẽ observe trực tiếp LiveData này
    public LiveData<List<Listing>> getDisplayedListings() {
        return displayedListings;
    }

    public LiveData<Boolean> isLoading() {
        return isLoading;
    }

    public LiveData<Boolean> isLoadingMore() {
        return isLoadingMore;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public boolean isLastPage() {
        return isLastPage;
    }

    public boolean isCurrentlyLoading() {
        return isCurrentlyLoading;
    }
}