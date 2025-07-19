package com.example.testapptradeup.fragments;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;
import com.example.testapptradeup.R;
import com.example.testapptradeup.models.User;
import com.example.testapptradeup.viewmodels.EditProfileViewModel;
import com.example.testapptradeup.viewmodels.MainViewModel;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Objects;

public class EditProfileFragment extends Fragment {

    private EditProfileViewModel editProfileViewModel;
    private MainViewModel mainViewModel;
    private NavController navController;

    // UI Components
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private ImageView editProfileImage;
    private Button btnChangeProfileImage;
    private ProgressBar profileSaveProgress;
    private ImageButton btnBack;
    private TextInputEditText editDisplayName, editPhoneNumber, editUserBio, editUserAddress, editEmailAddress;
    private Button btnSaveProfile;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // --- SỬA LỖI 1: Sử dụng đúng tên biến đã khai báo ---
        editProfileViewModel = new ViewModelProvider(this).get(EditProfileViewModel.class);
        mainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri selectedImageUri = result.getData().getData();
                        if (selectedImageUri != null) {
                            Glide.with(this).load(selectedImageUri).circleCrop().into(editProfileImage);
                            // Gọi ViewModel để xử lý việc tải ảnh
                            editProfileViewModel.uploadAndSaveProfilePicture(selectedImageUri);
                        }
                    }
                }
        );
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_edit_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(view);

        initViews(view);
        setupListeners();
        observeViewModels();
    }

    private void initViews(View view) {
        btnBack = view.findViewById(R.id.btn_back);
        editProfileImage = view.findViewById(R.id.edit_profile_image);
        btnChangeProfileImage = view.findViewById(R.id.btn_change_profile_image);
        editDisplayName = view.findViewById(R.id.edit_display_name);
        editEmailAddress = view.findViewById(R.id.edit_email_address);
        editPhoneNumber = view.findViewById(R.id.edit_phone_number);
        editUserBio = view.findViewById(R.id.edit_user_bio);
        editUserAddress = view.findViewById(R.id.edit_user_address);
        btnSaveProfile = view.findViewById(R.id.btn_save_profile);
        profileSaveProgress = view.findViewById(R.id.profile_save_progress);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> navController.popBackStack());

        // --- SỬA LỖI 3: Hoàn thiện logic trong listeners ---
        btnSaveProfile.setOnClickListener(v -> saveProfileChanges());

        View.OnClickListener pickImageListener = v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            imagePickerLauncher.launch(intent);
        };
        editProfileImage.setOnClickListener(pickImageListener);
        btnChangeProfileImage.setOnClickListener(pickImageListener);
    }

    private void observeViewModels() {
        // Lấy dữ liệu người dùng ban đầu từ MainViewModel để điền vào form
        mainViewModel.getCurrentUser().observe(getViewLifecycleOwner(), this::populateUI);

        // Lắng nghe trạng thái loading từ ViewModel
        editProfileViewModel.isLoading().observe(getViewLifecycleOwner(), this::showLoading);

        // Lắng nghe kết quả LƯU THÔNG TIN TEXT
        editProfileViewModel.getSaveStatus().observe(getViewLifecycleOwner(), success -> {
            if (Boolean.TRUE.equals(success)) {
                Toast.makeText(getContext(), "Cập nhật thông tin thành công!", Toast.LENGTH_SHORT).show();
                // Cập nhật MainViewModel với user mới
                mainViewModel.setCurrentUser(editProfileViewModel.getUpdatedUser().getValue());
                navController.popBackStack(); // Quay về trang profile sau khi lưu thành công
            } else if (Boolean.FALSE.equals(success)) {
                // Chỉ hiển thị toast khi `success` là false, không phải null
                Toast.makeText(getContext(), "Cập nhật thông tin thất bại.", Toast.LENGTH_SHORT).show();
            }
        });

        // Lắng nghe kết quả CẬP NHẬT ẢNH ĐẠI DIỆN
        editProfileViewModel.getUpdateImageResult().observe(getViewLifecycleOwner(), result -> {
            if (result == null) return;

            if (result.isSuccess()) {
                Toast.makeText(getContext(), "Cập nhật ảnh đại diện thành công!", Toast.LENGTH_SHORT).show();
                String newUrl = result.getData();
                // Cập nhật MainViewModel để UI trên toàn app được đồng bộ
                User currentUser = mainViewModel.getCurrentUser().getValue();
                if (currentUser != null) {
                    currentUser.setProfileImageUrl(newUrl);
                    mainViewModel.setCurrentUser(currentUser);
                }
            } else {
                Toast.makeText(getContext(), "Lỗi tải ảnh: " + Objects.requireNonNull(result.getError()).getMessage(), Toast.LENGTH_LONG).show();
                // Tải lại ảnh cũ nếu có lỗi
                populateUI(mainViewModel.getCurrentUser().getValue());
            }
        });
    }

    private void populateUI(User user) {
        if (user == null) {
            Toast.makeText(getContext(), "Lỗi tải dữ liệu người dùng.", Toast.LENGTH_SHORT).show();
            navController.popBackStack();
            return;
        }

        editDisplayName.setText(user.getName());
        editEmailAddress.setText(user.getEmail());
        editEmailAddress.setEnabled(false); // Không cho sửa email
        editPhoneNumber.setText(user.getPhone());
        editUserBio.setText(user.getBio());
        editUserAddress.setText(user.getAddress());

        if (getContext() != null && user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
            Glide.with(this).load(user.getProfileImageUrl()).circleCrop().placeholder(R.drawable.img).into(editProfileImage);
        } else {
            editProfileImage.setImageResource(R.drawable.img);
        }
    }

    private void saveProfileChanges() {
        User currentUser = mainViewModel.getCurrentUser().getValue();
        if (currentUser == null) {
            Toast.makeText(getContext(), "Dữ liệu chưa sẵn sàng để lưu.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Tạo một đối tượng User mới từ dữ liệu gốc để không thay đổi trực tiếp
        // đối tượng trong MainViewModel trước khi lưu thành công
        User userToUpdate = new User();
        userToUpdate.setId(currentUser.getId());
        userToUpdate.setEmail(currentUser.getEmail());
        userToUpdate.setProfileImageUrl(currentUser.getProfileImageUrl());
        userToUpdate.setRating(currentUser.getRating());
        userToUpdate.setReviewCount(currentUser.getReviewCount());
        userToUpdate.setFavoriteListingIds(currentUser.getFavoriteListingIds());
        userToUpdate.setCompletedSalesCount(currentUser.getCompletedSalesCount());

        // Lấy và gán các giá trị mới từ EditText
        userToUpdate.setName(editDisplayName.getText() != null ? editDisplayName.getText().toString().trim() : "");
        userToUpdate.setPhone(editPhoneNumber.getText() != null ? editPhoneNumber.getText().toString().trim() : "");
        userToUpdate.setBio(editUserBio.getText() != null ? editUserBio.getText().toString().trim() : "");
        userToUpdate.setAddress(editUserAddress.getText() != null ? editUserAddress.getText().toString().trim() : "");

        // Gọi ViewModel để thực hiện lưu
        editProfileViewModel.saveUserProfile(userToUpdate);
    }

    private void showLoading(boolean isLoading) {
        profileSaveProgress.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnSaveProfile.setEnabled(!isLoading);
        btnChangeProfileImage.setEnabled(!isLoading);
        btnSaveProfile.setText(isLoading ? "" : "Lưu thay đổi");
    }
}