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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import com.example.testapptradeup.R;

public class PublicProfileFragment extends Fragment {

    private ImageView profileImage, backButton, moreOptions;
    private TextView displayName, onlineStatus, rating, joinDate, location, bio;
    private TextView totalSales, totalReviews, responseRate;
    private TextView viewAllReviews, viewAllProducts, reportUser;
    private MaterialButton messageButton, followButton;
    private RecyclerView recentProductsRecycler;
    private FirebaseFirestore firestore;
    private String currentUserId;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_public_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeViews(view);
        setupFirestore();
        setupClickListeners();

        // Nhận userId từ Bundle
        Bundle arguments = getArguments();
        if (arguments != null) {
            currentUserId = arguments.getString("userId");
            if (currentUserId != null && !currentUserId.isEmpty()) {
                loadUserProfile(currentUserId);
            } else {
                showError("ID người dùng không hợp lệ");
            }
        } else {
            showError("Không tìm thấy thông tin người dùng");
        }
    }

    private void initializeViews(View view) {
        // Header views
        profileImage = view.findViewById(R.id.profile_image);
        backButton = view.findViewById(R.id.back_button);
        moreOptions = view.findViewById(R.id.more_options);

        // Profile info views
        displayName = view.findViewById(R.id.display_name);
        onlineStatus = view.findViewById(R.id.online_status);
        rating = view.findViewById(R.id.rating);
        joinDate = view.findViewById(R.id.join_date);
        location = view.findViewById(R.id.location);
        bio = view.findViewById(R.id.bio);

        // Statistics views
        totalSales = view.findViewById(R.id.total_sales);
        totalReviews = view.findViewById(R.id.total_reviews);
        responseRate = view.findViewById(R.id.response_rate);

        // Action buttons
        messageButton = view.findViewById(R.id.message_button);
        followButton = view.findViewById(R.id.follow_button);

        // Other interactive elements
        viewAllReviews = view.findViewById(R.id.view_all_reviews);
        viewAllProducts = view.findViewById(R.id.view_all_products);
        reportUser = view.findViewById(R.id.report_user);

        // RecyclerView
        recentProductsRecycler = view.findViewById(R.id.recent_products_recycler);
    }

    private void setupFirestore() {
        firestore = FirebaseFirestore.getInstance();
    }

    private void setupClickListeners() {
        // Back button
        backButton.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });

        // More options menu
        moreOptions.setOnClickListener(v -> showMoreOptionsMenu());

        // Action buttons
        messageButton.setOnClickListener(v -> openMessageDialog());
        followButton.setOnClickListener(v -> toggleFollowUser());

        // View all links
        viewAllReviews.setOnClickListener(v -> openAllReviews());
        viewAllProducts.setOnClickListener(v -> openUserStore());

        // Report user
        reportUser.setOnClickListener(v -> showReportDialog());
    }

    @SuppressLint("DefaultLocale")
    private void loadUserProfile(String userId) {
        // Show loading state
        showLoadingState(true);

        firestore.collection("users").document(userId)
                .get()
                .addOnSuccessListener(document -> {
                    showLoadingState(false);

                    if (document.exists()) {
                        try {
                            // Get user data with null checks
                            String name = document.getString("name");
                            String imageUrl = document.getString("profileImageUrl");
                            Boolean isOnlineObj = document.getBoolean("isOnline");
                            boolean isOnline = isOnlineObj != null ? isOnlineObj : false;

                            Double ratingValue = document.getDouble("rating");
                            Long reviewCountValue = document.getLong("reviewCount");
                            String joinDateValue = document.getString("joinDate");
                            String locationValue = document.getString("location");
                            String bioValue = document.getString("bio");
                            Long soldCountValue = document.getLong("soldCount");
                            Double responseRateValue = document.getDouble("responseRate");

                            // Update UI with null checks
                            displayName.setText(name != null ? name : "Tên không xác định");

                            // Online status
                            onlineStatus.setText(isOnline ? "Online" : "Offline");
                            onlineStatus.setTextColor(getResources().getColor(
                                    isOnline ? R.color.success : R.color.text_secondary));

                            // Rating
                            double rating = ratingValue != null ? ratingValue : 0.0;
                            long reviewCount = reviewCountValue != null ? reviewCountValue : 0;
                            this.rating.setText(String.format("★ %.1f (%d đánh giá)", rating, reviewCount));

                            // Join date
                            joinDate.setText(joinDateValue != null ? joinDateValue : "Chưa xác định");

                            // Location
                            location.setText(locationValue != null ? locationValue : "Chưa cập nhật");

                            // Bio
                            bio.setText(bioValue != null ? bioValue : "Chưa có mô tả");

                            // Statistics
                            long soldCount = soldCountValue != null ? soldCountValue : 0;
                            totalSales.setText(String.valueOf(soldCount));
                            totalReviews.setText(String.valueOf(reviewCount));

                            double responseRateVal = responseRateValue != null ? responseRateValue : 0.0;
                            responseRate.setText(String.format("%.0f%%", responseRateVal));

                            // Load profile image
                            loadProfileImage(imageUrl);

                            // Load additional data
                            loadRecentProducts(userId);

                        } catch (Exception e) {
                            showError("Lỗi khi hiển thị thông tin: " + e.getMessage());
                        }
                    } else {
                        showError("Không tìm thấy người dùng");
                    }
                })
                .addOnFailureListener(e -> {
                    showLoadingState(false);
                    showError("Không thể tải hồ sơ: " + e.getMessage());
                });
    }

    private void loadProfileImage(String imageUrl) {
        if (getContext() == null) return;

        Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.img)
                .error(R.drawable.img)
                .transform(new CircleCrop())
                .into(profileImage);
    }

    private void loadRecentProducts(String userId) {
        firestore.collection("items")
                .whereEqualTo("sellerId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(10)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Item> items = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        try {
                            Item item = doc.toObject(Item.class);
                            item.setId(doc.getId()); // Set document ID
                            items.add(item);
                        } catch (Exception e) {
                            // Log error but continue processing other items
                            e.printStackTrace();
                        }
                    }

                    if (getContext() != null) {
                        setupProductsRecyclerView(items);
                    }
                })
                .addOnFailureListener(e -> showError("Không thể tải sản phẩm: " + e.getMessage()));
    }

    private void setupProductsRecyclerView(List<Item> items) {
        if (recentProductsRecycler != null && getContext() != null) {
            LinearLayoutManager layoutManager = new LinearLayoutManager(
                    getContext(), LinearLayoutManager.HORIZONTAL, false);
            recentProductsRecycler.setLayoutManager(layoutManager);

            ItemAdapter adapter = new ItemAdapter(items);
            recentProductsRecycler.setAdapter(adapter);
        }
    }

    @SuppressLint("SetTextI18n")
    private void showLoadingState(boolean isLoading) {
        // You can implement loading indicators here
        if (isLoading) {
            displayName.setText("Đang tải...");
        }
    }

    private void showError(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    // Click handler methods
    private void showMoreOptionsMenu() {
        // Implement more options menu (share profile, block user, etc.)
        Toast.makeText(getContext(), "Tùy chọn khác", Toast.LENGTH_SHORT).show();
    }

    private void openMessageDialog() {
        // Implement messaging functionality
        Toast.makeText(getContext(), "Mở tin nhắn", Toast.LENGTH_SHORT).show();
    }

    private void toggleFollowUser() {
        // Implement follow/unfollow functionality
        Toast.makeText(getContext(), "Theo dõi/Bỏ theo dõi", Toast.LENGTH_SHORT).show();
    }

    private void openAllReviews() {
        // Navigate to all reviews screen
        Toast.makeText(getContext(), "Xem tất cả đánh giá", Toast.LENGTH_SHORT).show();
    }

    private void openUserStore() {
        // Navigate to user's store
        Toast.makeText(getContext(), "Xem cửa hàng", Toast.LENGTH_SHORT).show();
    }

    private void showReportDialog() {
        // Show report user dialog
        Toast.makeText(getContext(), "Báo cáo người dùng", Toast.LENGTH_SHORT).show();
    }

    // Helper method to create bundle for navigation
    public static PublicProfileFragment newInstance(String userId) {
        PublicProfileFragment fragment = new PublicProfileFragment();
        Bundle args = new Bundle();
        args.putString("userId", userId);
        fragment.setArguments(args);
        return fragment;
    }

    // Adapter cho RecyclerView hiển thị sản phẩm
    private class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.ItemViewHolder> {
        private final List<Item> items;

        ItemAdapter(List<Item> items) {
            this.items = items != null ? items : new ArrayList<>();
        }

        @NonNull
        @Override
        public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_product_small, parent, false);
            return new ItemViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
            Item item = items.get(position);
            if (item != null) {
                holder.bind(item);
            }
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class ItemViewHolder extends RecyclerView.ViewHolder {
            TextView productName, productPrice;
            ImageView productImage;

            ItemViewHolder(View itemView) {
                super(itemView);
                productName = itemView.findViewById(R.id.product_name);
                productPrice = itemView.findViewById(R.id.product_price);
                productImage = itemView.findViewById(R.id.product_image);
            }

            @SuppressLint("SetTextI18n")
            void bind(Item item) {
                if (item.getName() != null) {
                    productName.setText(item.getName());
                } else {
                    productName.setText("Sản phẩm không tên");
                }

                if (item.getPrice() != null) {
                    productPrice.setText(String.format(Locale.getDefault(),
                            "₫%,.0f", item.getPrice()));
                } else {
                    productPrice.setText("Liên hệ");
                }

                // Load product image
                if (getContext() != null) {
                    Glide.with(itemView.getContext())
                            .load(item.getImageUrl())
                            .placeholder(R.drawable.img)
                            .error(R.drawable.img)
                            .centerCrop()
                            .into(productImage);
                }

                // Set click listener for item
                itemView.setOnClickListener(v -> {
                    // Navigate to product details
                    if (item.getId() != null) {
                        // Implement navigation to product detail
                        Toast.makeText(v.getContext(),
                                "Xem chi tiết: " + item.getName(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Clean up resources if needed
    }
}