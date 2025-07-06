package com.example.testapptradeup.fragments;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.testapptradeup.R;
import com.example.testapptradeup.activities.LoginActivity;
import com.example.testapptradeup.activities.MainActivity;
import com.example.testapptradeup.adapters.ReviewAdapter;
import com.example.testapptradeup.models.Review;
import com.example.testapptradeup.models.User;
import com.example.testapptradeup.viewmodels.ProfileViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;

import java.util.List;

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
    private MaterialCardView cardSavedItems, cardOffers, cardPurchases, cardPayments;
    private LinearLayout menuReviews;

    // SỬA Ở ĐÂY: Khai báo biến adapter
    private ReviewAdapter reviewAdapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(ProfileViewModel.class);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        try {
            navController = Navigation.findNavController(view);
            initViews(view);
            setupClickListeners();
            observeViewModel();
        } catch (Exception e) {
            Log.e("ProfileFragmentCrash", "Lỗi trong onViewCreated", e);
            Toast.makeText(getContext(), "Đã xảy ra lỗi khi mở trang hồ sơ.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        viewModel.refreshUserProfile();
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
        cardSavedItems = view.findViewById(R.id.card_saved_items);
        cardOffers = view.findViewById(R.id.card_offers);
        cardPurchases = view.findViewById(R.id.card_purchases);
        cardPayments = view.findViewById(R.id.card_payments);
        textSavedItemsCount = view.findViewById(R.id.text_saved_items_count);
        textOffersCount = view.findViewById(R.id.text_offers_count);
        textPurchasesCount = view.findViewById(R.id.text_purchases_count);
        textPaymentsCount = view.findViewById(R.id.text_payments_count);
        menuPersonalInfo = view.findViewById(R.id.menu_personal_info);
        menuChangePassword = view.findViewById(R.id.menu_change_password);
        menuNotificationSettings = view.findViewById(R.id.menu_notification_settings);
        menuPaymentMethods = view.findViewById(R.id.menu_payment_methods);
        menuReviews = view.findViewById(R.id.menu_reviews_section);
        recyclerViewReviews = view.findViewById(R.id.recycler_view_reviews);
        emptyReviewsText = view.findViewById(R.id.empty_reviews_text);
    }

    private void setupClickListeners() {
        btnEditProfile.setOnClickListener(v -> navController.navigate(R.id.action_navigation_profile_to_editProfileFragment));
        btnViewPublic.setOnClickListener(v -> {
            if (currentUser != null && currentUser.getId() != null) {
                ProfileFragmentDirections.ActionNavigationProfileToPublicProfileFragment action =
                        ProfileFragmentDirections.actionNavigationProfileToPublicProfileFragment(currentUser.getId());
                navController.navigate(action);
            } else {
                Toast.makeText(getContext(), "Không thể xem hồ sơ lúc này.", Toast.LENGTH_SHORT).show();
            }
        });
        cardSavedItems.setOnClickListener(v -> navController.navigate(R.id.action_navigation_profile_to_favoritesFragment));
        cardOffers.setOnClickListener(v -> Toast.makeText(getContext(), "Mở lịch sử đề xuất...", Toast.LENGTH_SHORT).show());
        cardPurchases.setOnClickListener(v -> Toast.makeText(getContext(), "Mở lịch sử mua hàng...", Toast.LENGTH_SHORT).show());
        cardPayments.setOnClickListener(v -> navController.navigate(R.id.action_navigation_profile_to_paymentSettingsFragment));
        menuPersonalInfo.setOnClickListener(v -> navController.navigate(R.id.action_navigation_profile_to_editProfileFragment));
        menuChangePassword.setOnClickListener(v -> navController.navigate(R.id.action_navigation_profile_to_changePasswordFragment));
        menuNotificationSettings.setOnClickListener(v -> navController.navigate(R.id.action_navigation_profile_to_notificationSettingsFragment));
        menuPaymentMethods.setOnClickListener(v -> navController.navigate(R.id.action_navigation_profile_to_paymentSettingsFragment));
        menuReviews.setOnClickListener(v -> navController.navigate(R.id.action_navigation_profile_to_reviewsFragment));
        btnLogout.setOnClickListener(v -> showLogoutConfirmationDialog());
        btnDeactivateAccount.setOnClickListener(v -> showDeactivateConfirmDialog());
        btnDeleteAccount.setOnClickListener(v -> showDeleteConfirmDialog());
    }

    private void observeViewModel() {
        viewModel.getUserProfileData().observe(getViewLifecycleOwner(), user -> {
            if (user != null) {
                this.currentUser = user;
                updateUI(user);
                // Giờ đây dòng này sẽ không báo lỗi nữa
                setupRecyclerView(user.getReviews());
            } else {
                Toast.makeText(getContext(), "Không thể tải dữ liệu người dùng.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupRecyclerView(List<Review> reviews) {
        if (getContext() == null) {
            return;
        }
        if (reviews != null && !reviews.isEmpty()) {
            recyclerViewReviews.setVisibility(View.VISIBLE);
            emptyReviewsText.setVisibility(View.GONE);
            // Giờ đây dòng này sẽ không báo lỗi nữa
            reviewAdapter = new ReviewAdapter(reviews);
            recyclerViewReviews.setLayoutManager(new LinearLayoutManager(getContext()));
            recyclerViewReviews.setAdapter(reviewAdapter);
        } else {
            recyclerViewReviews.setVisibility(View.GONE);
            emptyReviewsText.setVisibility(View.VISIBLE);
        }
    }

    @SuppressLint({"SetTextI18n", "StringFormatMatches"})
    private void updateUI(User user) {
        if (user == null) {
            textDisplayName.setText("Không tải được dữ liệu");
            textEmail.setText("");
            textRatingInfo.setText("");
            textBio.setText("");
            return;
        }

        textDisplayName.setText(user.getName() != null ? user.getName() : "Chưa có tên");
        textEmail.setText(user.getEmail() != null ? user.getEmail() : "Chưa có email");
        textBio.setText(user.getBio() != null && !user.getBio().isEmpty() ? user.getBio() : "Chưa có tiểu sử.");

        if (user.getReviewCount() > 0) {
            textRatingInfo.setText(getString(R.string.rating_info_format, user.getRating(), user.getReviewCount()));
        } else {
            textRatingInfo.setText("Chưa có đánh giá");
        }

        if (getContext() != null && user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
            Glide.with(this).load(user.getProfileImageUrl()).circleCrop().placeholder(R.drawable.ic_profile_placeholder).into(profileImage);
        } else {
            profileImage.setImageResource(R.drawable.ic_profile_placeholder);
        }

        textSavedItemsCount.setText(String.valueOf(user.getFavoriteListingIds() != null ? user.getFavoriteListingIds().size() : 0));
        textOffersCount.setText("0");
        textPurchasesCount.setText(String.valueOf(user.getCompletedSalesCount()));
        textPaymentsCount.setText("0");
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
                .setPositiveButton("Xóa vĩnh viễn", (dialog, which) -> {
                    Toast.makeText(getContext(), "Đang xử lý yêu cầu...", Toast.LENGTH_SHORT).show();
                    viewModel.deleteAccountPermanently().observe(getViewLifecycleOwner(), success -> {
                        if (success != null && success) {
                            Toast.makeText(getContext(), "Tài khoản đã được xóa.", Toast.LENGTH_LONG).show();
                            logout();
                        } else if (success != null) {
                            Toast.makeText(getContext(), "Có lỗi xảy ra khi xóa tài khoản.", Toast.LENGTH_LONG).show();
                        }
                    });
                })
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