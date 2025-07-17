package com.example.testapptradeup.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import com.example.testapptradeup.models.Transaction;
import com.example.testapptradeup.repositories.HistoryRepository;
import java.util.List;

public class HistoryViewModel extends ViewModel {
    private final HistoryRepository repository;
    private LiveData<List<Transaction>> myPurchases;
    private LiveData<List<Transaction>> mySales;

    public HistoryViewModel() {
        this.repository = new HistoryRepository();
    }

    public LiveData<List<Transaction>> getMyPurchases() {
        if (myPurchases == null) {
            myPurchases = repository.getMyPurchases();
        }
        return myPurchases;
    }

    public LiveData<List<Transaction>> getMySales() {
        if (mySales == null) {
            mySales = repository.getMySales();
        }
        return mySales;
    }
}