package com.example.testapptradeup.models;

import androidx.annotation.DrawableRes;
import java.util.Objects;

public class Category {
    private String id;
    private String name;
    @DrawableRes
    private int iconResId; // Chỉ giữ lại một biến cho icon
    private String color;
    private boolean isSelected;

    public Category() {
        // Constructor rỗng cho Firebase
    }

    public Category(String id, String name, @DrawableRes int iconResId) {
        this.id = id;
        this.name = name;
        this.iconResId = iconResId;
        this.color = "#FFFFFF"; // Màu mặc định
        this.isSelected = false;
    }

    // Getters and Setters
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


    /**
     * Lớp nội bộ chứa các hằng số ID cho danh mục.
     * Đây là "nguồn chân lý duy nhất" (single source of truth) cho ID,
     * giúp tránh lỗi gõ nhầm và đảm bảo tính nhất quán.
     */
    public final static class AppConstants {

        // private constructor để không ai có thể tạo đối tượng (new AppConstants())
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
}