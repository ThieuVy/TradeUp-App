package com.example.testapptradeup.models;

import androidx.annotation.DrawableRes; // Import annotation này
import java.util.Objects;

public class Category {
    private String id;
    private String name;

    // Thay đổi kiểu dữ liệu của icon từ String thành int
    @DrawableRes
    private int iconResId;

    private String color;
    private boolean isSelected;
    private int icon;

    public Category() {
        // Constructor rỗng cho Firebase
    }

    // Sửa lại constructor này
    public Category(String id, String name, @DrawableRes int iconResId) {
        this.id = id;
        this.name = name;
        this.iconResId = iconResId;
        this.color = "#FFFFFF"; // Có thể gán màu mặc định
        this.isSelected = false;
    }

    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    @DrawableRes
    public int getIconResId() { return iconResId; } // Sửa getter
    public String getColor() { return color; }
    public boolean isSelected() { return isSelected; }

    // Setters
    public void setId(String id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setIconResId(@DrawableRes int iconResId) { this.iconResId = iconResId; } // Sửa setter
    public void setColor(String color) { this.color = color; }
    public void setSelected(boolean selected) { isSelected = selected; }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Category category = (Category) obj;
        return Objects.equals(id, category.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    public int getIcon() {
        return icon;
    }

    public void setIcon(int icon) {
        this.icon = icon;
    }
}