package com.example.testapptradeup.repositories;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.testapptradeup.models.StripeTransaction;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken; // <<< SỬA ĐỔI: Import đúng thư viện TypeToken

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class PaymentRepository {
    private final FirebaseFunctions functions = FirebaseFunctions.getInstance();

    public LiveData<List<StripeTransaction>> getPaymentHistory() {
        MutableLiveData<List<StripeTransaction>> historyLiveData = new MutableLiveData<>();

        functions.getHttpsCallable("getPaymentHistory")
                .call()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        try {
                            Map<String, Object> result = (Map<String, Object>) task.getResult().getData();

                            // <<< SỬA ĐỔI: Kiểm tra null an toàn để tránh NullPointerException >>>
                            if (result != null && Boolean.TRUE.equals(result.get("success"))) {
                                // Dùng Gson để parse danh sách an toàn
                                Gson gson = new Gson();
                                String historyJson = gson.toJson(result.get("history"));
                                Type listType = new TypeToken<ArrayList<StripeTransaction>>() {}.getType();
                                List<StripeTransaction> transactions = gson.fromJson(historyJson, listType);
                                historyLiveData.setValue(transactions);
                            } else {
                                historyLiveData.setValue(new ArrayList<>());
                            }
                        } catch (Exception e) {
                            Log.e("PaymentRepository", "Lỗi khi parse dữ liệu getPaymentHistory", e);
                            historyLiveData.setValue(null); // Báo lỗi
                        }
                    } else {
                        Log.e("PaymentRepository", "Lỗi khi gọi getPaymentHistory", task.getException());
                        historyLiveData.setValue(null); // Báo lỗi
                    }
                });

        return historyLiveData;
    }

    /**
     * Gọi Cloud Function để tạo Payment Intent cho escrow.
     */
    public LiveData<Map<String, String>> createEscrowIntent(String listingId, String sellerId, double amount) {
        MutableLiveData<Map<String, String>> result = new MutableLiveData<>();

        Map<String, Object> data = new HashMap<>();
        data.put("listingId", listingId);
        data.put("sellerId", sellerId);
        data.put("amount", amount);

        functions.getHttpsCallable("createPaymentIntentForEscrow")
                .call(data)
                .continueWith(task -> (Map<String, String>) Objects.requireNonNull(task.getResult()).getData())
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        result.setValue(task.getResult());
                    } else {
                        Log.e("PaymentRepository", "Error calling createPaymentIntentForEscrow", task.getException());
                        result.setValue(null);
                    }
                });
        return result;
    }

    /**
     * Gọi Cloud Function để thu tiền (capture) từ Payment Intent.
     */
    public LiveData<Boolean> captureEscrowPayment(String paymentIntentId) {
        MutableLiveData<Boolean> success = new MutableLiveData<>();
        Map<String, Object> data = new HashMap<>();
        data.put("paymentIntentId", paymentIntentId);

        functions.getHttpsCallable("captureEscrowPayment")
                .call(data)
                .addOnCompleteListener(task -> success.setValue(task.isSuccessful()));
        return success;
    }
}