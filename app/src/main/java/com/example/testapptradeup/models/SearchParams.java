package com.example.testapptradeup.models;

import android.location.Location;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import java.util.Objects;

public class SearchParams {
    private String query;
    private String category;
    private Double minPrice;
    private Double maxPrice;
    private String condition;
    private int maxDistance = -1; // -1 means no distance filter
    private String location;
    private Location userLocation;
    private int sortBy = 0; // 0 = Relevance, 1 = Price Low to High, etc.
    private int page = 1;
    private int pageSize = 20;

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
    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Double getMinPrice() {
        return minPrice;
    }

    public void setMinPrice(Double minPrice) {
        this.minPrice = minPrice;
    }

    public Double getMaxPrice() {
        return maxPrice;
    }

    public void setMaxPrice(Double maxPrice) {
        this.maxPrice = maxPrice;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public int getMaxDistance() {
        return maxDistance;
    }

    public void setMaxDistance(int maxDistance) {
        this.maxDistance = maxDistance;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Location getUserLocation() {
        return userLocation;
    }

    public void setUserLocation(Location userLocation) {
        this.userLocation = userLocation;
    }

    public int getSortBy() {
        return sortBy;
    }

    public void setSortBy(int sortBy) {
        this.sortBy = sortBy;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

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
        sortBy = 0;
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

    public String getSortByString() {
        switch (sortBy) {
            case 0: return "relevance";
            case 1: return "price_asc";
            case 2: return "price_desc";
            case 3: return "newest";
            case 4: return "distance";
            default: return "relevance";
        }
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

        if (maxDistance != that.maxDistance) return false;
        if (sortBy != that.sortBy) return false;
        if (page != that.page) return false;
        if (pageSize != that.pageSize) return false;
        if (!Objects.equals(query, that.query)) return false;
        if (!Objects.equals(category, that.category))
            return false;
        if (!Objects.equals(minPrice, that.minPrice))
            return false;
        if (!Objects.equals(maxPrice, that.maxPrice))
            return false;
        if (!Objects.equals(condition, that.condition))
            return false;
        if (!Objects.equals(location, that.location))
            return false;
        return Objects.equals(userLocation, that.userLocation);
    }

    @Override
    public int hashCode() {
        int result = query != null ? query.hashCode() : 0;
        result = 31 * result + (category != null ? category.hashCode() : 0);
        result = 31 * result + (minPrice != null ? minPrice.hashCode() : 0);
        result = 31 * result + (maxPrice != null ? maxPrice.hashCode() : 0);
        result = 31 * result + (condition != null ? condition.hashCode() : 0);
        result = 31 * result + maxDistance;
        result = 31 * result + (location != null ? location.hashCode() : 0);
        result = 31 * result + (userLocation != null ? userLocation.hashCode() : 0);
        result = 31 * result + sortBy;
        result = 31 * result + page;
        result = 31 * result + pageSize;
        return result;
    }
}