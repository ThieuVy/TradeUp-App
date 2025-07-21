package com.example.testapptradeup.viewmodels;

import android.app.Application;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.Transformations;
import com.example.testapptradeup.models.Listing;
import com.example.testapptradeup.repositories.ListingRepository;
import com.example.testapptradeup.repositories.PostRepository;
import java.util.ArrayList;
import java.util.List;

public class EditPostViewModel extends AndroidViewModel {
    private final ListingRepository listingRepository;
    private final PostRepository postRepository;
    private final MutableLiveData<String> listingIdTrigger = new MutableLiveData<>();
    private final LiveData<Listing> listingData;
    private final MutableLiveData<Boolean> _updateStatus = new MutableLiveData<>();
    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>();

    public EditPostViewModel(@NonNull Application application) {
        super(application);
        this.listingRepository = new ListingRepository();
        this.postRepository = new PostRepository();
        listingData = Transformations.switchMap(listingIdTrigger, listingRepository::getListingById);
    }

    public LiveData<Listing> getListingData() {
        return listingData;
    }

    public LiveData<Boolean> getUpdateStatus() {
        return _updateStatus;
    }

    public LiveData<Boolean> isLoading() {
        return _isLoading;
    }

    public void loadListingData(String listingId) {
        if (listingId != null && !listingId.equals(listingIdTrigger.getValue())) {
            listingIdTrigger.setValue(listingId);
        }
    }

    public void saveChanges(Listing updatedListing, MainViewModel mainViewModel) {
        _isLoading.setValue(true);

        List<String> existingImageUrls = new ArrayList<>();
        List<Uri> newImageUris = new ArrayList<>();

        if (updatedListing.getImageUrls() != null) {
            for (String urlOrUriString : updatedListing.getImageUrls()) {
                if (urlOrUriString != null) {
                    Uri uri = Uri.parse(urlOrUriString);
                    // Ảnh cũ là URL http/https, ảnh mới là URI content
                    if ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme())) {
                        existingImageUrls.add(urlOrUriString);
                    } else {
                        newImageUris.add(uri);
                    }
                }
            }
        }

        if (!newImageUris.isEmpty()) {
            // Nếu có ảnh mới, upload chúng trước
            postRepository.uploadImages(newImageUris, getApplication()).observeForever(newlyUploadedUrls -> {
                if (newlyUploadedUrls != null) {
                    // Gộp danh sách ảnh cũ và mới
                    existingImageUrls.addAll(newlyUploadedUrls);
                    updatedListing.setImageUrls(existingImageUrls);
                    updateListingDocument(updatedListing, mainViewModel);
                } else {
                    _isLoading.setValue(false);
                    _updateStatus.setValue(false); // Báo lỗi
                }
            });
        } else {
            // Nếu không có ảnh mới, cập nhật trực tiếp
            updatedListing.setImageUrls(existingImageUrls);
            updateListingDocument(updatedListing, mainViewModel);
        }
    }

    private void updateListingDocument(Listing listingToSave, MainViewModel mainViewModel) {
        LiveData<Boolean> result = listingRepository.updateListing(listingToSave);
        result.observeForever(new Observer<>() {
            @Override
            public void onChanged(Boolean success) {
                _isLoading.setValue(false);
                _updateStatus.setValue(success);
                if (Boolean.TRUE.equals(success)) {
                    mainViewModel.postListingUpdateEvent();
                }
                result.removeObserver(this);
            }
        });
    }
}