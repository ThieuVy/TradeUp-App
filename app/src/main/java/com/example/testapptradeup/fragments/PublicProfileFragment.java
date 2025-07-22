package com.example.testapptradeup.fragments;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.testapptradeup.R;
import com.example.testapptradeup.adapters.ProductGridAdapter;
import com.example.testapptradeup.adapters.PublicReviewAdapter;
import com.example.testapptradeup.models.User;
import com.example.testapptradeup.viewmodels.MainViewModel;
import com.example.testapptradeup.viewmodels.PublicProfileViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.functions.FirebaseFunctions;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class PublicProfileFragment extends Fragment {

    private String userId;
    private NavController navController;
    private PublicProfileViewModel viewModel;

    // UI Elements
    private View contentLayout;
    private ProgressBar progressBar;
    private ImageView profileImage, btnBack, btnMoreOptions;
    private TextView textDisplayName, textMemberSince, textRatingStars, textRatingInfo, textBio, textLocation;
    private Button btnMessage;
    private TextView statsActiveListings, statsCompletedSales, statsReviews;
    private TextView btnViewAllListings, btnViewAllReviews;

    // Adapters và RecyclerViews
    private RecyclerView recyclerActiveListings;
    private RecyclerView recyclerReviews;
    private ProductGridAdapter listingsAdapter;
    private PublicReviewAdapter reviewAdapter;
    private MainViewModel mainViewModel; // Thêm ViewModel chung
    private Button btnMakeAdmin; // Thêm tham chiếu cho nút mới


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(PublicProfileViewModel.class);
        mainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class); // Khởi tạo
        if (getArguments() != null) {
            userId = PublicProfileFragmentArgs.fromBundle(getArguments()).getUserId();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_public_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(view);
        initViews(view);
        setupRecyclerViews();
        setupClickListeners();
        observeViewModel();

        if (userId != null && !userId.isEmpty()) {
            viewModel.loadProfileData(userId);
        } else {
            Toast.makeText(getContext(), "Lỗi: ID người dùng không hợp lệ.", Toast.LENGTH_SHORT).show();
            navController.popBackStack();
        }
    }

    private void initViews(View view) {
        contentLayout = view.findViewById(R.id.profile_content_layout);
        progressBar = view.findViewById(R.id.profile_loading_progress);
        profileImage = view.findViewById(R.id.profile_image);
        btnBack = view.findViewById(R.id.btn_back);
        textDisplayName = view.findViewById(R.id.text_display_name);
        textMemberSince = view.findViewById(R.id.text_member_since);
        textRatingStars = view.findViewById(R.id.text_rating_stars);
        textRatingInfo = view.findViewById(R.id.text_rating_info);
        textBio = view.findViewById(R.id.text_bio);
        textLocation = view.findViewById(R.id.text_location);
        btnMessage = view.findViewById(R.id.btn_message);
        statsActiveListings = view.findViewById(R.id.stats_active_listings);
        statsCompletedSales = view.findViewById(R.id.stats_completed_sales);
        statsReviews = view.findViewById(R.id.stats_reviews);
        recyclerActiveListings = view.findViewById(R.id.recycler_active_listings);
        recyclerReviews = view.findViewById(R.id.recycler_reviews);
        btnViewAllListings = view.findViewById(R.id.btn_view_all_listings);
        btnViewAllReviews = view.findViewById(R.id.btn_view_all_reviews);
        btnMoreOptions = view.findViewById(R.id.btn_more_options);
        btnMakeAdmin = view.findViewById(R.id.btn_make_admin);
    }

    private void setupRecyclerViews() {
        // Cài đặt cho danh sách sản phẩm
        // SỬA ĐỔI Ở ĐÂY: Cung cấp cả hai listener theo yêu cầu của constructor mới
        listingsAdapter = new ProductGridAdapter(
                // 1. Listener cho sự kiện click vào cả item (giữ nguyên)
                listing -> {
                    PublicProfileFragmentDirections.ActionPublicProfileFragmentToProductDetailFragment action =
                            PublicProfileFragmentDirections.actionPublicProfileFragmentToProductDetailFragment(listing.getId());
                    navController.navigate(action);
                },
                // 2. Listener cho sự kiện click vào nút tim (chúng ta sẽ thông báo tính năng này chưa có ở đây)
                (listing, favoriteIcon) -> Toast.makeText(getContext(), "Bạn chỉ có thể yêu thích sản phẩm từ trang chủ.", Toast.LENGTH_SHORT).show()
        );
        recyclerActiveListings.setLayoutManager(new GridLayoutManager(getContext(), 2));
        recyclerActiveListings.setAdapter(listingsAdapter);

        // Cài đặt cho danh sách đánh giá (giữ nguyên)
        recyclerReviews.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerReviews.setNestedScrollingEnabled(false);
        reviewAdapter = new PublicReviewAdapter();
        recyclerReviews.setAdapter(reviewAdapter);
        btnMakeAdmin.setOnClickListener(v -> showGrantAdminConfirmationDialog());
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> navController.popBackStack());

        btnMessage.setOnClickListener(v -> {
            String currentAuthUserId = FirebaseAuth.getInstance().getUid();
            // Kiểm tra người dùng có tự nhắn tin cho mình không
            if (userId.equals(currentAuthUserId)) {
                Toast.makeText(getContext(), "Bạn không thể tự nhắn tin cho mình.", Toast.LENGTH_SHORT).show();
                return;
            }
            // Kiểm tra đã đăng nhập chưa
            if (currentAuthUserId == null) {
                Toast.makeText(getContext(), "Vui lòng đăng nhập để nhắn tin.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Gọi ViewModel để tìm hoặc tạo cuộc trò chuyện
            viewModel.findOrCreateChat(userId).observe(getViewLifecycleOwner(), chatId -> {
                if (chatId != null && !chatId.isEmpty()) {
                    // Đã có chatId, điều hướng đến màn hình chat chi tiết
                    PublicProfileFragmentDirections.ActionPublicProfileFragmentToChatDetailFragment action =
                            PublicProfileFragmentDirections.actionPublicProfileFragmentToChatDetailFragment(chatId, textDisplayName.getText().toString());
                    navController.navigate(action);
                } else {
                    Toast.makeText(getContext(), "Không thể tạo cuộc trò chuyện.", Toast.LENGTH_SHORT).show();
                }
            });
        });

        btnMoreOptions.setOnClickListener(v -> showReportDialog());
        btnViewAllListings.setOnClickListener(v -> {
            // Tạo action với tham số bắt buộc
            PublicProfileFragmentDirections.ActionPublicProfileFragmentToProductListFragment action =
                    PublicProfileFragmentDirections.actionPublicProfileFragmentToProductListFragment("user");
            // Sử dụng setter để gán userId. Tham số categoryId sẽ mặc định là null.
            action.setUserId(userId);
            navController.navigate(action);
        });
        btnViewAllReviews.setOnClickListener(v -> Toast.makeText(getContext(), "Xem tất cả đánh giá cho " + textDisplayName.getText(), Toast.LENGTH_SHORT).show());
    }

    private void observeViewModel() {
        mainViewModel.getCurrentUser().observe(getViewLifecycleOwner(), currentUser -> {
            if (currentUser != null && currentUser.isAdmin()) {
                // Nếu người xem là admin VÀ họ không xem hồ sơ của chính mình
                if (userId != null && !userId.equals(currentUser.getId())) {
                    btnMakeAdmin.setVisibility(View.VISIBLE);
                }
            }
        });
        viewModel.getUserProfile().observe(getViewLifecycleOwner(), this::updateProfileUI);

        // Observer này chỉ nhận TỐI ĐA 4 tin đăng để hiển thị
        viewModel.getUserListings().observe(getViewLifecycleOwner(), listings -> {
            if (listings != null) {
                listingsAdapter.submitList(listings);
            }
        });

        viewModel.getUserReviews().observe(getViewLifecycleOwner(), userReviews -> {
            int maxReviewsToShow = 2;
            if (userReviews != null) {
                if (userReviews.size() > maxReviewsToShow) {
                    reviewAdapter.submitList(userReviews.subList(0, maxReviewsToShow));
                    btnViewAllReviews.setVisibility(View.VISIBLE);
                } else {
                    reviewAdapter.submitList(userReviews);
                    btnViewAllReviews.setVisibility(View.GONE);
                }
            }
        });

        viewModel.isLoading().observe(getViewLifecycleOwner(), isLoading -> {
            if (progressBar != null) progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            if (contentLayout != null) contentLayout.setVisibility(isLoading ? View.GONE : View.VISIBLE);
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
                navController.popBackStack();
            }
        });
    }

    @SuppressLint("SetTextI18n")
    private void updateProfileUI(User user) {
        if (user == null || getContext() == null) return;

        textDisplayName.setText(user.getName());
        textBio.setText(user.getBio());
        textLocation.setText(user.getAddress());

        if (user.getMemberSince() != null) {
            Locale vietnameseLocale = new Locale("vi", "VN");
            SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", vietnameseLocale);
            textMemberSince.setText(getString(R.string.profile_member_since_format, sdf.format(user.getMemberSince())));
        }

        Glide.with(this)
                .load(user.getProfileImageUrl())
                .placeholder(R.drawable.ic_profile_placeholder)
                .error(R.drawable.ic_profile_placeholder)
                .circleCrop()
                .into(profileImage);

        textRatingInfo.setText(getString(R.string.profile_rating_info_format, user.getRating(), user.getReviewCount()));
        textRatingStars.setText(getStarString(user.getRating()));

        statsActiveListings.setText(String.valueOf(user.getActiveListingsCount()));
        statsCompletedSales.setText(String.valueOf(user.getCompletedSalesCount()));
        statsReviews.setText(String.valueOf(user.getReviewCount()));

        // Lấy tổng số tin đăng từ đối tượng User
        int totalActiveListings = user.getActiveListingsCount();

        // Hiển thị nút nếu tổng số tin đăng lớn hơn 4
        if (totalActiveListings > 2) {
            btnViewAllListings.setVisibility(View.VISIBLE);
        } else {
            btnViewAllListings.setVisibility(View.GONE);
        }
    }

    private String getStarString(double rating) {
        int fullStars = (int) Math.round(rating);
        StringBuilder stars = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            stars.append(i < fullStars ? "★" : "☆");
        }
        return stars.toString();
    }

    private void showReportDialog() {
        if (getContext() == null || userId == null) return;
        final String[] reportReasons = {"Lừa đảo/Gian lận", "Nội dung không phù hợp", "Spam", "Lý do khác"};

        new AlertDialog.Builder(getContext())
                .setTitle("Báo cáo người dùng")
                .setItems(reportReasons, (dialog, which) -> {
                    String reason = reportReasons[which];
                    sendReport(reason);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void sendReport(String reason) {
        String reporterId = FirebaseAuth.getInstance().getUid();
        if (reporterId == null) {
            Toast.makeText(getContext(), "Bạn cần đăng nhập để báo cáo.", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> report = new HashMap<>();
        report.put("reporterId", reporterId);
        report.put("reportedUserId", userId);
        report.put("reason", reason);
        report.put("timestamp", FieldValue.serverTimestamp());
        report.put("type", "profile");
        report.put("status", "pending");

        FirebaseFirestore.getInstance().collection("reports").add(report)
                .addOnSuccessListener(documentReference ->
                        Toast.makeText(getContext(), "Cảm ơn bạn đã gửi báo cáo.", Toast.LENGTH_LONG).show()
                )
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Gửi báo cáo thất bại. Vui lòng thử lại.", Toast.LENGTH_SHORT).show()
                );
    }
    private void showGrantAdminConfirmationDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Xác nhận Cấp quyền")
                .setMessage("Bạn có chắc chắn muốn cấp quyền Quản trị viên cho người dùng này không? Hành động này không thể dễ dàng hoàn tác.")
                .setPositiveButton("Đồng ý", (dialog, which) -> grantAdminRole())
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void grantAdminRole() {
        Toast.makeText(getContext(), "Đang xử lý...", Toast.LENGTH_SHORT).show();

        Map<String, Object> data = new HashMap<>();
        data.put("uid", userId); // userId là ID của người đang được xem hồ sơ

        FirebaseFunctions.getInstance()
                .getHttpsCallable("grantAdminRole")
                .call(data)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(getContext(), "Cấp quyền Admin thành công!", Toast.LENGTH_LONG).show();
                    } else {
                        String errorMessage = task.getException() != null ? task.getException().getMessage() : "Lỗi không xác định.";
                        Toast.makeText(getContext(), "Lỗi: " + errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }
}