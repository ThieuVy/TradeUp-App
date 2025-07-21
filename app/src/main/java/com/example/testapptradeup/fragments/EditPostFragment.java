package com.example.testapptradeup.fragments;

import android.annotation.SuppressLint;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.example.testapptradeup.R;
import com.example.testapptradeup.models.Category;
import com.example.testapptradeup.models.Listing;
import com.example.testapptradeup.models.User;
import com.example.testapptradeup.viewmodels.EditPostViewModel;
import com.example.testapptradeup.viewmodels.MainViewModel;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class EditPostFragment extends PostFragment { // Kế thừa từ PostFragment

    private EditPostViewModel editViewModel;
    private String listingIdToEdit;
    private MainViewModel mainViewModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        editViewModel = new ViewModelProvider(this).get(EditPostViewModel.class);
        // Lấy MainViewModel từ activity chứa nó
        mainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        if (getArguments() != null) {
            listingIdToEdit = EditPostFragmentArgs.fromBundle(getArguments()).getListingIdToEdit();
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        // Không gọi super.onViewCreated() để tùy chỉnh hoàn toàn
        navController = Navigation.findNavController(view);
        initViews(view);
        setupRecyclerView();

        MaterialToolbar toolbar = view.findViewById(R.id.toolbar_post);
        LinearLayout headerLayout = view.findViewById(R.id.header_layout);

        toolbar.setVisibility(View.VISIBLE);
        headerLayout.setVisibility(View.GONE);
        toolbar.setTitle("Chỉnh sửa tin đăng");
        toolbar.setNavigationOnClickListener(v -> navController.popBackStack());

        btnPostListing.setText("Lưu thay đổi");
        setupListenersForEdit();
        observeViewModel();

        if (listingIdToEdit != null) {
            editViewModel.loadListingData(listingIdToEdit);
        } else {
            Toast.makeText(getContext(), "Lỗi: Không tìm thấy tin đăng.", Toast.LENGTH_LONG).show();
            navController.popBackStack();
        }
    }

    // Tạo một hàm listener riêng cho màn hình edit để tránh xung đột
    private void setupListenersForEdit() {
        btnPostListing.setOnClickListener(v -> saveChanges());
        btnSaveDraft.setOnClickListener(v -> Toast.makeText(getContext(), "Chức năng đang phát triển", Toast.LENGTH_SHORT).show());
        tvUseCurrentLocation.setOnClickListener(v -> requestLocationPermission());

        etAdditionalTags.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                String tag = v.getText().toString().trim();
                if (!tag.isEmpty()) {
                    addTagToGroup(tag);
                    v.setText("");
                }
                return true;
            }
            return false;
        });
    }

    @Override
    protected void observeViewModel() {
        // Thêm đoạn mã này để lắng nghe sự thay đổi của danh sách ảnh từ
        // PostViewModel (của lớp cha) và cập nhật giao diện.
        // Đây là bước bị thiếu, gây ra lỗi không hiển thị ảnh.
        viewModel.getSelectedImageUris().observe(getViewLifecycleOwner(), uris -> {
            if (photoAdapter != null && tvPhotoCountHeader != null) {
                photoAdapter.setImageUris(new ArrayList<>(uris));
                tvPhotoCountHeader.setText(String.format(Locale.getDefault(), "Thêm hình ảnh (%d/%d)", uris.size(), 10));
            }
        });

        // Các observer hiện có cho EditPostViewModel được giữ nguyên
        editViewModel.isLoading().observe(getViewLifecycleOwner(), this::showLoading);

        editViewModel.getListingData().observe(getViewLifecycleOwner(), listing -> {
            showLoading(false);
            if (listing != null) {
                populateForm(listing);
            }
        });

        editViewModel.getUpdateStatus().observe(getViewLifecycleOwner(), success -> {
            if (success != null) {
                showLoading(false);
                if (success) {
                    Toast.makeText(getContext(), "Cập nhật thành công!", Toast.LENGTH_SHORT).show();
                    navController.popBackStack();
                } else {
                    Toast.makeText(getContext(), "Cập nhật thất bại, vui lòng thử lại.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @SuppressLint("DefaultLocale")
    private void populateForm(Listing listing) {
        etProductTitle.setText(listing.getTitle());
        etPrice.setText(String.format("%.0f", listing.getPrice()));
        etDescription.setText(listing.getDescription());
        etLocation.setText(listing.getLocation());

        // Lấy ID từ Firestore (ví dụ: "furniture") và dùng hàm "phiên dịch" để lấy tên tiếng Việt
        String vietnameseCategoryName = Category.getCategoryNameById(listing.getCategory());
        spinnerCategory.setText(vietnameseCategoryName, false); // Hiển thị "Đồ gia dụng"

        if (listing.getCondition() != null) {
            switch (listing.getCondition()) {
                case "new": chipGroupCondition.check(R.id.chip_condition_new); break;
                case "like_new": chipGroupCondition.check(R.id.chip_condition_like_new); break;
                case "used": chipGroupCondition.check(R.id.chip_condition_used); break;
            }
        }

        // Cập nhật tọa độ từ listing đã tải
        this.listingLatitude = listing.getLatitude();
        this.listingLongitude = listing.getLongitude();

        // Hiển thị ảnh
        if (listing.getImageUrls() != null && !listing.getImageUrls().isEmpty()) {
            List<Uri> imageUris = listing.getImageUrls().stream().map(Uri::parse).collect(Collectors.toList());
            // Cập nhật ViewModel của lớp cha
            super.viewModel.setSelectedImageUris(imageUris);
        }

        chipGroupTags.removeAllViews();
        if (listing.getTags() != null) {
            for (String tag : listing.getTags()) {
                addTagToGroup(tag);
            }
        }
    }

    private void saveChanges() {
        if (validateAllFields()) {
            return;
        }

        User currentUser = prefsHelper.getCurrentUser();
        String currentUserId = (FirebaseAuth.getInstance().getCurrentUser() != null) ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (currentUser == null || currentUserId == null) {
            Toast.makeText(getContext(), "Lỗi phiên đăng nhập.", Toast.LENGTH_SHORT).show();
            return;
        }

        Listing updatedListing = buildListingFromUI(currentUser, currentUserId);
        updatedListing.setId(listingIdToEdit);

        // <<< SỬA LỖI 3: Truyền mainViewModel vào đây >>>
        editViewModel.saveChanges(updatedListing, mainViewModel);
    }
}