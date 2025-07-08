package com.example.testapptradeup.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import com.example.testapptradeup.models.Category;
import com.example.testapptradeup.models.Listing;
import com.example.testapptradeup.repositories.CategoryRepository;
import com.example.testapptradeup.repositories.ListingRepository;
import java.util.List;

/**
 * HomeViewModel tuân thủ đúng kiến trúc MVVM.
 * ViewModel không tự observe dữ liệu mà chỉ giữ và cung cấp LiveData từ Repository.
 * Fragment sẽ là nơi duy nhất observe các LiveData này.
 */
public class HomeViewModel extends ViewModel {

    private final ListingRepository listingRepository;
    private final CategoryRepository categoryRepository;

    // Các LiveData này được khởi tạo một lần và được trả về trực tiếp từ Repository.
    // Chúng là final để đảm bảo ViewModel không thể thay đổi nguồn dữ liệu.
    private final LiveData<List<Listing>> featuredItems;
    private final LiveData<List<Category>> categories;
    private final LiveData<List<Listing>> recommendations;
    private final LiveData<List<Listing>> recentListings;
    private final LiveData<Boolean> isLoading;
    private final LiveData<String> errorMessage;


    public HomeViewModel() {
        this.listingRepository = new ListingRepository();
        this.categoryRepository = new CategoryRepository();

        // Lấy các đối tượng LiveData từ repository một lần duy nhất.
        this.featuredItems = listingRepository.getFeaturedListings();
        this.recommendations = listingRepository.getRecommendedListings(4);
        this.recentListings = listingRepository.getRecentListings();
        this.categories = categoryRepository.getTopCategories(8); // Lấy 8 danh mục
        this.isLoading = listingRepository.isLoading(); // Lấy trạng thái loading từ repo
        this.errorMessage = listingRepository.getErrorMessage(); // Lấy thông báo lỗi từ repo
    }

    /**
     * Hàm này được gọi từ HomeFragment khi có một bài đăng mới.
     * Nó sẽ ủy quyền cho Repository để thêm bài đăng đó vào đầu danh sách "Tin rao gần đây" hiện tại.
     * Repository sẽ cập nhật MutableLiveData của nó, và thay đổi sẽ được tự động lan truyền đến Fragment.
     * @param newListing Đối tượng Listing mới được tạo từ PostFragment.
     */
    public void addNewListingToTop(Listing newListing) {
        listingRepository.prependLocalListing(newListing);
    }

    /**
     * Yêu cầu repository tải lại toàn bộ dữ liệu từ server.
     * Được gọi khi người dùng thực hiện hành động "pull-to-refresh".
     * Repository sẽ tự xử lý trạng thái loading.
     */
    public void refreshData() {
        listingRepository.fetchAll();
        categoryRepository.fetchAll(); // Giả sử CategoryRepository cũng có hàm tương tự
    }

    // --- GETTERS ĐỂ FRAGMENT CÓ THỂ OBSERVE ---

    public LiveData<List<Listing>> getFeaturedItems() {
        return featuredItems;
    }

    public LiveData<List<Category>> getCategories() {
        return categories;
    }

    public LiveData<List<Listing>> getRecommendations() {
        return recommendations;
    }

    public LiveData<List<Listing>> getListings() {
        return recentListings;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }
}