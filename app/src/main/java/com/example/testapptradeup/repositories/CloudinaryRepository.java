package com.example.testapptradeup.repositories;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.example.testapptradeup.utils.Result; // Lớp Result generic

import java.util.Map;
import java.util.UUID;

public class CloudinaryRepository {
    private static final String TAG = "CloudinaryRepository";
    // Sử dụng upload preset riêng cho ảnh đại diện nếu có, hoặc dùng chung
    private static final String UPLOAD_PRESET = "upload_product";

    public LiveData<Result<String>> uploadProfileImage(Uri imageUri, Context context) {
        MutableLiveData<Result<String>> resultLiveData = new MutableLiveData<>();

        String requestId = MediaManager.get().upload(imageUri)
                .unsigned(UPLOAD_PRESET)
                // Tạo public_id ngẫu nhiên để tránh trùng lặp và ghi đè
                .option("public_id", "profiles/" + UUID.randomUUID().toString())
                .callback(new UploadCallback() {
                    @Override
                    public void onStart(String requestId) {
                        Log.d(TAG, "Bắt đầu tải ảnh đại diện...");
                    }

                    @Override
                    public void onProgress(String requestId, long bytes, long totalBytes) { }

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String url = (String) resultData.get("secure_url");
                        if (url != null) {
                            Log.d(TAG, "Tải ảnh đại diện thành công: " + url);
                            resultLiveData.postValue(Result.success(url));
                        } else {
                            resultLiveData.postValue(Result.error(new Exception("URL trả về từ Cloudinary là null.")));
                        }
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        Log.e(TAG, "Lỗi tải ảnh đại diện: " + error.getDescription());
                        resultLiveData.postValue(Result.error(new Exception(error.getDescription())));
                    }

                    @Override
                    public void onReschedule(String requestId, ErrorInfo error) { }
                })
                .dispatch(context); // Sử dụng context được truyền vào

        return resultLiveData;
    }
}