package com.example.testapptradeup.fragments;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.testapptradeup.R;
import com.example.testapptradeup.models.Listing;
import com.example.testapptradeup.models.User;
import com.example.testapptradeup.utils.SharedPrefsHelper;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class PostFragment extends Fragment implements LocationListener {

    private static final String TAG = "PostFragment";
    private static final int MAX_IMAGES = 5; // Giảm số lượng ảnh tối đa vì Base64 rất nặng
    private static final int LOCATION_TIMEOUT_MS = 10000;

    // UI Components
    private TextInputEditText etProductTitle, etDescription, etPrice, etItemBehavior, etAdditionalTags;
    private AutoCompleteTextView spinnerCategory;
    private TextView tvLocation, tvUseCurrentLocation, btnPreview, tvCharacterCount, tvPhotoCount;
    private Button btnConditionNew, btnConditionLikeNew, btnConditionUsed;
    private Button btnSaveDraft, btnPostListing;
    private ChipGroup chipGroupTags;
    private CardView cardAddPhotos;
    private LinearLayout layoutPhotoThumbnails;
    private ImageView imgAddPhotoPlaceholder;
    private ProgressBar postProgressBar;

    // Data
    private List<Uri> selectedImageUris;
    private String selectedCondition = "";
    private List<String> tagsList;
    private String selectedLocation = "";

    // Location
    private LocationManager locationManager;
    private Geocoder geocoder;
    private boolean isLocationRequestActive = false;

    // Firebase
    private FirebaseFirestore db;
    private SharedPrefsHelper prefsHelper;

    // Categories
    private final String[] categories = {
            "Điện thoại & Phụ kiện", "Máy tính & Laptop", "Thời trang Nam",
            "Thời trang Nữ", "Đồ gia dụng", "Xe cộ", "Sách & Văn phòng phẩm",
            "Thiết bị điện tử", "Khác"
    };

    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private ActivityResultLauncher<String> locationPermissionLauncher;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_post, container, false);

        db = FirebaseFirestore.getInstance();
        prefsHelper = new SharedPrefsHelper(requireContext());

        initViews(view);
        initData();
        setupListeners();
        setupActivityResultLaunchers();
        loadDraft();

        return view;
    }

    private void initViews(View view) {
        etProductTitle = view.findViewById(R.id.et_product_title);
        etDescription = view.findViewById(R.id.et_description);
        etPrice = view.findViewById(R.id.et_price);
        etItemBehavior = view.findViewById(R.id.et_item_behavior);
        etAdditionalTags = view.findViewById(R.id.et_additional_tags);
        spinnerCategory = view.findViewById(R.id.spinner_category);
        tvLocation = view.findViewById(R.id.tv_location);
        tvUseCurrentLocation = view.findViewById(R.id.tv_use_current_location);
        btnPreview = view.findViewById(R.id.btn_preview);
        tvCharacterCount = view.findViewById(R.id.tv_character_count);
        btnConditionNew = view.findViewById(R.id.btn_condition_new);
        btnConditionLikeNew = view.findViewById(R.id.btn_condition_like_new);
        btnConditionUsed = view.findViewById(R.id.btn_condition_used);
        btnSaveDraft = view.findViewById(R.id.btn_save_draft);
        btnPostListing = view.findViewById(R.id.btn_post_listing);
        chipGroupTags = view.findViewById(R.id.chip_group_tags);
        cardAddPhotos = view.findViewById(R.id.card_add_photos);
        layoutPhotoThumbnails = view.findViewById(R.id.layout_photo_thumbnails);
        imgAddPhotoPlaceholder = view.findViewById(R.id.img_add_photo_placeholder);
        tvPhotoCount = view.findViewById(R.id.tv_photo_count);
        postProgressBar = view.findViewById(R.id.post_progress_bar);

        locationManager = (LocationManager) requireContext().getSystemService(Context.LOCATION_SERVICE);
        geocoder = new Geocoder(requireContext(), Locale.getDefault());
    }

    private void initData() {
        selectedImageUris = new ArrayList<>();
        tagsList = new ArrayList<>();
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, categories);
        spinnerCategory.setAdapter(categoryAdapter);
        setupCharacterCounter();
        updatePhotoCountText();
    }

    private void setupCharacterCounter() {
        etDescription.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @SuppressLint("SetTextI18n")
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (tvCharacterCount != null) {
                    tvCharacterCount.setText(s.length() + "/5000");
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupListeners() {
        cardAddPhotos.setOnClickListener(v -> showImagePicker());
        tvUseCurrentLocation.setOnClickListener(v -> getCurrentLocation());
        btnConditionNew.setOnClickListener(v -> selectCondition("new", btnConditionNew));
        btnConditionLikeNew.setOnClickListener(v -> selectCondition("like_new", btnConditionLikeNew));
        btnConditionUsed.setOnClickListener(v -> selectCondition("used", btnConditionUsed));
        btnPreview.setOnClickListener(v -> previewProduct());
        btnSaveDraft.setOnClickListener(v -> saveDraft());

        // THAY ĐỔI: Gọi đúng phương thức postListing()
        btnPostListing.setOnClickListener(v -> postListing());

        etAdditionalTags.setOnEditorActionListener((v, actionId, event) -> {
            String tag = Objects.requireNonNull(etAdditionalTags.getText()).toString().trim();
            if (!tag.isEmpty()) {
                addTag(tag);
                etAdditionalTags.setText("");
                return true;
            }
            return false;
        });
    }

    private void selectCondition(String condition, @Nullable Button selectedButton) {
        selectedCondition = condition;
        setConditionButtonState(btnConditionNew, "new".equals(condition));
        setConditionButtonState(btnConditionLikeNew, "like_new".equals(condition));
        setConditionButtonState(btnConditionUsed, "used".equals(condition));
    }

    private void setConditionButtonState(Button button, boolean isSelected) {
        if(button == null) return;
        button.setSelected(isSelected);
        if (isSelected) {
            button.setBackgroundResource(R.drawable.btn_condition_selected);
            button.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white));
        } else {
            button.setBackgroundResource(R.drawable.btn_condition_normal);
            button.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
        }
    }


    private void addTag(String tagText) {
        if (tagsList.contains(tagText.toLowerCase(Locale.getDefault())) || tagText.trim().isEmpty()) return;
        tagsList.add(tagText.toLowerCase(Locale.getDefault()));
        Chip chip = (Chip) LayoutInflater.from(requireContext()).inflate(R.layout.chip_tag_item, chipGroupTags, false);
        chip.setText(tagText);
        chip.setCloseIconVisible(true);
        chip.setOnCloseIconClickListener(v -> {
            chipGroupTags.removeView(chip);
            tagsList.remove(tagText.toLowerCase(Locale.getDefault()));
        });
        chipGroupTags.addView(chip);
    }

    private void setupActivityResultLaunchers() {
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        // Cho phép chọn nhiều ảnh
                        if (result.getData().getClipData() != null) {
                            int count = result.getData().getClipData().getItemCount();
                            int canAdd = MAX_IMAGES - selectedImageUris.size();
                            for (int i = 0; i < count && i < canAdd; i++) {
                                Uri imageUri = result.getData().getClipData().getItemAt(i).getUri();
                                handleAddImage(imageUri);
                            }
                        } else if (result.getData().getData() != null) { // Xử lý chọn một ảnh
                            Uri imageUri = result.getData().getData();
                            handleAddImage(imageUri);
                        }
                    }
                }
        );

        locationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) getCurrentLocation();
                    else Toast.makeText(requireContext(), "Quyền truy cập vị trí bị từ chối", Toast.LENGTH_SHORT).show();
                }
        );
    }

    @SuppressLint("IntentReset")
    private void showImagePicker() {
        if (selectedImageUris.size() >= MAX_IMAGES) {
            Toast.makeText(requireContext(), "Chỉ có thể thêm tối đa " + MAX_IMAGES + " ảnh.", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true); // Cho phép chọn nhiều ảnh
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    private void handleAddImage(Uri imageUri) {
        if (selectedImageUris.size() < MAX_IMAGES) {
            selectedImageUris.add(imageUri);
            addThumbnailToLayout(imageUri);
            updatePhotoCountText();
        }
    }

    private void addThumbnailToLayout(Uri imageUri) {
        CardView thumbnailCard = (CardView) LayoutInflater.from(requireContext())
                .inflate(R.layout.thumbnail_image_item, layoutPhotoThumbnails, false);
        ImageView thumbnailImage = thumbnailCard.findViewById(R.id.thumbnail_image);
        ImageView closeButton = thumbnailCard.findViewById(R.id.thumbnail_remove_button);

        Glide.with(this).load(imageUri).centerCrop().into(thumbnailImage);

        closeButton.setOnClickListener(v -> {
            selectedImageUris.remove(imageUri);
            layoutPhotoThumbnails.removeView(thumbnailCard);
            updatePhotoCountText();
        });

        layoutPhotoThumbnails.addView(thumbnailCard);
    }

    @SuppressLint("SetTextI18n")
    private void updatePhotoCountText() {
        int count = selectedImageUris.size();
        tvPhotoCount.setText("Thêm ảnh (" + count + "/" + MAX_IMAGES + ")");
        imgAddPhotoPlaceholder.setVisibility(count == 0 ? View.VISIBLE : View.GONE);
        // Có thể ẩn luôn text nếu đã có ảnh
        tvPhotoCount.setVisibility(count == 0 ? View.VISIBLE : View.GONE);
    }


    @SuppressLint("MissingPermission")
    private void getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
            return;
        }
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) && !locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            Toast.makeText(requireContext(), "Vui lòng bật GPS hoặc kết nối mạng", Toast.LENGTH_SHORT).show();
            return;
        }
        isLocationRequestActive = true;
        tvUseCurrentLocation.setEnabled(false);
        Toast.makeText(requireContext(), "Đang lấy vị trí...", Toast.LENGTH_SHORT).show();
        locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, this, Looper.getMainLooper());
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (isLocationRequestActive) {
                isLocationRequestActive = false;
                locationManager.removeUpdates(this);
                tvUseCurrentLocation.setEnabled(true);
                Toast.makeText(requireContext(), "Không thể lấy vị trí, thử lại sau", Toast.LENGTH_SHORT).show();
            }
        }, LOCATION_TIMEOUT_MS);
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        if (isLocationRequestActive) {
            isLocationRequestActive = false;
            locationManager.removeUpdates(this);
            tvUseCurrentLocation.setEnabled(true);
            try {
                List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                if (addresses != null && !addresses.isEmpty()) {
                    Address address = addresses.get(0);
                    selectedLocation = address.getAddressLine(0);
                    tvLocation.setText(selectedLocation);
                    tvLocation.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
                    Toast.makeText(requireContext(), "Đã cập nhật vị trí", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(requireContext(), "Không tìm thấy địa chỉ", Toast.LENGTH_SHORT).show();
                }
            } catch (IOException e) {
                Log.e(TAG, "Lỗi lấy địa chỉ", e);
            }
        }
    }


    private void saveDraft() {
        SharedPreferences prefs = requireContext().getSharedPreferences("ProductDraft", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("title", Objects.requireNonNull(etProductTitle.getText()).toString().trim());
        editor.putString("category", spinnerCategory.getText().toString().trim());
        editor.putString("condition", selectedCondition);
        editor.putString("description", Objects.requireNonNull(etDescription.getText()).toString().trim());
        editor.putString("price", Objects.requireNonNull(etPrice.getText()).toString().trim());
        editor.putString("location", selectedLocation);
        editor.putString("itemBehavior", Objects.requireNonNull(etItemBehavior.getText()).toString().trim());
        editor.putString("tags", TextUtils.join(",", tagsList));
        editor.apply();
        Toast.makeText(requireContext(), "Đã lưu nháp (không bao gồm ảnh)", Toast.LENGTH_SHORT).show();
    }

    private void loadDraft() {
        SharedPreferences prefs = requireContext().getSharedPreferences("ProductDraft", Context.MODE_PRIVATE);
        etProductTitle.setText(prefs.getString("title", ""));
        spinnerCategory.setText(prefs.getString("category", ""), false);
        etDescription.setText(prefs.getString("description", ""));
        etPrice.setText(prefs.getString("price", ""));
        selectedLocation = prefs.getString("location", "");
        if (!selectedLocation.isEmpty()) {
            tvLocation.setText(selectedLocation);
            tvLocation.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
        }
        etItemBehavior.setText(prefs.getString("itemBehavior", ""));
        selectCondition(prefs.getString("condition", ""), null);
        String loadedTags = prefs.getString("tags", "");
        if (!loadedTags.isEmpty()) {
            tagsList.clear();
            chipGroupTags.removeAllViews();
            String[] tagsArray = loadedTags.split(",");
            for (String tag : tagsArray) {
                if (!tag.trim().isEmpty()) addTag(tag.trim());
            }
        }
    }

    private void clearDraft() {
        requireContext().getSharedPreferences("ProductDraft", Context.MODE_PRIVATE).edit().clear().apply();
    }

    private void previewProduct() {
        if (validateAllFields()) return;
        showPreviewDialog();
    }

    private void postListing() {
        if (validateAllFields()) return;
        showLoading(true);
        // Chuyển đổi ảnh sang Base64 trên một luồng nền để không làm treo UI
        new Thread(() -> {
            List<String> base64Images = new ArrayList<>();
            try {
                for (Uri imageUri : selectedImageUris) {
                    String base64String = convertUriToBase64(requireContext(), imageUri);
                    if (base64String != null) {
                        base64Images.add(base64String);
                    } else {
                        throw new IOException("Không thể chuyển đổi ảnh: " + imageUri.getPath());
                    }
                }
                // Quay lại UI thread để lưu vào Firestore
                new Handler(Looper.getMainLooper()).post(() -> saveListingToFirestore(base64Images));
            } catch (Exception e) {
                Log.e(TAG, "Lỗi khi chuyển đổi ảnh", e);
                new Handler(Looper.getMainLooper()).post(() -> {
                    Toast.makeText(getContext(), "Lỗi xử lý ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    showLoading(false);
                });
            }
        }).start();
    }

    @Nullable
    private String convertUriToBase64(Context context, Uri imageUri) throws IOException {
        InputStream imageStream = context.getContentResolver().openInputStream(imageUri);
        if (imageStream == null) throw new FileNotFoundException("Không thể mở luồng từ Uri: " + imageUri);
        Bitmap bitmap = BitmapFactory.decodeStream(imageStream);
        Bitmap resizedBitmap = resizeBitmap(bitmap);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 75, outputStream);
        byte[] imageBytes = outputStream.toByteArray();
        return Base64.encodeToString(imageBytes, Base64.DEFAULT);
    }

    private Bitmap resizeBitmap(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        float bitmapRatio = (float) width / (float) height;
        if (bitmapRatio > 1) {
            width = 800;
            height = (int) (width / bitmapRatio);
        } else {
            height = 800;
            width = (int) (height * bitmapRatio);
        }
        return Bitmap.createScaledBitmap(bitmap, width, height, true);
    }

    private void saveListingToFirestore(List<String> base64ImageStrings) {
        User currentUser = prefsHelper.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(requireContext(), "Lỗi: Không tìm thấy người dùng.", Toast.LENGTH_LONG).show();
            showLoading(false);
            return;
        }

        String title = Objects.requireNonNull(etProductTitle.getText()).toString().trim();
        String description = Objects.requireNonNull(etDescription.getText()).toString().trim();
        double price = Double.parseDouble(Objects.requireNonNull(etPrice.getText()).toString().replace(",", ""));
        String categoryId = spinnerCategory.getText().toString().trim();

        Listing newListing = new Listing(
                title, description, price, base64ImageStrings,
                selectedLocation, categoryId, currentUser.getId(),
                currentUser.getName(), selectedCondition, true
        );
        newListing.setTimePosted(new Date()); // @ServerTimestamp không hoạt động khi tạo object ở client, cần set thủ công.

        db.collection("listings")
                .add(newListing)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(requireContext(), "Đăng sản phẩm thành công!", Toast.LENGTH_LONG).show();
                    clearDraft();
                    resetForm();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi đăng tin lên Firestore", e);
                    String errorMessage = "Lỗi đăng sản phẩm: " + e.getMessage();
                    if (e.getMessage() != null && e.getMessage().contains("ENTITY_TOO_LARGE")) {
                        errorMessage = "Lỗi: Tổng kích thước ảnh quá lớn. Vui lòng chọn ít ảnh hơn.";
                    }
                    Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show();
                })
                .addOnCompleteListener(task -> showLoading(false));
    }

    private void showLoading(boolean isLoading) {
        postProgressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnPostListing.setEnabled(!isLoading);
        btnSaveDraft.setEnabled(!isLoading);
        btnPostListing.setText(isLoading ? "Đang xử lý..." : "Đăng tin");
    }

    @SuppressLint("DefaultLocale")
    private void showPreviewDialog() {
        String title = Objects.requireNonNull(etProductTitle.getText()).toString().trim();
        String category = spinnerCategory.getText().toString().trim();
        String description = Objects.requireNonNull(etDescription.getText()).toString().trim();
        double price = TextUtils.isEmpty(etPrice.getText()) ? 0 : Double.parseDouble(etPrice.getText().toString().trim());
        String location = selectedLocation;
        int imageCount = selectedImageUris.size();
        String previewText = String.format(
                "Tiêu đề: %s\n\nDanh mục: %s\n\nTình trạng: %s\n\nMô tả: %s\n\nGiá: %,.0f VNĐ\n\nĐịa điểm: %s\n\nSố ảnh: %d",
                title, category, getConditionTextForPreview(selectedCondition), description, price, location, imageCount);

        new AlertDialog.Builder(requireContext())
                .setTitle("Xem trước sản phẩm")
                .setMessage(previewText)
                .setPositiveButton("Đóng", null)
                .setNegativeButton("Đăng tin", (dialog, which) -> postListing())
                .show();
    }

    private String getConditionTextForPreview(String conditionKey) {
        switch (conditionKey) {
            case "new": return "Mới";
            case "like_new": return "Như mới";
            case "used": return "Đã sử dụng";
            default: return "Không xác định";
        }
    }

    @SuppressLint("SetTextI18n")
    private void resetForm() {
        etProductTitle.setText("");
        spinnerCategory.setText("", false);
        etDescription.setText("");
        tvCharacterCount.setText("0/5000");
        etPrice.setText("");
        etItemBehavior.setText("");
        etAdditionalTags.setText("");
        selectedLocation = "";
        tvLocation.setText("Thêm địa điểm");
        tvLocation.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
        selectCondition("", null);
        selectedImageUris.clear();
        layoutPhotoThumbnails.removeAllViews();
        updatePhotoCountText();
        tagsList.clear();
        chipGroupTags.removeAllViews();
    }

    private boolean validateAllFields() {
        if (TextUtils.isEmpty(etProductTitle.getText())) {
            etProductTitle.setError("Vui lòng nhập tiêu đề"); return true;
        }
        if (TextUtils.isEmpty(spinnerCategory.getText())) {
            spinnerCategory.setError("Vui lòng chọn danh mục"); return true;
        }
        if (TextUtils.isEmpty(selectedCondition)) {
            Toast.makeText(requireContext(), "Vui lòng chọn tình trạng sản phẩm", Toast.LENGTH_SHORT).show(); return true;
        }
        if (TextUtils.isEmpty(etDescription.getText())) {
            etDescription.setError("Vui lòng nhập mô tả"); return true;
        }
        if (TextUtils.isEmpty(etPrice.getText())) {
            etPrice.setError("Vui lòng nhập giá"); return true;
        }
        if (TextUtils.isEmpty(selectedLocation)) {
            Toast.makeText(requireContext(), "Vui lòng thêm địa điểm", Toast.LENGTH_SHORT).show(); return true;
        }
        if (selectedImageUris.isEmpty()) {
            Toast.makeText(requireContext(), "Vui lòng thêm ít nhất 1 ảnh", Toast.LENGTH_SHORT).show(); return true;
        }
        return false;
    }

    // Các phương thức không sử dụng của LocationListener
    @Override public void onStatusChanged(String provider, int status, Bundle extras) {}
    @Override public void onProviderEnabled(@NonNull String provider) {}
    @Override public void onProviderDisabled(@NonNull String provider) {}

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (locationManager != null && isLocationRequestActive) {
            locationManager.removeUpdates(this);
        }
    }
}