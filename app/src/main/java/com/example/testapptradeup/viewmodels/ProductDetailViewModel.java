package com.example.testapptradeup.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;
import com.example.testapptradeup.models.Listing;
import com.example.testapptradeup.models.Offer;
import com.example.testapptradeup.repositories.ListingRepository;
import com.example.testapptradeup.repositories.OfferRepository;
import com.example.testapptradeup.repositories.UserRepository;
import com.google.firebase.auth.FirebaseAuth;

public class ProductDetailViewModel extends ViewModel {
    private final ListingRepository listingRepository;
    // Bỏ public ở listingDetail để tuân thủ quy tắc đóng gói
    private final LiveData<Listing> listingDetail;
    private final OfferRepository offerRepository;
    private final UserRepository userRepository;
    private final String currentUserId;
    private final MutableLiveData<String> listingIdTrigger = new MutableLiveData<>();

    public ProductDetailViewModel() {
        this.listingRepository = new ListingRepository();
        this.offerRepository = new OfferRepository();
        this.userRepository = new UserRepository();
        this.currentUserId = FirebaseAuth.getInstance().getUid();

        // Sử dụng switchMap để phản ứng lại sự thay đổi của listingIdTrigger
        listingDetail = Transformations.switchMap(listingIdTrigger, id -> {
            if (id == null || id.isEmpty()) {
                return new MutableLiveData<>(null); // Trả về LiveData rỗng nếu không có ID
            }

            // Mỗi khi một ID mới được thiết lập, logic này sẽ chạy
            // === BẮT ĐẦU SỬA LỖI ===
            // 1. Gọi hàm tăng view thông qua repository
            listingRepository.incrementViewCount(id);
            // 2. Trả về LiveData từ repository
            return listingRepository.getListingById(id);
            // === KẾT THÚC SỬA LỖI ===
        });
    }

    // Hàm loadListingDetail bây giờ chỉ cần set giá trị cho trigger
    public void loadListingDetail(String listingId) {
        // Chỉ trigger nếu ID mới khác ID cũ để tránh tải lại không cần thiết
        if (listingId != null && !listingId.equals(listingIdTrigger.getValue())) {
            listingIdTrigger.setValue(listingId);
        }
    }

    public LiveData<Listing> getListingDetail() {
        return listingDetail;
    }

    public LiveData<Boolean> makeOffer(String listingId, String sellerId, double price, String message) {
        if (currentUserId == null) {
            MutableLiveData<Boolean> failure = new MutableLiveData<>();
            failure.setValue(false);
            return failure;
        }

        Offer offer = new Offer();
        offer.setListingId(listingId);
        offer.setSellerId(sellerId);
        offer.setBuyerId(currentUserId);

        // TODO: Lấy tên và avatar người dùng hiện tại
        offer.setBuyerName("Tên người mua");
        offer.setBuyerAvatarUrl("url_avatar_nguoi_mua");

        offer.setOfferPrice(price);
        offer.setMessage(message);
        offer.setStatus("pending");

        return offerRepository.createOffer(offer);
    }
}