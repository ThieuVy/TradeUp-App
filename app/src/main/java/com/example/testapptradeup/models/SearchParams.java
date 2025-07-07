package com.example.testapptradeup.models;

import android.location.Location;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.Objects;

public class SearchParams {
    private String query;
    private String category;
    private Double minPrice;
    private Double maxPrice;
    private String condition;
    private String sortBy;
    private boolean sortAscending; // true = ASC, false = DESC
    private String location; // Địa chỉ do người dùng nhập
    private Location userLocation; // Vị trí GPS của người dùng
    private int maxDistance; // Khoảng cách tối đa (km)

    // Constructors, Getters, Setters...

    public SearchParams() {
        // Mặc định
        this.sortAscending = false;
        this.maxDistance = -1; // -1 có nghĩa là không lọc theo khoảng cách
    }

    // Getters and Setters for all fields
    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public Double getMinPrice() { return minPrice; }
    public void setMinPrice(Double minPrice) { this.minPrice = minPrice; }
    public Double getMaxPrice() { return maxPrice; }
    public void setMaxPrice(Double maxPrice) { this.maxPrice = maxPrice; }
    public String getCondition() { return condition; }
    public void setCondition(String condition) { this.condition = condition; }
    @Nullable
    public String getSortBy() { return sortBy; }
    public void setSortBy(@Nullable String sortBy) { this.sortBy = sortBy; }
    public boolean isSortAscending() { return sortAscending; }
    public void setSortAscending(boolean sortAscending) { this.sortAscending = sortAscending; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    @Nullable
    public Location getUserLocation() { return userLocation; }
    public void setUserLocation(@Nullable Location userLocation) { this.userLocation = userLocation; }
    public int getMaxDistance() { return maxDistance; }
    public void setMaxDistance(int maxDistance) { this.maxDistance = maxDistance; }

    // Helper methods
    public boolean hasPriceFilter() {
        return minPrice != null || maxPrice != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SearchParams that = (SearchParams) o;
        return sortAscending == that.sortAscending && maxDistance == that.maxDistance && Objects.equals(query, that.query) && Objects.equals(category, that.category) && Objects.equals(minPrice, that.minPrice) && Objects.equals(maxPrice, that.maxPrice) && Objects.equals(condition, that.condition) && Objects.equals(sortBy, that.sortBy) && Objects.equals(location, that.location) && Objects.equals(userLocation, that.userLocation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(query, category, minPrice, maxPrice, condition, sortBy, sortAscending, location, userLocation, maxDistance);
    }

    @NonNull
    @Override
    public String toString() {
        return "SearchParams{" +
                "query='" + query + '\'' +
                ", category='" + category + '\'' +
                ", minPrice=" + minPrice +
                ", maxPrice=" + maxPrice +
                ", condition='" + condition + '\'' +
                ", sortBy='" + sortBy + '\'' +
                ", sortAscending=" + sortAscending +
                ", location='" + location + '\'' +
                ", userLocation=" + userLocation +
                ", maxDistance=" + maxDistance +
                '}';
    }
}