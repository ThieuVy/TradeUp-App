package com.example.testapptradeup.fragments;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
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
import com.example.testapptradeup.adapters.PublicListingAdapter;
import com.example.testapptradeup.adapters.PublicReviewAdapter;
import com.example.testapptradeup.models.User;
import com.example.testapptradeup.viewmodels.PublicProfileViewModel; // Import ViewModel

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

public class PublicProfileFragment extends Fragment {

    private String userId;
    private NavController navController;
    private PublicProfileViewModel viewModel;

    // UI Elements
    private ImageView profileImage, btnBack;
    private TextView textDisplayName, textMemberSince, textRatingStars, textRatingInfo, textBio, textLocation;
    private TextView statsActiveListings, statsCompletedSales, statsReviews;
    private RecyclerView recyclerActiveListings, recyclerReviews;

    private PublicListingAdapter listingAdapter;
    private PublicReviewAdapter reviewAdapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Khởi tạo ViewModel
        viewModel = new ViewModelProvider(this).get(PublicProfileViewModel.class);
        if (getArguments() != null) {
            userId = getArguments().getString("userId");
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
            Toast.makeText(getContext(), R.string.error_invalid_user_id, Toast.LENGTH_SHORT).show();
            navController.popBackStack();
        }
    }

    private void observeViewModel() {
        // Các lời gọi .observe() giờ đây hoàn toàn an toàn vì các LiveData trong ViewModel không bao giờ null
        viewModel.getUserProfile().observe(getViewLifecycleOwner(), user -> {
            if (user != null) {
                updateProfileUI(user);
            } else {
                Toast.makeText(getContext(), R.string.error_loading_profile, Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.getUserListings().observe(getViewLifecycleOwner(), userListings -> {
            if (userListings != null) {
                listingAdapter.updateData(userListings);
            }
        });

        viewModel.getUserReviews().observe(getViewLifecycleOwner(), userReviews -> {
            if (userReviews != null) {
                reviewAdapter.updateData(userReviews);
            }
        });
    }

    private void initViews(View view) {
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
        recyclerActiveListings = view.findViewById(R.id.recycler_active_listings);
        recyclerReviews = view.findViewById(R.id.recycler_reviews);
    }

    private void setupRecyclerViews() {
        // Khởi tạo adapter với danh sách rỗng
        recyclerActiveListings.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        listingAdapter = new PublicListingAdapter(getContext(), new ArrayList<>());
        recyclerActiveListings.setAdapter(listingAdapter);

        recyclerReviews.setLayoutManager(new LinearLayoutManager(getContext()));
        reviewAdapter = new PublicReviewAdapter(getContext(), new ArrayList<>());
        recyclerReviews.setAdapter(reviewAdapter);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> navController.popBackStack());
    }

    @SuppressLint("SetTextI18n")
    private void updateProfileUI(User user) {
        textDisplayName.setText(user.getName());
        textBio.setText(user.getBio());
        textLocation.setText(user.getLocation());

        // THAY ĐỔI 1: Sử dụng Locale tiếng Việt cho định dạng ngày
        if (user.getMemberSince() != null) {
            Locale vietnameseLocale = new Locale("vi", "VN");
            SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", vietnameseLocale);
            String formattedDate = sdf.format(user.getMemberSince());
            // Sử dụng format string từ strings.xml
            textMemberSince.setText(getString(R.string.profile_member_since_format, formattedDate));
        }

        if (getContext() != null && user.getProfileImageUrl() != null) {
            Glide.with(getContext())
                    .load(user.getProfileImageUrl())
                    .placeholder(R.drawable.img)
                    .error(R.drawable.img)
                    .circleCrop()
                    .into(profileImage);
        }

        // THAY ĐỔI 2: Sử dụng format string cho thông tin đánh giá
        textRatingInfo.setText(getString(R.string.profile_rating_info_format, user.getRating(), user.getReviewCount()));
        textRatingStars.setText(getStarString(user.getRating()));

        statsActiveListings.setText(String.valueOf(user.getActiveListingsCount()));
        statsCompletedSales.setText(String.valueOf(user.getCompletedSalesCount()));
        statsReviews.setText(String.valueOf(user.getReviewCount()));
    }

    private String getStarString(double rating) {
        int fullStars = (int) Math.round(rating); // Làm tròn để có nửa sao
        StringBuilder stars = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            if (i < fullStars) {
                stars.append("★");
            } else {
                stars.append("☆");
            }
        }
        return stars.toString();
    }
}