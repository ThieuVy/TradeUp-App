package com.example.testapptradeup.repositories;

import android.util.Log; // Thêm import này
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.testapptradeup.models.Transaction;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;
import java.util.List;

public class HistoryRepository {
    private static final String TAG = "HistoryRepository"; // Thêm TAG để log lỗi
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final String currentUserId = FirebaseAuth.getInstance().getUid();

    // Lấy danh sách các giao dịch mà người dùng hiện tại là người MUA
    public LiveData<List<Transaction>> getMyPurchases() {
        MutableLiveData<List<Transaction>> data = new MutableLiveData<>();
        if (currentUserId == null) {
            data.setValue(new ArrayList<>());
            return data;
        }

        // === SỬA LỖI TỪ .get() SANG .addSnapshotListener() ===
        db.collection("transactions")
                .whereEqualTo("buyerId", currentUserId)
                .orderBy("transactionDate", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Lỗi khi lắng nghe lịch sử mua: ", error);
                        data.setValue(new ArrayList<>()); // Trả về list rỗng nếu có lỗi
                        return;
                    }

                    if (snapshots != null) {
                        data.setValue(snapshots.toObjects(Transaction.class));
                    } else {
                        data.setValue(new ArrayList<>());
                    }
                });
        return data;
    }

    // Lấy danh sách các giao dịch mà người dùng hiện tại là người BÁN
    public LiveData<List<Transaction>> getMySales() {
        MutableLiveData<List<Transaction>> data = new MutableLiveData<>();
        if (currentUserId == null) {
            data.setValue(new ArrayList<>());
            return data;
        }

        // === SỬA LỖI TỪ .get() SANG .addSnapshotListener() ===
        db.collection("transactions")
                .whereEqualTo("sellerId", currentUserId)
                .orderBy("transactionDate", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Lỗi khi lắng nghe lịch sử bán: ", error);
                        data.setValue(new ArrayList<>());
                        return;
                    }

                    if (snapshots != null) {
                        data.setValue(snapshots.toObjects(Transaction.class));
                    } else {
                        data.setValue(new ArrayList<>());
                    }
                });
        return data;
    }
}