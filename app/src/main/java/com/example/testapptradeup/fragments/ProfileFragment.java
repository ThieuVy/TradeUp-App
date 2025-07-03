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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.testapptradeup.R;
import com.example.testapptradeup.activities.LoginActivity;
import com.example.testapptradeup.adapters.ReviewAdapter;
import com.example.testapptradeup.models.User;
import com.example.testapptradeup.viewmodels.ProfileViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;

import java.util.Locale; // <<< THÊM IMPORT NÀY

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

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);
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

    private void initViews(View view) {
        profileImage = view.findViewById(R.id.profile_image);
        textDisplayName = view.findViewById(R.id.text_display_name);
        textRatingInfo = view.findViewById(R.id.text_rating_info);
        textBio = view.findViewById(R.id.text_bio);
        textEmail = view.findViewById(R.id.text_email);
        btnEditProfile = view.findViewById(R.id.btn_edit_profile);
        btnViewPublic = view.findViewById(R.id.btn_view_public);
        textSavedItemsCount = view.findViewById(R.id.text_saved_items_count);
        textOffersCount = view.findViewById(R.id.text_offers_count);
        textPurchasesCount = view.findViewById(R.id.text_purchases_count);
        textPaymentsCount = view.findViewById(R.id.text_payments_count);
        menuPaymentMethods = view.findViewById(R.id.menu_payment_methods);
        recyclerViewReviews = view.findViewById(R.id.recycler_view_reviews);
        emptyReviewsText = view.findViewById(R.id.empty_reviews_text);
        btnDeactivateAccount = view.findViewById(R.id.btn_deactivate_account);
        btnDeleteAccount = view.findViewById(R.id.btn_delete_account);
        btnLogout = view.findViewById(R.id.btn_logout);
    }

    private void setupClickListeners() {
        btnEditProfile.setOnClickListener(v -> {
            if (currentUser != null) {
                // Sửa: Sử dụng lớp Directions đã được tạo tự động
                ProfileFragmentDirections.ActionProfileFragmentToEditProfileFragment action =
                        ProfileFragmentDirections.actionProfileFragmentToEditProfileFragment(currentUser);
                navController.navigate(action);
            }
        });

        btnViewPublic.setOnClickListener(v -> {
            if (currentUser != null) {
                // Sửa: Sử dụng lớp Directions đã được tạo tự động
                ProfileFragmentDirections.ActionProfileFragmentToPublicProfileFragment action =
                        ProfileFragmentDirections.actionProfileFragmentToPublicProfileFragment(currentUser.getId());
                navController.navigate(action);
            }
        });

        menuPaymentMethods.setOnClickListener(v -> Toast.makeText(getContext(), "Mở cài đặt thanh toán Stripe...", Toast.LENGTH_SHORT).show());

        btnDeactivateAccount.setOnClickListener(v -> showDeactivateConfirmDialog());
        btnDeleteAccount.setOnClickListener(v -> showDeleteConfirmDialog());
        btnLogout.setOnClickListener(v -> logout());
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

        viewModel.getUserReviewsData().observe(getViewLifecycleOwner(), reviews -> {
            if (reviews != null && !reviews.isEmpty()) {
                recyclerViewReviews.setVisibility(View.VISIBLE);
                emptyReviewsText.setVisibility(View.GONE);
                recyclerViewReviews.setLayoutManager(new LinearLayoutManager(getContext()));
                recyclerViewReviews.setAdapter(new ReviewAdapter(reviews));
            } else {
                recyclerViewReviews.setVisibility(View.GONE);
                emptyReviewsText.setVisibility(View.VISIBLE);
            }
        });
    }

    @SuppressLint("SetTextI18n")
    private void updateUI(User user) {
        textDisplayName.setText(user.getName());
        textEmail.setText(user.getEmail());
        textBio.setText(user.getBio() != null && !user.getBio().isEmpty() ? user.getBio() : "Chưa có tiểu sử.");

        if (user.getReviewCount() > 0) {
            // Sửa: Sử dụng Locale đã được import
            textRatingInfo.setText(String.format(Locale.getDefault(), "%.1f từ %d đánh giá", user.getRating(), user.getReviewCount()));
        } else {
            textRatingInfo.setText("Chưa có đánh giá");
        }

        if (getContext() != null && user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
            Glide.with(getContext()).load(user.getProfileImageUrl()).circleCrop().into(profileImage);
        } else {
            // Đặt ảnh mặc định nếu không có ảnh
            profileImage.setImageResource(R.drawable.img);
        }

        textSavedItemsCount.setText(String.valueOf(user.getFavoriteListingIds() != null ? user.getFavoriteListingIds().size() : 0));
        textPurchasesCount.setText(String.valueOf(user.getCompletedSalesCount()));
        textOffersCount.setText("0");
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