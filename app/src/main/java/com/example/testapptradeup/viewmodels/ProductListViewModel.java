package com.example.testapptradeup.viewmodels;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.testapptradeup.models.Listing;
import com.example.testapptradeup.repositories.ListingRepository;
// <<< THÊM MỚI: Import UserRepository >>>
import com.example.testapptradeup.repositories.UserRepository;
// <<< THÊM MỚI: Import các lớp cần thiết cho việc lấy tên Category >>>
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.List;

public class ProductListViewModel extends ViewModel {
    private final ListingRepository listingRepository;
    private final UserRepository userRepository; // <<< THÊM MỚI: Khai báo UserRepository >>>
    public LiveData<List<Listing>> productList;
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();

    // <<< BƯỚC 1: Thêm LiveData để giữ tiêu đề của Toolbar >>>
    private final MutableLiveData<String> toolbarTitle = new MutableLiveData<>();

    public ProductListViewModel() {
        this.listingRepository = new ListingRepository();
        this.userRepository = new UserRepository(); // <<< THÊM MỚI: Khởi tạo UserRepository >>>
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
        if ("category".equals(filterType) && categoryId != null) {
            // Lấy tên category từ ID để hiển thị
            FirebaseFirestore.getInstance().collection("categories").document(categoryId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            // Giả sử có trường "name" trong document category
                            String categoryName = documentSnapshot.getString("name");
                            toolbarTitle.setValue(categoryName);
                        } else {
                            toolbarTitle.setValue("Danh mục không tồn tại");
                        }
                    })
                    .addOnFailureListener(e -> toolbarTitle.setValue("Danh mục"));
        } else if ("recommended".equals(filterType)) {
            toolbarTitle.setValue("Đề xuất cho bạn");
        } else if ("recent".equals(filterType)) {
            toolbarTitle.setValue("Tin đăng gần đây");
        } else {
            toolbarTitle.setValue("Danh sách sản phẩm");
        }
    }

    // <<< BƯỚC 4 (Tùy chọn): Thêm logic xử lý yêu thích >>>
    public void toggleFavorite(String listingId, boolean isFavorite) {
        // <<< THAY ĐỔI: Gọi userRepository.toggleFavorite() >>>
        String userId = FirebaseAuth.getInstance().getCurrentUser() != null ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (userId != null) {
            userRepository.toggleFavorite(userId, listingId, isFavorite);
        }
    }
}