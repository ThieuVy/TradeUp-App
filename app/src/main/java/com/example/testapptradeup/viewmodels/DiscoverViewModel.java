package com.example.testapptradeup.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import com.example.testapptradeup.models.Listing;
import com.example.testapptradeup.repositories.ListingRepository;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class DiscoverViewModel extends ViewModel {
    private final ListingRepository listingRepository = new ListingRepository();
    private final MutableLiveData<String> filterTrigger = new MutableLiveData<>();

    // Sử dụng switchMap để tự động chuyển đổi nguồn dữ liệu khi filterTrigger thay đổi
    public final LiveData<List<Listing>> discoverListings = Transformations.switchMap(filterTrigger, filter -> {
        if (filter == null) {
            MutableLiveData<List<Listing>> emptyData = new MutableLiveData<>();
            emptyData.setValue(Collections.emptyList());
            return emptyData;
        }

        // Logic để tải dữ liệu dựa trên tab được chọn
        switch (filter) {
            case "Gần bạn":
                // TODO: Triển khai logic lấy tin đăng gần vị trí người dùng
                return listingRepository.getRecentListings(); // Tạm thời dùng tin mới nhất
            case "Dành cho bạn":
                return listingRepository.getRecommendedListings(20);
            case "Khám phá":
            default:
                return listingRepository.getFeaturedListings();
        }
    });

    /**
     * Fragment gọi phương thức này khi người dùng chọn một tab mới.
     * @param filter Tên của tab được chọn (ví dụ: "Gần bạn").
     */
    public void setFilter(String filter) {
        if (!Objects.equals(filter, filterTrigger.getValue())) {
            filterTrigger.setValue(filter);
        }
    }
}