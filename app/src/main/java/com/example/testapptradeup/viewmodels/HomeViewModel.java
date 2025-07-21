package com.example.testapptradeup.viewmodels;

import android.annotation.SuppressLint;
import android.app.Application;
import android.location.Location;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.testapptradeup.models.Category;
import com.example.testapptradeup.models.Listing;
import com.example.testapptradeup.repositories.CategoryRepository;
import com.example.testapptradeup.repositories.ListingRepository;
import com.example.testapptradeup.repositories.UserRepository;
import com.firebase.geofire.GeoFireUtils;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class HomeViewModel extends AndroidViewModel {

    private final ListingRepository listingRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final String currentUserId;

    // LiveData cho các danh sách
    private final LiveData<List<Listing>> featuredItems;
    private final LiveData<List<Listing>> recommendations;
    private final LiveData<List<Category>> categories; // Giữ lại để có thể mở rộng

    // LiveData cho việc xử lý vị trí
    private final FusedLocationProviderClient fusedLocationClient;
    private final MutableLiveData<Location> userLocation = new MutableLiveData<>();
    private final MediatorLiveData<List<Listing>> prioritizedRecentListings = new MediatorLiveData<>();

    public HomeViewModel(@NonNull Application application) {
        super(application);
        this.listingRepository = new ListingRepository();
        this.userRepository = new UserRepository();
        this.categoryRepository = new CategoryRepository(); // Vẫn khởi tạo nếu cần
        this.currentUserId = FirebaseAuth.getInstance().getUid();

        // Lấy dữ liệu trực tiếp từ repository, chúng sẽ tự cập nhật
        this.featuredItems = listingRepository.getFeaturedListings();
        this.recommendations = listingRepository.getRecommendedListings(4);
        this.categories = categoryRepository.getTopCategories(8); // Giả sử CategoryRepository cũng dùng listener

        // Thiết lập FusedLocationProviderClient
        this.fusedLocationClient = LocationServices.getFusedLocationProviderClient(application);

        // Lấy LiveData gốc của "Tin gần đây" từ repository
        LiveData<List<Listing>> recentListingsSource = listingRepository.getRecentListings();

        // Mediator sẽ lắng nghe cả vị trí và danh sách gốc để kết hợp và sắp xếp
        prioritizedRecentListings.addSource(userLocation, location ->
                combineAndSort(location, recentListingsSource.getValue())
        );
        prioritizedRecentListings.addSource(recentListingsSource, listings ->
                combineAndSort(userLocation.getValue(), listings)
        );

        // Bắt đầu lấy vị trí người dùng
        fetchUserLocation();
    }

    /**
     * Kết hợp và sắp xếp danh sách tin đăng dựa trên vị trí người dùng.
     * Phương thức này tạo một danh sách mới để sắp xếp, tránh thay đổi danh sách gốc.
     */
    private void combineAndSort(Location location, List<Listing> listings) {
        if (listings == null) {
            prioritizedRecentListings.setValue(new ArrayList<>()); // Trả về danh sách rỗng nếu nguồn là null
            return;
        }

        // Tạo một bản sao của danh sách để thực hiện sắp xếp
        List<Listing> listToSort = new ArrayList<>(listings);

        if (location != null) {
            final GeoLocation center = new GeoLocation(location.getLatitude(), location.getLongitude());
            listToSort.sort(Comparator.comparingDouble(listing -> {
                if (listing.getLatitude() != 0 && listing.getLongitude() != 0) {
                    return GeoFireUtils.getDistanceBetween(new GeoLocation(listing.getLatitude(), listing.getLongitude()), center);
                }
                return Double.MAX_VALUE; // Đẩy các tin không có vị trí xuống cuối
            }));
        }
        // Cập nhật MediatorLiveData với danh sách đã được sắp xếp
        prioritizedRecentListings.setValue(listToSort);
    }

    /**
     * Lấy vị trí cuối cùng đã biết của người dùng.
     */
    @SuppressLint("MissingPermission")
    private void fetchUserLocation() {
        // Hàm này có thể được gọi lại nếu người dùng thực hiện "kéo để làm mới"
        fusedLocationClient.getLastLocation().addOnSuccessListener(userLocation::setValue);
    }

    /**
     * Xử lý hành động "kéo để làm mới" từ Fragment.
     * Hiện tại, chỉ cần lấy lại vị trí người dùng vì dữ liệu tin đăng đã tự cập nhật.
     */
    public void refreshData() {
        fetchUserLocation();
    }

    /**
     * Thêm hoặc xóa một tin đăng khỏi danh sách yêu thích của người dùng.
     */
    public void toggleFavorite(String listingId, boolean isFavorite) {
        if (currentUserId != null) {
            userRepository.toggleFavorite(currentUserId, listingId, isFavorite);
        }
    }

    // --- GETTERS ĐỂ FRAGMENT OBSERVE ---

    public LiveData<List<Listing>> getFeaturedItems() {
        return featuredItems;
    }

    public LiveData<List<Category>> getCategories() {
        return categories;
    }

    public LiveData<List<Listing>> getRecommendations() {
        return recommendations;
    }

    /**
     * Cung cấp danh sách "Tin gần đây" đã được ưu tiên hóa theo vị trí.
     */
    public LiveData<List<Listing>> getPrioritizedRecentListings() {
        return prioritizedRecentListings;
    }

    public LiveData<String> getErrorMessage() {
        return listingRepository.getErrorMessage();
    }
}