package com.example.testapptradeup.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.testapptradeup.models.StripeTransaction;
import com.example.testapptradeup.repositories.PaymentRepository;

import java.util.List;

public class PaymentHistoryViewModel extends ViewModel {

    private final PaymentRepository paymentRepository;
    private LiveData<List<StripeTransaction>> paymentHistory;
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public PaymentHistoryViewModel() {
        this.paymentRepository = new PaymentRepository();
        loadHistory();
    }

    private void loadHistory() {
        isLoading.setValue(true);
        paymentHistory = paymentRepository.getPaymentHistory();
        // Quan sát kết quả để tắt loading
        paymentHistory.observeForever(transactions -> {
            isLoading.setValue(false);
            if (transactions == null) {
                errorMessage.setValue("Không thể tải lịch sử giao dịch. Vui lòng thử lại.");
            }
        });
    }

    public LiveData<List<StripeTransaction>> getPaymentHistory() {
        return paymentHistory;
    }

    public LiveData<Boolean> isLoading() {
        return isLoading;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }
}