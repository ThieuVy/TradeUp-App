package com.example.testapptradeup.fragments;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
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
import com.example.testapptradeup.viewmodels.MainViewModel;
import com.example.testapptradeup.viewmodels.PostViewModel;
import com.firebase.geofire.GeoFireUtils;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

public class PostFragment extends Fragment {

    private static final int MAX_IMAGES = 10;

    private PostViewModel viewModel;
    private NavController navController;
    private TextInputEditText etProductTitle, etDescription, etPrice, etLocation, etAdditionalTags;
    private AutoCompleteTextView spinnerCategory;
    private TextView tvPhotoCountHeader;
    private ChipGroup chipGroupCondition, chipGroupTags;
    private Button btnPostListing, btnSaveDraft, btnPreview;
    private TextView tvUseCurrentLocation;
    private RecyclerView recyclerPhotoThumbnails;
    private PhotoAdapter photoAdapter;
    private ProgressBar postProgressBar;
    private SharedPrefsHelper prefsHelper;
    private FusedLocationProviderClient fusedLocationClient;
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private ActivityResultLauncher<String> locationPermissionLauncher;
    private MainViewModel mainViewModel;
    private double listingLatitude = 0.0;
    private double listingLongitude = 0.0;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(PostViewModel.class);
        mainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
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
        chipGroupCondition = view.findViewById(R.id.chip_group_condition);
        btnPostListing = view.findViewById(R.id.btn_post_listing);
        recyclerPhotoThumbnails = view.findViewById(R.id.recycler_photo_thumbnails);
        tvPhotoCountHeader = view.findViewById(R.id.tv_photo_count_header);
        postProgressBar = view.findViewById(R.id.post_progress_bar);
        btnPreview = view.findViewById(R.id.btn_preview);
        btnSaveDraft = view.findViewById(R.id.btn_save_draft);
        tvUseCurrentLocation = view.findViewById(R.id.tv_use_current_location);
        chipGroupTags = view.findViewById(R.id.chip_group_tags);

        String[] categories = {"Điện thoại", "Laptop", "Thời trang", "Đồ gia dụng", "Xe cộ", "Khác"};
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, categories);
        spinnerCategory.setAdapter(categoryAdapter);
    }

    private void setupRecyclerView() {
        photoAdapter = new PhotoAdapter(
                () -> {
                    if (viewModel.getSelectedImageUris().getValue() != null && viewModel.getSelectedImageUris().getValue().size() < MAX_IMAGES) {
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
        btnPostListing.setOnClickListener(v -> postListing(false));
        btnPreview.setOnClickListener(v -> postListing(true)); // Sửa ở đây
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

    private void observeViewModel() {
        viewModel.getSelectedImageUris().observe(getViewLifecycleOwner(), uris -> {
            photoAdapter.setImageUris(new ArrayList<>(uris));
            tvPhotoCountHeader.setText(String.format(Locale.getDefault(), "Thêm hình ảnh (%d/%d)", uris.size(), MAX_IMAGES));
        });

        viewModel.getPostStatus().observe(getViewLifecycleOwner(), status -> {
            showLoading(status.equals(PostViewModel.PostStatus.LOADING));

            if (status instanceof PostViewModel.PostStatus.SUCCESS) {
                Listing postedListing = ((PostViewModel.PostStatus.SUCCESS) status).listing;
                Toast.makeText(getContext(), "Đăng tin thành công!", Toast.LENGTH_LONG).show();
                mainViewModel.onNewListingPosted(postedListing);
                navController.popBackStack();
            } else if (status instanceof PostViewModel.PostStatus.Error) {
                String errorMessage = ((PostViewModel.PostStatus.Error) status).message;
                Toast.makeText(getContext(), "Lỗi: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void postListing(boolean isPreview) {
        if (!validateAllFields()) {
            return;
        }

        User currentUser = prefsHelper.getCurrentUser();
        String currentUserId = (FirebaseAuth.getInstance().getCurrentUser() != null) ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        if (currentUser == null || currentUserId == null) {
            Toast.makeText(getContext(), "Lỗi: Phiên đăng nhập không hợp lệ.", Toast.LENGTH_SHORT).show();
            return;
        }

        Listing listing = buildListingFromUI(currentUser, currentUserId);

        if (isPreview) {
            // Chuyển sang màn hình xem trước
            // PostFragmentDirections.ActionPostFragmentToProductDetailFragment action =
            // PostFragmentDirections.actionPostFragmentToProductDetailFragment(listing);
            // navController.navigate(action);
            Toast.makeText(getContext(), "Chức năng xem trước đang phát triển", Toast.LENGTH_SHORT).show();

        } else {
            // Bắt đầu quá trình đăng tin
            Log.d("PostFragment", "Bắt đầu quá trình đăng tin...");
            viewModel.postListing(listing);
        }
    }

    // ========== HÀM ĐÃ ĐƯỢC SỬA LỖI ==========
    private Listing buildListingFromUI(User currentUser, String currentUserId) {
        Listing listing = new Listing();
        listing.setTitle(Objects.requireNonNull(etProductTitle.getText()).toString().trim());
        listing.setPrice(Double.parseDouble(Objects.requireNonNull(etPrice.getText()).toString().trim()));
        listing.setCategoryId(spinnerCategory.getText().toString());
        listing.setDescription(Objects.requireNonNull(etDescription.getText()).toString().trim());
        listing.setCondition(getSelectedCondition());
        listing.setSellerId(currentUserId);
        listing.setSellerName(currentUser.getName());
        listing.setStatus("available");
        listing.setViews(0);
        listing.setOffersCount(0);
        listing.setRating(0.0f);
        listing.setReviewCount(0);
        listing.setSold(false);
        listing.setNegotiable(true);

        // <<< BẮT ĐẦU SỬA LỖI >>>
        // 1. Lấy chuỗi địa chỉ từ EditText và lưu vào một biến.
        String locationString = Objects.requireNonNull(etLocation.getText()).toString().trim();
        // 2. Gán chuỗi địa chỉ này cho đối tượng listing.
        listing.setLocation(locationString);
        // 3. Sử dụng biến đã lưu để gọi hàm updateCoordinatesFromLocation.
        updateCoordinatesFromLocation(locationString);
        // <<< KẾT THÚC SỬA LỖI >>>

        listing.setLatitude(listingLatitude);
        listing.setLongitude(listingLongitude);
        if(listingLatitude != 0.0 && listingLongitude != 0.0) {
            String hash = GeoFireUtils.getGeoHashForLocation(new GeoLocation(listingLatitude, listingLongitude));
            listing.setGeohash(hash);
        }

        List<String> tags = new ArrayList<>();
        for (int i = 0; i < chipGroupTags.getChildCount(); i++) {
            Chip chip = (Chip) chipGroupTags.getChildAt(i);
            tags.add(chip.getText().toString());
        }
        listing.setTags(tags);

        List<Uri> imageUris = viewModel.getSelectedImageUris().getValue();
        if (imageUris != null) {
            listing.setImageUrls(imageUris.stream().map(Uri::toString).collect(Collectors.toList()));
        }

        return listing;
    }

    private void showPreview() {
        if (validateAllFields()) {
            Toast.makeText(getContext(), "Chức năng xem trước đang phát triển", Toast.LENGTH_SHORT).show();
        }
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
        if (requireContext().checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            getCurrentLocation();
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    private void updateCoordinatesFromLocation(String locationString) {
        if (getContext() == null || locationString.isEmpty()) return;
        Geocoder geocoder = new Geocoder(getContext(), Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocationName(locationString, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                listingLatitude = address.getLatitude();
                listingLongitude = address.getLongitude();
                Log.d("PostFragment", "Geocoded: " + locationString + " -> lat: " + listingLatitude + ", lon: " + listingLongitude);
            } else {
                Log.w("PostFragment", "Không tìm thấy tọa độ cho địa chỉ: " + locationString);
            }
        } catch (IOException e) {
            Log.e("PostFragment", "Lỗi Geocoding", e);
        }
    }

    @SuppressLint("MissingPermission")
    private void getCurrentLocation() {
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        // === SỬA ĐỔI: Lưu lại tọa độ ===
                        listingLatitude = location.getLatitude();
                        listingLongitude = location.getLongitude();
                        // ============================
                        try {
                            Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
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
                            int canAdd = MAX_IMAGES - (viewModel.getSelectedImageUris().getValue() != null ? viewModel.getSelectedImageUris().getValue().size() : 0);
                            for (int i = 0; i < count && i < canAdd; i++) {
                                viewModel.addImage(result.getData().getClipData().getItemAt(i).getUri());
                            }
                        } else if (result.getData().getData() != null) {
                            if (viewModel.getSelectedImageUris().getValue() == null || viewModel.getSelectedImageUris().getValue().size() < MAX_IMAGES) {
                                viewModel.addImage(result.getData().getData());
                            }
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
        if (viewModel.getSelectedImageUris().getValue() == null || viewModel.getSelectedImageUris().getValue().isEmpty()) {
            Toast.makeText(getContext(), "Vui lòng thêm ít nhất 1 ảnh", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void showLoading(boolean isLoading) {
        postProgressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnPostListing.setEnabled(!isLoading);
    }
}