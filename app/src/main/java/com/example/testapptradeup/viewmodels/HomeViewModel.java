package com.example.testapptradeup.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.testapptradeup.models.Listing;
import com.example.testapptradeup.models.Category;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Comparator; // Import Comparator for sorting

public class HomeViewModel extends ViewModel {

    // LiveData for various lists
    private final MutableLiveData<List<Category>> categories = new MutableLiveData<>();
    private final MutableLiveData<List<Listing>> recommendations = new MutableLiveData<>();
    private final MutableLiveData<List<Listing>> trending = new MutableLiveData<>();
    private final MutableLiveData<List<Listing>> listings = new MutableLiveData<>();
    private final MutableLiveData<List<Listing>> featuredItems = new MutableLiveData<>();

    // LiveData for UI state
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private final MutableLiveData<String> currentLocation = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    // Original list for filtering
    private List<Listing> originalListings = new ArrayList<>();

    // Firebase Firestore instance
    private final FirebaseFirestore db;

    public HomeViewModel() {
        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Initialize default values
        isLoading.setValue(false);
        currentLocation.setValue("TP. Hồ Chí Minh, Việt Nam");

        // Load data from Firestore
        loadAllHomeData();
    }

    // Getters for LiveData
    public LiveData<List<Category>> getCategories() { return categories; }
    public LiveData<List<Listing>> getRecommendations() { return recommendations; }
    public LiveData<List<Listing>> getTrending() { return trending; }
    public LiveData<List<Listing>> getListings() { return listings; }
    public LiveData<List<Listing>> getFeaturedItems() { return featuredItems; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getErrorMessage() { return errorMessage; }

    /**
     * Loads all necessary data for the home screen from Firestore.
     */
    public void loadAllHomeData() {
        isLoading.setValue(true); // Set loading state to true
        loadCategoriesFromFirestore();
        loadFeaturedItemsFromFirestore();
        loadRecommendationsFromFirestore();
        loadTrendingFromFirestore();
        loadListingsFromFirestore(); // This will also set isLoading to false when done
    }

    /**
     * Loads categories from Firestore.
     * Assumes a 'categories' collection where each document represents a category.
     */
    private void loadCategoriesFromFirestore() {
        db.collection("categories")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Category> fetchedCategories = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        // Assuming Category model has a constructor or setter for ID
                        Category category = document.toObject(Category.class);
                        category.setId(document.getId()); // Set the document ID as category ID
                        fetchedCategories.add(category);
                    }
                    categories.setValue(fetchedCategories);
                })
                .addOnFailureListener(e -> errorMessage.setValue("Lỗi tải danh mục: " + e.getMessage()));
    }

    /**
     * Loads featured products from Firestore.
     * Assumes a 'products' collection with a 'isFeatured' field.
     */
    private void loadFeaturedItemsFromFirestore() {
        db.collection("products")
                .whereEqualTo("isFeatured", true)
                .limit(5) // Limit to a reasonable number for featured items
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Listing> fetchedFeaturedItems = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Listing listing = document.toObject(Listing.class);
                        listing.setId(document.getId());
                        fetchedFeaturedItems.add(listing);
                    }
                    featuredItems.setValue(fetchedFeaturedItems);
                })
                .addOnFailureListener(e -> errorMessage.setValue("Lỗi tải sản phẩm nổi bật: " + e.getMessage()));
    }

    /**
     * Loads recommended products from Firestore.
     * This would ideally involve personalization, but for now, it's a general query.
     * Assumes a 'products' collection, perhaps ordered by 'recommendationScore' or similar.
     */
    private void loadRecommendationsFromFirestore() {
        db.collection("products")
                // .orderBy("recommendationScore", Query.Direction.DESCENDING) // Requires index
                .limit(10)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Listing> fetchedRecommendations = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Listing listing = document.toObject(Listing.class);
                        listing.setId(document.getId());
                        fetchedRecommendations.add(listing);
                    }
                    recommendations.setValue(fetchedRecommendations);
                })
                .addOnFailureListener(e -> errorMessage.setValue("Lỗi tải sản phẩm đề xuất: " + e.getMessage()));
    }

    /**
     * Loads trending products from Firestore.
     * Assumes a 'products' collection, perhaps ordered by 'views' or 'salesCount'.
     */
    private void loadTrendingFromFirestore() {
        db.collection("products")
                // .orderBy("views", Query.Direction.DESCENDING) // Requires index
                .limit(10)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Listing> fetchedTrending = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Listing listing = document.toObject(Listing.class);
                        listing.setId(document.getId());
                        fetchedTrending.add(listing);
                    }
                    trending.setValue(fetchedTrending);
                })
                .addOnFailureListener(e -> errorMessage.setValue("Lỗi tải sản phẩm thịnh hành: " + e.getMessage()));
    }

    /**
     * Loads main product listings from Firestore.
     * This is the primary list displayed on the home screen.
     */
    private void loadListingsFromFirestore() {
        db.collection("products")
                // .orderBy("timePosted", Query.Direction.DESCENDING) // Requires index
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Listing> fetchedListings = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Listing listing = document.toObject(Listing.class);
                        listing.setId(document.getId());
                        fetchedListings.add(listing);
                    }
                    originalListings = new ArrayList<>(fetchedListings); // Save original for filtering
                    listings.setValue(fetchedListings);
                    isLoading.setValue(false); // Set loading state to false after all listings are loaded
                })
                .addOnFailureListener(e -> {
                    errorMessage.setValue("Lỗi tải danh sách sản phẩm: " + e.getMessage());
                    isLoading.setValue(false); // Set loading state to false even on failure
                });
    }


    /**
     * Filters product listings by category.
     * For actual Firestore filtering, you'd perform a new query.
     * For now, this filters the `originalListings` in memory.
     * @param categoryId ID of the category to filter by.
     */
    public void filterByCategory(String categoryId) {
        if (originalListings.isEmpty()) {
            errorMessage.setValue("Không có dữ liệu gốc để lọc.");
            return;
        }

        if (categoryId == null || categoryId.isEmpty() || categoryId.equals("all")) {
            listings.setValue(new ArrayList<>(originalListings)); // Show all if categoryId is null/empty/all
        } else {
            // In a real app, you would query Firestore:
            // db.collection("products").whereEqualTo("categoryId", categoryId).get()...
            // For now, filter in memory
            List<Listing> filteredList = new ArrayList<>();
            for (Listing listing : originalListings) {
                // This is a dummy filter. Replace with actual category matching logic.
                // Assuming Product has a getCategoryId() method
                if (listing.getCategoryId() != null && listing.getCategoryId().equals(categoryId)) {
                    filteredList.add(listing);
                }
            }
            listings.setValue(filteredList);
        }
    }


    /**
     * Searches for products by keyword.
     * @param query The search keyword.
     */
    public void searchProducts(String query) {
        if (originalListings.isEmpty()) {
            errorMessage.setValue("Không có dữ liệu gốc để tìm kiếm.");
            return;
        }

        List<Listing> searchResults = new ArrayList<>();
        if (query == null || query.trim().isEmpty()) {
            searchResults.addAll(originalListings); // If query is empty, show all
        } else {
            String lowerCaseQuery = query.toLowerCase();
            for (Listing listing : originalListings) {
                if (listing.getTitle().toLowerCase().contains(lowerCaseQuery) ||
                        listing.getDescription().toLowerCase().contains(lowerCaseQuery) ||
                        listing.getLocation().toLowerCase().contains(lowerCaseQuery)) { // You might want to remove location from search criteria unless specifically needed.
                    searchResults.add(listing);
                }
            }
        }
        listings.setValue(searchResults);
    }

    /**
     * Toggles a product's favorite status (dummy implementation).
     * In a real app, this would update a user's 'favorites' collection in Firestore.
     * @param productId ID of the product.
     */
    public void toggleFavorite(String productId) {
        // This is a client-side only change. For persistence, update Firestore.
        // Example: db.collection("users").document(userId).collection("favorites").document(productId).set(...);
        List<Listing> currentList = listings.getValue();
        if (currentList != null) {
            for (int i = 0; i < currentList.size(); i++) {
                Listing listing = currentList.get(i);
                if (listing.getId().equals(productId)) {
                    // This assumes Product has a mutable favorite status.
                    // product.setFavorite(!product.isFavorite());
                    // For now, just a Toast
                    errorMessage.setValue("Tính năng yêu thích cho sản phẩm " + listing.getTitle() + " (ID: " + productId + ") chưa được lưu vào database thực tế.");
                    // listings.setValue(new ArrayList<>(currentList)); // Trigger UI update if product model is mutable
                    break;
                }
            }
        }
    }

    /**
     * Sorts the list of products.
     * @param sortType Type of sort (e.g., "price_asc", "price_desc", "rating", "time")
     */
    public void sortProducts(String sortType) {
        List<Listing> currentList = listings.getValue();
        if (currentList == null) return;

        List<Listing> sortedList = new ArrayList<>(currentList);

        switch (sortType) {
            case "price_asc":
                sortedList.sort(Comparator.comparingDouble(Listing::getPrice));
                break;
            case "price_desc":
                sortedList.sort(Comparator.comparingDouble(Listing::getPrice).reversed());
                break;
            case "rating":
                sortedList.sort(Comparator.comparingDouble(Listing::getRating).reversed());
                break;
            case "time":
                sortedList.sort(Comparator.comparing(Listing::getTimePosted).reversed());
                break;
            default:
                // No sorting if default or unknown type
                break;
        }

        listings.setValue(sortedList);
    }

    /**
     * Updates the current location.
     * This could trigger a reload of relevant data based on the new location.
     */
    public void updateLocation(String location) {
        currentLocation.setValue(location);
        // Reload data based on new location if applicable (e.g., nearby listings)
        loadListingsFromFirestore(); // Reload main listings, which might filter by location in a real app
    }

    /**
     * Clears any current error message.
     */
    public void clearErrorMessage() {
        errorMessage.setValue(null);
    }

    public void loadHomeData() {
        loadAllHomeData();
    }
}
