package com.example.testapptradeup.repositories;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.example.testapptradeup.models.Listing;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

public class PostRepository {
    private static final String TAG = "PostRepository";
    private static final String UPLOAD_PRESET_NAME = "upload_product";
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    // Biến này không còn cần thiết vì Cloudinary SDK tự quản lý luồng
    // private final ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    public PostRepository() {}

    /**
     * Upload nhiều ảnh lên Cloudinary và trả về danh sách URL.
     * @param imageUris Danh sách Uri của các ảnh cần upload.
     * @param context Context để Cloudinary SDK có thể hoạt động (ví dụ: truy cập ContentResolver).
     * @return LiveData chứa danh sách các URL ảnh đã upload thành công.
     */
    public LiveData<List<String>> uploadImages(List<Uri> imageUris, Context context) {
        MutableLiveData<List<String>> uploadedUrlsLiveData = new MutableLiveData<>();
        if (imageUris == null || imageUris.isEmpty()) {
            uploadedUrlsLiveData.setValue(Collections.emptyList());
            return uploadedUrlsLiveData;
        }

        final List<String> uploadedUrls = Collections.synchronizedList(new ArrayList<>());
        final CountDownLatch latch = new CountDownLatch(imageUris.size());

        for (Uri uri : imageUris) {
            // Dòng code này sẽ không còn báo lỗi `context` nữa
            MediaManager.get().upload(uri)
                    .unsigned(UPLOAD_PRESET_NAME)
                    .callback(new UploadCallback() {
                        @Override
                        public void onStart(String requestId) {
                            Log.d(TAG, "Bắt đầu upload ảnh: " + requestId);
                        }

                        @Override
                        public void onProgress(String requestId, long bytes, long totalBytes) {
                            // No-op for now
                        }

                        @Override
                        public void onSuccess(String requestId, Map resultData) {
                            String url = (String) resultData.get("secure_url");
                            if (url != null) {
                                Log.d(TAG, "Upload thành công: " + url);
                                uploadedUrls.add(url);
                            }
                            latch.countDown();
                        }

                        @Override
                        public void onError(String requestId, ErrorInfo error) {
                            Log.e(TAG, "Lỗi upload ảnh: " + error.getDescription());
                            latch.countDown();
                        }

                        @Override
                        public void onReschedule(String requestId, ErrorInfo error) {
                            Log.w(TAG, "Upload được lên lịch lại: " + error.getDescription());
                        }
                    }).dispatch(context.getApplicationContext()); // Dùng application context để an toàn hơn
        }

        new Thread(() -> {
            try {
                latch.await();
                uploadedUrlsLiveData.postValue(new ArrayList<>(uploadedUrls));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Log.e(TAG, "Luồng chờ upload bị gián đoạn", e);
                uploadedUrlsLiveData.postValue(null);
            }
        }).start();

        return uploadedUrlsLiveData;
    }

    /**
     * Lưu thông tin tin đăng vào Firestore.
     */
    public Task<DocumentReference> saveListing(Listing listing) {
        // Chỉ cần trả về Task mà Firestore cung cấp
        return db.collection("listings").add(listing);
    }
}