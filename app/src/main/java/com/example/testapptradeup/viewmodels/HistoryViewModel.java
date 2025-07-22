package com.example.testapptradeup.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData; // Thêm import này
import androidx.lifecycle.ViewModel;
import com.example.testapptradeup.models.Transaction;
import com.example.testapptradeup.repositories.HistoryRepository;
import java.util.List;

public class HistoryViewModel extends ViewModel {
    private final HistoryRepository repository;
    private LiveData<List<Transaction>> myPurchases;
    private LiveData<List<Transaction>> mySales;

    // THÊM MỚI: LiveData để quản lý trạng thái UI
    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>();
    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();

    public HistoryViewModel() {
        this.repository = new HistoryRepository();
    }

    // THÊM MỚI: Getters cho trạng thái UI
    public LiveData<Boolean> isLoading() {
        return _isLoading;
    }
    public LiveData<String> getErrorMessage() {
        return _errorMessage;
    }

    public LiveData<List<Transaction>> getMyPurchases() {
        if (myPurchases == null) {
            _isLoading.setValue(true); // Bắt đầu tải
            myPurchases = repository.getMyPurchases();
            // Lắng nghe kết quả để tắt loading
            myPurchases.observeForever(transactions -> {
                _isLoading.setValue(false);
                if (transactions == null) {
                    _errorMessage.setValue("Không thể tải lịch sử mua hàng.");
                }
            });
        }
        return myPurchases;
    }

    public LiveData<List<Transaction>> getMySales() {
        if (mySales == null) {
            _isLoading.setValue(true); // Bắt đầu tải
            mySales = repository.getMySales();
            // Lắng nghe kết quả để tắt loading
            mySales.observeForever(transactions -> {
                _isLoading.setValue(false);
                if (transactions == null) {
                    _errorMessage.setValue("Không thể tải lịch sử bán hàng.");
                }
            });
        }
        return mySales;
    }
}