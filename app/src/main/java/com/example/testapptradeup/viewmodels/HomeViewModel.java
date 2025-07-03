package com.example.testapptradeup.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.testapptradeup.models.Category;
import com.example.testapptradeup.models.Listing;
import com.example.testapptradeup.repositories.CategoryRepository;
import com.example.testapptradeup.repositories.ListingRepository;

import java.util.List;

public class HomeViewModel extends ViewModel {

    private final ListingRepository listingRepository;
    private final CategoryRepository categoryRepository;

    public final LiveData<List<Listing>> featuredListings;
    public final LiveData<List<Listing>> recommendedListings;
    public final LiveData<List<Listing>> recentListings;
    public final LiveData<List<Category>> categories;

    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public HomeViewModel() {
        listingRepository = new ListingRepository();
        categoryRepository = new CategoryRepository(); // Khởi tạo

        // Gán LiveData từ repository
        featuredListings = listingRepository.getFeaturedListings();
        recommendedListings = listingRepository.getRecommendedListings(6);
        recentListings = listingRepository.getRecentListings(10);
        categories = categoryRepository.getCategories();
    }

    // Getters cho UI
    public LiveData<List<Listing>> getFeaturedItems() { return featuredListings; }
    public LiveData<List<Listing>> getRecommendations() { return recommendedListings; }
    public LiveData<List<Listing>> getListings() { return recentListings; }
    public LiveData<List<Category>> getCategories() { return categories; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getErrorMessage() { return errorMessage; }

    public void clearErrorMessage() { errorMessage.setValue(null); }

    public void refreshData() {
        // Trong một ứng dụng thực tế, bạn sẽ gọi lại các phương thức tải dữ liệu.
        // Vì chúng ta dùng observeForever, nó sẽ tự cập nhật.
        // Tuy nhiên, hàm này có thể hữu ích để kích hoạt lại một cách thủ công.
    }
}