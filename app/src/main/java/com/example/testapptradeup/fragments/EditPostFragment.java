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
import com.example.testapptradeup.models.Listing;
import com.example.testapptradeup.models.User;
import com.example.testapptradeup.viewmodels.EditPostViewModel;
import com.example.testapptradeup.viewmodels.MainViewModel;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;

import java.util.List;
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

    // Ghi đè hàm observeViewModel
    @Override
    protected void observeViewModel() {
        editViewModel.isLoading().observe(getViewLifecycleOwner(), this::showLoading);

        // <<< SỬA LỖI 2: Lỗi này sẽ tự hết sau khi sửa lỗi 1 >>>
        editViewModel.getListingData().observe(getViewLifecycleOwner(), listing -> {
            // Hiển thị loading trong khi chờ dữ liệu
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

    private void populateForm(Listing listing) {
        etProductTitle.setText(listing.getTitle());
        // Chuyển đổi double sang chuỗi một cách an toàn
        etPrice.setText(String.format("%.0f", listing.getPrice()));
        spinnerCategory.setText(listing.getCategory(), false);
        etDescription.setText(listing.getDescription());
        etLocation.setText(listing.getLocation());

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