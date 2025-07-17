package com.example.testapptradeup.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;
import com.example.testapptradeup.models.Listing;
import com.example.testapptradeup.repositories.ListingRepository;

public class EditPostViewModel extends ViewModel {
    private final ListingRepository repository;

    // Trigger để bắt đầu tải dữ liệu
    private final MutableLiveData<String> listingIdTrigger = new MutableLiveData<>();

    // LiveData chứa dữ liệu tin đăng gốc để hiển thị lên form
    private final LiveData<Listing> listingData;

    // LiveData báo cáo trạng thái của thao tác cập nhật
    private final MutableLiveData<Boolean> _updateStatus = new MutableLiveData<>();
    public LiveData<Boolean> getUpdateStatus() { return _updateStatus; }

    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>();
    public LiveData<Boolean> isLoading() { return _isLoading; }

    public EditPostViewModel() {
        this.repository = new ListingRepository();

        // Sử dụng switchMap để tự động gọi repository khi listingIdTrigger thay đổi
        listingData = Transformations.switchMap(listingIdTrigger, id -> {
            _isLoading.setValue(true);
            return repository.getListingById(id);
        });
    }

    /**
     * Fragment gọi hàm này để bắt đầu quá trình tải dữ liệu.
     * @param listingId ID của tin đăng cần sửa.
     */
    public void loadListingData(String listingId) {
        if (listingId != null && !listingId.equals(listingIdTrigger.getValue())) {
            listingIdTrigger.setValue(listingId);
        }
    }

    /**
     * Fragment gọi hàm này để lưu các thay đổi.
     * @param updatedListing Đối tượng Listing đã chứa các thông tin mới từ UI.
     */
    public void saveChanges(Listing updatedListing) {
        _isLoading.setValue(true);
        repository.updateListing(updatedListing).observeForever(success -> {
            _isLoading.setValue(false);
            _updateStatus.setValue(success);
        });
    }

    /**
     * Getter để Fragment có thể observe dữ liệu tin đăng.
     */
    public LiveData<Listing> getListingData() {
        return listingData;
    }
}