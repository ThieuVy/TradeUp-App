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
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.Editable;
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
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.testapptradeup.R;
import com.example.testapptradeup.adapters.PhotoAdapter;
import com.example.testapptradeup.models.Category;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class PostFragment extends Fragment {

    private static final int MAX_IMAGES = 10;

    protected PostViewModel viewModel;
    protected NavController navController;
    protected TextInputEditText etProductTitle, etDescription, etPrice, etLocation, etAdditionalTags;
    protected AutoCompleteTextView spinnerCategory;
    protected TextView tvPhotoCountHeader, tvUseCurrentLocation;
    protected ChipGroup chipGroupCondition, chipGroupTags;
    protected Button btnPostListing, btnSaveDraft, btnPreview;
    protected RecyclerView recyclerPhotoThumbnails;
    protected PhotoAdapter photoAdapter;
    protected ProgressBar postProgressBar;
    protected SharedPrefsHelper prefsHelper;
    protected double listingLatitude = 0.0;
    protected double listingLongitude = 0.0;

    private FusedLocationProviderClient fusedLocationClient;
    private final ExecutorService geocodingExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private ActivityResultLauncher<String> locationPermissionLauncher;
    private MainViewModel mainViewModel;

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

    protected void initViews(View view) {
        etProductTitle = view.findViewById(R.id.et_product_title);
        etDescription = view.findViewById(R.id.et_description);
        etPrice = view.findViewById(R.id.et_price);
        etLocation = view.findViewById(R.id.et_location);
        spinnerCategory = view.findViewById(R.id.spinner_category);
        chipGroupCondition = view.findViewById(R.id.chip_group_condition);
        btnPostListing = view.findViewById(R.id.btn_post_listing);
        recyclerPhotoThumbnails = view.findViewById(R.id.recycler_photo_thumbnails);
        tvPhotoCountHeader = view.findViewById(R.id.tv_photo_count_header);
        postProgressBar = view.findViewById(R.id.post_progress_bar);
        btnPreview = view.findViewById(R.id.btn_preview);
        btnSaveDraft = view.findViewById(R.id.btn_save_draft);
        tvUseCurrentLocation = view.findViewById(R.id.tv_use_current_location);
        etAdditionalTags = view.findViewById(R.id.et_additional_tags);
        chipGroupTags = view.findViewById(R.id.chip_group_tags);

        String[] categories = {"Điện thoại", "Laptop", "Thời trang", "Đồ gia dụng", "Xe cộ", "Khác"};
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, categories);
        spinnerCategory.setAdapter(categoryAdapter);
    }

    protected void setupRecyclerView() {
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
        btnPostListing.setOnClickListener(v -> postListing());
        btnPreview.setOnClickListener(v -> showPreview());
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

    protected void observeViewModel() {
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

    private void postListing() {
        if (validateAllFields()) {
            return;
        }

        User currentUser = prefsHelper.getCurrentUser();
        String currentUserId = (FirebaseAuth.getInstance().getCurrentUser() != null) ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        if (currentUser == null || currentUserId == null) {
            Toast.makeText(getContext(), "Lỗi: Phiên đăng nhập không hợp lệ.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Đặt trạng thái loading ngay lập tức trên luồng UI
        viewModel.setLoadingState(true);
        String locationString = Objects.requireNonNull(etLocation.getText()).toString().trim();

        // Thực thi tác vụ I/O (Geocoding) trên luồng nền
        geocodingExecutor.execute(() -> {
            updateCoordinatesFromLocation(locationString);

            // Quay lại luồng chính để tạo đối tượng và gọi ViewModel
            mainThreadHandler.post(() -> {
                Listing listing = buildListingFromUI(currentUser, currentUserId);
                Log.d("PostFragment", "Bắt đầu quá trình đăng tin sau khi Geocoding...");
                viewModel.postListing(listing);
            });
        });
    }

    protected Listing buildListingFromUI(User currentUser, String currentUserId) {
        Editable titleEditable = etProductTitle.getText();
        Editable priceEditable = etPrice.getText();
        Editable descEditable = etDescription.getText();
        Editable locationEditable = etLocation.getText();

        String title = titleEditable != null ? titleEditable.toString().trim() : "";
        String priceStr = priceEditable != null ? priceEditable.toString().trim() : "0";
        String description = descEditable != null ? descEditable.toString().trim() : "";
        String location = locationEditable != null ? locationEditable.toString().trim() : "";

        Listing listing = new Listing();
        listing.setTitle(title);
        listing.setPrice(Double.parseDouble(priceStr));
        listing.setCategory(spinnerCategory.getText().toString());
        listing.setDescription(description);
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
        listing.setLocation(location);
        listing.setLatitude(listingLatitude);
        listing.setLongitude(listingLongitude);
        if (listingLatitude != 0.0 && listingLongitude != 0.0) {
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

        String selectedCategoryName = spinnerCategory.getText().toString(); // Ví dụ: "Laptop"
        String categoryIdToSave = "";

        // Ánh xạ từ TÊN HIỂN THỊ về ID CHUẨN
        switch (selectedCategoryName) {
            case "Điện tử": // Tên này phải khớp với tên trong mảng `categories` bạn tạo
                categoryIdToSave = Category.AppConstants.CATEGORY_ELECTRONICS;
                break;
            case "Laptop":
                categoryIdToSave = Category.AppConstants.CATEGORY_LAPTOPS;
                break;
            case "Thời trang":
                categoryIdToSave = Category.AppConstants.CATEGORY_FASHION;
                break;
            case "Đồ gia dụng":
                categoryIdToSave = Category.AppConstants.CATEGORY_HOME_GOODS;
                break;
            case "Xe cộ":
                categoryIdToSave = Category.AppConstants.CATEGORY_CARS;
                break;
            case "Đồ thể thao":
                categoryIdToSave = Category.AppConstants.CATEGORY_SPORTS;
                break;
            case "Sách":
                categoryIdToSave = Category.AppConstants.CATEGORY_BOOKS;
                break;
            default:
                categoryIdToSave = Category.AppConstants.CATEGORY_OTHER;
                break;
        }

        listing.setCategory(getSelectedCategoryId());
        listing.setCategory(categoryIdToSave);

        return listing;
    }

    private void showPreview() {
        if (validateAllFields()) {
            return;
        }
        User currentUser = prefsHelper.getCurrentUser();
        String currentUserId = (FirebaseAuth.getInstance().getCurrentUser() != null) ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (currentUser == null || currentUserId == null) {
            Toast.makeText(getContext(), "Lỗi: Phiên đăng nhập không hợp lệ.", Toast.LENGTH_SHORT).show();
            return;
        }
        Listing listingToPreview = buildListingFromUI(currentUser, currentUserId);
        PostFragmentDirections.ActionPostFragmentToProductDetailFragment action =
                PostFragmentDirections.actionPostFragmentToProductDetailFragment();
        action.setListingPreview(listingToPreview);
        navController.navigate(action);
    }

    protected void addTagToGroup(String tagText) {
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

    protected void requestLocationPermission() {
        if (getContext() == null) return;

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // Quyền đã được cấp, lấy vị trí ngay
            getCurrentLocation();
        } else if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
            // Quyền đã bị từ chối trước đó, hiển thị dialog giải thích
            new AlertDialog.Builder(requireContext())
                    .setTitle("Yêu cầu quyền vị trí")
                    .setMessage("TradeUp cần truy cập vị trí của bạn để tự động điền thông tin địa điểm, giúp người mua dễ dàng tìm thấy sản phẩm của bạn hơn.")
                    .setPositiveButton("OK", (dialog, which) -> locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION))
                    .setNegativeButton("Hủy", null)
                    .show();
        } else {
            // Lần đầu yêu cầu quyền hoặc người dùng đã chọn "Don't ask again"
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    private void showGoToSettingsDialog() {
        if (getContext() == null) return;
        new AlertDialog.Builder(requireContext())
                .setTitle("Quyền vị trí đã bị từ chối")
                .setMessage("Dường như bạn đã từ chối quyền truy cập vị trí vĩnh viễn. Để sử dụng tính năng này, vui lòng vào Cài đặt và cấp quyền cho ứng dụng.")
                .setPositiveButton("Mở Cài đặt", (dialog, which) -> {
                    // Mở màn hình cài đặt của ứng dụng
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", requireActivity().getPackageName(), null);
                    intent.setData(uri);
                    startActivity(intent);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void updateCoordinatesFromLocation(String locationString) {
        if (getContext() == null || locationString.isEmpty()) return;
        Geocoder geocoder = new Geocoder(getContext(), Locale.getDefault());
        try {
            // Lời gọi mạng này giờ đang ở luồng nền, không gây ANR
            List<Address> addresses = geocoder.getFromLocationName(locationString, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                this.listingLatitude = address.getLatitude();
                this.listingLongitude = address.getLongitude();
                Log.d("PostFragment", "Geocoded in background: " + locationString + " -> lat: " + listingLatitude + ", lon: " + listingLongitude);
            }
        } catch (IOException e) {
            Log.e("PostFragment", "Lỗi Geocoding", e);
        }
    }

    @SuppressLint("MissingPermission")
    private void getCurrentLocation() {
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                this.listingLatitude = location.getLatitude();
                this.listingLongitude = location.getLongitude();

                geocodingExecutor.execute(() -> {
                    try {
                        Geocoder geocoder = new Geocoder(requireContext(), new Locale("vi", "VN"));
                        List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                        if (addresses != null && !addresses.isEmpty()) {
                            String addressLine = addresses.get(0).getAddressLine(0);
                            mainThreadHandler.post(() -> {
                                etLocation.setText(addressLine);
                                Toast.makeText(getContext(), "Đã cập nhật vị trí", Toast.LENGTH_SHORT).show();
                            });
                        }
                    } catch (IOException e) {
                        mainThreadHandler.post(() -> Toast.makeText(getContext(), "Lỗi lấy địa chỉ", Toast.LENGTH_SHORT).show());
                    }
                });
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
                        // Người dùng đã cấp quyền, lấy vị trí
                        getCurrentLocation();
                    } else {
                        // Người dùng đã từ chối
                        // Bây giờ chúng ta kiểm tra xem họ có từ chối vĩnh viễn không
                        if (!shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                            // Đây là trường hợp từ chối vĩnh viễn
                            showGoToSettingsDialog();
                        } else {
                            // Chỉ từ chối một lần, không làm gì thêm
                            Toast.makeText(getContext(), "Quyền vị trí đã bị từ chối.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    protected boolean validateAllFields() {
        if (TextUtils.isEmpty(etProductTitle.getText())) {
            etProductTitle.setError("Vui lòng nhập tiêu đề");
            etProductTitle.requestFocus();
            return true;
        }
        if (TextUtils.isEmpty(etPrice.getText())) {
            etPrice.setError("Vui lòng nhập giá");
            etPrice.requestFocus();
            return true;
        }
        if (TextUtils.isEmpty(spinnerCategory.getText())) {
            spinnerCategory.setError("Vui lòng chọn danh mục");
            spinnerCategory.requestFocus();
            return true;
        }
        if (chipGroupCondition.getCheckedChipId() == View.NO_ID) {
            Toast.makeText(getContext(), "Vui lòng chọn tình trạng sản phẩm", Toast.LENGTH_SHORT).show();
            return true;
        }
        if (TextUtils.isEmpty(etDescription.getText())) {
            etDescription.setError("Vui lòng nhập mô tả");
            etDescription.requestFocus();
            return true;
        }
        if (TextUtils.isEmpty(etLocation.getText())) {
            etLocation.setError("Vui lòng nhập địa điểm");
            etLocation.requestFocus();
            return true;
        }
        if (viewModel.getSelectedImageUris().getValue() == null || viewModel.getSelectedImageUris().getValue().isEmpty()) {
            Toast.makeText(getContext(), "Vui lòng thêm ít nhất 1 ảnh", Toast.LENGTH_SHORT).show();
            return true;
        }
        return false;
    }

    protected void showLoading(boolean isLoading) {
        postProgressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnPostListing.setEnabled(!isLoading);
    }

    /**
     * Chuyển đổi tên danh mục người dùng chọn trên UI thành ID chuẩn để lưu vào DB.
     * @return ID chuẩn của danh mục (ví dụ: "electronics").
     */
    private String getSelectedCategoryId() {
        if (spinnerCategory == null || spinnerCategory.getText() == null) {
            return Category.AppConstants.CATEGORY_OTHER;
        }

        String selectedName = spinnerCategory.getText().toString();
        switch (selectedName) {
            case "Điện thoại": return Category.AppConstants.CATEGORY_ELECTRONICS;
            case "Laptop": return Category.AppConstants.CATEGORY_LAPTOPS;
            case "Thời trang": return Category.AppConstants.CATEGORY_FASHION;
            case "Đồ gia dụng": return Category.AppConstants.CATEGORY_HOME_GOODS;
            case "Xe cộ": return Category.AppConstants.CATEGORY_CARS;
            case "Đồ thể thao": return Category.AppConstants.CATEGORY_SPORTS;
            case "Sách": return Category.AppConstants.CATEGORY_BOOKS;
            default: return Category.AppConstants.CATEGORY_OTHER;
        }
    }
}