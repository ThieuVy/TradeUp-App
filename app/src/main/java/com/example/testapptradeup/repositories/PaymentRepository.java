package com.example.testapptradeup.repositories;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.functions.FirebaseFunctions;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class PaymentRepository {
    private final FirebaseFunctions functions = FirebaseFunctions.getInstance();

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