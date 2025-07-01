package com.example.testapptradeup.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView; // Make sure this import exists for TextView in search bar
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.testapptradeup.databinding.FragmentHomeBinding;
import com.example.testapptradeup.models.Category;
import com.example.testapptradeup.viewmodels.HomeViewModel;
import com.example.testapptradeup.adapters.ListingsAdapter;
import com.example.testapptradeup.models.Listing;
import com.example.testapptradeup.adapters.RecommendationsAdapter;
import com.example.testapptradeup.adapters.TrendingAdapter;
import com.example.testapptradeup.adapters.FeaturedAdapter;
import com.google.android.material.card.MaterialCardView;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private HomeViewModel viewModel;

    // Adapters for RecyclerViews
    private FeaturedAdapter featuredAdapter;
    private RecommendationsAdapter recommendationsAdapter;
    private TrendingAdapter trendingAdapter;
    private ListingsAdapter listingsAdapter;

    // UI Components for header
    private ImageView chatIcon; // Đã đổi tên từ notificationIcon thành chatIcon
    private ImageView searchIconHeader; // Đã đổi tên từ searchIcon thành searchIconHeader để phân biệt

    // UI Components for search bar
    private MaterialCardView searchBarCard; // This is the clickable search bar
    private TextView searchBarText; // Text view inside the search bar
    private ImageView filterIcon; // Inside the search bar

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        initViews(view);
        setupRecyclerViews();
        setupClickListeners();
        observeViewModel();

        viewModel.loadAllHomeData(); // Đã đổi từ loadHomeData() sang loadAllHomeData()
    }

    private void initViews(View view) {
        // Initialize header icons (chat and search in top bar)
        chatIcon = binding.notificationIcon; // notification_icon trong XML giờ là icon chat
        searchIconHeader = binding.searchIcon; // search_icon trong XML vẫn là search

        // Initialize search bar components (the card itself and elements inside it)
        searchBarCard = binding.searchBarCard;
        // Search bar in XML has a TextView for "Tìm kiếm sản phẩm, danh mục..."
        // If you need to interact with this TextView programmatically, you'd give it an ID
        // For now, it's just descriptive text.
        filterIcon = binding.filterIcon;
    }

    private void setupRecyclerViews() {
        // Featured Items RecyclerView
        featuredAdapter = new FeaturedAdapter(this::onProductClick);
        binding.featuredRecycler.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false)
        );
        binding.featuredRecycler.setAdapter(featuredAdapter);

        // Recommendations RecyclerView
        recommendationsAdapter = new RecommendationsAdapter(this::onProductClick);
        binding.recommendationsRecycler.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false)
        );
        binding.recommendationsRecycler.setAdapter(recommendationsAdapter);

        // Listings RecyclerView
        listingsAdapter = new ListingsAdapter(this::onProductClick, this::onFavoriteClick);
        binding.listingsRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.listingsRecycler.setAdapter(listingsAdapter);

        // Categories are static in the current fragment_home.xml, so no adapter for them here.
    }

    private void setupClickListeners() {
        // Header action icons
        if (chatIcon != null) {
            chatIcon.setOnClickListener(v -> showToast("Mở chat")); // Thay đổi chức năng từ thông báo sang chat
        }
        if (searchIconHeader != null) {
            searchIconHeader.setOnClickListener(v -> showToast("Thực hiện tìm kiếm nhanh")); // Cập nhật toast message
        }

        // Search Bar (the clickable CardView)
        if (searchBarCard != null) {
            searchBarCard.setOnClickListener(v -> showToast("Mở màn hình tìm kiếm chi tiết"));
        }
        if (filterIcon != null) {
            filterIcon.setOnClickListener(v -> showFilterDialog());
        }

        // "See All" for Recommendations
        binding.seeAllRecommendations.setOnClickListener(v -> showToast("Xem tất cả gợi ý"));

        // Click listeners for static category items
        // You need to add IDs to your LinearLayouts for categories in fragment_home.xml
        // and then set click listeners for them here, e.g.:
        // LinearLayout categoryLaptop = binding.getRoot().findViewById(R.id.category_laptop_layout);
        // if (categoryLaptop != null) {
        //     categoryLaptop.setOnClickListener(v -> onCategoryClick(new Category("cat_id_1", "Máy tính & Laptop", "")));
        // }
        // Repeat for other categories.
    }

    private void observeViewModel() {
        viewModel.getFeaturedItems().observe(getViewLifecycleOwner(), featuredItems -> {
            if (featuredItems != null) {
                featuredAdapter.submitList(featuredItems);
            }
        });

        viewModel.getRecommendations().observe(getViewLifecycleOwner(), recommendations -> {
            if (recommendations != null) {
                recommendationsAdapter.submitList(recommendations);
            }
        });

        viewModel.getListings().observe(getViewLifecycleOwner(), listings -> {
            if (listings != null) {
                listingsAdapter.submitList(listings);
            }
        });

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            // Handle loading state, e.g., show/hide a ProgressBar
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.isEmpty()) {
                showToast(message);
                viewModel.clearErrorMessage();
            }
        });
    }

    // --- Event Handlers ---

    private void onCategoryClick(Category category) {
        showToast("Lọc theo: " + category.getName());
        viewModel.filterByCategory(category.getId());
    }

    private void onProductClick(Listing listing) {
        showToast("Xem chi tiết: " + listing.getTitle());
        // TODO: Navigate to product detail screen, e.g., using Navigation Component
    }

    private void onFavoriteClick(Listing listing) {
        viewModel.toggleFavorite(listing.getId());
        showToast("Yêu thích: " + listing.getTitle());
        // TODO: Update favorite icon UI
    }

    private void performSearch(String query) {
        viewModel.searchProducts(query);
        showToast("Tìm kiếm: " + query);
    }

    private void showFilterDialog() {
        showToast("Mở bộ lọc");
    }

    private void showSortDialog() {
        showToast("Mở bộ sắp xếp");
    }

    private void showToast(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
