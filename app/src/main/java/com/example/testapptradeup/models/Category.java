package com.example.testapptradeup.models;

import java.util.Objects;

public class Category {
    private String id;
    private String name;
    private String icon;
    private String color;
    private boolean isSelected;

    public Category() {
        // Constructor rỗng cho Firebase
    }

    public Category(String id, String name, String icon, String color) {
        this.id = id;
        this.name = name;
        this.icon = icon;
        this.color = color;
        this.isSelected = false;
    }

    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getIcon() { return icon; }
    public String getColor() { return color; }
    public boolean isSelected() { return isSelected; }

    // Setters
    public void setId(String id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setIcon(String icon) { this.icon = icon; }
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

}