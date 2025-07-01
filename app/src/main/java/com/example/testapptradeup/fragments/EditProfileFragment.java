package com.example.testapptradeup.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.widget.ImageButton; // Import ImageButton

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;
import com.example.testapptradeup.R;
import com.example.testapptradeup.models.User;
import com.example.testapptradeup.utils.SharedPrefsHelper;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Objects;

public class EditProfileFragment extends Fragment {

    private static final String TAG = "EditProfileFragment";

    // UI Components
    private ImageButton btnBack; // Khai báo ImageButton cho nút back
    private ImageView editProfileImage;
    private Button btnChangeProfileImage;
    private TextInputEditText editDisplayName;
    private TextInputEditText editEmailAddress;
    private TextInputEditText editPhoneNumber;
    private TextInputEditText editUserBio;
    private TextInputEditText editUserAddress;
    private Button btnSaveProfile;
    private ProgressBar profileSaveProgress;

    // Data
    private User currentUser;
    private SharedPrefsHelper prefsHelper;
    private FirebaseFirestore db;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        prefsHelper = new SharedPrefsHelper(requireContext());
        currentUser = prefsHelper.getCurrentUser(); // Lấy thông tin người dùng từ SharedPrefs
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_edit_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        loadUserProfileData();
        setupListeners();
    }

    private void initViews(View view) {
        btnBack = view.findViewById(R.id.btn_back); // Ánh xạ nút back
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

    private void loadUserProfileData() {
        if (currentUser != null) {
            // Load profile image
            if (currentUser.getProfileImageUrl() != null && !currentUser.getProfileImageUrl().isEmpty()) {
                Glide.with(this)
                        .load(currentUser.getProfileImageUrl())
                        .placeholder(R.drawable.img)
                        .error(R.drawable.img)
                        .circleCrop()
                        .into(editProfileImage);
            } else {
                editProfileImage.setImageResource(R.drawable.img);
            }

            // Load text data
            editDisplayName.setText(currentUser.getName());
            editEmailAddress.setText(currentUser.getEmail()); // Email không chỉnh sửa được
            editPhoneNumber.setText(currentUser.getPhone());
            editUserBio.setText(currentUser.getBio());
            editUserAddress.setText(currentUser.getAddress());
        } else {
            Toast.makeText(requireContext(), "Không thể tải dữ liệu hồ sơ.", Toast.LENGTH_SHORT).show();
            // Optional: navigate back if no user data
            NavController navController = Navigation.findNavController(requireView());
            navController.popBackStack();
        }
    }

    private void setupListeners() {
        // Listener cho nút back
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                NavController navController = Navigation.findNavController(requireView());
                navController.popBackStack(); // Quay lại Fragment trước đó trên back stack
            });
        }

        btnChangeProfileImage.setOnClickListener(v -> handleChangeProfileImage());
        btnSaveProfile.setOnClickListener(v -> saveProfileChanges());
    }

    private void handleChangeProfileImage() {
        // TODO: Implement logic to pick image from gallery/camera
        Toast.makeText(requireContext(), "Chức năng thay đổi ảnh đang phát triển.", Toast.LENGTH_SHORT).show();
    }

    private void saveProfileChanges() {
        if (currentUser == null) {
            Toast.makeText(requireContext(), "Lỗi: Không có dữ liệu người dùng để lưu.", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);

        // Get updated data from EditTexts
        String newDisplayName = Objects.requireNonNull(editDisplayName.getText()).toString().trim();
        String newPhoneNumber = Objects.requireNonNull(editPhoneNumber.getText()).toString().trim();
        String newUserBio = Objects.requireNonNull(editUserBio.getText()).toString().trim();
        String newUserAddress = Objects.requireNonNull(editUserAddress.getText()).toString().trim();

        // Update currentUser object
        currentUser.setName(newDisplayName);
        currentUser.setPhone(newPhoneNumber);
        currentUser.setBio(newUserBio);
        currentUser.setAddress(newUserAddress);

        // Save to Firestore
        db.collection("users").document(currentUser.getId())
                .set(currentUser) // Set will overwrite existing document
                .addOnSuccessListener(aVoid -> {
                    // Save to SharedPrefs after successful Firestore update
                    prefsHelper.saveCurrentUser(currentUser);
                    showLoading(false);
                    Toast.makeText(requireContext(), "Cập nhật hồ sơ thành công!", Toast.LENGTH_SHORT).show();
                    // Optionally navigate back to ProfileFragment
                    NavController navController = Navigation.findNavController(requireView());
                    navController.popBackStack();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Log.e(TAG, "Error updating profile: " + e.getMessage(), e);
                    Toast.makeText(requireContext(), "Lỗi cập nhật hồ sơ: " + e.getMessage(), Toast.LENGTH_LONG).show();
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
