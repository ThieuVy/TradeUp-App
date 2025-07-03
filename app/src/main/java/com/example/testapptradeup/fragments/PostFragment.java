package com.example.testapptradeup.fragments;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.testapptradeup.R;
import com.example.testapptradeup.adapters.PhotoAdapter;
import com.example.testapptradeup.models.Listing;
import com.example.testapptradeup.models.User;
import com.example.testapptradeup.utils.SharedPrefsHelper;
import com.example.testapptradeup.viewmodels.PostViewModel;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class PostFragment extends Fragment {

    private static final String TAG = "PostFragment";
    private static final int MAX_IMAGES = 10;

    private PostViewModel viewModel;
    private NavController navController;
    private TextInputEditText etProductTitle, etDescription, etPrice, etLocation, etAdditionalTags;
    private AutoCompleteTextView spinnerCategory;
    private TextView btnPreview, tvUseCurrentLocation, tvPhotoCountHeader;
    private ChipGroup chipGroupCondition, chipGroupTags;
    private Button btnSaveDraft, btnPostListing;
    private RecyclerView recyclerPhotoThumbnails;
    private PhotoAdapter photoAdapter;
    private ProgressBar postProgressBar;
    private SharedPrefsHelper prefsHelper;
    private FusedLocationProviderClient fusedLocationClient;
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private ActivityResultLauncher<String> locationPermissionLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(PostViewModel.class);
        prefsHelper = new SharedPrefsHelper(requireContext());
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        setupActivityResultLaunchers();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_post, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = NavHostFragment.findNavController(this);
        initViews(view);
        setupRecyclerView();
        setupListeners();
        observeViewModel();
    }

    private void initViews(View view) {
        etProductTitle = view.findViewById(R.id.et_product_title);
        etDescription = view.findViewById(R.id.et_description);
        etPrice = view.findViewById(R.id.et_price);
        etLocation = view.findViewById(R.id.et_location);
        etAdditionalTags = view.findViewById(R.id.et_additional_tags);
        spinnerCategory = view.findViewById(R.id.spinner_category);
        btnPreview = view.findViewById(R.id.btn_preview);
        tvUseCurrentLocation = view.findViewById(R.id.tv_use_current_location);
        chipGroupCondition = view.findViewById(R.id.chip_group_condition);
        chipGroupTags = view.findViewById(R.id.chip_group_tags);
        btnSaveDraft = view.findViewById(R.id.btn_save_draft);
        btnPostListing = view.findViewById(R.id.btn_post_listing);
        recyclerPhotoThumbnails = view.findViewById(R.id.recycler_photo_thumbnails);
        tvPhotoCountHeader = view.findViewById(R.id.tv_photo_count_header);
        postProgressBar = view.findViewById(R.id.post_progress_bar);

        String[] categories = {"Điện thoại", "Laptop", "Thời trang", "Đồ gia dụng", "Xe cộ", "Khác"};
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, categories);
        spinnerCategory.setAdapter(categoryAdapter);
    }

    private void setupRecyclerView() {
        photoAdapter = new PhotoAdapter(
                () -> {
                    if (Objects.requireNonNull(viewModel.getSelectedImageUris().getValue()).size() < MAX_IMAGES) {
                        showImagePicker();
                    } else {
                        Toast.makeText(getContext(), "Đã đạt số lượng ảnh tối đa", Toast.LENGTH_SHORT).show();
                    }
                },
                uri -> viewModel.removeImage(uri)
        );
        recyclerPhotoThumbnails.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        recyclerPhotoThumbnails.setAdapter(photoAdapter);
    }

    private void setupListeners() {
        btnPostListing.setOnClickListener(v -> postListing());
        btnPreview.setOnClickListener(v -> showPreview());
        btnSaveDraft.setOnClickListener(v -> Toast.makeText(getContext(), "Chức năng đang phát triển", Toast.LENGTH_SHORT).show());
        tvUseCurrentLocation.setOnClickListener(v -> requestLocation());

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

    // ========== PHẦN SỬA LỖI ==========
    private void observeViewModel() {
        viewModel.getSelectedImageUris().observe(getViewLifecycleOwner(), uris -> {
            photoAdapter.setImageUris(new ArrayList<>(uris));
            tvPhotoCountHeader.setText(String.format(Locale.getDefault(), "Thêm hình ảnh (%d/%d)", uris.size(), MAX_IMAGES));
        });
        viewModel.getImageUploadStates().observe(getViewLifecycleOwner(), states -> {
            photoAdapter.setUploadStates(states);
        });

        viewModel.getPostStatus().observe(getViewLifecycleOwner(), status -> {
            // Sửa: So sánh với các hằng số static trong PostViewModel.PostStatus
            showLoading(status == PostViewModel.PostStatus.LOADING);
            if (status == PostViewModel.PostStatus.SUCCESS) {
                Toast.makeText(getContext(), "Đăng tin thành công!", Toast.LENGTH_LONG).show();
                navController.navigate(R.id.myListingsFragment); // Điều hướng đến trang quản lý
            } else if (status instanceof PostViewModel.PostStatus.Error) {
                String errorMessage = ((PostViewModel.PostStatus.Error) status).message;
                Toast.makeText(getContext(), "Lỗi: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void postListing() {
        if (!validateAllFields()) {
            return;
        }

        User currentUser = prefsHelper.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), "Lỗi: Không tìm thấy người dùng.", Toast.LENGTH_SHORT).show();
            return;
        }

        Listing listing = new Listing();
        listing.setTitle(Objects.requireNonNull(etProductTitle.getText()).toString().trim());
        listing.setPrice(Double.parseDouble(Objects.requireNonNull(etPrice.getText()).toString().trim()));
        listing.setCategoryId(spinnerCategory.getText().toString());
        listing.setDescription(Objects.requireNonNull(etDescription.getText()).toString().trim());
        listing.setLocation(Objects.requireNonNull(etLocation.getText()).toString().trim());
        listing.setCondition(getSelectedCondition());
        listing.setSellerId(currentUser.getId());
        listing.setSellerName(currentUser.getName());
        listing.setStatus("available");

        List<String> tags = new ArrayList<>();
        for (int i = 0; i < chipGroupTags.getChildCount(); i++) {
            Chip chip = (Chip) chipGroupTags.getChildAt(i);
            tags.add(chip.getText().toString());
        }
        listing.setTags(tags);

        // ========== SỬA LỖI Ở ĐÂY ==========
        // Chỉ cần truyền đối tượng listing. ViewModel sẽ tự lấy danh sách URL đã upload.
        viewModel.postListing(listing);
    }
    // ========== KẾT THÚC PHẦN SỬA LỖI ==========

    private void showPreview() {
        if (!validateAllFields()) return;
        Toast.makeText(getContext(), "Chức năng xem trước đang phát triển", Toast.LENGTH_SHORT).show();
    }

    private void addTagToGroup(String tagText) {
        if (chipGroupTags.getChildCount() >= 5) {
            Toast.makeText(getContext(), "Chỉ có thể thêm tối đa 5 thẻ", Toast.LENGTH_SHORT).show();
            return;
        }
        for (int i = 0; i < chipGroupTags.getChildCount(); i++) {
            Chip chip = (Chip) chipGroupTags.getChildAt(i);
            if (chip.getText().toString().equalsIgnoreCase(tagText)) {
                Toast.makeText(getContext(), "Thẻ đã tồn tại", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        Chip chip = (Chip) getLayoutInflater().inflate(R.layout.chip_tag_item, chipGroupTags, false);
        chip.setText(tagText);
        chip.setOnCloseIconClickListener(v -> chipGroupTags.removeView(v));
        chipGroupTags.addView(chip);
    }

    private String getSelectedCondition() {
        int checkedChipId = chipGroupCondition.getCheckedChipId();
        if (checkedChipId == R.id.chip_condition_new) return "new";
        if (checkedChipId == R.id.chip_condition_like_new) return "like_new";
        if (checkedChipId == R.id.chip_condition_used) return "used";
        return "";
    }

    private void requestLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            getCurrentLocation();
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    @SuppressLint("MissingPermission")
    private void getCurrentLocation() {
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> { // Xóa `this`
                    if (location != null) {
                        try {
                            Geocoder geocoder = new Geocoder(getContext(), Locale.getDefault());
                            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                            if (addresses != null && !addresses.isEmpty()) {
                                String address = addresses.get(0).getAddressLine(0);
                                etLocation.setText(address);
                                Toast.makeText(getContext(), "Đã cập nhật vị trí", Toast.LENGTH_SHORT).show();
                            }
                        } catch (IOException e) {
                            Toast.makeText(getContext(), "Lỗi lấy địa chỉ", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(getContext(), "Không thể lấy vị trí. Vui lòng bật GPS.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        imagePickerLauncher.launch(intent);
    }

    private void setupActivityResultLaunchers() {
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        if (result.getData().getClipData() != null) {
                            int count = result.getData().getClipData().getItemCount();
                            int canAdd = MAX_IMAGES - Objects.requireNonNull(viewModel.getSelectedImageUris().getValue()).size();
                            for (int i = 0; i < count && i < canAdd; i++) {
                                viewModel.addImage(result.getData().getClipData().getItemAt(i).getUri());
                            }
                        } else if (result.getData().getData() != null) {
                            viewModel.addImage(result.getData().getData());
                        }
                    }
                });

        locationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        getCurrentLocation();
                    } else {
                        Toast.makeText(getContext(), "Quyền vị trí bị từ chối", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private boolean validateAllFields() {
        if (TextUtils.isEmpty(etProductTitle.getText())) {
            etProductTitle.setError("Vui lòng nhập tiêu đề");
            etProductTitle.requestFocus();
            return false;
        }
        if (TextUtils.isEmpty(etPrice.getText())) {
            etPrice.setError("Vui lòng nhập giá");
            etPrice.requestFocus();
            return false;
        }
        if (TextUtils.isEmpty(spinnerCategory.getText())) {
            spinnerCategory.setError("Vui lòng chọn danh mục");
            spinnerCategory.requestFocus();
            return false;
        }
        if (chipGroupCondition.getCheckedChipId() == View.NO_ID) {
            Toast.makeText(getContext(), "Vui lòng chọn tình trạng sản phẩm", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (TextUtils.isEmpty(etDescription.getText())) {
            etDescription.setError("Vui lòng nhập mô tả");
            etDescription.requestFocus();
            return false;
        }
        if (TextUtils.isEmpty(etLocation.getText())) {
            etLocation.setError("Vui lòng nhập địa điểm");
            etLocation.requestFocus();
            return false;
        }
        if (Objects.requireNonNull(viewModel.getSelectedImageUris().getValue()).isEmpty()) {
            Toast.makeText(getContext(), "Vui lòng thêm ít nhất 1 ảnh", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void showLoading(boolean isLoading) {
        postProgressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnPostListing.setEnabled(!isLoading);
        btnSaveDraft.setEnabled(!isLoading);
    }
}