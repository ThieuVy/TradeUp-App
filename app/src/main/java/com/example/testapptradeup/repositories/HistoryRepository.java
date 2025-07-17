package com.example.testapptradeup.repositories;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.testapptradeup.models.Transaction;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;
import java.util.List;

public class HistoryRepository {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final String currentUserId = FirebaseAuth.getInstance().getUid();

    // Lấy danh sách các giao dịch mà người dùng hiện tại là người MUA
    public LiveData<List<Transaction>> getMyPurchases() {
        MutableLiveData<List<Transaction>> data = new MutableLiveData<>();
        if (currentUserId == null) {
            data.setValue(new ArrayList<>());
            return data;
        }
        db.collection("transactions")
                .whereEqualTo("buyerId", currentUserId)
                .orderBy("transactionDate", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snapshots -> {
                    if (snapshots != null) {
                        data.setValue(snapshots.toObjects(Transaction.class));
                    } else {
                        data.setValue(new ArrayList<>());
                    }
                })
                .addOnFailureListener(e -> data.setValue(new ArrayList<>()));
        return data;
    }

    // Lấy danh sách các giao dịch mà người dùng hiện tại là người BÁN
    public LiveData<List<Transaction>> getMySales() {
        MutableLiveData<List<Transaction>> data = new MutableLiveData<>();
        if (currentUserId == null) {
            data.setValue(new ArrayList<>());
            return data;
        }
        db.collection("transactions")
                .whereEqualTo("sellerId", currentUserId)
                .orderBy("transactionDate", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snapshots -> {
                    if (snapshots != null) {
                        data.setValue(snapshots.toObjects(Transaction.class));
                    } else {
                        data.setValue(new ArrayList<>());
                    }
                })
                .addOnFailureListener(e -> data.setValue(new ArrayList<>()));
        return data;
    }
}