package com.example.testapptradeup.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.testapptradeup.repositories.PaymentRepository;
import java.util.Map;

public class PaymentViewModel extends ViewModel {
    private final PaymentRepository paymentRepository;
    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();
    private LiveData<Map<String, String>> paymentKeys;

    public PaymentViewModel() {
        this.paymentRepository = new PaymentRepository();
    }

    public LiveData<Boolean> isLoading() { return _isLoading; }
    public LiveData<String> getErrorMessage() { return _errorMessage; }
    public LiveData<Map<String, String>> getPaymentKeys() { return paymentKeys; }

    public void startEscrowPayment(String listingId, String sellerId, double amount) {
        _isLoading.setValue(true);
        paymentKeys = paymentRepository.createEscrowIntent(listingId, sellerId, amount);
        paymentKeys.observeForever(keys -> {
            _isLoading.setValue(false);
            if (keys == null) {
                _errorMessage.setValue("Không thể khởi tạo thanh toán. Vui lòng thử lại.");
            }
        });
    }
}