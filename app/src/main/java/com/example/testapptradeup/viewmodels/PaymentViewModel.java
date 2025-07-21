package com.example.testapptradeup.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.Transformations; // Đảm bảo có import này

import com.example.testapptradeup.repositories.OfferRepository;
import com.example.testapptradeup.repositories.PaymentRepository;
import java.util.Map;

public class PaymentViewModel extends ViewModel {
    private final PaymentRepository paymentRepository;
    private final OfferRepository offerRepository;
    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();

    // SỬA LỖI 1: Khai báo trigger trước
    private final MutableLiveData<EscrowParams> _escrowTrigger = new MutableLiveData<>();

    // SỬA LỖI 2: Chỉ khai báo biến LiveData ở đây
    private final LiveData<Map<String, String>> paymentKeys;

    public PaymentViewModel() {
        // SỬA LỖI 3: Khởi tạo các repository trước tiên
        this.paymentRepository = new PaymentRepository();
        this.offerRepository = new OfferRepository();

        // SỬA LỖI 4: Khởi tạo LiveData `paymentKeys` bên trong constructor
        // Bây giờ, `_escrowTrigger` và `paymentRepository` đã chắc chắn không phải là null
        paymentKeys = Transformations.switchMap(_escrowTrigger, params -> {
            if (params == null) {
                return new MutableLiveData<>(null); // Trả về LiveData rỗng nếu không có params
            }
            _isLoading.setValue(true);
            LiveData<Map<String, String>> result = paymentRepository.createEscrowIntent(params.listingId, params.sellerId, params.amount);

            // Quan sát kết quả để tắt loading hoặc báo lỗi
            result.observeForever(keys -> {
                _isLoading.setValue(false);
                if (keys == null) {
                    postErrorMessage("Không thể khởi tạo thanh toán. Vui lòng thử lại.");
                }
            });
            return result;
        });
    }

    // --- Getters cho Fragment ---
    public LiveData<Boolean> isLoading() { return _isLoading; }
    public LiveData<String> getErrorMessage() { return _errorMessage; }
    public LiveData<Map<String, String>> getPaymentKeys() { return paymentKeys; }

    public void postErrorMessage(String message) {
        _errorMessage.setValue(message);
    }

    public void startEscrowPayment(String listingId, String sellerId, double amount) {
        _errorMessage.setValue(null);
        _escrowTrigger.setValue(new EscrowParams(listingId, sellerId, amount));
    }

    public LiveData<Boolean> finalizeTransaction(String listingId, String offerId) {
        if (offerId != null && offerId.startsWith("BUY_NOW_")) {
            return offerRepository.completeTransactionForBuyNow(listingId);
        }
        return offerRepository.completeTransaction(listingId, offerId);
    }

    // Lớp nội bộ để chứa tham số, không thay đổi
    private static class EscrowParams {
        final String listingId;
        final String sellerId;
        final double amount;

        EscrowParams(String listingId, String sellerId, double amount) {
            this.listingId = listingId;
            this.sellerId = sellerId;
            this.amount = amount;
        }
    }
}