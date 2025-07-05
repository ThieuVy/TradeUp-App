package com.example.testapptradeup.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

import com.example.testapptradeup.models.Category;
import com.example.testapptradeup.models.Listing;
import com.example.testapptradeup.repositories.CategoryRepository;
import com.example.testapptradeup.repositories.ListingRepository;

import java.util.ArrayList;
import java.util.List;

public class HomeViewModel extends ViewModel {

    private final ListingRepository listingRepository;
    private final CategoryRepository categoryRepository;

    private final MutableLiveData<List<Listing>> featuredItems = new MutableLiveData<>();
    private final MutableLiveData<List<Category>> categories = new MutableLiveData<>();
    private final MutableLiveData<List<Listing>> recommendations = new MutableLiveData<>();
    private final MutableLiveData<List<Listing>> recentListings = new MutableLiveData<>();

    private final MediatorLiveData<Boolean> isLoading = new MediatorLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    private int runningTasks = 0;

    public HomeViewModel() {
        this.listingRepository = new ListingRepository();
        this.categoryRepository = new CategoryRepository();
        refreshData();
    }

    public void refreshData() {
        if (Boolean.TRUE.equals(isLoading.getValue())) {
            return;
        }

        isLoading.setValue(true);
        runningTasks = 4;

        Observer<Object> taskCompletionObserver = result -> {
            runningTasks--;
            if (runningTasks <= 0) {
                isLoading.postValue(false);
            }
        };

        // ========== PHẦN SỬA LỖI Ở ĐÂY ==========

        // Tải dữ liệu từ các repository
        LiveData<List<Listing>> featuredSource = listingRepository.getFeaturedListings();
        isLoading.addSource(featuredSource, listings -> {
            featuredItems.setValue(listings);
            isLoading.removeSource(featuredSource);
            taskCompletionObserver.onChanged(null);
        });

        // Gọi đúng phương thức getTopCategories với số lượng mong muốn
        LiveData<List<Category>> categorySource = categoryRepository.getTopCategories(5);
        isLoading.addSource(categorySource, cats -> {
            categories.setValue(cats);
            isLoading.removeSource(categorySource);
            taskCompletionObserver.onChanged(null);
        });

        LiveData<List<Listing>> recommendedSource = listingRepository.getRecommendedListings(4);
        isLoading.addSource(recommendedSource, listings -> {
            recommendations.setValue(listings);
            isLoading.removeSource(recommendedSource);
            taskCompletionObserver.onChanged(null);
        });

        LiveData<List<Listing>> recentSource = listingRepository.getRecentListings(5);
        isLoading.addSource(recentSource, listings -> {
            recentListings.setValue(listings);
            isLoading.removeSource(recentSource);
            taskCompletionObserver.onChanged(null);
        });

        categoryRepository.getTopCategories(5).observeForever(categories::setValue);
        listingRepository.getFeaturedListings().observeForever(featuredItems::setValue);
        listingRepository.getRecommendedListings(4).observeForever(recommendations::setValue);
        listingRepository.getRecentListings(10).observeForever(recentListings::setValue);

        // ======================================
    }

    // Hàm này sẽ thêm một bài đăng mới vào đầu danh sách "Gần đây"
    /**
     * Hàm này được gọi từ HomeFragment khi có một bài đăng mới.
     * Nó sẽ thêm bài đăng đó vào đầu danh sách "Tin rao gần đây" hiện tại.
     * @param newListing Đối tượng Listing mới được tạo từ PostFragment.
     */
    public void addNewListingToTop(Listing newListing) {
        // 1. Lấy danh sách hiện tại từ LiveData.
        List<Listing> currentList = recentListings.getValue();
        if (currentList == null) {
            currentList = new ArrayList<>();
        }

        // 2. Tạo một danh sách MỚI (đây là bước quan trọng để LiveData nhận biết sự thay đổi).
        List<Listing> updatedList = new ArrayList<>(currentList);

        // 3. Thêm bài đăng mới vào vị trí đầu tiên (index 0).
        updatedList.add(0, newListing);

        // 4. Cập nhật LiveData với danh sách mới.
        // Bất kỳ Fragment nào đang observe "recentListings" sẽ nhận được cập nhật này.
        recentListings.setValue(updatedList);
    }

    // Getters
    public LiveData<List<Listing>> getFeaturedItems() { return featuredItems; }
    public LiveData<List<Category>> getCategories() { return categories; }
    public LiveData<List<Listing>> getRecommendations() { return recommendations; }
    public LiveData<List<Listing>> getListings() { return recentListings; } // Đảm bảo getter này trả về recentListings
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getErrorMessage() { return errorMessage; }

    public void clearErrorMessage() {
        errorMessage.setValue(null);
    }
}