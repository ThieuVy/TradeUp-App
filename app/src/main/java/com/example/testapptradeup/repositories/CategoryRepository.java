package com.example.testapptradeup.repositories;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.testapptradeup.R; // Cần cho dữ liệu giả lập
import com.example.testapptradeup.models.Category;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class CategoryRepository {

    private static final String TAG = "CategoryRepository";
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    /**
     * Lấy một số lượng giới hạn các danh mục hàng đầu từ Firestore.
     * Thường dùng cho màn hình chính.
     * @param limit Số lượng danh mục cần lấy.
     * @return LiveData chứa danh sách Category.
     */
    public LiveData<List<Category>> getTopCategories(int limit) {
        MutableLiveData<List<Category>> categoriesData = new MutableLiveData<>();

        // Giả sử bạn có một trường 'order' để sắp xếp các danh mục phổ biến lên đầu
        db.collection("categories")
                .orderBy("order", Query.Direction.ASCENDING)
                .limit(limit)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots != null) {
                        List<Category> categories = new ArrayList<>();
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            Category category = document.toObject(Category.class);
                            category.setId(document.getId());
                            categories.add(category);
                        }
                        categoriesData.setValue(categories);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi khi lấy top categories. Sử dụng dữ liệu giả lập.", e);
                    // Fallback: Nếu lỗi, trả về dữ liệu giả lập để UI không bị trống
                    categoriesData.setValue(getHardcodedCategories());
                });

        return categoriesData;
    }

    /**
     * Lấy danh sách TẤT CẢ các danh mục từ Firestore.
     * Thường dùng cho màn hình lọc hoặc trang danh sách danh mục.
     * @return LiveData chứa danh sách Category.
     */
    public LiveData<List<Category>> getAllCategories() {
        MutableLiveData<List<Category>> categoriesData = new MutableLiveData<>();

        db.collection("categories")
                .orderBy("name", Query.Direction.ASCENDING) // Sắp xếp theo tên A-Z
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots != null) {
                        List<Category> categories = new ArrayList<>();
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            Category category = document.toObject(Category.class);
                            category.setId(document.getId());
                            categories.add(category);
                        }
                        categoriesData.setValue(categories);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi khi lấy tất cả categories.", e);
                    categoriesData.setValue(null); // Báo lỗi bằng cách trả về null
                });

        return categoriesData;
    }

    /**
     * Phương thức này cung cấp dữ liệu giả lập (hardcoded)
     * Rất hữu ích khi Firestore chưa có dữ liệu hoặc khi API bị lỗi.
     * Giúp quá trình phát triển không bị gián đoạn.
     * @return Một danh sách các danh mục mặc định.
     */
    private List<Category> getHardcodedCategories() {
        List<Category> categories = new ArrayList<>();
        // Giả sử bạn có các icon này trong drawable
        categories.add(new Category("cat_phone", "Điện thoại", R.drawable.ic_phone));
        categories.add(new Category("cat_laptop", "Laptop", R.drawable.ic_laptop));
        categories.add(new Category("cat_fashion", "Thời trang", R.drawable.ic_fashion));
        categories.add(new Category("cat_home", "Đồ gia dụng", R.drawable.ic_home_appliances));
        categories.add(new Category("cat_other", "Khác", R.drawable.ic_other));
        return categories;
    }
}