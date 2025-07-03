package com.example.testapptradeup.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;
import com.example.testapptradeup.R;
import com.example.testapptradeup.models.User;
import com.example.testapptradeup.utils.SharedPrefsHelper;
import com.example.testapptradeup.viewmodels.EditProfileViewModel;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

import java.util.Objects;

public class EditProfileFragment extends Fragment {

    // UI Components
    private ImageButton btnBack;
    private ImageView editProfileImage;
    private Button btnChangeProfileImage;
    private TextInputEditText editDisplayName, editEmailAddress, editPhoneNumber, editUserBio, editUserAddress;
    private Button btnSaveProfile;
    private ProgressBar profileSaveProgress;

    // ViewModel and Data
    private EditProfileViewModel viewModel;
    private User currentUserData; // Giữ bản sao để cập nhật
    private SharedPrefsHelper prefsHelper;
    private NavController navController;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(EditProfileViewModel.class);
        prefsHelper = new SharedPrefsHelper(requireContext());
    }

    @Nullable
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
        observeViewModel();
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

    private void observeViewModel() {
        String userId = FirebaseAuth.getInstance().getUid();
        if (userId == null) {
            Toast.makeText(getContext(), "Lỗi: Người dùng không hợp lệ", Toast.LENGTH_SHORT).show();
            navController.popBackStack();
            return;
        }

        viewModel.getUserProfile(userId).observe(getViewLifecycleOwner(), user -> {
            if (user != null) {
                // Khi nhận được dữ liệu, gán vào biến local
                this.currentUserData = user;
                // Gọi hàm để điền dữ liệu lên UI
                populateUI(user);
            } else {
                Toast.makeText(requireContext(), "Không thể tải dữ liệu hồ sơ.", Toast.LENGTH_SHORT).show();
                navController.popBackStack();
            }
        });
    }

    private void populateUI(User user) {
        // <<< SỬA: Thay `currentUser` bằng `user` (tham số của hàm) để đảm bảo tính nhất quán.
        if (user != null) {
            // Load profile image
            if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
                Glide.with(this)
                        .load(user.getProfileImageUrl())
                        .placeholder(R.drawable.img)
                        .error(R.drawable.img)
                        .circleCrop()
                        .into(editProfileImage);
            } else {
                editProfileImage.setImageResource(R.drawable.img);
            }

            // Load text data
            editDisplayName.setText(user.getName());
            editEmailAddress.setText(user.getEmail()); // Email không chỉnh sửa được
            editPhoneNumber.setText(user.getPhone());
            editUserBio.setText(user.getBio());
            editUserAddress.setText(user.getAddress());
        }
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> navController.popBackStack());
        btnChangeProfileImage.setOnClickListener(v -> handleChangeProfileImage());
        btnSaveProfile.setOnClickListener(v -> saveProfileChanges());
    }

    private void handleChangeProfileImage() {
        // TODO: Implement logic to pick image from gallery/camera
        Toast.makeText(requireContext(), "Chức năng thay đổi ảnh đang phát triển.", Toast.LENGTH_SHORT).show();
    }

    private void saveProfileChanges() {
        if (currentUserData == null) {
            Toast.makeText(requireContext(), "Lỗi: Không có dữ liệu để lưu.", Toast.LENGTH_SHORT).show();
            return;
        }
        showLoading(true);

        // <<< SỬA: Lấy dữ liệu từ UI và cập nhật trực tiếp vào đối tượng currentUserData
        String newDisplayName = Objects.requireNonNull(editDisplayName.getText()).toString().trim();
        String newPhoneNumber = Objects.requireNonNull(editPhoneNumber.getText()).toString().trim();
        String newUserBio = Objects.requireNonNull(editUserBio.getText()).toString().trim();
        String newUserAddress = Objects.requireNonNull(editUserAddress.getText()).toString().trim();

        // Update currentUserData object
        currentUserData.setName(newDisplayName);
        currentUserData.setPhone(newPhoneNumber);
        currentUserData.setBio(newUserBio);
        currentUserData.setAddress(newUserAddress);

        // Gọi ViewModel để lưu đối tượng đã được cập nhật
        viewModel.saveUserProfile(currentUserData).observe(getViewLifecycleOwner(), success -> {
            showLoading(false);
            if (success != null && success) {
                // Cập nhật lại dữ liệu trong SharedPreferences sau khi lưu thành công
                prefsHelper.saveCurrentUser(currentUserData);
                Toast.makeText(requireContext(), "Cập nhật hồ sơ thành công!", Toast.LENGTH_SHORT).show();
                // Quay lại màn hình Profile
                navController.popBackStack();
            } else {
                Toast.makeText(requireContext(), "Lỗi cập nhật hồ sơ.", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showLoading(boolean isLoading) {
        if (isLoading) {
            btnSaveProfile.setVisibility(View.GONE);
            profileSaveProgress.setVisibility(View.VISIBLE);
        } else {
            btnSaveProfile.setVisibility(View.VISIBLE);
            profileSaveProgress.setVisibility(View.GONE);
        }
    }
}