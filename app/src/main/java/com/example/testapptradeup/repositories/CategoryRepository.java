package com.example.testapptradeup.repositories;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.testapptradeup.models.Category;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class CategoryRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    /**
     * Lấy danh sách tất cả các danh mục từ Firestore.
     * @return LiveData chứa danh sách Category.
     */
    public LiveData<List<Category>> getCategories() {
        MutableLiveData<List<Category>> categoriesData = new MutableLiveData<>();

        // Giả sử bạn có một collection tên là "categories" trên Firestore
        // và muốn sắp xếp chúng theo một trường 'order' hoặc 'name'
        db.collection("categories")
                .orderBy("name", Query.Direction.ASCENDING) // Sắp xếp theo tên A-Z
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots != null) {
                        List<Category> categories = new ArrayList<>();
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            Category category = document.toObject(Category.class);
                            category.setId(document.getId()); // Gán ID của document vào đối tượng
                            categories.add(category);
                        }
                        categoriesData.setValue(categories);
                    }
                })
                .addOnFailureListener(e -> {
                    // Xử lý lỗi, có thể post một danh sách rỗng hoặc null
                    categoriesData.setValue(null);
                });

        return categoriesData;
    }
}