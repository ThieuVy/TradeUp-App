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
import com.example.testapptradeup.viewmodels.EditProfileViewModel;
import com.example.testapptradeup.viewmodels.MainViewModel;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Objects;

public class EditProfileFragment extends Fragment {

    private EditProfileViewModel editProfileViewModel;
    private MainViewModel mainViewModel; // ViewModel chia sẻ
    private NavController navController;

    // UI Components
    private ImageButton btnBack;
    private ImageView editProfileImage;
    private TextInputEditText editDisplayName, editPhoneNumber, editUserBio, editUserAddress, editEmailAddress;
    private Button btnSaveProfile;
    private ProgressBar profileSaveProgress;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        editProfileViewModel = new ViewModelProvider(this).get(EditProfileViewModel.class);
        mainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
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

        // Lấy dữ liệu người dùng từ MainViewModel để điền vào form
        User currentUser = mainViewModel.getCurrentUser().getValue();
        if (currentUser != null) {
            populateUI(currentUser);
        } else {
            Toast.makeText(getContext(), "Lỗi tải dữ liệu người dùng.", Toast.LENGTH_SHORT).show();
            navController.popBackStack();
        }
    }

    private void initViews(View view) {
        btnBack = view.findViewById(R.id.btn_back);
        editProfileImage = view.findViewById(R.id.edit_profile_image);
        editDisplayName = view.findViewById(R.id.edit_display_name);
        editEmailAddress = view.findViewById(R.id.edit_email_address);
        editPhoneNumber = view.findViewById(R.id.edit_phone_number);
        editUserBio = view.findViewById(R.id.edit_user_bio);
        editUserAddress = view.findViewById(R.id.edit_user_address);
        btnSaveProfile = view.findViewById(R.id.btn_save_profile);
        profileSaveProgress = view.findViewById(R.id.profile_save_progress);
    }

    private void observeViewModels() {
        editProfileViewModel.getSaveStatus().observe(getViewLifecycleOwner(), success -> {
            if (success != null) {
                showLoading(false);
                if (success) {
                    Toast.makeText(getContext(), "Cập nhật thành công!", Toast.LENGTH_SHORT).show();
                    // Lấy dữ liệu đã cập nhật và đẩy vào MainViewModel
                    User updatedUser = editProfileViewModel.getUpdatedUser().getValue();
                    if (updatedUser != null) {
                        mainViewModel.setCurrentUser(updatedUser);
                    }
                    navController.popBackStack();
                } else {
                    Toast.makeText(getContext(), "Cập nhật thất bại. Vui lòng thử lại.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        editProfileViewModel.isLoading().observe(getViewLifecycleOwner(), this::showLoading);
    }

    private void populateUI(User user) {
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

    private void setupListeners() {
        btnBack.setOnClickListener(v -> navController.popBackStack());
        btnSaveProfile.setOnClickListener(v -> saveProfileChanges());
    }

    private void saveProfileChanges() {
        User currentUser = mainViewModel.getCurrentUser().getValue();
        if (currentUser == null) {
            Toast.makeText(getContext(), "Dữ liệu chưa sẵn sàng để lưu.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Tạo một bản sao để chỉnh sửa, tránh thay đổi trực tiếp đối tượng trong MainViewModel
        User userToUpdate = new User();
        userToUpdate.setId(currentUser.getId());
        userToUpdate.setEmail(currentUser.getEmail()); // Email không đổi
        userToUpdate.setProfileImageUrl(currentUser.getProfileImageUrl()); // Tạm thời giữ ảnh cũ
        userToUpdate.setRating(currentUser.getRating()); // Tạm thời giữ đánh giá cũ
        userToUpdate.setReviewCount(currentUser.getReviewCount()); // Tạm thời giữ số lượng đánh giá cũ
        userToUpdate.setFavoriteListingIds(currentUser.getFavoriteListingIds()); // Tạm thời giữ danh sách yêu thích cũ
        userToUpdate.setCompletedSalesCount(currentUser.getCompletedSalesCount()); // Tạm thời giữ số lượng giao dịch cũ
        userToUpdate.setName(Objects.requireNonNull(editDisplayName.getText()).toString().trim());
        userToUpdate.setPhone(Objects.requireNonNull(editPhoneNumber.getText()).toString().trim());
        userToUpdate.setBio(Objects.requireNonNull(editUserBio.getText()).toString().trim());
        userToUpdate.setAddress(Objects.requireNonNull(editUserAddress.getText()).toString().trim());

        editProfileViewModel.saveUserProfile(userToUpdate);
    }

    private void showLoading(boolean isLoading) {
        profileSaveProgress.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnSaveProfile.setEnabled(!isLoading);
        btnSaveProfile.setText(isLoading ? "" : "Lưu thay đổi");
    }
}