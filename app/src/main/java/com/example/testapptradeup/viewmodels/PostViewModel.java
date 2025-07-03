package com.example.testapptradeup.viewmodels;

import android.net.Uri;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.example.testapptradeup.models.Listing;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class PostViewModel extends ViewModel {
    private static final String TAG = "PostViewModel";

    // Lớp trạng thái để UI lắng nghe
    public static class PostStatus {
        public static final PostStatus IDLE = new PostStatus();
        public static final PostStatus LOADING = new PostStatus();
        public static final PostStatus SUCCESS = new PostStatus();
        public static class Error extends PostStatus {
            public final String message;
            public Error(String message) { this.message = message; }
        }
    }

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    // Các LiveData quản lý ảnh
    private final MutableLiveData<List<Uri>> selectedImageUris = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<String>> uploadedImageUrls = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Map<Uri, UploadState>> imageUploadStates = new MutableLiveData<>(new HashMap<>());
    public enum UploadState { UPLOADING, SUCCESS, FAILED }

    // LiveData cho trạng thái đăng bài
    private final MutableLiveData<PostStatus> postStatus = new MutableLiveData<>(PostStatus.IDLE);

    // Getters
    public LiveData<List<Uri>> getSelectedImageUris() { return selectedImageUris; }
    public LiveData<Map<Uri, UploadState>> getImageUploadStates() { return imageUploadStates; }
    public LiveData<PostStatus> getPostStatus() { return postStatus; }

    /**
     * Khi người dùng chọn một ảnh mới.
     */
    public void addImage(Uri uri) {
        List<Uri> currentUris = new ArrayList<>(Objects.requireNonNull(selectedImageUris.getValue()));
        if (!currentUris.contains(uri)) {
            currentUris.add(uri);
            selectedImageUris.setValue(currentUris);
            startImageUpload(uri);
        }
    }

    /**
     * Khi người dùng xóa một ảnh.
     */
    public void removeImage(Uri uri) {
        // Cập nhật danh sách Uri hiển thị trên UI
        List<Uri> currentUris = new ArrayList<>(Objects.requireNonNull(selectedImageUris.getValue()));
        currentUris.remove(uri);
        selectedImageUris.setValue(currentUris);

        // Cập nhật trạng thái upload
        Map<Uri, UploadState> currentStates = new HashMap<>(Objects.requireNonNull(imageUploadStates.getValue()));
        currentStates.remove(uri);
        imageUploadStates.setValue(currentStates);

        // TODO: Cần một cơ chế phức tạp hơn để xóa URL đã upload khỏi uploadedImageUrls
        // Ví dụ: Dùng một Map<Uri, String> để lưu cặp Uri-Url
    }

    /**
     * Khi người dùng nhấn "Đăng tin".
     */
    public void postListing(Listing listing) {
        postStatus.setValue(PostStatus.LOADING);

        List<Uri> urisToUpload = selectedImageUris.getValue();
        List<String> finalUrls = uploadedImageUrls.getValue();

        if (urisToUpload == null || finalUrls == null || urisToUpload.size() != finalUrls.size()) {
            postStatus.setValue(new PostStatus.Error("Vui lòng chờ tất cả ảnh được tải lên hoặc xóa các ảnh bị lỗi."));
            return;
        }

        listing.setImageUrls(finalUrls);
        listing.setTimePosted(new Date());
        saveListingToFirestore(listing);
    }

    // ========== PHẦN SỬA LỖI ==========
    /**
     * Bắt đầu quá trình upload một URI ảnh lên Cloudinary.
     */
    private void startImageUpload(final Uri uri) {
        updateImageState(uri, UploadState.UPLOADING);

        MediaManager.get().upload(uri).callback(new UploadCallback() {
            @Override
            public void onStart(String requestId) {
                Log.d(TAG, "Bắt đầu tải lên: " + uri.toString());
            }

            @Override
            public void onSuccess(String requestId, Map resultData) {
                String url = (String) resultData.get("secure_url");
                if (url != null) {
                    Log.d(TAG, "Tải lên thành công: " + url);
                    List<String> currentUrls = new ArrayList<>(Objects.requireNonNull(uploadedImageUrls.getValue()));
                    currentUrls.add(url);
                    uploadedImageUrls.setValue(currentUrls);
                    updateImageState(uri, UploadState.SUCCESS);
                } else {
                    // SỬA: Tạo một đối tượng ErrorInfo mới với mã lỗi tùy chỉnh
                    // và một thông điệp rõ ràng.
                    ErrorInfo errorInfo = new ErrorInfo(-1, "Phản hồi từ Cloudinary không hợp lệ (thiếu URL).");
                    onError(requestId, errorInfo);
                }
            }

            @Override
            public void onError(String requestId, ErrorInfo error) {
                Log.e(TAG, "Lỗi tải lên: " + error.getDescription());
                updateImageState(uri, UploadState.FAILED);
            }

            @Override
            public void onReschedule(String requestId, ErrorInfo error) { }
            @Override
            public void onProgress(String requestId, long bytes, long totalBytes) { }
        }).dispatch();
    }
    // ========== KẾT THÚC PHẦN SỬA LỖI ==========

    private void updateImageState(Uri uri, UploadState state) {
        Map<Uri, UploadState> currentStates = new HashMap<>(Objects.requireNonNull(imageUploadStates.getValue()));
        currentStates.put(uri, state);
        imageUploadStates.setValue(currentStates);
    }

    private void saveListingToFirestore(Listing listing) {
        db.collection("listings").add(listing)
                .addOnSuccessListener(documentReference -> postStatus.setValue(PostStatus.SUCCESS))
                .addOnFailureListener(e -> postStatus.setValue(new PostStatus.Error("Lỗi lưu dữ liệu: " + e.getMessage())));
    }
}