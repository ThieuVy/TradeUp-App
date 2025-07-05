package com.example.testapptradeup.fragments;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.testapptradeup.R;
import com.example.testapptradeup.activities.LoginActivity;
import com.example.testapptradeup.activities.MainActivity;
import com.example.testapptradeup.models.User;
import com.example.testapptradeup.viewmodels.ProfileViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;

// <<< THÊM IMPORT NÀY


public class ProfileFragment extends Fragment {

    private ProfileViewModel viewModel;
    private NavController navController;
    private User currentUser;

    // UI Components
    private ImageView profileImage;
    private TextView textDisplayName, textRatingInfo, textBio, textEmail;
    private MaterialButton btnEditProfile, btnViewPublic, btnDeactivateAccount, btnDeleteAccount, btnLogout;
    private TextView textSavedItemsCount, textOffersCount, textPurchasesCount, textPaymentsCount;
    private LinearLayout menuPaymentMethods;
    private RecyclerView recyclerViewReviews;
    private TextView emptyReviewsText;
    private LinearLayout menuPersonalInfo, menuChangePassword, menuNotificationSettings;
    private LinearLayout cardSavedItems, cardOffers, cardPurchases, cardPayments; // Sửa lại thành LinearLayout nếu cần
    private LinearLayout menuReviews;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(ProfileViewModel.class); // Dùng scope Activity
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(view);
        initViews(view);
        setupClickListeners();
        observeViewModel();
    }

    @Override
    public void onResume() {
        super.onResume();
        viewModel.refreshUserProfile(); // Yêu cầu tải lại dữ liệu mới nhất
    }

    private void initViews(View view) {
        profileImage = view.findViewById(R.id.profile_image);
        textDisplayName = view.findViewById(R.id.text_display_name);
        textRatingInfo = view.findViewById(R.id.text_rating_info);
        textBio = view.findViewById(R.id.text_bio);
        textEmail = view.findViewById(R.id.text_email);
        btnEditProfile = view.findViewById(R.id.btn_edit_profile);
        btnViewPublic = view.findViewById(R.id.btn_view_public);
        btnLogout = view.findViewById(R.id.btn_logout);
        btnDeactivateAccount = view.findViewById(R.id.btn_deactivate_account);
        btnDeleteAccount = view.findViewById(R.id.btn_delete_account);

        // Lịch sử hoạt động
        cardSavedItems = view.findViewById(R.id.card_saved_items);
        cardOffers = view.findViewById(R.id.card_offers);
        cardPurchases = view.findViewById(R.id.card_purchases);
        cardPayments = view.findViewById(R.id.card_payments);
        textSavedItemsCount = view.findViewById(R.id.text_saved_items_count);
        textOffersCount = view.findViewById(R.id.text_offers_count);
        textPurchasesCount = view.findViewById(R.id.text_purchases_count);
        textPaymentsCount = view.findViewById(R.id.text_payments_count);


        // Quản lý tài khoản
        menuPersonalInfo = view.findViewById(R.id.menu_personal_info);
        menuChangePassword = view.findViewById(R.id.menu_change_password);
        menuNotificationSettings = view.findViewById(R.id.menu_notification_settings);
        menuPaymentMethods = view.findViewById(R.id.menu_payment_methods);

        // Đánh giá
        menuReviews = view.findViewById(R.id.menu_reviews_section); // Giả sử có ID này
        recyclerViewReviews = view.findViewById(R.id.recycler_view_reviews);
        emptyReviewsText = view.findViewById(R.id.empty_reviews_text);
    }

    private void setupClickListeners() {
        // --- CÁC NÚT CHÍNH ---
        btnEditProfile.setOnClickListener(v ->
                // Sử dụng ID action mới
                navController.navigate(R.id.action_navigation_profile_to_editProfileFragment)
        );

        btnViewPublic.setOnClickListener(v -> {
            if (currentUser != null && currentUser.getId() != null) {
                // Lớp và phương thức được tạo ra sẽ khớp với ID action mới
                ProfileFragmentDirections.ActionNavigationProfileToPublicProfileFragment action =
                        ProfileFragmentDirections.actionNavigationProfileToPublicProfileFragment(currentUser.getId());
                navController.navigate(action);
            } else {
                Toast.makeText(getContext(), "Không thể xem hồ sơ lúc này.", Toast.LENGTH_SHORT).show();
            }
        });

        // --- LỊCH SỬ HOẠT ĐỘNG ---
        cardSavedItems.setOnClickListener(v ->
                navController.navigate(R.id.action_navigation_profile_to_favoritesFragment)
        );
        cardOffers.setOnClickListener(v ->
                Toast.makeText(getContext(), "Mở lịch sử đề xuất...", Toast.LENGTH_SHORT).show()
        );
        cardPurchases.setOnClickListener(v ->
                Toast.makeText(getContext(), "Mở lịch sử mua hàng...", Toast.LENGTH_SHORT).show()
        );
        cardPayments.setOnClickListener(v ->
                navController.navigate(R.id.action_navigation_profile_to_paymentSettingsFragment)
        );


        // --- QUẢN LÝ TÀI KHOẢN ---
        menuPersonalInfo.setOnClickListener(v ->
                navController.navigate(R.id.action_navigation_profile_to_editProfileFragment)
        );
        menuChangePassword.setOnClickListener(v ->
                navController.navigate(R.id.action_navigation_profile_to_changePasswordFragment)
        );
        menuNotificationSettings.setOnClickListener(v ->
                navController.navigate(R.id.action_navigation_profile_to_notificationSettingsFragment)
        );
        menuPaymentMethods.setOnClickListener(v ->
                navController.navigate(R.id.action_navigation_profile_to_paymentSettingsFragment)
        );

        // --- ĐÁNH GIÁ & NHẬN XÉT ---
        menuReviews.setOnClickListener(v ->
                navController.navigate(R.id.action_navigation_profile_to_reviewsFragment)
        );

        // --- CÁC HÀNH ĐỘNG KHÁC ---
        btnLogout.setOnClickListener(v -> showLogoutConfirmationDialog());
        btnDeactivateAccount.setOnClickListener(v -> showDeactivateConfirmDialog());
        btnDeleteAccount.setOnClickListener(v -> showDeleteConfirmDialog());
    }

    private void observeViewModel() {
        viewModel.getUserProfileData().observe(getViewLifecycleOwner(), user -> {
            if (user != null) {
                this.currentUser = user;
                updateUI(user);
            } else {
                Toast.makeText(getContext(), "Không thể tải dữ liệu người dùng.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @SuppressLint({"SetTextI18n", "StringFormatMatches"})
    private void updateUI(User user) {
        if (user == null) {
            // Xử lý trường hợp user null (ví dụ: hiển thị trạng thái lỗi)
            textDisplayName.setText("Không tải được dữ liệu");
            textEmail.setText("");
            textRatingInfo.setText("");
            textBio.setText("");
            return;
        }

        // Gán dữ liệu, kiểm tra null trước khi dùng
        textDisplayName.setText(user.getName() != null ? user.getName() : "Chưa có tên");
        textEmail.setText(user.getEmail() != null ? user.getEmail() : "Chưa có email");
        textBio.setText(user.getBio() != null && !user.getBio().isEmpty() ? user.getBio() : "Chưa có tiểu sử.");

        if (user.getReviewCount() > 0) {
            // Sử dụng String resource để dễ dịch thuật sau này
            textRatingInfo.setText(getString(R.string.rating_info_format, user.getRating(), user.getReviewCount()));
        } else {
            textRatingInfo.setText("Chưa có đánh giá");
        }

        // Tải ảnh đại diện
        if (getContext() != null && user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
            Glide.with(this).load(user.getProfileImageUrl()).circleCrop().placeholder(R.drawable.ic_profile_placeholder).into(profileImage);
        } else {
            profileImage.setImageResource(R.drawable.ic_profile_placeholder);
        }

        // Cập nhật các số đếm
        // *** FIX LỖI HIỂN THỊ TRỐNG THAY VÌ SỐ 0 ***
        textSavedItemsCount.setText(String.valueOf(user.getFavoriteListingIds() != null ? user.getFavoriteListingIds().size() : 0));
        textOffersCount.setText("0"); // Cần thêm trường này vào User model
        textPurchasesCount.setText(String.valueOf(user.getCompletedSalesCount())); // Cần thêm trường này vào User model
        textPaymentsCount.setText("0"); // Cần thêm trường này vào User model
    }

    private void showDeactivateConfirmDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Tạm dừng tài khoản")
                .setMessage("Bạn có chắc chắn muốn tạm dừng tài khoản? Bạn có thể kích hoạt lại bằng cách đăng nhập.")
                .setPositiveButton("Xác nhận", (dialog, which) -> viewModel.deactivateAccount().observe(getViewLifecycleOwner(), success -> {
                    if (success != null && success) {
                        Toast.makeText(getContext(), "Tài khoản đã được tạm dừng.", Toast.LENGTH_SHORT).show();
                        logout();
                    } else {
                        Toast.makeText(getContext(), "Có lỗi xảy ra.", Toast.LENGTH_SHORT).show();
                    }
                }))
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showLogoutConfirmationDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Đăng xuất")
                .setMessage("Bạn có chắc chắn muốn đăng xuất?")
                .setPositiveButton("Đăng xuất", (dialog, which) -> {
                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).performLogout();
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showDeleteConfirmDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Xóa tài khoản vĩnh viễn")
                .setMessage("Hành động này không thể hoàn tác. Tất cả dữ liệu của bạn sẽ bị xóa. Bạn có chắc chắn không?")
                .setPositiveButton("Xóa vĩnh viễn", (dialog, which) -> Toast.makeText(getContext(), "Đang xử lý yêu cầu xóa...", Toast.LENGTH_SHORT).show())
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void logout() {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        if (getActivity() != null) {
            getActivity().finish();
        }
    }
}