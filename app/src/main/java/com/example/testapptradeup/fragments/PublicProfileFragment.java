package com.example.testapptradeup.fragments;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.testapptradeup.R;
import com.example.testapptradeup.adapters.PublicReviewAdapter;
import com.example.testapptradeup.models.Listing; // <<< SỬA LỖI: Thêm import quan trọng này
import com.example.testapptradeup.models.User;
import com.example.testapptradeup.viewmodels.PublicProfileViewModel;
import com.google.android.flexbox.FlexboxLayout; // <<< THÊM MỚI: Import FlexboxLayout
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List; // <<< SỬA LỖI: Đảm bảo import java.util.List
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
    private TextView statsActiveListings, statsCompletedSales, statsReviews;
    private TextView btnViewAllListings, btnViewAllReviews;

    // SỬA LỖI: Chỉ cần FlexboxLayout và RecyclerView cho reviews
    private FlexboxLayout flexboxActiveListings;
    private RecyclerView recyclerReviews;
    private PublicReviewAdapter reviewAdapter;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(PublicProfileViewModel.class);
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
        setupReviewRecyclerView();
        setupClickListeners();
        observeViewModel();

        if (userId != null && !userId.isEmpty()) {
            viewModel.loadProfileData(userId);
        } else {
            Toast.makeText(getContext(), "Lỗi: ID người dùng không hợp lệ.", Toast.LENGTH_SHORT).show();
            navController.popBackStack();
        }
    }

    private void observeViewModel() {
        viewModel.getUserProfile().observe(getViewLifecycleOwner(), this::updateProfileUI);

        // SỬA LỖI: Gọi đúng hàm populateListingsFlexbox
        viewModel.getUserListings().observe(getViewLifecycleOwner(), this::populateListingsFlexbox);

        viewModel.getUserReviews().observe(getViewLifecycleOwner(), userReviews -> {
            int maxReviewsToShow = 2;
            if (userReviews.size() > maxReviewsToShow) {
                reviewAdapter.submitList(userReviews.subList(0, maxReviewsToShow));
                btnViewAllReviews.setVisibility(View.VISIBLE);
            } else {
                reviewAdapter.submitList(userReviews);
                btnViewAllReviews.setVisibility(View.GONE);
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
        statsActiveListings = view.findViewById(R.id.stats_active_listings);
        statsCompletedSales = view.findViewById(R.id.stats_completed_sales);
        statsReviews = view.findViewById(R.id.stats_reviews);
        flexboxActiveListings = view.findViewById(R.id.flexbox_active_listings);
        recyclerReviews = view.findViewById(R.id.recycler_reviews);
        btnViewAllListings = view.findViewById(R.id.btn_view_all_listings);
        btnViewAllReviews = view.findViewById(R.id.btn_view_all_reviews);
        btnMoreOptions = view.findViewById(R.id.btn_more_options);
    }

    private void setupReviewRecyclerView() {
        recyclerReviews.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerReviews.setNestedScrollingEnabled(false);
        reviewAdapter = new PublicReviewAdapter();
        recyclerReviews.setAdapter(reviewAdapter);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> navController.popBackStack());
        btnMoreOptions.setOnClickListener(v -> showReportDialog());

        btnViewAllListings.setOnClickListener(v -> Toast.makeText(getContext(), "Xem tất cả sản phẩm của " + textDisplayName.getText(), Toast.LENGTH_SHORT).show());

        btnViewAllReviews.setOnClickListener(v -> Toast.makeText(getContext(), "Xem tất cả đánh giá cho " + textDisplayName.getText(), Toast.LENGTH_SHORT).show());
    }

    private void populateListingsFlexbox(List<Listing> listings) {
        if (getContext() == null || listings == null) return;
        flexboxActiveListings.removeAllViews();
        int maxItemsToShow = 4;

        if (listings.size() > maxItemsToShow) {
            btnViewAllListings.setVisibility(View.VISIBLE);
        } else {
            btnViewAllListings.setVisibility(View.GONE);
        }

        for (int i = 0; i < listings.size() && i < maxItemsToShow; i++) {
            Listing listing = listings.get(i);

            LayoutInflater inflater = LayoutInflater.from(getContext());
            View itemView = inflater.inflate(R.layout.item_public_listing, flexboxActiveListings, false);

            ImageView listingImage = itemView.findViewById(R.id.listing_image);
            TextView listingTitle = itemView.findViewById(R.id.listing_title);
            TextView listingPrice = itemView.findViewById(R.id.listing_price);
            TextView listingStatus = itemView.findViewById(R.id.listing_status);

            listingTitle.setText(listing.getTitle());
            listingPrice.setText(listing.getFormattedPrice());

            if ("available".equalsIgnoreCase(listing.getStatus())) {
                listingStatus.setVisibility(View.VISIBLE);
                listingStatus.setText(R.string.listing_status_available);
            } else {
                listingStatus.setVisibility(View.GONE);
            }

            Glide.with(this)
                    .load(listing.getPrimaryImageUrl())
                    .placeholder(R.drawable.img_placeholder)
                    .error(R.drawable.img_placeholder)
                    .into(listingImage);

            itemView.setOnClickListener(v -> Toast.makeText(getContext(), "Mở chi tiết: " + listing.getTitle(), Toast.LENGTH_SHORT).show());

            flexboxActiveListings.addView(itemView);
        }
    }

    @SuppressLint("SetTextI18n")
    private void updateProfileUI(User user) {
        if (user == null) return;
        textDisplayName.setText(user.getName());
        textBio.setText(user.getBio());
        textLocation.setText(user.getAddress());

        if (user.getMemberSince() != null) {
            Locale vietnameseLocale = new Locale("vi", "VN");
            SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", vietnameseLocale);
            textMemberSince.setText(getString(R.string.profile_member_since_format, sdf.format(user.getMemberSince())));
        }

        if (getContext() != null && user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
            Glide.with(getContext())
                    .load(user.getProfileImageUrl())
                    .placeholder(R.drawable.ic_profile_placeholder)
                    .error(R.drawable.ic_profile_placeholder)
                    .circleCrop()
                    .into(profileImage);
        }

        textRatingInfo.setText(getString(R.string.profile_rating_info_format, user.getRating(), user.getReviewCount()));
        textRatingStars.setText(getStarString(user.getRating()));

        statsActiveListings.setText(String.valueOf(user.getActiveListingsCount()));
        statsCompletedSales.setText(String.valueOf(user.getCompletedSalesCount()));
        statsReviews.setText(String.valueOf(user.getReviewCount()));
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
}