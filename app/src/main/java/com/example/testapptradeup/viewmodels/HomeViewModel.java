package com.example.testapptradeup.viewmodels;

import android.annotation.SuppressLint;
import android.app.Application;
import android.location.Location;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel; // SỬA LỖI 1: Import AndroidViewModel
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

public class HomeViewModel extends AndroidViewModel { // SỬA LỖI 1: Kế thừa từ AndroidViewModel

    private final ListingRepository listingRepository;
    private final CategoryRepository categoryRepository;

    private final LiveData<List<Listing>> featuredItems;
    private final LiveData<List<Category>> categories;
    private final LiveData<List<Listing>> recommendations;
    private final LiveData<List<Listing>> recentListings;
    private final LiveData<Boolean> isLoading;
    private final LiveData<String> errorMessage;
    private final UserRepository userRepository; // Cần có UserRepository
    private final String currentUserId; // Cần có ID người dùng

    // SỬA LỖI 2: Thêm 'final'
    private final FusedLocationProviderClient fusedLocationClient;
    private final MutableLiveData<Location> userLocation = new MutableLiveData<>();
    private final MediatorLiveData<List<Listing>> prioritizedRecentListings = new MediatorLiveData<>();

    public HomeViewModel(@NonNull Application application) {
        super(application); // SỬA LỖI 1: Gọi super constructor của AndroidViewModel
        this.listingRepository = new ListingRepository();
        this.categoryRepository = new CategoryRepository();

        this.featuredItems = listingRepository.getFeaturedListings();
        this.recommendations = listingRepository.getRecommendedListings(4);
        this.recentListings = listingRepository.getRecentListings();
        this.categories = categoryRepository.getTopCategories(8);
        this.isLoading = listingRepository.isLoading();
        this.errorMessage = listingRepository.getErrorMessage();
        this.userRepository = new UserRepository(); // Khởi tạo
        this.currentUserId = FirebaseAuth.getInstance().getUid(); // Lấy ID

        // SỬA LỖI 1: Lấy context từ application được truyền vào
        this.fusedLocationClient = LocationServices.getFusedLocationProviderClient(application);

        // Lấy LiveData gốc từ repository
        LiveData<List<Listing>> recentListingsSource = listingRepository.getRecentListings();

        // Mediator sẽ lắng nghe cả vị trí và danh sách gốc
        prioritizedRecentListings.addSource(userLocation, location ->
                combineAndSort(location, recentListingsSource.getValue())
        );
        prioritizedRecentListings.addSource(recentListingsSource, listings ->
                combineAndSort(userLocation.getValue(), listings)
        );

        fetchUserLocation();
    }

    private void combineAndSort(Location location, List<Listing> listings) {
        if (listings == null) return;

        if (location != null) {
            final GeoLocation center = new GeoLocation(location.getLatitude(), location.getLongitude());
            listings.sort(Comparator.comparingDouble(l ->
                    (l.getLatitude() != 0 && l.getLongitude() != 0)
                            ? GeoFireUtils.getDistanceBetween(new GeoLocation(l.getLatitude(), l.getLongitude()), center)
                            : Double.MAX_VALUE
            ));
        }
        prioritizedRecentListings.setValue(listings);
    }

    public LiveData<List<Listing>> getPrioritizedRecentListings() {
        return prioritizedRecentListings;
    }

    @SuppressLint("MissingPermission")
    private void fetchUserLocation() {
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                userLocation.setValue(location);
            }
        });
    }

    private void combineAndSortListings(Location location, List<Listing> listings) {
        if (listings == null) {
            prioritizedRecentListings.setValue(new ArrayList<>());
            return;
        }

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

        prioritizedRecentListings.setValue(listToSort);
    }

    public void addNewListingToTop(Listing newListing) {
        listingRepository.prependLocalListing(newListing);
    }

    public void refreshData() {
        listingRepository.fetchAll();
        categoryRepository.fetchAll();
        fetchUserLocation(); // Lấy lại vị trí mới khi refresh
    }

    public void toggleFavorite(String listingId, boolean isFavorite) {
        if (currentUserId == null) {
            // Có thể báo lỗi nếu cần
            return;
        }
        userRepository.toggleFavorite(currentUserId, listingId, isFavorite);
    }

    // --- GETTERS CHO FRAGMENT OBSERVE ---

    public LiveData<List<Listing>> getFeaturedItems() {
        return featuredItems;
    }

    // Phương thức này hiện tại chưa được dùng nhưng vẫn giữ lại để có thể mở rộng
    public LiveData<List<Category>> getCategories() {
        return categories;
    }

    public LiveData<List<Listing>> getRecommendations() {
        return recommendations;
    }

    // Lấy prioritizedRecentListings thay vì recentListings
    public LiveData<List<Listing>> getListings() {
        return prioritizedRecentListings;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public LiveData<Boolean> isLoading() {
        return isLoading;
    }
}