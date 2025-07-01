package com.example.testapptradeup.fragments;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.testapptradeup.R;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PostDiscoverFragment extends Fragment {

    private RecyclerView feedRecyclerView;
    private DiscoverPostAdapter adapter;
    private List<ProductDiscover> postList;
    private TabLayout tabLayout;
    private Set<String> followedUsers; // Track followed users

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_discover, container, false);
        initViews(view);
        setupRecyclerView();
        setupListeners();
        loadInitialData();
        return view;
    }

    private void initViews(View view) {
        feedRecyclerView = view.findViewById(R.id.feed_recycler_view);
        tabLayout = view.findViewById(R.id.tab_layout);
    }

    private void setupRecyclerView() {
        postList = new ArrayList<>();
        followedUsers = new HashSet<>();
        adapter = new DiscoverPostAdapter(postList);
        feedRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        feedRecyclerView.setAdapter(adapter);
    }

    private void setupListeners() {
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                String selectedCategory = tab.getText() != null ? tab.getText().toString() : "Tất cả";
                Toast.makeText(requireContext(), "Đã chọn: " + selectedCategory, Toast.LENGTH_SHORT).show();
                loadDataForCategory(selectedCategory);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                String selectedCategory = tab.getText() != null ? tab.getText().toString() : "Tất cả";
                loadDataForCategory(selectedCategory);
            }
        });
    }

    @SuppressLint("NotifyDataSetChanged")
    private void loadInitialData() {
        postList.clear();
        // Load from SharedPreferences (simulating posted data from PostFragment)
        SharedPreferences prefs = requireContext().getSharedPreferences("ProductDraft", Context.MODE_PRIVATE);
        String title = prefs.getString("title", "Sản phẩm mẫu");
        String category = prefs.getString("category", "Khác");
        String price = String.valueOf(prefs.getFloat("price", 0f));
        String location = prefs.getString("location", "HCM");

        // Add sample posts or saved draft
        postList.add(new ProductDiscover("User 1", "Hôm nay", title, price + " đ", location));
        postList.add(new ProductDiscover("User 2", "Hôm qua", "Sản phẩm 2", "3.000.000 đ", "Hà Nội"));
        postList.add(new ProductDiscover("User 3", "Hôm nay", "Sản phẩm 3", "7.500.000 đ", "Đà Nẵng"));

        adapter.notifyDataSetChanged();
    }

    @SuppressLint("NotifyDataSetChanged")
    private void loadDataForCategory(String category) {
        postList.clear();
        SharedPreferences prefs = requireContext().getSharedPreferences("ProductDraft", Context.MODE_PRIVATE);
        String savedTitle = prefs.getString("title", "Sản phẩm mẫu");
        String savedCategory = prefs.getString("category", "Khác");
        String savedPrice = String.valueOf(prefs.getFloat("price", 0f));
        String savedLocation = prefs.getString("location", "HCM");

        // Filter posts based on category (simulated)
        if (category.equals("Tất cả") || savedCategory.equals(category)) {
            postList.add(new ProductDiscover("User 1", "Hôm nay", savedTitle, savedPrice + " đ", savedLocation));
        }
        if (category.equals("Tất cả") || category.equals("Điện thoại & Phụ kiện")) {
            postList.add(new ProductDiscover("User 2", "Hôm qua", "Điện thoại iPhone", "10.000.000 đ", "Hà Nội"));
        }
        if (category.equals("Tất cả") || category.equals("Máy tính & Laptop")) {
            postList.add(new ProductDiscover("User 3", "Hôm nay", "Laptop Dell", "15.000.000 đ", "Đà Nẵng"));
        }

        adapter.notifyDataSetChanged();
        if (postList.isEmpty()) {
            Toast.makeText(requireContext(), "Không có sản phẩm trong danh mục " + category, Toast.LENGTH_SHORT).show();
        }
    }

    public static class ProductDiscover {
        private final String username;
        private final String postedTime;
        private final String title;
        private final String price;
        private final String location;

        public ProductDiscover(String username, String postedTime, String title, String price, String location) {
            this.username = username;
            this.postedTime = postedTime;
            this.title = title;
            this.price = price;
            this.location = location;
        }

        public String getUsername() { return username; }
        public String getPostedTime() { return postedTime; }
        public String getTitle() { return title; }
        public String getPrice() { return price; }
        public String getLocation() { return location; }
    }

    private class DiscoverPostAdapter extends RecyclerView.Adapter<DiscoverPostAdapter.ViewHolder> {

        private final List<ProductDiscover> postList;

        public DiscoverPostAdapter(List<ProductDiscover> postList) {
            this.postList = postList;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_product_discover, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ProductDiscover post = postList.get(position);
            holder.userName.setText(post.getUsername());
            holder.postTimestamp.setText(post.getPostedTime());
            holder.postTitle.setText(post.getTitle());
            holder.postPrice.setText(post.getPrice());
            holder.postLocation.setText(post.getLocation());
            holder.postImage.setImageResource(R.drawable.img);
            holder.userAvatar.setImageResource(R.drawable.ic_default_avatar);
            // Update follow button state
            holder.followButton.setText(followedUsers.contains(post.getUsername()) ? "Bỏ theo dõi" : "Theo dõi");
        }

        @Override
        public int getItemCount() {
            return postList.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            ImageView userAvatar, postImage;
            TextView userName, postTimestamp, postTitle, postPrice, postLocation;
            Button followButton;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                userAvatar = itemView.findViewById(R.id.user_avatar);
                postImage = itemView.findViewById(R.id.post_image);
                userName = itemView.findViewById(R.id.user_name);
                postTimestamp = itemView.findViewById(R.id.post_timestamp);
                postTitle = itemView.findViewById(R.id.post_title);
                postPrice = itemView.findViewById(R.id.post_price);
                postLocation = itemView.findViewById(R.id.post_location);
                followButton = itemView.findViewById(R.id.follow_button);

                itemView.setOnClickListener(v -> {
                    int position = getBindingAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        Toast.makeText(requireContext(), "Xem chi tiết: " + postList.get(position).getTitle(), Toast.LENGTH_SHORT).show();
                        // TODO: Navigate to a detail fragment
                    }
                });

                followButton.setOnClickListener(v -> {
                    int position = getBindingAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        String username = postList.get(position).getUsername();
                        if (followedUsers.contains(username)) {
                            followedUsers.remove(username);
                            Toast.makeText(requireContext(), "Đã bỏ theo dõi: " + username, Toast.LENGTH_SHORT).show();
                        } else {
                            followedUsers.add(username);
                            Toast.makeText(requireContext(), "Đã theo dõi: " + username, Toast.LENGTH_SHORT).show();
                        }
                        notifyItemChanged(position);
                    }
                });
            }
        }
    }
}