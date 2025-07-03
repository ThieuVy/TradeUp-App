package com.example.testapptradeup.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavDirections;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.testapptradeup.R;
import com.example.testapptradeup.adapters.CategoryAdapter;
import com.example.testapptradeup.adapters.FeaturedAdapter;
import com.example.testapptradeup.adapters.ListingsAdapter;
import com.example.testapptradeup.adapters.ProductGridAdapter;
import com.example.testapptradeup.databinding.FragmentHomeBinding;
import com.example.testapptradeup.models.Category;
import com.example.testapptradeup.models.Listing;
import com.example.testapptradeup.viewmodels.HomeViewModel;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private HomeViewModel viewModel;
    private NavController navController;

    // Adapters
    private FeaturedAdapter featuredAdapter;
    private CategoryAdapter categoryAdapter;
    private ProductGridAdapter recommendationsAdapter;
    private ListingsAdapter recentListingsAdapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Sử dụng ViewBinding để tránh lỗi NullPointerException với view
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(view);
        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        setupRecyclerViews();
        setupClickListeners();
        observeViewModel();
    }

    private void setupRecyclerViews() {
        // Nổi bật
        featuredAdapter = new FeaturedAdapter(this::navigateToProductDetail);
        binding.featuredRecycler.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.featuredRecycler.setAdapter(featuredAdapter);

        // Danh mục
        categoryAdapter = new CategoryAdapter(this::navigateToCategoryListings);
        binding.categoriesRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.categoriesRecycler.setAdapter(categoryAdapter);

        // Đề xuất
        recommendationsAdapter = new ProductGridAdapter(this::navigateToProductDetail);
        binding.recommendationsRecycler.setLayoutManager(new GridLayoutManager(getContext(), 2));
        binding.recommendationsRecycler.setAdapter(recommendationsAdapter);

        // Gần đây
        recentListingsAdapter = new ListingsAdapter(this::navigateToProductDetail, this::onFavoriteClick);
        binding.listingsRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.listingsRecycler.setAdapter(recentListingsAdapter);
    }

    private void setupClickListeners() {
        // Sửa: Dùng ID từ file layout fragment_home.xml mới
        binding.chatIcon.setOnClickListener(v -> navController.navigate(R.id.action_home_to_chat_list));
        binding.searchIcon.setOnClickListener(v -> navController.navigate(R.id.action_home_to_search));
        binding.searchBarCard.setOnClickListener(v -> navController.navigate(R.id.action_home_to_search));

        binding.seeAllRecommendations.setOnClickListener(v -> {
            HomeFragmentDirections.ActionHomeToProductList action = HomeFragmentDirections.actionHomeToProductList("recommended");
            navController.navigate(action);
        });

        binding.seeAllRecent.setOnClickListener(v -> {
            HomeFragmentDirections.ActionHomeToProductList action = HomeFragmentDirections.actionHomeToProductList("recent");
            navController.navigate(action);
        });
    }

    private void observeViewModel() {
        viewModel.getFeaturedItems().observe(getViewLifecycleOwner(), listings -> {
            if (listings != null) featuredAdapter.submitList(listings);
        });

        viewModel.getCategories().observe(getViewLifecycleOwner(), categories -> {
            if (categories != null) categoryAdapter.submitList(categories);
        });

        viewModel.getRecommendations().observe(getViewLifecycleOwner(), listings -> {
            if (listings != null) recommendationsAdapter.submitList(listings);
        });

        viewModel.getListings().observe(getViewLifecycleOwner(), listings -> {
            if (listings != null) recentListingsAdapter.submitList(listings);
        });

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            // binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
                viewModel.clearErrorMessage();
            }
        });
    }

    private void navigateToCategoryListings(Category category) {
        // Dùng lớp Directions được tạo tự động để truyền ID danh mục
        HomeFragmentDirections.ActionHomeToProductList action = HomeFragmentDirections.actionHomeToProductList(null);
        action.setCategoryId(category.getId());
        action.setFilterType("category");
        navController.navigate(action);
    }

    private void navigateToProductDetail(Listing listing) {
        // Dùng lớp Directions được tạo tự động để truyền ID sản phẩm
        HomeFragmentDirections.ActionHomeToProductDetail action = HomeFragmentDirections.actionHomeToProductDetail(listing.getId());
        NavDirections s =
                HomeFragmentDirections.actionHomeFragmentToProductDetailFragment(listing.getId());

        navController.navigate(s);
    }

    private void onFavoriteClick(Listing listing) {
        // TODO: Xử lý logic yêu thích thông qua ViewModel
        Toast.makeText(getContext(), "Yêu thích: " + listing.getTitle(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onResume() {
        super.onResume();
        viewModel.refreshData();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null; // Quan trọng để tránh memory leak với ViewBinding
    }
}