package com.example.testapptradeup.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.testapptradeup.models.Offer;
import com.example.testapptradeup.repositories.OfferRepository;
import java.util.List;

public class OffersViewModel extends ViewModel {
    private final OfferRepository offerRepository;
    private LiveData<List<Offer>> offersForListing;
    private final MutableLiveData<ActionStatus> actionStatus = new MutableLiveData<>();

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

    /**
     * Chấp nhận một đề nghị.
     * @param offer Đề nghị cần chấp nhận.
     */
    public void acceptOffer(Offer offer) {
        // Phương thức này chỉ nhận 1 tham số
        offerRepository.acceptOffer(offer).observeForever(success -> {
            if (Boolean.TRUE.equals(success)) {
                actionStatus.setValue(new ActionStatus(true, "Đã chấp nhận đề nghị! Đang chờ người mua thanh toán."));
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
     * Reset trạng thái để thông báo không hiện lại.
     */
    public void clearActionStatus() {
        actionStatus.setValue(null);
    }
}