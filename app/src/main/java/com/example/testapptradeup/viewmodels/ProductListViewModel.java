package com.example.testapptradeup.viewmodels;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.testapptradeup.models.Listing;
import com.example.testapptradeup.repositories.ListingRepository;
import com.example.testapptradeup.repositories.UserRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.List;

public class ProductListViewModel extends ViewModel {
    private final ListingRepository listingRepository;
    private final UserRepository userRepository;
    public LiveData<List<Listing>> productList;
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private final MutableLiveData<String> toolbarTitle = new MutableLiveData<>();

    public ProductListViewModel() {
        this.listingRepository = new ListingRepository();
        this.userRepository = new UserRepository();
    }

    /**
     * Tải danh sách sản phẩm dựa trên loại bộ lọc và một ID (có thể là categoryId hoặc userId).
     * @param filterType Loại bộ lọc ("category", "user", "recommended", v.v.).
     * @param id ID tương ứng với bộ lọc (categoryId hoặc userId).
     */
    public void loadProducts(String filterType, @Nullable String id) {
        isLoading.setValue(true);
        // Truyền cả filterType và id vào repository
        productList = listingRepository.getListingsByFilter(filterType, id);
        productList.observeForever(listings -> isLoading.setValue(false));
    }

    public LiveData<String> getToolbarTitle() {
        return toolbarTitle;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    /**
     * Cập nhật tiêu đề cho Toolbar dựa trên loại bộ lọc và ID.
     * @param filterType Loại bộ lọc.
     * @param id ID tương ứng (categoryId hoặc userId).
     */
    public void updateToolbarTitle(String filterType, @Nullable String id) {
        if ("category".equals(filterType) && id != null) {
            // Lấy tên category từ ID để hiển thị
            FirebaseFirestore.getInstance().collection("categories").document(id)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
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
        } else if ("user".equals(filterType)) {
            // Sử dụng tham số 'id' đã được truyền vào, vì nó chính là userId trong trường hợp này.
            if (id != null) {
                FirebaseFirestore.getInstance().collection("users").document(id).get()
                        .addOnSuccessListener(doc -> {
                            if (doc.exists()) {
                                toolbarTitle.setValue("Tin đăng của " + doc.getString("name"));
                            } else {
                                toolbarTitle.setValue("Tin đăng của người dùng");
                            }
                        });
            }
        } else {
            toolbarTitle.setValue("Danh sách sản phẩm");
        }
    }

    public void toggleFavorite(String listingId, boolean isFavorite) {
        String userId = FirebaseAuth.getInstance().getCurrentUser() != null ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (userId != null) {
            userRepository.toggleFavorite(userId, listingId, isFavorite);
        }
    }
}