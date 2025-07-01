package com.example.testapptradeup.fragments; // Hoặc package chính xác của bạn

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
// Added for navigation
// Added for navigation

import com.bumptech.glide.Glide; // Import Glide for image loading
import com.example.testapptradeup.R;
import com.example.testapptradeup.models.Listing; // Sử dụng lại Product model của bạn
import com.google.firebase.auth.FirebaseAuth; // Firebase Auth để lấy UID
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
// For sorting
import java.util.Date;
import java.util.List;
import java.util.Locale; // For SimpleDateFormat

public class NewsFragment extends Fragment {

    private static final String TAG = "NewsFragment";

    private RecyclerView managePostsRecyclerView;
    private ManagePostAdapter adapter;
    private List<Listing> postList;
    private LinearLayout emptyState;
    private LinearLayout loadingState;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView headerTitle;
    private Button btnCreatePost;

    // Firebase instances
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    // Filter buttons
    private TextView btnAll, btnActive, btnPaused, btnSold;
    private ImageView btnSortDropdown;

    private String currentFilterStatus = "all"; // Mặc định là "Tất cả"

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_newsmanage, container, false);
        initViews(view);
        setupRecyclerView();
        setupSwipeRefresh();
        setupFilterListeners();
        setupSortListener();
        setupCreatePostButton();
        loadPosts(); // Load posts from Firebase
        return view;
    }

    @SuppressLint("WrongViewCast")
    private void initViews(View view) {
        managePostsRecyclerView = view.findViewById(R.id.manage_posts_recycler_view);
        emptyState = view.findViewById(R.id.manage_empty_state);
        loadingState = view.findViewById(R.id.manage_loading_state);
        headerTitle = view.findViewById(R.id.header_title);
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout);
        btnCreatePost = view.findViewById(R.id.btn_create_post);

        // Ánh xạ các nút lọc trạng thái
        btnAll = view.findViewById(R.id.btn_all);
        btnActive = view.findViewById(R.id.btn_active);
        btnPaused = view.findViewById(R.id.btn_paused);
        btnSold = view.findViewById(R.id.btn_sold);

        // Ánh xạ nút sắp xếp
        btnSortDropdown = view.findViewById(R.id.btn_sort_dropdown);
    }

    private void setupRecyclerView() {
        postList = new ArrayList<>();
        // Truyền thêm this::onViewPost vào constructor của adapter
        adapter = new ManagePostAdapter(postList, this::onViewPost, this::onEditPost, this::onDeletePost);
        managePostsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        managePostsRecyclerView.setAdapter(adapter);
    }

    private void setupSwipeRefresh() {
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(this::refreshPosts);
            swipeRefreshLayout.setColorSchemeResources(
                    R.color.background_light,
                    R.color.charcoal_black
            );
        }
    }

    private void setupCreatePostButton() {
        if (btnCreatePost != null) {
            btnCreatePost.setOnClickListener(v -> Toast.makeText(requireContext(), "Mở màn hình tạo tin mới", Toast.LENGTH_SHORT).show());
            // TODO: Navigate to CreatePostFragment or Activity for creating new post
        }
    }

    private void setupFilterListeners() {
        // Handle initial selection for "Tất cả"
        updateFilterButtonUI(btnAll, true);

        btnAll.setOnClickListener(v -> {
            currentFilterStatus = "all";
            updateFilterButtonUI(btnAll, true);
            updateFilterButtonUI(btnActive, false);
            updateFilterButtonUI(btnPaused, false);
            updateFilterButtonUI(btnSold, false);
            loadPosts();
        });

        btnActive.setOnClickListener(v -> {
            currentFilterStatus = "available";
            updateFilterButtonUI(btnAll, false);
            updateFilterButtonUI(btnActive, true);
            updateFilterButtonUI(btnPaused, false);
            updateFilterButtonUI(btnSold, false);
            loadPosts();
        });

        btnPaused.setOnClickListener(v -> {
            currentFilterStatus = "paused";
            updateFilterButtonUI(btnAll, false);
            updateFilterButtonUI(btnActive, false);
            updateFilterButtonUI(btnPaused, true);
            updateFilterButtonUI(btnSold, false);
            loadPosts();
        });

        btnSold.setOnClickListener(v -> {
            currentFilterStatus = "sold";
            updateFilterButtonUI(btnAll, false);
            updateFilterButtonUI(btnActive, false);
            updateFilterButtonUI(btnPaused, false);
            updateFilterButtonUI(btnSold, true);
            loadPosts();
        });
    }

    private void setupSortListener() {
        if (btnSortDropdown != null) {
            btnSortDropdown.setOnClickListener(v -> showToast("Mở dialog sắp xếp"));
            // TODO: Implement a dialog or bottom sheet for sorting options
        }
    }

    private void updateFilterButtonUI(TextView button, boolean isSelected) {
        if (isSelected) {
            button.setBackgroundResource(R.drawable.filter_tab_selected);
            button.setTextColor(requireContext().getColor(R.color.accent_color));
            // Assuming you have a style defined for bold text for selected state
            // button.setTextAppearance(R.style.TextAppearance_AppCompat_Body2);
            // Or set typeface directly
            button.setTypeface(null, android.graphics.Typeface.BOLD);
        } else {
            button.setBackgroundResource(android.R.color.transparent);
            button.setTextColor(requireContext().getColor(R.color.text_secondary));
            // Assuming you have a style defined for normal text for unselected state
            // button.setTextAppearance(R.style.TextAppearance_AppCompat_Body1);
            // Or set typeface directly
            button.setTypeface(null, android.graphics.Typeface.NORMAL);
        }
    }


    @SuppressLint("NotifyDataSetChanged")
    private void loadPosts() {
        showLoadingState();

        String userId = null;
        if (mAuth.getCurrentUser() != null) {
            userId = mAuth.getCurrentUser().getUid();
        }

        if (userId == null) {
            Log.e(TAG, "User not logged in. Cannot load posts.");
            showError("Bạn chưa đăng nhập. Vui lòng đăng nhập để xem tin của bạn.");
            hideLoadingState();
            return;
        }

        db.collection("products")
                .whereEqualTo("ownerId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    postList.clear();
                    List<Listing> fetchedListings = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Listing listing = document.toObject(Listing.class);
                        listing.setId(document.getId());

                        if (currentFilterStatus.equals("all") || (listing.getStatus() != null && listing.getStatus().equals(currentFilterStatus))) {
                            fetchedListings.add(listing);
                        }
                    }

                    fetchedListings.sort((p1, p2) -> {
                        Date d1 = p1.getTimePosted();
                        Date d2 = p2.getTimePosted();
                        if (d1 == null && d2 == null) return 0;
                        if (d1 == null) return 1;
                        if (d2 == null) return -1;
                        return d2.compareTo(d1);
                    });

                    postList.addAll(fetchedListings);
                    adapter.notifyDataSetChanged();
                    hideLoadingState();
                    updateHeaderTitle();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading posts from Firestore: " + e.getMessage(), e);
                    showError("Lỗi tải tin của bạn: " + e.getMessage());
                    hideLoadingState();
                });
    }

    private void refreshPosts() {
        loadPosts();
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(false);
        }
        showToast("Đã cập nhật danh sách tin");
    }

    private void showLoadingState() {
        loadingState.setVisibility(View.VISIBLE);
        managePostsRecyclerView.setVisibility(View.GONE);
        emptyState.setVisibility(View.GONE);
    }

    private void hideLoadingState() {
        loadingState.setVisibility(View.GONE);
        if (postList.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            managePostsRecyclerView.setVisibility(View.GONE);
        } else {
            emptyState.setVisibility(View.GONE);
            managePostsRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void updateHeaderTitle() {
        if (headerTitle != null) {
            String title = "Quản lý tin (" + postList.size() + ")";
            headerTitle.setText(title);
        }
    }

    // Callback methods for adapter
    private void onViewPost(Listing listing) {
        showToast("Xem chi tiết: " + listing.getTitle());
        // TODO: Navigate to ProductDetailFragment/Activity, passing product ID
    }

    private void onEditPost(Listing listing) {
        showToast("Chỉnh sửa: " + listing.getTitle());
        /* TODO: Implement edit functionality, e.g., navigate to EditPostFragment */
    }

    private void onDeletePost(Listing listing, int position) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Xác nhận xóa")
                .setMessage("Bạn có chắc chắn muốn xóa tin \"" + listing.getTitle() + "\"?")
                .setPositiveButton("Xóa", (dialog, which) -> db.collection("products").document(listing.getId())
                        .delete()
                        .addOnSuccessListener(aVoid -> {
                            postList.remove(position);
                            adapter.notifyItemRemoved(position);
                            adapter.notifyItemRangeChanged(position, postList.size());

                            updateHeaderTitle();

                            if (postList.isEmpty()) {
                                emptyState.setVisibility(View.VISIBLE);
                                managePostsRecyclerView.setVisibility(View.GONE);
                            }
                            showToast("Đã xóa tin thành công");
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Error deleting post from Firestore: " + e.getMessage(), e);
                            showError("Lỗi xóa tin: " + e.getMessage());
                        }))
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

    // --- RecyclerView Adapter ---
    private static class ManagePostAdapter extends RecyclerView.Adapter<ManagePostAdapter.ViewHolder> {
        private final List<Listing> posts;
        private final OnPostViewListener viewListener; // New listener for viewing
        private final OnPostEditListener editListener;
        private final OnPostDeleteListener deleteListener;

        public interface OnPostViewListener {
            void onView(Listing listing);
        }

        public interface OnPostEditListener {
            void onEdit(Listing listing);
        }

        public interface OnPostDeleteListener {
            void onDelete(Listing listing, int position);
        }

        // Updated constructor to include OnPostViewListener
        public ManagePostAdapter(List<Listing> posts, OnPostViewListener viewListener,
                                 OnPostEditListener editListener, OnPostDeleteListener deleteListener) {
            this.posts = posts;
            this.viewListener = viewListener;
            this.editListener = editListener;
            this.deleteListener = deleteListener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_post_management, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Listing listing = posts.get(position);
            holder.bind(listing, viewListener, editListener, deleteListener); // Pass new listener
        }

        @Override
        public int getItemCount() {
            return posts.size();
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            private final ImageView imgPost;
            private final TextView txtTitle, txtPrice, txtPostedTime, txtStatus, txtLocation, txtViews, txtOffers;
            private final Button btnEdit, btnDelete;
            private final ImageView btnMoreOptions;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                imgPost = itemView.findViewById(R.id.img_post);
                txtTitle = itemView.findViewById(R.id.txt_title);
                txtPrice = itemView.findViewById(R.id.txt_price);
                txtPostedTime = itemView.findViewById(R.id.txt_posted_time);
                txtStatus = itemView.findViewById(R.id.txt_status);
                txtLocation = itemView.findViewById(R.id.txt_location);
                txtViews = itemView.findViewById(R.id.txt_views);
                txtOffers = itemView.findViewById(R.id.txt_offers);
                btnEdit = itemView.findViewById(R.id.btn_edit);
                btnDelete = itemView.findViewById(R.id.btn_delete);
                btnMoreOptions = itemView.findViewById(R.id.btn_more_options);
            }

            @SuppressLint("SetTextI18n")
            public void bind(Listing listing, OnPostViewListener viewListener, // New listener parameter
                             OnPostEditListener editListener, OnPostDeleteListener deleteListener) {
                txtTitle.setText(listing.getTitle());
                txtPrice.setText(String.format(Locale.getDefault(), "%.0f đ", listing.getPrice()));

                if (listing.getTimePosted() != null) {
                    long diffMillis = System.currentTimeMillis() - listing.getTimePosted().getTime();
                    long seconds = diffMillis / 1000;
                    long minutes = seconds / 60;
                    long hours = minutes / 60;
                    long days = hours / 24;

                    if (seconds < 60) {
                        txtPostedTime.setText("Vừa xong");
                    } else if (minutes < 60) {
                        txtPostedTime.setText(minutes + " phút trước");
                    } else if (hours < 24) {
                        txtPostedTime.setText(hours + " giờ trước");
                    } else if (days == 1) {
                        txtPostedTime.setText("Đăng hôm qua");
                    } else if (days < 7) {
                        txtPostedTime.setText(days + " ngày trước");
                    } else {
                        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                        txtPostedTime.setText("Đăng ngày " + sdf.format(listing.getTimePosted()));
                    }
                } else {
                    txtPostedTime.setText("Thời gian không rõ");
                }

                if (listing.getImageUrl() != null && !listing.getImageUrl().isEmpty()) {
                    Glide.with(itemView.getContext())
                            .load(listing.getImageUrl())
                            .placeholder(R.drawable.img)
                            .error(R.drawable.img)
                            .into(imgPost);
                } else {
                    imgPost.setImageResource(R.drawable.img);
                }

                if (txtStatus != null && listing.getStatus() != null) {
                    txtStatus.setText(getStatusDisplayName(listing.getStatus()));
                    int statusColor;
                    switch (listing.getStatus()) {
                        case "available":
                            statusColor = itemView.getContext().getColor(android.R.color.holo_green_dark);
                            break;
                        case "paused":
                            statusColor = itemView.getContext().getColor(android.R.color.holo_orange_dark);
                            break;
                        case "sold":
                            statusColor = itemView.getContext().getColor(android.R.color.holo_red_dark);
                            break;
                        default:
                            statusColor = itemView.getContext().getColor(android.R.color.darker_gray);
                            break;
                    }
                    txtStatus.setTextColor(statusColor);
                }

                txtLocation.setText(listing.getLocation() != null ? listing.getLocation() : "Không rõ");
                txtViews.setText(String.valueOf(listing.getViews()));
                txtOffers.setText(listing.getOffersCount() + " đề nghị");

                // Set listener for the entire item view
                itemView.setOnClickListener(v -> {
                    if (viewListener != null) {
                        viewListener.onView(listing);
                    }
                });

                btnEdit.setOnClickListener(v -> {
                    if (editListener != null) {
                        editListener.onEdit(listing);
                    }
                });

                btnDelete.setOnClickListener(v -> {
                    if (deleteListener != null) {
                        int position = getBindingAdapterPosition();
                        if (position != RecyclerView.NO_POSITION) {
                            deleteListener.onDelete(listing, position);
                        }
                    }
                });

                btnMoreOptions.setOnClickListener(v -> {
                    // SỬA LỖI TẠI ĐÂY: Gọi Toast.makeText trực tiếp
                    Toast.makeText(itemView.getContext(), "Mở tùy chọn khác cho " + listing.getTitle(), Toast.LENGTH_SHORT).show();
                });
            }

            private String getStatusDisplayName(String status) {
                switch (status) {
                    case "available": return "Đang hiển thị";
                    case "paused": return "Tạm dừng";
                    case "sold": return "Đã bán";
                    case "expired": return "Hết hạn";
                    default: return status;
                }
            }
            // Loại bỏ phương thức showToast trùng lặp khỏi ViewHolder
            // private void showToast(View contextView, String message) {
            //     Toast.makeText(contextView.getContext(), message, Toast.LENGTH_SHORT).show();
            // }
        }
    }
}
