package com.example.testapptradeup.models;

import androidx.annotation.DrawableRes;

import com.example.testapptradeup.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Category {
    private String id;
    private String name;
    @DrawableRes
    private int iconResId;
    private String color;
    private boolean isSelected;

    public Category() {
        // Constructor rỗng cho Firebase
    }

    public Category(String id, String name, @DrawableRes int iconResId) {
        this.id = id;
        this.name = name;
        this.iconResId = iconResId;
        this.color = "#FFFFFF";
        this.isSelected = false;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    @DrawableRes
    public int getIconResId() { return iconResId; }
    public void setIconResId(@DrawableRes int iconResId) { this.iconResId = iconResId; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public boolean isSelected() { return isSelected; }
    public void setSelected(boolean selected) { isSelected = selected; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Category category = (Category) o;
        return Objects.equals(id, category.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public final static class AppConstants {
        private AppConstants() {}
        public static final String CATEGORY_ELECTRONICS = "electronics";
        public static final String CATEGORY_LAPTOPS     = "laptops";
        public static final String CATEGORY_FASHION     = "fashion";
        public static final String CATEGORY_HOME_GOODS  = "home_goods";
        public static final String CATEGORY_CARS        = "cars";
        public static final String CATEGORY_SPORTS      = "sports";
        public static final String CATEGORY_BOOKS       = "books";
        public static final String CATEGORY_OTHER       = "other";
    }

    /**
     * Cung cấp danh sách tất cả các danh mục chuẩn của ứng dụng bằng tiếng Việt.
     * @return List<Category>
     */
    public static List<Category> getAppCategories() {
        List<Category> categories = new ArrayList<>();
        categories.add(new Category(AppConstants.CATEGORY_ELECTRONICS, "Đồ điện tử", R.drawable.ic_phone));
        categories.add(new Category(AppConstants.CATEGORY_LAPTOPS, "Laptop", R.drawable.ic_laptop));
        categories.add(new Category(AppConstants.CATEGORY_FASHION, "Thời trang", R.drawable.ic_fashion));
        categories.add(new Category(AppConstants.CATEGORY_HOME_GOODS, "Đồ gia dụng", R.drawable.ic_home_appliances));
        categories.add(new Category(AppConstants.CATEGORY_CARS, "Xe cộ", R.drawable.ic_car));
        categories.add(new Category(AppConstants.CATEGORY_SPORTS, "Thể thao", R.drawable.ic_sports));
        categories.add(new Category(AppConstants.CATEGORY_BOOKS, "Sách", R.drawable.ic_book));
        categories.add(new Category(AppConstants.CATEGORY_OTHER, "Khác", R.drawable.ic_other));
        return categories;
    }

    /**
     * === HÀM QUAN TRỌNG NHẤT: BỘ PHIÊN DỊCH VÀ ÁNH XẠ ===
     * Chuyển đổi ID danh mục từ Firestore (tiếng Anh) sang tên hiển thị (tiếng Việt).
     * Hàm này xử lý cả việc gộp danh mục.
     * @param firestoreCategoryId ID lưu trên Firestore (e.g., "electronics", "furniture").
     * @return Tên tiếng Việt tương ứng (e.g., "Đồ điện tử", "Đồ gia dụng").
     */
    public static String getCategoryNameById(String firestoreCategoryId) {
        if (firestoreCategoryId == null) return "Không rõ";

        // Sử dụng switch-case để dễ dàng ánh xạ và gộp nhóm
        switch (firestoreCategoryId.toLowerCase()) {
            case AppConstants.CATEGORY_ELECTRONICS:
            case "mobiles": // Ví dụ nếu bạn có cả "mobiles"
                return "Đồ điện tử";

            case AppConstants.CATEGORY_LAPTOPS:
                return "Laptop";

            case AppConstants.CATEGORY_FASHION:
            case "clothing": // Ví dụ
            case "shoes":    // Ví dụ
                return "Thời trang";

            case AppConstants.CATEGORY_HOME_GOODS:
            case "furniture": // << ĐÂY LÀ TRƯỜNG HỢP CỦA BẠN
            case "appliances":// Ví dụ
                return "Đồ gia dụng";

            case AppConstants.CATEGORY_CARS:
            case "vehicles": // Ví dụ
                return "Xe cộ";

            case AppConstants.CATEGORY_SPORTS:
                return "Thể thao";

            case AppConstants.CATEGORY_BOOKS:
                return "Sách";

            default:
                return "Khác";
        }
    }

    /**
     * === HÀM HỖ TRỢ NGƯỢC LẠI ===
     * Chuyển đổi từ tên tiếng Việt trên UI về ID chuẩn để lưu vào Firestore.
     * @param vietnameseName Tên tiếng Việt người dùng chọn.
     * @return ID chuẩn để lưu (e.g., "electronics").
     */
    public static String getCategoryIdByName(String vietnameseName) {
        if (vietnameseName == null) return AppConstants.CATEGORY_OTHER;

        for (Category category : getAppCategories()) {
            if (vietnameseName.equals(category.getName())) {
                return category.getId(); // Trả về ID chuẩn của app
            }
        }
        return AppConstants.CATEGORY_OTHER; // Mặc định nếu không tìm thấy
    }
}