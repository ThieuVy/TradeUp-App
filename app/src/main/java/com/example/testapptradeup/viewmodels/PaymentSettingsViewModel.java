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

    // Lớp nội tại để chứa các khóa cần thiết từ backend
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
    public LiveData<StripeKeys> getStripeKeys() {
        return _stripeKeys;
    }

    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>();
    public LiveData<Boolean> isLoading() {
        return _isLoading;
    }

    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();
    public LiveData<String> getErrorMessage() {
        return _errorMessage;
    }

    public void fetchStripeKeys() {
        _isLoading.setValue(true);

        // 1. Gọi Cloud Function 'createSetupIntent'
        callCreateSetupIntent().continueWithTask(task -> {
            if (!task.isSuccessful()) {
                // Nếu bước 1 thất bại, ném ra exception để dừng chuỗi
                throw Objects.requireNonNull(task.getException());
            }

            // Lấy kết quả từ bước 1
            Map<String, String> setupIntentResult = task.getResult();
            if (setupIntentResult == null) {
                throw new Exception("Dữ liệu từ createSetupIntent là null.");
            }
            final String clientSecret = setupIntentResult.get("clientSecret");
            final String customerId = setupIntentResult.get("customerId");

            // 2. Gọi Cloud Function 'createEphemeralKey' với customerId đã có
            return callCreateEphemeralKey().continueWith(keyTask -> {
                if (!keyTask.isSuccessful()) {
                    // Nếu bước 2 thất bại, ném ra exception
                    throw Objects.requireNonNull(keyTask.getException());
                }
                Map<String, String> ephemeralKeyResult = keyTask.getResult();
                if (ephemeralKeyResult == null) {
                    throw new Exception("Dữ liệu từ createEphemeralKey là null.");
                }
                String ephemeralKey = ephemeralKeyResult.get("ephemeralKey");

                // 3. Khi có đủ cả 3 khóa, tạo đối tượng StripeKeys
                if (clientSecret == null || customerId == null || ephemeralKey == null) {
                    throw new Exception("Thiếu một trong các khóa Stripe cần thiết.");
                }
                return new StripeKeys(clientSecret, ephemeralKey, customerId);
            });
        }).addOnCompleteListener(task -> {
            _isLoading.setValue(false);
            if (task.isSuccessful()) {
                // 4. Cập nhật LiveData để Fragment nhận được
                _stripeKeys.setValue(task.getResult());
            } else {
                Log.e(TAG, "Lỗi khi lấy thông tin thanh toán: ", task.getException());
                _errorMessage.setValue("Lỗi khi lấy thông tin thanh toán: " + Objects.requireNonNull(task.getException()).getMessage());
            }
        });
    }

    // --- Tách các lệnh gọi Function ra thành các hàm riêng để code sạch hơn ---

    @NonNull
    private Task<Map<String, String>> callCreateSetupIntent() {
        return functions.getHttpsCallable("createSetupIntent")
                .call()
                .continueWith(task -> {
                    // *** ĐÂY LÀ PHẦN SỬA LỖI CẢNH BÁO ***
                    // Chúng ta kiểm tra kiểu dữ liệu một cách an toàn
                    Object data = Objects.requireNonNull(task.getResult()).getData();
                    if (data instanceof Map) {
                        // Chỉ ép kiểu sau khi đã kiểm tra
                        @SuppressWarnings("unchecked")
                        Map<String, String> mapData = (Map<String, String>) data;
                        return mapData;
                    } else {
                        throw new Exception("Kiểu dữ liệu trả về từ createSetupIntent không hợp lệ.");
                    }
                });
    }

    @NonNull
    private Task<Map<String, String>> callCreateEphemeralKey() {
        Map<String, Object> requestData = new HashMap<>();
        // Luôn dùng phiên bản API mới nhất mà thư viện Android của bạn hỗ trợ
        requestData.put("apiVersion", "2024-04-10");

        return functions.getHttpsCallable("createEphemeralKey")
                .call(requestData)
                .continueWith(task -> {
                    // *** SỬA LỖI CẢNH BÁO Ở ĐÂY ***
                    Object data = Objects.requireNonNull(task.getResult()).getData();
                    if (data instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, String> mapData = (Map<String, String>) data;
                        return mapData;
                    } else {
                        throw new Exception("Kiểu dữ liệu trả về từ createEphemeralKey không hợp lệ.");
                    }
                });
    }
}