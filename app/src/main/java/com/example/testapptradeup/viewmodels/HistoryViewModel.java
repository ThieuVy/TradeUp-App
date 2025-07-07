package com.example.testapptradeup.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import com.example.testapptradeup.models.Transaction;
import com.example.testapptradeup.repositories.HistoryRepository;
import java.util.List;

public class HistoryViewModel extends ViewModel {
    private final HistoryRepository repository;

    public HistoryViewModel() {
        this.repository = new HistoryRepository();
    }

    public LiveData<List<Transaction>> getMyPurchases() {
        return repository.getMyPurchases();
    }

    public LiveData<List<Transaction>> getMySales() {
        return repository.getMySales();
    }
}