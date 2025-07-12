package com.example.testapptradeup.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import com.example.testapptradeup.models.Listing;
import com.example.testapptradeup.models.Review;
import com.example.testapptradeup.models.User;
import com.example.testapptradeup.repositories.ListingRepository;
import com.example.testapptradeup.repositories.UserRepository;
import com.example.testapptradeup.utils.Result; // <<< THÊM IMPORT

import java.util.Collections;
import java.util.List;

public class PublicProfileViewModel extends ViewModel {

    private final UserRepository userRepository;
    private final ListingRepository listingRepository;

    // --- Trigger và LiveData trung gian ---
    private final MutableLiveData<String> userIdTrigger = new MutableLiveData<>();
    private final LiveData<Result<User>> userProfileResult; // Giữ kết quả thô từ repo

    // --- LiveData cuối cùng cho UI ---
    private final LiveData<User> userProfile;
    private final LiveData<List<Listing>> userListings;
    private final LiveData<List<Review>> userReviews;
    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>();
    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();


    public PublicProfileViewModel() {
        userRepository = new UserRepository();
        listingRepository = new ListingRepository();

        // ========== BẮT ĐẦU PHẦN SỬA ĐỔI ==========

        // 1. switchMap nhận Result<User> từ repository
        userProfileResult = Transformations.switchMap(userIdTrigger, id -> {
            if (id == null || id.isEmpty()) {
                MutableLiveData<Result<User>> errorResult = new MutableLiveData<>();
                errorResult.setValue(Result.error(new IllegalArgumentException("User ID is invalid.")));
                return errorResult;
            }
            return userRepository.getUserProfile(id);
        });

        // 2. map để xử lý Result, tách dữ liệu và lỗi ra các LiveData riêng
        userProfile = Transformations.map(userProfileResult, result -> {
            _isLoading.setValue(false); // Luôn tắt loading khi có kết quả
            if (result.isSuccess()) {
                _errorMessage.setValue(null);
                return result.getData();
            } else {
                _errorMessage.setValue(result.getError() != null ? result.getError().getMessage() : "Lỗi không xác định");
                return null;
            }
        });

        // ==========================================

        // Các switchMap khác vẫn giữ nguyên vì chúng chưa được nâng cấp để trả về Result
        userListings = Transformations.switchMap(userIdTrigger, id -> {
            if (id == null || id.isEmpty()) {
                return new MutableLiveData<>(Collections.emptyList());
            }
            return listingRepository.getActiveListingsByUser(id, 10);
        });

        userReviews = Transformations.switchMap(userIdTrigger, id -> {
            if (id == null || id.isEmpty()) {
                return new MutableLiveData<>(Collections.emptyList());
            }
            return userRepository.getUserReviews(id);
        });
    }

    /**
     * Kích hoạt việc tải dữ liệu cho một hồ sơ công khai.
     * @param newUserId ID của người dùng cần xem.
     */
    public void loadProfileData(String newUserId) {
        if (newUserId != null && !newUserId.equals(userIdTrigger.getValue())) {
            _isLoading.setValue(true); // Bật loading
            _errorMessage.setValue(null); // Xóa lỗi cũ
            userIdTrigger.setValue(newUserId);
        }
    }

    // --- GETTERS CHO FRAGMENT OBSERVE ---

    public LiveData<User> getUserProfile() {
        return userProfile;
    }

    public LiveData<List<Listing>> getUserListings() {
        return userListings;
    }

    public LiveData<List<Review>> getUserReviews() {
        return userReviews;
    }

    public LiveData<Boolean> isLoading() {
        return _isLoading;
    }

    public LiveData<String> getErrorMessage() {
        return _errorMessage;
    }
}