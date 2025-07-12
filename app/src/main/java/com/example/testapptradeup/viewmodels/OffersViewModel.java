package com.example.testapptradeup.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.testapptradeup.models.Listing;
import com.example.testapptradeup.models.Offer;
import com.example.testapptradeup.repositories.OfferRepository;
import java.util.List;

public class OffersViewModel extends ViewModel {
    private final OfferRepository offerRepository;
    private LiveData<List<Offer>> offersForListing;
    // Sử dụng LiveData này cho tất cả các hành động để theo dõi trạng thái chung
    private final MutableLiveData<ActionStatus> actionStatus = new MutableLiveData<>();

    // Lớp nội bộ để biểu thị kết quả của một hành động
    public static class ActionStatus {
        public final boolean isSuccess;
        public final String message;

        private ActionStatus(boolean isSuccess, String message) {
            this.isSuccess = isSuccess;
            this.message = message;
        }
    }

    public OffersViewModel() {
        this.offerRepository = new OfferRepository();
    }

    public LiveData<List<Offer>> getOffers(String listingId) {
        if (offersForListing == null) {
            offersForListing = offerRepository.getOffersForListing(listingId);
        }
        return offersForListing;
    }

    public LiveData<ActionStatus> getActionStatus() {
        return actionStatus;
    }

    // SỬA LỖI: Hàm này bây giờ nhận cả Listing
    public void acceptOffer(Offer offer, Listing listing) {
        // Gọi hàm repository đã được sửa đổi
        offerRepository.acceptOffer(offer, listing).observeForever(success -> {
            if (Boolean.TRUE.equals(success)) {
                actionStatus.setValue(new ActionStatus(true, "Đã chấp nhận đề nghị và hoàn tất giao dịch!"));
            } else {
                actionStatus.setValue(new ActionStatus(false, "Lỗi khi chấp nhận đề nghị."));
            }
        });
    }

    /**
     * Từ chối một đề nghị.
     */
    public void rejectOffer(Offer offer) {
        offerRepository.updateOfferStatus(offer.getId(), "rejected").observeForever(success -> {
            if (Boolean.TRUE.equals(success)) {
                actionStatus.setValue(new ActionStatus(true, "Đã từ chối đề nghị."));
                // Làm mới danh sách để cập nhật UI
                refreshOffers();
            } else {
                actionStatus.setValue(new ActionStatus(false, "Lỗi khi từ chối đề nghị."));
            }
        });
    }

    /**
     * Tải lại danh sách offers sau một hành động.
     */
    public void refreshOffers() {
        if (offersForListing != null && offersForListing.getValue() != null && !offersForListing.getValue().isEmpty()) {
            String listingId = offersForListing.getValue().get(0).getListingId();
            offersForListing = offerRepository.getOffersForListing(listingId);
        }
    }

    /**
     * Reset trạng thái để dialog không hiện lại khi xoay màn hình
     */
    public void clearActionStatus() {
        actionStatus.setValue(null);
    }
}