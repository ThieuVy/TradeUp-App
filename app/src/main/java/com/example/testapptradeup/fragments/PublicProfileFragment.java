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
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.testapptradeup.R;
import com.example.testapptradeup.adapters.PublicListingAdapter;
import com.example.testapptradeup.adapters.PublicReviewAdapter;
import com.example.testapptradeup.models.Listing;
import com.example.testapptradeup.models.Review;
import com.example.testapptradeup.models.User;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PublicProfileFragment extends Fragment {

    private String userId;
    private NavController navController;
    private FirebaseFirestore db;

    // UI Elements
    private ImageView profileImage, btnBack;
    private TextView textDisplayName, textMemberSince, textRatingStars, textRatingInfo, textBio, textLocation;
    private TextView statsActiveListings, statsCompletedSales, statsReviews;
    private RecyclerView recyclerActiveListings, recyclerReviews;

    private PublicListingAdapter listingAdapter;
    private PublicReviewAdapter reviewAdapter;
    private final List<Listing> listings = new ArrayList<>();
    private final List<Review> reviews = new ArrayList<>();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        if (getArguments() != null) {
            userId = getArguments().getString("userId");
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_public_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(view);

        initViews(view);
        setupRecyclerViews();
        setupClickListeners();

        if (userId != null && !userId.isEmpty()) {
            loadUserProfile();
            loadUserListings();
            loadUserReviews();
        } else {
            Toast.makeText(getContext(), R.string.error_invalid_user_id, Toast.LENGTH_SHORT).show();
            navController.popBackStack();
        }
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
        // Listings RecyclerView
        recyclerActiveListings.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        listingAdapter = new PublicListingAdapter(getContext(), listings);
        recyclerActiveListings.setAdapter(listingAdapter);

        // Reviews RecyclerView
        recyclerReviews.setLayoutManager(new LinearLayoutManager(getContext()));
        reviewAdapter = new PublicReviewAdapter(getContext(), reviews);
        recyclerReviews.setAdapter(reviewAdapter);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> navController.popBackStack());
    }

    private void loadUserProfile() {
        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            updateProfileUI(user);
                        }
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), R.string.error_loading_profile, Toast.LENGTH_SHORT).show());
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
            textMemberSince.setText(getString(R.string.member_since_format, formattedDate));
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
        textRatingInfo.setText(getString(R.string.rating_info_format, user.getRating(), user.getReviewCount()));
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

    @SuppressLint("NotifyDataSetChanged")
    private void loadUserListings() {
        db.collection("listings")
                .whereEqualTo("sellerId", userId)
                .whereEqualTo("status", "active")
                .orderBy("timePosted", Query.Direction.DESCENDING)
                .limit(10)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                        listings.clear();
                        listings.addAll(queryDocumentSnapshots.toObjects(Listing.class));
                        listingAdapter.notifyDataSetChanged();
                    }
                });
    }

    @SuppressLint("NotifyDataSetChanged")
    private void loadUserReviews() {
        db.collection("reviews")
                .whereEqualTo("reviewedUserId", userId)
                .orderBy("reviewDate", Query.Direction.DESCENDING)
                .limit(3)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                        reviews.clear();
                        reviews.addAll(queryDocumentSnapshots.toObjects(Review.class));
                        reviewAdapter.notifyDataSetChanged();
                    }
                });
    }
}