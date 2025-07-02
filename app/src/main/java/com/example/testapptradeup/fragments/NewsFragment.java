package com.example.testapptradeup.fragments;

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

import com.bumptech.glide.Glide;
import com.example.testapptradeup.R;
import com.example.testapptradeup.models.Listing;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

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

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private TextView btnAll, btnActive, btnPaused, btnSold;
    private ImageView btnSortDropdown;

    private String currentFilterStatus = "all";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        loadPosts();
        return view;
    }

    private void initViews(View view) {
        managePostsRecyclerView = view.findViewById(R.id.manage_posts_recycler_view);
        emptyState = view.findViewById(R.id.manage_empty_state);
        loadingState = view.findViewById(R.id.manage_loading_state);
        headerTitle = view.findViewById(R.id.header_title);
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout);
        btnCreatePost = view.findViewById(R.id.btn_create_post);
        btnAll = view.findViewById(R.id.btn_all);
        btnActive = view.findViewById(R.id.btn_active);
        btnPaused = view.findViewById(R.id.btn_paused);
        btnSold = view.findViewById(R.id.btn_sold);
        btnSortDropdown = view.findViewById(R.id.btn_sort_dropdown);
    }

    private void setupRecyclerView() {
        postList = new ArrayList<>();
        adapter = new ManagePostAdapter(postList, this::onViewPost, this::onEditPost, this::onDeletePost);
        managePostsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        managePostsRecyclerView.setAdapter(adapter);
    }

    private void setupSwipeRefresh() {
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(this::refreshPosts);
            swipeRefreshLayout.setColorSchemeResources(R.color.text_primary, R.color.accent_color);
        }
    }

    private void setupCreatePostButton() {
        if (btnCreatePost != null) {
            btnCreatePost.setOnClickListener(v -> Toast.makeText(requireContext(), "Mở màn hình tạo tin mới", Toast.LENGTH_SHORT).show());
        }
    }

    private void setupFilterListeners() {
        updateFilterButtonUI(btnAll, true);

        View.OnClickListener filterClickListener = v -> {
            int id = v.getId();
            if (id == R.id.btn_all) {
                currentFilterStatus = "all";
            } else if (id == R.id.btn_active) {
                currentFilterStatus = "available";
            } else if (id == R.id.btn_paused) {
                currentFilterStatus = "paused";
            } else if (id == R.id.btn_sold) {
                currentFilterStatus = "sold";
            }
            updateAllFilterButtons();
            loadPosts();
        };

        btnAll.setOnClickListener(filterClickListener);
        btnActive.setOnClickListener(filterClickListener);
        btnPaused.setOnClickListener(filterClickListener);
        btnSold.setOnClickListener(filterClickListener);
    }

    private void updateAllFilterButtons() {
        updateFilterButtonUI(btnAll, "all".equals(currentFilterStatus));
        updateFilterButtonUI(btnActive, "available".equals(currentFilterStatus));
        updateFilterButtonUI(btnPaused, "paused".equals(currentFilterStatus));
        updateFilterButtonUI(btnSold, "sold".equals(currentFilterStatus));
    }


    private void setupSortListener() {
        if (btnSortDropdown != null) {
            btnSortDropdown.setOnClickListener(v -> showToast("Mở dialog sắp xếp"));
        }
    }

    private void updateFilterButtonUI(TextView button, boolean isSelected) {
        if (button == null) return;
        if (isSelected) {
            button.setBackgroundResource(R.drawable.filter_tab_selected);
            button.setTextColor(requireContext().getColor(R.color.text_primary)); // Use your primary color
            button.setTypeface(null, android.graphics.Typeface.BOLD);
        } else {
            button.setBackgroundResource(android.R.color.transparent);
            button.setTextColor(requireContext().getColor(R.color.text_secondary));
            button.setTypeface(null, android.graphics.Typeface.NORMAL);
        }
    }


    @SuppressLint("NotifyDataSetChanged")
    private void loadPosts() {
        showLoadingState();

        String userId = mAuth.getUid();
        if (userId == null) {
            Log.e(TAG, "User not logged in.");
            showError("Bạn chưa đăng nhập.");
            hideLoadingState();
            return;
        }

        // Tạo query cơ sở
        Query query = db.collection("listings").whereEqualTo("sellerId", userId);

        // Thêm điều kiện lọc nếu không phải "Tất cả"
        if (!"all".equals(currentFilterStatus)) {
            query = query.whereEqualTo("status", currentFilterStatus);
        }

        query.orderBy("timePosted", Query.Direction.DESCENDING) // Sắp xếp theo mới nhất
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    postList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Listing listing = document.toObject(Listing.class);
                        listing.setId(document.getId());
                        postList.add(listing);
                    }
                    adapter.notifyDataSetChanged();
                    hideLoadingState();
                    updateHeaderTitle();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading posts: ", e);
                    showError("Lỗi tải tin: " + e.getMessage());
                    hideLoadingState();
                });
    }


    private void refreshPosts() {
        loadPosts();
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(false);
        }
        showToast("Đã làm mới danh sách");
    }

    private void showLoadingState() {
        if (loadingState != null) loadingState.setVisibility(View.VISIBLE);
        if (managePostsRecyclerView != null) managePostsRecyclerView.setVisibility(View.GONE);
        if (emptyState != null) emptyState.setVisibility(View.GONE);
    }

    private void hideLoadingState() {
        if (loadingState != null) loadingState.setVisibility(View.GONE);
        if (postList.isEmpty()) {
            if (emptyState != null) emptyState.setVisibility(View.VISIBLE);
            if (managePostsRecyclerView != null) managePostsRecyclerView.setVisibility(View.GONE);
        } else {
            if (emptyState != null) emptyState.setVisibility(View.GONE);
            if (managePostsRecyclerView != null) managePostsRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    @SuppressLint("SetTextI18n")
    private void updateHeaderTitle() {
        if (headerTitle != null) {
            headerTitle.setText("Quản lý tin (" + postList.size() + ")");
        }
    }

    private void onViewPost(Listing listing) {
        showToast("Xem chi tiết: " + listing.getTitle());
    }

    private void onEditPost(Listing listing) {
        showToast("Chỉnh sửa: " + listing.getTitle());
    }

    private void onDeletePost(Listing listing, int position) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Xác nhận xóa")
                .setMessage("Bạn có chắc chắn muốn xóa tin \"" + listing.getTitle() + "\"?")
                .setPositiveButton("Xóa", (dialog, which) -> db.collection("listings").document(listing.getId())
                        .delete()
                        .addOnSuccessListener(aVoid -> {
                            postList.remove(position);
                            adapter.notifyItemRemoved(position);
                            adapter.notifyItemRangeChanged(position, postList.size());
                            updateHeaderTitle();
                            hideLoadingState(); // Check empty state again
                            showToast("Đã xóa tin thành công");
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Error deleting post: ", e);
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

    private static class ManagePostAdapter extends RecyclerView.Adapter<ManagePostAdapter.ViewHolder> {
        private final List<Listing> posts;
        private final OnPostViewListener viewListener;
        private final OnPostEditListener editListener;
        private final OnPostDeleteListener deleteListener;

        public interface OnPostViewListener { void onView(Listing listing); }
        public interface OnPostEditListener { void onEdit(Listing listing); }
        public interface OnPostDeleteListener { void onDelete(Listing listing, int position); }

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
            holder.bind(listing, viewListener, editListener, deleteListener);
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

            @SuppressLint({"SetTextI18n", "DefaultLocale"})
            public void bind(Listing listing, OnPostViewListener viewListener,
                             OnPostEditListener editListener, OnPostDeleteListener deleteListener) {
                txtTitle.setText(listing.getTitle());
                txtPrice.setText(listing.getFormattedPrice()); // Sử dụng hàm tiện ích từ model

                if (listing.getTimePosted() != null) {
                    // Hiển thị thời gian tương đối
                    txtPostedTime.setText(android.text.format.DateUtils.getRelativeTimeSpanString(listing.getTimePosted().getTime()));
                } else {
                    txtPostedTime.setText("Thời gian không rõ");
                }

                // *** BẮT ĐẦU SỬA LỖI ***
                // Kiểm tra danh sách imageUrls có tồn tại và không rỗng
                if (listing.getImageUrls() != null && !listing.getImageUrls().isEmpty()) {
                    // Tải ảnh đầu tiên trong danh sách bằng Glide
                    Glide.with(itemView.getContext())
                            .load(listing.getImageUrls().get(0))
                            .placeholder(R.drawable.img)
                            .error(R.drawable.img)
                            .centerCrop()
                            .into(imgPost);
                } else {
                    // Nếu không có ảnh, hiển thị ảnh mặc định
                    imgPost.setImageResource(R.drawable.img);
                }
                // *** KẾT THÚC SỬA LỖI ***

                if (txtStatus != null && listing.getStatus() != null) {
                    txtStatus.setText(getStatusDisplayName(listing.getStatus()));
                    int statusColor;
                    switch (listing.getStatus()) {
                        case "available":
                            statusColor = itemView.getContext().getColor(R.color.success);
                            break;
                        case "paused":
                            statusColor = itemView.getContext().getColor(R.color.warning);
                            break;
                        case "sold":
                            statusColor = itemView.getContext().getColor(R.color.red_error);
                            break;
                        default:
                            statusColor = itemView.getContext().getColor(R.color.text_secondary);
                            break;
                    }
                    txtStatus.setTextColor(statusColor);
                }

                txtLocation.setText(listing.getLocation() != null ? listing.getLocation() : "Không rõ");
                txtViews.setText(String.valueOf(listing.getViews()));
                txtOffers.setText(listing.getOffersCount() + " đề nghị");

                itemView.setOnClickListener(v -> { if (viewListener != null) viewListener.onView(listing); });
                btnEdit.setOnClickListener(v -> { if (editListener != null) editListener.onEdit(listing); });
                btnDelete.setOnClickListener(v -> {
                    if (deleteListener != null) {
                        int position = getBindingAdapterPosition();
                        if (position != RecyclerView.NO_POSITION) {
                            deleteListener.onDelete(listing, position);
                        }
                    }
                });
                btnMoreOptions.setOnClickListener(v -> Toast.makeText(itemView.getContext(), "Tùy chọn khác cho " + listing.getTitle(), Toast.LENGTH_SHORT).show());
            }

            private String getStatusDisplayName(String status) {
                if (status == null) return "Không rõ";
                switch (status) {
                    case "available": return "Đang hiển thị";
                    case "paused": return "Tạm dừng";
                    case "sold": return "Đã bán";
                    case "expired": return "Hết hạn";
                    default: return status;
                }
            }
        }
    }
}