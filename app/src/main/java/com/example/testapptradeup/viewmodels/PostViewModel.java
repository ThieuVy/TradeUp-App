package com.example.testapptradeup.viewmodels;

import android.app.Application;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.testapptradeup.models.Listing;
import com.example.testapptradeup.repositories.PostRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PostViewModel extends AndroidViewModel {

    public static class PostStatus {
        public static final PostStatus IDLE = new PostStatus("IDLE");
        public static final PostStatus LOADING = new PostStatus("LOADING");

        private final MutableLiveData<PostStatus> postStatus = new MutableLiveData<>(PostStatus.IDLE);

        // Thay đổi SUCCESS thành một lớp riêng để chứa dữ liệu
        public static class SUCCESS extends PostStatus {
            public final Listing listing;
            public SUCCESS(Listing listing) {
                super("SUCCESS");
                this.listing = listing;
            }
        }

        public static class Error extends PostStatus {
            public final String message;
            public Error(String message) { super("ERROR"); this.message = message; }
        }

        private final String state;
        private PostStatus(String state) { this.state = state; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || !getClass().getSimpleName().equals(o.getClass().getSimpleName())) return false;
            PostStatus that = (PostStatus) o;
            return Objects.equals(state, that.state);
        }
        @Override
        public int hashCode() { return Objects.hash(state); }
    }

    private final PostRepository postRepository;
    private final MutableLiveData<List<Uri>> selectedImageUris = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<PostStatus> postStatus = new MutableLiveData<>(PostStatus.IDLE);

    public PostViewModel(@NonNull Application application) {
        super(application);
        this.postRepository = new PostRepository();
    }

    public LiveData<List<Uri>> getSelectedImageUris() { return selectedImageUris; }
    public LiveData<PostStatus> getPostStatus() { return postStatus; }


    public void setSelectedImageUris(List<Uri> uris) {
        selectedImageUris.setValue(uris);
    }

    // Logic quản lý ảnh
    public void addImage(Uri uri) {
        List<Uri> currentUris = new ArrayList<>(Objects.requireNonNull(selectedImageUris.getValue()));
        if (!currentUris.contains(uri)) {
            currentUris.add(uri);
            selectedImageUris.setValue(currentUris);
        }
    }

    public void removeImage(Uri uri) {
        List<Uri> currentUris = new ArrayList<>(Objects.requireNonNull(selectedImageUris.getValue()));
        currentUris.remove(uri);
        selectedImageUris.setValue(currentUris);
    }

    /**
     * Bắt đầu quá trình đăng tin: upload ảnh rồi lưu thông tin.
     * @param listing Đối tượng tin đăng đã có thông tin (trừ URL ảnh).
     */
    public void postListing(Listing listing) {
        List<Uri> urisToUpload = selectedImageUris.getValue();
        if (urisToUpload == null || urisToUpload.isEmpty()) {
            postStatus.setValue(new PostStatus.Error("Vui lòng chọn ít nhất một ảnh."));
            return;
        }

        //postStatus.setValue(PostStatus.LOADING);

        // Bước 1: Upload ảnh
        postRepository.uploadImages(urisToUpload, getApplication()).observeForever(uploadedUrls -> {
            if (uploadedUrls == null || uploadedUrls.isEmpty()) {
                postStatus.postValue(new PostStatus.Error("Lỗi upload ảnh."));
                return;
            }

            // Bước 2: Gán URLs và lưu vào Firestore
            listing.setImageUrls(uploadedUrls);

            postRepository.saveListing(listing).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    // KHI THÀNH CÔNG: Gửi về trạng thái SUCCESS cùng với đối tượng Listing
                    postStatus.postValue(new PostStatus.SUCCESS(listing));
                } else {
                    postStatus.postValue(new PostStatus.Error("Lỗi khi lưu tin đăng: " + Objects.requireNonNull(task.getException()).getMessage()));
                }
            });
        });
    }

    /**
     * Cho phép Fragment chủ động đặt trạng thái loading.
     * @param isLoading True nếu đang tải, false nếu không.
     */
    public void setLoadingState(boolean isLoading) {
        if (isLoading) {
            postStatus.setValue(PostStatus.LOADING);
        } else {
            // Chỉ reset về IDLE nếu trạng thái hiện tại là LOADING
            // để tránh ghi đè lên trạng thái SUCCESS hoặc ERROR
            if (postStatus.getValue() == PostStatus.LOADING) {
                postStatus.setValue(PostStatus.IDLE);
            }
        }
    }
}