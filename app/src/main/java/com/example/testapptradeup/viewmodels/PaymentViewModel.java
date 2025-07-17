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

    public void postErrorMessage(String message) {
        _errorMessage.setValue(message);
    }

    public void startEscrowPayment(String listingId, String sellerId, double amount) {
        _isLoading.setValue(true);
        _errorMessage.setValue(null);
        paymentKeys = paymentRepository.createEscrowIntent(listingId, sellerId, amount);

        paymentKeys.observeForever(keys -> {
            _isLoading.setValue(false);
            if (keys == null) {
                postErrorMessage("Không thể khởi tạo thanh toán. Vui lòng thử lại.");
            }
        });
    }

    /**
     * === HÀM MỚI ===
     * Hoàn tất giao dịch sau khi thanh toán thành công.
     * @param listingId ID của tin đăng đã bán.
     * @param offerId ID của đề nghị đã được thanh toán.
     * @return LiveData<Boolean> báo hiệu trạng thái của việc hoàn tất.
     */
    public LiveData<Boolean> finalizeTransaction(String listingId, String offerId) {
        // ViewModel chỉ đơn giản là gọi phương thức tương ứng trong repository
        return offerRepository.completeTransaction(listingId, offerId);
    }
}