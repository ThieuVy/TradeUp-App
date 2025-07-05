package com.example.testapptradeup.models;

import android.location.Location;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;

public class SearchParams {
    private String query;
    private String category;
    private Double minPrice;
    private Double maxPrice;
    private String condition;
    private int maxDistance = -1;
    private String location;
    private Location userLocation;
    private String sortBy = String.valueOf(0); // 0 = Relevance, 1 = Price Low to High, etc.
    private int page = 1;
    private int pageSize = 20;
    private boolean sortAscending = false; // Mặc định là giảm dần (DESC)

    public SearchParams() {
        // Default constructor
    }

    // Copy constructor
    public SearchParams(SearchParams other) {
        this.query = other.query;
        this.category = other.category;
        this.minPrice = other.minPrice;
        this.maxPrice = other.maxPrice;
        this.condition = other.condition;
        this.maxDistance = other.maxDistance;
        this.location = other.location;
        this.userLocation = other.userLocation;
        this.sortBy = other.sortBy;
        this.page = other.page;
        this.pageSize = other.pageSize;
    }

    // Getters and Setters
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
    public int getMaxDistance() { return maxDistance; }
    public void setMaxDistance(int maxDistance) { this.maxDistance = maxDistance; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public Location getUserLocation() { return userLocation; }
    public void setUserLocation(Location userLocation) { this.userLocation = userLocation; }
    @Nullable
    public String getSortBy() { return String.valueOf(sortBy); }
    public void setSortBy(@Nullable String sortBy) { this.sortBy = sortBy; }
    public boolean isSortAscending() { return sortAscending; }
    public void setSortAscending(boolean sortAscending) { this.sortAscending = sortAscending; }

    // Utility methods
    public boolean isEmpty() {
        return TextUtils.isEmpty(query) &&
                TextUtils.isEmpty(category) &&
                minPrice == null &&
                maxPrice == null &&
                TextUtils.isEmpty(condition) &&
                maxDistance == -1 &&
                TextUtils.isEmpty(location);
    }

    public void clearFilters() {
        category = null;
        minPrice = null;
        maxPrice = null;
        condition = null;
        maxDistance = -1;
        location = null;
        userLocation = null;
        sortBy = String.valueOf(0);
    }

    public void reset() {
        query = null;
        clearFilters();
        page = 1;
    }

    public boolean hasPriceFilter() {
        return minPrice != null || maxPrice != null;
    }

    public boolean hasLocationFilter() {
        return !TextUtils.isEmpty(location) || userLocation != null;
    }

    public boolean hasDistanceFilter() {
        return maxDistance > 0;
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
                ", maxDistance=" + maxDistance +
                ", location='" + location + '\'' +
                ", userLocation=" + userLocation +
                ", sortBy=" + sortBy +
                ", page=" + page +
                ", pageSize=" + pageSize +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SearchParams that = (SearchParams) o;
        return maxDistance == that.maxDistance &&
                sortAscending == that.sortAscending &&
                Objects.equals(query, that.query) &&
                Objects.equals(category, that.category) &&
                Objects.equals(minPrice, that.minPrice) &&
                Objects.equals(maxPrice, that.maxPrice) &&
                Objects.equals(condition, that.condition) &&
                Objects.equals(location, that.location) &&
                Objects.equals(userLocation, that.userLocation) &&
                Objects.equals(sortBy, that.sortBy);
    }
    @Override
    public int hashCode() {
        return Objects.hash(query, category, minPrice, maxPrice, condition, maxDistance, location, userLocation, sortBy, sortAscending);
    }
}