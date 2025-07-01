package com.example.testapptradeup.fragments;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.cardview.widget.CardView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;
import com.example.testapptradeup.R;
import com.example.testapptradeup.activities.MainActivity;
import com.example.testapptradeup.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;


public class ProfileFragment extends Fragment {

    private static final String TAG = "ProfileFragment";

    // UI Components
    private ImageView profileImage;
    private TextView displayName;
    private TextView memberSince;
    private TextView soldCount;
    private TextView sellingCount;
    // private TextView responseRate; // Không có ID trong fragment_profile.xml
    private CardView profileHeaderCard;
    private ImageView settingsIcon;
    private ImageView cameraIcon;
    private TextView viewAll;

    private TextView rating;

    // Các phần tử trong "Tài khoản"
    private LinearLayout editProfile; // Ánh xạ LinearLayout cho "Chỉnh sửa hồ sơ"
    private LinearLayout changePassword;
    private LinearLayout deleteAccount;
    private LinearLayout logout;

    // Các phần tử trong "Hỗ trợ"
    private LinearLayout helpCenter;
    private LinearLayout reportIssue;
    // private LinearLayout liveChat; // Không có ID trong fragment_profile.xml
    // private LinearLayout faq;      // Không có ID trong fragment_profile.xml

    // Data
    private User currentUser;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private NavController navController;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(view);

        initViews(view);
        setupClickListeners();
        loadUserData(); // Luôn tải dữ liệu từ Firestore
    }

    @SuppressLint("WrongViewCast")
    private void initViews(View view) {
        try {
            // Profile components
            profileHeaderCard = view.findViewById(R.id.profile_header_card);
            profileImage = view.findViewById(R.id.profile_image);
            displayName = view.findViewById(R.id.display_name);
            rating = view.findViewById(R.id.rating);
            memberSince = view.findViewById(R.id.member_since);
            soldCount = view.findViewById(R.id.sold_count);
            sellingCount = view.findViewById(R.id.selling_count);
            // responseRate = view.findViewById(R.id.response_rate); // Không có ID trong XML
            settingsIcon = view.findViewById(R.id.settings_icon);
            cameraIcon = view.findViewById(R.id.camera_icon);
            viewAll = view.findViewById(R.id.view_all);
            // Các phần tử trong "Tài khoản"
            editProfile = view.findViewById(R.id.edit_profile); // Ánh xạ LinearLayout cho "Chỉnh sửa hồ sơ"
            changePassword = view.findViewById(R.id.change_password);
            deleteAccount = view.findViewById(R.id.delete_account);
            logout = view.findViewById(R.id.logout);

            // Các phần tử trong "Hỗ trợ"
            helpCenter = view.findViewById(R.id.help_center);
            reportIssue = view.findViewById(R.id.report_issue);
            // liveChat = view.findViewById(R.id.live_chat); // Không có ID trong XML
            // faq = view.findViewById(R.id.faq);            // Không có ID trong XML

        } catch (Exception e) {
            Log.e(TAG, "Error initializing views", e);
            showError("Lỗi khởi tạo giao diện");
        }
    }

    private void setupClickListeners() {
        if (profileHeaderCard != null) {
            // Click vào thẻ profile header sẽ điều hướng đến trang chỉnh sửa hồ sơ
            profileHeaderCard.setOnClickListener(v -> navigateToEditProfile());
        }
        if (settingsIcon != null) {
            settingsIcon.setOnClickListener(v -> navigateToEditProfile()); // Điều hướng từ icon cài đặt đến trang chỉnh sửa hồ sơ
        }
        if (cameraIcon != null) {
            cameraIcon.setOnClickListener(v -> showToast("Thay đổi ảnh đại diện")); // TODO: Implement image picker
        }

        // Thêm sự kiện click cho display_name
        if (displayName != null) {
            displayName.setOnClickListener(v -> {
                if (currentUser != null) {
                    String userInfo = "Tên: " + currentUser.getName() + "\n" +
                            "Email: " + currentUser.getEmail() + "\n" +
                            "ID: " + currentUser.getId();
                    showToast(userInfo);
                } else {
                    showToast("Không có thông tin người dùng để hiển thị.");
                }
            });
        }

        if (viewAll != null) {
            viewAll.setOnClickListener(v -> {
                try {
                    // Sử dụng ID của action đã định nghĩa ở Bước 1
                    navController.navigate(R.id.action_profileFragment_to_myListingsFragment);
                } catch (Exception e) {
                    Log.e(TAG, "Lỗi điều hướng đến MyListingsFragment: ", e);
                    showError("Không thể mở trang danh sách của bạn.");
                }
            });
        }

        // Đặt lắng nghe sự kiện cho các mục trong "Tài khoản"
        if (editProfile != null) editProfile.setOnClickListener(v -> navigateToEditProfile());
        if (changePassword != null) changePassword.setOnClickListener(v -> showToast("Đổi mật khẩu")); // TODO: Implement navigation
        if (deleteAccount != null) deleteAccount.setOnClickListener(v -> showToast("Xóa tài khoản")); // TODO: Implement deletion logic
        if (logout != null) logout.setOnClickListener(v -> showLogoutDialog());

        // Đặt lắng nghe sự kiện cho các mục trong "Hỗ trợ"
        if (helpCenter != null) helpCenter.setOnClickListener(v -> navigateToHelpCenter());
        if (reportIssue != null) reportIssue.setOnClickListener(v -> navigateToReportIssue());
        // if (liveChat != null) liveChat.setOnClickListener(v -> navigateToLiveChat()); // Không có ID trong XML
        // if (faq != null) faq.setOnClickListener(v -> navigateToFAQ());            // Không có ID trong XML
    }

    private void loadUserData() {
        FirebaseUser firebaseUser = mAuth.getCurrentUser();
        if (firebaseUser == null) {
            showToast("Người dùng chưa đăng nhập. Vui lòng đăng nhập lại.");
            Log.d(TAG, "No FirebaseUser found. Triggering logout.");
            // Chuyển hướng về LoginActivity nếu không có người dùng Firebase
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).performLogout();
            }
            return;
        }

        String userId = firebaseUser.getUid();
        showToast("Đang tải thông tin người dùng từ máy chủ...");

        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        currentUser = documentSnapshot.toObject(User.class);
                        if (currentUser != null) {
                            currentUser.setId(userId); // Đảm bảo ID được đặt
                            updateUI(); // Cập nhật giao diện với dữ liệu mới
                            // fetchDynamicCounts(); // Không có các TextView để hiển thị các số liệu động này
                            Log.d(TAG, "Loaded user from Firestore: " + currentUser.getName());
                        } else {
                            Log.e(TAG, "User object is null after conversion from Firestore for UID: " + userId);
                            showError("Dữ liệu người dùng bị lỗi. Đang tạo bản ghi mặc định.");
                            createDefaultUserInFirestore(userId, firebaseUser);
                        }
                    } else {
                        Log.w(TAG, "Firestore document not found for user: " + userId + ". Creating default record.");
                        showError("Không tìm thấy dữ liệu người dùng. Đang tạo bản ghi.");
                        createDefaultUserInFirestore(userId, firebaseUser);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading user data from Firestore: " + e.getMessage(), e);
                    showError("Không thể tải thông tin người dùng: " + e.getMessage());
                    updateUI(); // Cố gắng cập nhật UI với dữ liệu hiện có (có thể là null)
                });
    }

    private void createDefaultUserInFirestore(String userId, FirebaseUser firebaseUser) {
        User defaultUser = new User(
                userId,
                firebaseUser.getDisplayName() != null ? firebaseUser.getDisplayName() : "Người dùng",
                firebaseUser.getEmail(),
                "", // phone default
                "", // bio default
                firebaseUser.getPhotoUrl() != null ? firebaseUser.getPhotoUrl().toString() : "",
                "", // address default
                0.0f, // rating default
                0,    // reviewCount default
                false, // isVerified default
                "active", // accountStatus default
                false, // isFlagged default
                "not_connected", // walletStatus default
                0    // notificationCount default
        );
        defaultUser.setBankAccount(""); // Đảm bảo bankAccount được khởi tạo

        db.collection("users").document(userId)
                .set(defaultUser)
                .addOnSuccessListener(aVoid -> {
                    currentUser = defaultUser; // Cập nhật currentUser
                    updateUI();
                    // fetchDynamicCounts(); // Không có các TextView để hiển thị các số liệu động này
                    showToast("Đã tạo bản ghi người dùng mặc định.");
                    Log.d(TAG, "Default user record created in Firestore.");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error creating default user in Firestore: " + e.getMessage(), e);
                    showError("Lỗi tạo bản ghi người dùng mặc định. Vui lòng thử lại.");
                });
    }

    @SuppressLint("SetTextI18n")
    private void updateUI() {
        if (currentUser == null) {
            Log.d(TAG, "updateUI called with null currentUser. Setting default values.");
            if (displayName != null) displayName.setText("Người dùng");
            if (rating != null) rating.setText("0.0 ★★★★★ (0)");
            if (memberSince != null) memberSince.setText("Thành viên từ năm ..."); // Không có dữ liệu trong User model
            if (profileImage != null) profileImage.setImageResource(R.drawable.img);
            if (soldCount != null) soldCount.setText("0");
            if (sellingCount != null) sellingCount.setText("0");
            // if (responseRate != null) responseRate.setText("0%"); // Không có ID trong XML
            return;
        }

        try {
            if (displayName != null) {
                displayName.setText(currentUser.getName() != null && !currentUser.getName().isEmpty() ? currentUser.getName() : "Người dùng");
            }
            if (rating != null) {
                updateRating(currentUser.getRating(), currentUser.getReviewCount());
            }
            // Member Since: Nếu có trường trong User model, cập nhật ở đây
            // Ví dụ: if (memberSince != null) memberSince.setText("Thành viên từ " + currentUser.getMemberSinceYear());

            if (profileImage != null) {
                String imageUrl = currentUser.getProfileImageUrl();
                if (imageUrl != null && !imageUrl.isEmpty()) {
                    Glide.with(this)
                            .load(imageUrl)
                            .placeholder(R.drawable.img)
                            .error(R.drawable.img)
                            .circleCrop()
                            .into(profileImage);
                } else {
                    profileImage.setImageResource(R.drawable.img);
                }
            }
            // Cập nhật các số liệu thống kê cứng trong XML (soldCount, sellingCount)
            if (soldCount != null) soldCount.setText("47"); // Giữ giá trị tĩnh hoặc thay bằng currentUser.getSoldCount()
            if (sellingCount != null) sellingCount.setText("12"); // Giữ giá trị tĩnh hoặc thay bằng currentUser.getSellingCount()
            // if (responseRate != null) responseRate.setText("98%"); // Giữ giá trị tĩnh hoặc thay bằng currentUser.getResponseRate()

        } catch (Exception e) {
            Log.e(TAG, "Error updating UI with user data", e);
            showError("Lỗi cập nhật giao diện người dùng.");
        }
    }

    // Phương thức này không còn cần thiết vì không có TextView để hiển thị các số liệu động
    // private void fetchDynamicCounts() { ... }

    // Phương thức này không còn cần thiết vì không có TextView để hiển thị các số liệu động
    // private void updateSectionCount(TextView textView, int count, String suffix) { ... }

    // Đặt lại tất cả các số liệu về trạng thái ẩn hoặc 0
    private void updateSectionCounts() {
        if (soldCount != null) soldCount.setText("0");
        if (sellingCount != null) sellingCount.setText("0");
        // if (responseRate != null) responseRate.setText("0%"); // Không có ID trong XML
    }


    // --- START: Corrected Navigation Methods ---
    private void navigateToEditProfile() {
        try {
            navController.navigate(R.id.editProfileFragment); // Điều hướng đến EditProfileFragment
        } catch (Exception e) {
            Log.e(TAG, "Error navigating to EditProfile: " + e.getMessage(), e);
            showError("Không thể mở trang chỉnh sửa hồ sơ");
        }
    }

    // Các phương thức điều hướng này không có LinearLayout tương ứng trong XML mới

    private void navigateToHelpCenter() {
        // TODO: Điều hướng đến Trung tâm trợ giúp
        showToast("Mở Trung tâm trợ giúp");
    }

    private void navigateToReportIssue() {
        // TODO: Điều hướng đến Báo cáo sự cố
        showToast("Mở Báo cáo sự cố");
    }

    // Các phương thức điều hướng này không có LinearLayout tương ứng trong XML mới
    // private void navigateToLiveChat() { navController.navigate(R.id.action_profile_to_live_chat); }
    // private void navigateToFAQ() { navController.navigate(R.id.action_profile_to_faq); }
    // private void navigateToAccountSettings() { navController.navigate(R.id.action_profile_to_account_settings); }
    // private void navigateToFeedback() { navController.navigate(R.id.action_profile_to_feedback); }
    // --- END: Corrected Navigation Methods ---

    private void showLogoutDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Đăng xuất")
                .setMessage("Bạn có chắc chắn muốn đăng xuất?")
                .setPositiveButton("Đăng xuất", (dialog, which) -> {
                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).performLogout();
                    } else {
                        showToast("Lỗi khi đăng xuất. Vui lòng khởi động lại ứng dụng.");
                        Log.e(TAG, "getActivity is not MainActivity during logout from ProfileFragment");
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showToast(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    private void showError(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
        }
    }

    public void updateProfileData(String name) {
        if (displayName != null) {
            displayName.setText(name);
        }
        if (currentUser != null) {
            currentUser.setName(name);
            // TODO: Cập nhật tên trong Firestore nếu cần
            // db.collection("users").document(currentUser.getId()).update("name", name);
        }
    }

    @SuppressLint("SetTextI18n")
    public void updateRating(float ratingValue, int reviewCount) {
        if (rating != null) {
            @SuppressLint("DefaultLocale") String ratingText = String.format("%.1f ★★★★★ (%d)", ratingValue, reviewCount);
            rating.setText(ratingText);
        }
    }

    public void setProfileImage(int drawableResId) {
        if (profileImage != null) {
            profileImage.setImageResource(drawableResId);
        }
    }

    public void setProfileImageUrl(String imageUrl) {
        if (profileImage != null && imageUrl != null) {
            Glide.with(this)
                    .load(imageUrl)
                    .placeholder(R.drawable.img)
                    .error(R.drawable.img)
                    .circleCrop()
                    .into(profileImage);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Nullify all views to prevent memory leaks
        profileImage = null;
        displayName = null;
        memberSince = null;
        soldCount = null;
        sellingCount = null;
        // responseRate = null;
        rating = null;
        profileHeaderCard = null;
        settingsIcon = null;
        cameraIcon = null;

        editProfile = null;
        changePassword = null;
        deleteAccount = null;
        logout = null;

        helpCenter = null;
        reportIssue = null;
        // liveChat = null;
        // faq = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Luôn tải lại dữ liệu khi fragment trở lại foreground
        loadUserData();
    }


    public void onViewAllClick(View view) {
        // Kiểm tra xem navController đã được khởi tạo chưa
        if (navController != null) {
            try {
                // Sử dụng ID của action đã định nghĩa ở Bước 1
                navController.navigate(R.id.action_profileFragment_to_myListingsFragment);
            } catch (Exception e) {
                Log.e(TAG, "Lỗi điều hướng đến MyListingsFragment: ", e);
                Toast.makeText(getContext(), "Không thể mở trang danh sách.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
