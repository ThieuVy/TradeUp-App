package com.example.testapptradeup.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

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

    private EditProfileViewModel viewModel;
    private NavController navController;
    private SharedPrefsHelper prefsHelper;
    private User currentUserData; // Giữ một bản sao của dữ liệu để chỉnh sửa

    // UI Components
    private ImageButton btnBack;
    private ImageView editProfileImage;
    private TextInputEditText editDisplayName, editPhoneNumber, editUserBio, editUserAddress;
    private Button btnSaveProfile;
    private ProgressBar profileSaveProgress;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(EditProfileViewModel.class);
        prefsHelper = new SharedPrefsHelper(requireContext());
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
        observeViewModel();

        // Tải dữ liệu người dùng ban đầu
        String userId = FirebaseAuth.getInstance().getUid();
        if (userId != null) {
            // *** SỬA LỖI 1: Gọi đúng phương thức để kích hoạt tải dữ liệu ***
            viewModel.loadUserProfile(userId);
        } else {
            Toast.makeText(getContext(), "Lỗi xác thực người dùng.", Toast.LENGTH_SHORT).show();
            navController.popBackStack();
        }
    }

    private void initViews(View view) {
        btnBack = view.findViewById(R.id.btn_back);
        editProfileImage = view.findViewById(R.id.edit_profile_image);
        editDisplayName = view.findViewById(R.id.edit_display_name);
        editPhoneNumber = view.findViewById(R.id.edit_phone_number);
        editUserBio = view.findViewById(R.id.edit_user_bio);
        editUserAddress = view.findViewById(R.id.edit_user_address);
        btnSaveProfile = view.findViewById(R.id.btn_save_profile);
        profileSaveProgress = view.findViewById(R.id.profile_save_progress);
    }

    private void observeViewModel() {
        // Observe dữ liệu người dùng
        viewModel.getUserProfile().observe(getViewLifecycleOwner(), user -> {
            if (user != null) {
                this.currentUserData = user; // Lưu lại dữ liệu gốc
                populateUI(user);
            } else {
                Toast.makeText(getContext(), "Không thể tải dữ liệu hồ sơ.", Toast.LENGTH_SHORT).show();
            }
        });

        // *** SỬA LỖI 2: Gọi đúng phương thức để observe trạng thái lưu ***
        viewModel.getSaveStatus().observe(getViewLifecycleOwner(), success -> {
            // Kiểm tra success != null để chỉ xử lý khi có kết quả mới
            if (success != null) {
                showLoading(false); // Ẩn loading khi có kết quả
                if (success) { // Lỗi 'Incompatible types' đã được sửa ở đây
                    Toast.makeText(getContext(), "Cập nhật thành công!", Toast.LENGTH_SHORT).show();
                    // Cập nhật dữ liệu trong SharedPreferences
                    prefsHelper.saveCurrentUser(currentUserData);
                    navController.popBackStack();
                } else {
                    Toast.makeText(getContext(), "Cập nhật thất bại. Vui lòng thử lại.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // (Tùy chọn) Observe trạng thái loading
        viewModel.isLoading().observe(getViewLifecycleOwner(), this::showLoading);
    }

    private void populateUI(User user) {
        editDisplayName.setText(user.getName());
        editPhoneNumber.setText(user.getPhone());
        editUserBio.setText(user.getBio());
        editUserAddress.setText(user.getAddress());

        if (getContext() != null && user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
            Glide.with(this).load(user.getProfileImageUrl()).circleCrop().placeholder(R.drawable.img).into(editProfileImage);
        } else {
            editProfileImage.setImageResource(R.drawable.img);
        }
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> navController.popBackStack());
        btnSaveProfile.setOnClickListener(v -> saveProfileChanges());
    }

    private void saveProfileChanges() {
        if (currentUserData == null) {
            Toast.makeText(getContext(), "Dữ liệu chưa sẵn sàng để lưu.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Cập nhật đối tượng currentUserData từ các trường EditText
        currentUserData.setName(Objects.requireNonNull(editDisplayName.getText()).toString().trim());
        currentUserData.setPhone(Objects.requireNonNull(editPhoneNumber.getText()).toString().trim());
        currentUserData.setBio(Objects.requireNonNull(editUserBio.getText()).toString().trim());
        currentUserData.setAddress(Objects.requireNonNull(editUserAddress.getText()).toString().trim());

        // *** SỬA LỖI 3: Gọi đúng phương thức để lưu ***
        viewModel.saveUserProfile(currentUserData);
    }

    private void showLoading(boolean isLoading) {
        profileSaveProgress.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnSaveProfile.setEnabled(!isLoading); // Nên dùng setEnabled để giữ vị trí layout
        btnSaveProfile.setText(isLoading ? "Đang lưu..." : "Lưu thay đổi");
    }
}