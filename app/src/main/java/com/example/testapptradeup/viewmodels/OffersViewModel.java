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
    private final MutableLiveData<Boolean> offerActionStatus = new MutableLiveData<>();

    public OffersViewModel() {
        this.offerRepository = new OfferRepository();
    }

    // Lấy danh sách các đề nghị cho một tin đăng
    public LiveData<List<Offer>> getOffers(String listingId) {
        if (offersForListing == null) { // Chỉ tải lần đầu
            offersForListing = offerRepository.getOffersForListing(listingId);
        }
        return offersForListing;
    }

    // Theo dõi trạng thái của hành động (chấp nhận, từ chối, v.v.)
    public LiveData<Boolean> getOfferActionStatus() {
        return offerActionStatus;
    }

    // Hành động chấp nhận một đề nghị
    public void acceptOffer(Offer offer, Listing listing) {
        offerRepository.acceptOffer(offer, listing).observeForever(offerActionStatus::setValue);
    }

    // (Tương lai) Thêm các hàm rejectOffer, counterOffer ở đây
}