package com.example.testapptradeup.viewmodels;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.Transformations;
import com.example.testapptradeup.models.Listing;
import com.example.testapptradeup.repositories.ListingRepository;
import com.example.testapptradeup.repositories.UserRepository;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

public class FavoritesViewModel extends ViewModel {
    private final UserRepository userRepository;
    private final ListingRepository listingRepository;
    private final LiveData<List<Listing>> favoriteListingsData;

    public FavoritesViewModel() {
        this.userRepository = new UserRepository();
        this.listingRepository = new ListingRepository();
        String userId = FirebaseAuth.getInstance().getUid();

        // Bước 1: Lấy LiveData chứa danh sách các ID yêu thích.
        // LiveData này sẽ tự động cập nhật mỗi khi dữ liệu trên Firestore thay đổi.
        LiveData<List<String>> favoriteIdsData = userRepository.getFavoriteIds(userId);

        // Bước 2: Sử dụng Transformations.switchMap.
        // Đây là "phép màu": mỗi khi `favoriteIdsData` có giá trị mới (vd: người dùng vừa thêm 1 sp),
        // switchMap sẽ tự động hủy yêu cầu cũ và gọi `listingRepository.getListingsByIds()`
        // với danh sách ID mới nhất.
        favoriteListingsData = Transformations.switchMap(favoriteIdsData, ids -> {
            if (ids == null || ids.isEmpty()) {
                // Nếu không có ID nào, trả về một LiveData chứa danh sách rỗng ngay lập tức.
                MutableLiveData<List<Listing>> emptyList = new MutableLiveData<>();
                emptyList.setValue(new ArrayList<>());
                return emptyList;
            }
            // Nếu có ID, gọi repository để lấy thông tin chi tiết các sản phẩm.
            return listingRepository.getListingsByIds(ids);
        });
    }

    /**
     * Fragment sẽ observe LiveData này. Nó sẽ luôn chứa danh sách
     * chi tiết các sản phẩm yêu thích đã được cập nhật.
     */
    public LiveData<List<Listing>> getFavoriteListings() {
        return favoriteListingsData;
    }

    public void toggleFavorite(String listingId, boolean shouldBeFavorite) {
        String userId = FirebaseAuth.getInstance().getUid();
        userRepository.updateFavoriteStatus(userId, listingId, shouldBeFavorite)
                .addOnSuccessListener(aVoid -> Log.d("ViewModel", "Cập nhật yêu thích thành công!"))
                .addOnFailureListener(e -> Log.e("ViewModel", "Lỗi cập nhật yêu thích", e));
    }

    public LiveData<List<String>> getFavoriteIds() {
        String userId = FirebaseAuth.getInstance().getUid();
        return userRepository.getFavoriteListingIds(userId);
    }
}