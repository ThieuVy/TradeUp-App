package com.example.testapptradeup.repositories;

import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.testapptradeup.R;
import com.example.testapptradeup.models.Category;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CategoryRepository {

    private static final String TAG = "CategoryRepository";
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    // Dùng MutableLiveData để có thể cập nhật danh sách từ bên trong Repository
    private final MutableLiveData<List<Category>> topCategoriesData = new MutableLiveData<>(Collections.emptyList());
    private final MutableLiveData<List<Category>> allCategoriesData = new MutableLiveData<>(Collections.emptyList());

    // LiveData để quản lý trạng thái
    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();


    public CategoryRepository() {
        // Tải dữ liệu lần đầu khi Repository được khởi tạo
        fetchAll();
    }

    /**
     * Tải lại toàn bộ dữ liệu danh mục từ Firestore.
     * Phương thức này sẽ được gọi bởi ViewModel.
     */
    public void fetchAll() {
        if (Boolean.TRUE.equals(_isLoading.getValue())) {
            return; // Đang tải, không làm gì cả
        }
        _isLoading.setValue(true);
        _errorMessage.setValue(null);

        // Tải danh sách top categories
        db.collection("categories")
                .orderBy("order", Query.Direction.ASCENDING)
                .limit(8) // Lấy 8 danh mục hàng đầu cho màn hình chính
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots != null) {
                        List<Category> categories = new ArrayList<>();
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            Category category = document.toObject(Category.class);
                            category.setId(document.getId());
                            categories.add(category);
                        }
                        topCategoriesData.setValue(categories);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi khi lấy top categories. Sử dụng dữ liệu giả lập.", e);
                    _errorMessage.setValue("Không thể tải danh mục.");
                    topCategoriesData.setValue(getHardcodedCategories()); // Dùng dữ liệu giả để UI không bị trống
                })
                .addOnCompleteListener(task -> _isLoading.setValue(false)); // Tắt loading khi hoàn tất
    }

    // --- GETTERS CHO VIEWMODEL ---

    /**
     * Trả về LiveData chứa danh sách các danh mục hàng đầu.
     * @param limit Số lượng danh mục mong muốn. (Lưu ý: Logic tải thực tế nằm trong fetchAll).
     * @return LiveData<List<Category>>
     */
    public LiveData<List<Category>> getTopCategories(int limit) {
        // Hàm này chỉ trả về LiveData đã được quản lý.
        // Logic tải đã được chuyển vào fetchAll().
        return topCategoriesData;
    }

    /**
     * Trả về LiveData chứa TẤT CẢ các danh mục.
     * (Logic tải chi tiết sẽ được thêm vào nếu cần)
     * @return LiveData<List<Category>>
     */
    public LiveData<List<Category>> getAllCategories() {
        // Tương tự, nếu cần, bạn có thể tạo một phương thức riêng để tải tất cả danh mục.
        // Tạm thời, nó có thể trả về cùng một LiveData hoặc một LiveData khác.
        if (allCategoriesData.getValue() == null || allCategoriesData.getValue().isEmpty()) {
            db.collection("categories")
                    .orderBy("name", Query.Direction.ASCENDING)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        if (queryDocumentSnapshots != null) {
                            List<Category> categories = new ArrayList<>();
                            for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                                Category category = document.toObject(Category.class);
                                category.setId(document.getId());
                                categories.add(category);
                            }
                            allCategoriesData.setValue(categories);
                        }
                    });
        }
        return allCategoriesData;
    }

    // --- GETTERS CHO STATE ---

    public LiveData<Boolean> isLoading() {
        return _isLoading;
    }

    public LiveData<String> getErrorMessage() {
        return _errorMessage;
    }


    /**
     * Phương thức cung cấp dữ liệu giả lập (hardcoded).
     * @return Một danh sách các danh mục mặc định.
     */
    private List<Category> getHardcodedCategories() {
        List<Category> categories = new ArrayList<>();
        categories.add(new Category("cat_phone", "Điện thoại", R.drawable.ic_phone));
        categories.add(new Category("cat_laptop", "Laptop", R.drawable.ic_laptop));
        categories.add(new Category("cat_fashion", "Thời trang", R.drawable.ic_fashion));
        categories.add(new Category("cat_home", "Đồ gia dụng", R.drawable.ic_home_appliances));
        categories.add(new Category("cat_car", "Xe cộ", R.drawable.ic_car));
        categories.add(new Category("cat_sport", "Thể thao", R.drawable.ic_sports));
        categories.add(new Category("cat_book", "Sách", R.drawable.ic_book));
        categories.add(new Category("cat_other", "Khác", R.drawable.ic_other));
        return categories;
    }
}