package com.example.testapptradeup.viewmodels;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.google.android.gms.tasks.Task;
import com.google.firebase.functions.FirebaseFunctions;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class PaymentSettingsViewModel extends ViewModel {
    private static final String TAG = "PaymentSettingsVM";

    public static class StripeKeys {
        public final String setupIntentClientSecret;
        public final String ephemeralKeySecret;
        public final String customerId;

        StripeKeys(String setupIntentClientSecret, String ephemeralKeySecret, String customerId) {
            this.setupIntentClientSecret = setupIntentClientSecret;
            this.ephemeralKeySecret = ephemeralKeySecret;
            this.customerId = customerId;
        }
    }

    private final FirebaseFunctions functions = FirebaseFunctions.getInstance();
    private final MutableLiveData<StripeKeys> _stripeKeys = new MutableLiveData<>();
    public LiveData<StripeKeys> getStripeKeys() { return _stripeKeys; }

    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    public LiveData<Boolean> isLoading() { return _isLoading; }

    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();
    public LiveData<String> getErrorMessage() { return _errorMessage; }

    public void fetchStripeKeys() {
        _isLoading.setValue(true);
        _errorMessage.setValue(null);
        _stripeKeys.setValue(null);

        callCreateSetupIntent().continueWithTask(task -> {
            if (!task.isSuccessful()) {
                throw Objects.requireNonNull(task.getException());
            }
            Map<String, String> setupIntentResult = task.getResult();
            if (setupIntentResult == null) {
                throw new Exception("Dữ liệu từ createSetupIntent là null.");
            }
            final String clientSecret = setupIntentResult.get("clientSecret");
            final String customerId = setupIntentResult.get("customerId");

            return callCreateEphemeralKey(customerId).continueWith(keyTask -> {
                if (!keyTask.isSuccessful()) {
                    throw Objects.requireNonNull(keyTask.getException());
                }
                Map<String, String> ephemeralKeyResult = keyTask.getResult();
                if (ephemeralKeyResult == null) {
                    throw new Exception("Dữ liệu từ createEphemeralKey là null.");
                }
                String ephemeralKey = ephemeralKeyResult.get("ephemeralKey");

                if (clientSecret == null || customerId == null || ephemeralKey == null) {
                    throw new Exception("Thiếu một trong các khóa Stripe cần thiết.");
                }
                return new StripeKeys(clientSecret, ephemeralKey, customerId);
            });
        }).addOnCompleteListener(task -> {
            _isLoading.setValue(false);
            if (task.isSuccessful()) {
                _stripeKeys.setValue(task.getResult());
            } else {
                Log.e(TAG, "Lỗi khi lấy thông tin thanh toán: ", task.getException());
                _errorMessage.setValue("Lỗi: " + Objects.requireNonNull(task.getException()).getMessage());
            }
        });
    }

    @NonNull
    private Task<Map<String, String>> callCreateSetupIntent() {
        return functions.getHttpsCallable("createSetupIntent")
                .call()
                .continueWith(task -> (Map<String, String>) Objects.requireNonNull(task.getResult()).getData());
    }

    @NonNull
    private Task<Map<String, String>> callCreateEphemeralKey(String customerId) {
        Map<String, Object> requestData = new HashMap<>();
        requestData.put("customerId", customerId);
        requestData.put("apiVersion", "2024-04-10");

        return functions.getHttpsCallable("createEphemeralKey")
                .call(requestData)
                .continueWith(task -> (Map<String, String>) Objects.requireNonNull(task.getResult()).getData());
    }
}