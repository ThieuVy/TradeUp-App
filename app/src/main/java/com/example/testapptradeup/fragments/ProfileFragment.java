package com.example.testapptradeup.fragments;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.testapptradeup.R;
import com.example.testapptradeup.models.User;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileFragment extends Fragment {

    // UI Components
    private FrameLayout frameProfileImage;
    private ImageView profileImage, cameraIcon;
    private TextView textDisplayName, textRatingInfo, textBio, textEmail;
    private MaterialButton btnEditProfile, btnViewPublic, btnDeactivateAccount, btnDeleteAccount;
    private MaterialCardView cardSavedItems, cardOffers, cardPurchases, cardPayments;
    private TextView textSavedItemsCount, textOffersCount, textPurchasesCount, textPaymentsCount;
    private LinearLayout menuPersonalInfo, menuChangePassword, menuNotificationSettings, menuPaymentMethods;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private User currentUser;
    private NavController navController;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
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
        loadUserData();
    }

    private void initViews(View view) {
        frameProfileImage = view.findViewById(R.id.frame_profile_image);
        profileImage = view.findViewById(R.id.profile_image);
        cameraIcon = view.findViewById(R.id.camera_icon);
        textDisplayName = view.findViewById(R.id.text_display_name);
        textRatingInfo = view.findViewById(R.id.text_rating_info);
        textBio = view.findViewById(R.id.text_bio);
        textEmail = view.findViewById(R.id.text_email);

        btnEditProfile = view.findViewById(R.id.btn_edit_profile);
        btnViewPublic = view.findViewById(R.id.btn_view_public);

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

        RecyclerView recyclerViewReviews = view.findViewById(R.id.recycler_view_reviews);
        recyclerViewReviews.setLayoutManager(new LinearLayoutManager(getContext()));

        btnDeactivateAccount = view.findViewById(R.id.btn_deactivate_account);
        btnDeleteAccount = view.findViewById(R.id.btn_delete_account);
    }

    private void setupClickListeners() {
        frameProfileImage.setOnClickListener(v -> showToast("Chọn ảnh mới (Cloudinary upload cần tích hợp)"));
        cameraIcon.setOnClickListener(v -> showToast("Chọn ảnh mới (Cloudinary upload cần tích hợp)"));

        btnEditProfile.setOnClickListener(v -> navController.navigate(R.id.editProfileFragment));
        btnViewPublic.setOnClickListener(v -> {
            FirebaseUser firebaseUser = mAuth.getCurrentUser();
            if (firebaseUser != null) {
                String currentUserId = firebaseUser.getUid();

                // Tạo một Bundle để truyền userId
                Bundle bundle = new Bundle();
                bundle.putString("userId", currentUserId);

                // Điều hướng đến PublicProfileFragment với userId của người dùng hiện tại
                // Đảm bảo bạn có action này trong nav_graph.xml
                navController.navigate(R.id.action_profileFragment_to_publicProfileFragment, bundle);
            } else {
                showToast("Vui lòng đăng nhập.");
            }
        });

        // *** BẮT ĐẦU THAY ĐỔI ***
        // Điều hướng đến màn hình MyListingsFragment khi nhấn vào
        // Lưu ý: MyListingsFragment hiện tại hiển thị tin đăng CỦA BẠN.
        // Để hiển thị tin ĐÃ LƯU, bạn cần sửa đổi logic của MyListingsFragment
        // hoặc tạo một Fragment mới. Đây là bước điều hướng cơ bản.
        cardSavedItems.setOnClickListener(v -> {
            // Đảm bảo bạn đã định nghĩa action này trong file navigation graph của bạn
            // ví dụ: res/navigation/nav_graph.xml
            navController.navigate(R.id.action_profileFragment_to_myListingsFragment);
        });
        // *** KẾT THÚC THAY ĐỔI ***

        cardOffers.setOnClickListener(v -> showToast("Xem đề xuất"));
        cardPurchases.setOnClickListener(v -> showToast("Xem lịch sử mua"));
        cardPayments.setOnClickListener(v -> showToast("Xem thanh toán"));

        menuPersonalInfo.setOnClickListener(v -> navController.navigate(R.id.editProfileFragment));
        menuChangePassword.setOnClickListener(v -> showToast("Chuyển đến đổi mật khẩu"));
        menuNotificationSettings.setOnClickListener(v -> showToast("Cài đặt thông báo"));
        menuPaymentMethods.setOnClickListener(v -> showToast("Cài đặt thanh toán"));

        btnDeactivateAccount.setOnClickListener(v -> showToast("Tạm dừng tài khoản (cần xử lý logic)"));
        btnDeleteAccount.setOnClickListener(v -> showToast("Xóa tài khoản (cần xử lý logic và xác nhận)"));
    }

    private void loadUserData() {
        FirebaseUser firebaseUser = mAuth.getCurrentUser();
        if (firebaseUser == null) {
            showToast("Chưa đăng nhập");
            navController.navigate(R.id.navigation_login); // Ví dụ: điều hướng đến màn hình đăng nhập
            return;
        }

        db.collection("users").document(firebaseUser.getUid())
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        currentUser = snapshot.toObject(User.class);
                        if (currentUser != null) {
                            updateUI();
                        }
                    } else {
                        showToast("Không tìm thấy thông tin người dùng.");
                    }
                })
                .addOnFailureListener(e -> showToast("Lỗi tải dữ liệu người dùng"));
    }

    @SuppressLint("SetTextI18n")
    private void updateUI() {
        textDisplayName.setText(currentUser.getName());
        textBio.setText(currentUser.getBio());
        textEmail.setText(currentUser.getEmail());

        if (currentUser.getRating() > 0 && currentUser.getReviewCount() > 0) {
            String ratingInfo = currentUser.getRating() + " từ " + currentUser.getReviewCount() + " đánh giá";
            textRatingInfo.setText(ratingInfo);
        } else {
            textRatingInfo.setText("Chưa có đánh giá");
        }


        if (currentUser.getProfileImageUrl() != null && !currentUser.getProfileImageUrl().isEmpty()) {
            Glide.with(this)
                    .load(currentUser.getProfileImageUrl())
                    .placeholder(R.drawable.img)
                    .error(R.drawable.img)
                    .circleCrop()
                    .into(profileImage);
        }

        // Placeholder counts until implemented
        textSavedItemsCount.setText("--");
        textOffersCount.setText("--");
        textPurchasesCount.setText("--");
        textPaymentsCount.setText("--");
    }

    private void showToast(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }
}