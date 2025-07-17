package com.example.testapptradeup.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.testapptradeup.repositories.OfferRepository;
import com.example.testapptradeup.repositories.PaymentRepository;
import java.util.Map;

public class PaymentViewModel extends ViewModel {
    private final PaymentRepository paymentRepository;
    private final OfferRepository offerRepository;
    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();
    private LiveData<Map<String, String>> paymentKeys;

    public PaymentViewModel() {
        this.paymentRepository = new PaymentRepository();
        this.offerRepository = new OfferRepository();
    }

    public LiveData<Boolean> isLoading() { return _isLoading; }
    public LiveData<String> getErrorMessage() { return _errorMessage; }
    public LiveData<Map<String, String>> getPaymentKeys() { return paymentKeys; }

    /**
     * Phương thức công khai để Fragment có thể báo lỗi cho ViewModel.
     * @param message Nội dung lỗi cần hiển thị.
     */
    public void postErrorMessage(String message) {
        _errorMessage.setValue(message);
    }

    public void startEscrowPayment(String listingId, String sellerId, double amount) {
        _isLoading.setValue(true);
        // Reset lỗi cũ trước khi bắt đầu
        _errorMessage.setValue(null);
        paymentKeys = paymentRepository.createEscrowIntent(listingId, sellerId, amount);

        // Quan sát kết quả từ repository
        paymentKeys.observeForever(keys -> {
            _isLoading.setValue(false);
            if (keys == null) {
                // Sử dụng phương thức mới để post lỗi
                postErrorMessage("Không thể khởi tạo thanh toán. Vui lòng thử lại.");
            }
        });
    }

    public LiveData<Boolean> finalizeTransaction(String listingId, String offerId) {
        return offerRepository.completeTransaction(listingId, offerId);
    }
}