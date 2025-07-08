package com.example.testapptradeup.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.testapptradeup.models.Listing;
import com.example.testapptradeup.models.Offer;
import com.example.testapptradeup.repositories.ListingRepository;
import com.example.testapptradeup.repositories.OfferRepository;
import com.example.testapptradeup.repositories.UserRepository;
import com.google.firebase.auth.FirebaseAuth;

public class ProductDetailViewModel extends ViewModel {
    private final ListingRepository listingRepository;
    public LiveData<Listing> listingDetail;
    private final OfferRepository offerRepository;
    private final UserRepository userRepository;
    private final String currentUserId;

    public ProductDetailViewModel() {
        this.listingRepository = new ListingRepository();
        this.offerRepository = new OfferRepository();
        this.userRepository = new UserRepository();
        this.currentUserId = FirebaseAuth.getInstance().getUid();
        // Thêm một observer vào LiveData chi tiết sản phẩm.
        // Mỗi khi có một listing mới được tải thành công, nó sẽ gọi hàm incrementViewCount.
        listingDetail.observeForever(listing -> {
            if (listing != null && listing.getId() != null) {
                // Tăng lượt xem chỉ khi người dùng xem tin của người khác
                String currentUserId = FirebaseAuth.getInstance().getUid();
                if (currentUserId != null && !currentUserId.equals(listing.getSellerId())) {
                    listingRepository.incrementViewCount(listing.getId());
                }
            }
        });
    }

    public void loadListingDetail(String listingId) {
        listingDetail = listingRepository.getListingById(listingId);
    }

    public LiveData<Listing> getListingDetail() {
        return listingDetail;
    }

    /**
     * Tạo một đề nghị mua hàng từ người dùng hiện tại.
     * @param listingId ID của tin đăng
     * @param sellerId ID của người bán
     * @param price Giá đề nghị
     * @param message Tin nhắn kèm theo (tùy chọn)
     * @return LiveData<Boolean> để theo dõi trạng thái gửi
     */
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

        // TODO: Lấy tên và avatar người dùng hiện tại từ SharedPreferences hoặc một ViewModel chung
        // Ví dụ: User currentUser = SharedPrefsHelper.getInstance(getApplication()).getCurrentUser();
        offer.setBuyerName("Tên người mua"); // Thay thế bằng dữ liệu thật
        offer.setBuyerAvatarUrl("url_avatar_nguoi_mua"); // Thay thế bằng dữ liệu thật

        offer.setOfferPrice(price);
        offer.setMessage(message);
        offer.setStatus("pending");

        return offerRepository.createOffer(offer);
    }
}