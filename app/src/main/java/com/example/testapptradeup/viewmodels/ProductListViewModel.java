package com.example.testapptradeup.viewmodels;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.testapptradeup.models.Listing;
import com.example.testapptradeup.repositories.ListingRepository;
// <<< THÊM MỚI: Import các lớp cần thiết cho việc lấy tên Category >>>
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.List;

public class ProductListViewModel extends ViewModel {
    private final ListingRepository listingRepository;
    public LiveData<List<Listing>> productList;
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();

    // <<< BƯỚC 1: Thêm LiveData để giữ tiêu đề của Toolbar >>>
    private final MutableLiveData<String> toolbarTitle = new MutableLiveData<>();

    public ProductListViewModel() {
        this.listingRepository = new ListingRepository();
    }

    public void loadProducts(String filterType, @Nullable String categoryId) {
        isLoading.setValue(true);
        productList = listingRepository.getListingsByFilter(filterType, categoryId);
        // Lắng nghe kết quả để tắt loading
        productList.observeForever(listings -> isLoading.setValue(false));
    }

    // <<< BƯỚC 2: Thêm Getter cho Fragment để observe >>>
    public LiveData<String> getToolbarTitle() {
        return toolbarTitle;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    // <<< BƯỚC 3: Tạo phương thức để cập nhật tiêu đề >>>
    public void updateToolbarTitle(String filterType, @Nullable String categoryId) {
        if ("recommended".equals(filterType)) {
            toolbarTitle.setValue("Sản phẩm đề xuất");
        } else if ("recent".equals(filterType)) {
            toolbarTitle.setValue("Sản phẩm gần đây");
        } else if ("category".equals(filterType) && categoryId != null) {
            // Khi là danh mục, cần lấy tên từ ID
            // Đây là một ví dụ đơn giản, bạn có thể cần một CategoryRepository để làm việc này tốt hơn
            FirebaseFirestore.getInstance().collection("categories").document(categoryId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String categoryName = documentSnapshot.getString("name");
                            toolbarTitle.setValue(categoryName);
                        } else {
                            toolbarTitle.setValue("Danh mục không xác định");
                        }
                    })
                    .addOnFailureListener(e -> toolbarTitle.setValue("Danh mục"));
        } else {
            toolbarTitle.setValue("Sản phẩm"); // Tiêu đề mặc định
        }
    }

    // <<< BƯỚC 4 (Tùy chọn): Thêm logic xử lý yêu thích >>>
    public void toggleFavorite(String listingId, boolean isFavorite) {
        // TODO: Gọi UserRepository để cập nhật trạng thái yêu thích trên Firestore
        // Ví dụ: userRepository.toggleFavorite(userId, listingId, isFavorite);
    }
}