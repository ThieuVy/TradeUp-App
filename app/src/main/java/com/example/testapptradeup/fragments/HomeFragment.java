package com.example.testapptradeup.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.testapptradeup.R;
import com.example.testapptradeup.adapters.FeaturedAdapter;
import com.example.testapptradeup.adapters.ListingsAdapter;
import com.example.testapptradeup.adapters.ProductGridAdapter;
import com.example.testapptradeup.databinding.FragmentHomeBinding;
import com.example.testapptradeup.models.Category;
import com.example.testapptradeup.models.Listing;
import com.example.testapptradeup.viewmodels.HomeViewModel;
import com.example.testapptradeup.viewmodels.MainViewModel;
import com.google.firebase.auth.FirebaseAuth;

import java.util.List;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private HomeViewModel viewModel;
    private MainViewModel mainViewModel;
    private NavController navController;

    private FeaturedAdapter featuredAdapter;
    private ProductGridAdapter recommendationsAdapter;
    private ListingsAdapter recentListingsAdapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        featuredAdapter = new FeaturedAdapter(this::navigateToProductDetail);
        recommendationsAdapter = new ProductGridAdapter(this::navigateToProductDetail);
        recentListingsAdapter = new ListingsAdapter(this::navigateToProductDetail, this::onFavoriteClick);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(view);
        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        mainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        setupRecyclerViews();
        setupClickListeners();
        observeViewModel();
        observeSharedViewModel();
    }

    private void observeSharedViewModel() {
        // <<< SỬA LỖI 1 & 2: Sửa cú pháp Observer cho Event >>>
        mainViewModel.getListingUpdatedEvent().observe(getViewLifecycleOwner(), event -> {
            if (event.getContentIfNotHandled() != null) {
                Toast.makeText(getContext(), "Dữ liệu đã được làm mới!", Toast.LENGTH_SHORT).show();
                binding.listingsRecycler.smoothScrollToPosition(0);
            }
        });
    }

    private void setupRecyclerViews() {
        binding.featuredRecycler.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.featuredRecycler.setAdapter(featuredAdapter);

        binding.recommendationsRecycler.setLayoutManager(new GridLayoutManager(getContext(), 2));
        binding.recommendationsRecycler.setAdapter(recommendationsAdapter);

        binding.listingsRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.listingsRecycler.setAdapter(recentListingsAdapter);
    }

    private void observeViewModel() {
        // --- Observer cho mục "Nổi bật" ---
        viewModel.getFeaturedItems().observe(getViewLifecycleOwner(), listings -> {
            binding.featuredProgress.setVisibility(View.GONE);
            updateListVisibility(listings, binding.featuredRecycler, binding.featuredEmptyText);
            featuredAdapter.submitList(listings);
        });

        // --- Observer cho mục "Đề xuất" ---
        viewModel.getRecommendations().observe(getViewLifecycleOwner(), listings -> {
            binding.recommendationsProgress.setVisibility(View.GONE);
            updateListVisibility(listings, binding.recommendationsRecycler, binding.recommendationsEmptyText);
            recommendationsAdapter.submitList(listings);
        });

        // <<< SỬA LỖI 3, 4, 5: Gọi đúng phương thức và các lỗi liên quan tự hết >>>
        // --- Observer cho mục "Tin rao gần đây" (đã được ưu tiên theo vị trí) ---
        viewModel.getPrioritizedRecentListings().observe(getViewLifecycleOwner(), listings -> {
            binding.listingsProgress.setVisibility(View.GONE);
            updateListVisibility(listings, binding.listingsRecycler, binding.listingsEmptyText);
            recentListingsAdapter.submitList(listings);
            Log.d("HomeFragment", "Danh sách tin gần đây đã được cập nhật!");
        });
    }

    private void updateListVisibility(List<?> list, View recyclerView, View emptyView) {
        boolean isEmpty = list == null || list.isEmpty();
        recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        emptyView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
    }

    // --- Các hàm còn lại giữ nguyên ---

    private void setupClickListeners() {
        binding.chatIcon.setOnClickListener(v -> navigateTo(R.id.action_home_to_chatList));
        binding.searchIcon.setOnClickListener(v -> navigateTo(R.id.action_home_to_search));
        binding.searchBarCard.setOnClickListener(v -> navigateTo(R.id.action_home_to_search));

        binding.seeAllRecommendations.setOnClickListener(v -> {
            HomeFragmentDirections.ActionHomeToProductList action =
                    HomeFragmentDirections.actionHomeToProductList("recommended");
            action.setCategoryId(null);
            navController.navigate(action);
        });

        binding.seeAllRecent.setOnClickListener(v -> {
            HomeFragmentDirections.ActionHomeToProductList action =
                    HomeFragmentDirections.actionHomeToProductList("recent");
            action.setCategoryId(null);
            navController.navigate(action);
        });

        binding.categoriesSectionLayout.categoryElectronics.setOnClickListener(v -> navigateToCategory(Category.AppConstants.CATEGORY_ELECTRONICS));
        binding.categoriesSectionLayout.categoryFashion.setOnClickListener(v -> navigateToCategory(Category.AppConstants.CATEGORY_FASHION));
        binding.categoriesSectionLayout.categoryHome.setOnClickListener(v -> navigateToCategory(Category.AppConstants.CATEGORY_HOME_GOODS));
        binding.categoriesSectionLayout.categoryCars.setOnClickListener(v -> navigateToCategory(Category.AppConstants.CATEGORY_CARS));
        binding.categoriesSectionLayout.categorySports.setOnClickListener(v -> navigateToCategory(Category.AppConstants.CATEGORY_SPORTS));
        binding.categoriesSectionLayout.categoryBooks.setOnClickListener(v -> navigateToCategory(Category.AppConstants.CATEGORY_BOOKS));
        binding.categoriesSectionLayout.categoryLaptop.setOnClickListener(v -> navigateToCategory(Category.AppConstants.CATEGORY_LAPTOPS));
        binding.categoriesSectionLayout.categoryOther.setOnClickListener(v -> navigateToCategory(Category.AppConstants.CATEGORY_OTHER));
    }

    private void navigateToCategory(String categoryId) {
        if (navController != null) {
            HomeFragmentDirections.ActionHomeToProductList action =
                    HomeFragmentDirections.actionHomeToProductList("category");
            action.setCategoryId(categoryId);
            navController.navigate(action);
        }
    }

    private void navigateToProductDetail(Listing listing) {
        if (navController != null && listing != null && listing.getId() != null) {
            HomeFragmentDirections.ActionHomeToProductDetail action =
                    HomeFragmentDirections.actionHomeToProductDetail(listing.getId());
            navController.navigate(action);
        } else {
            Log.e("HomeFragment", "Không thể điều hướng: listing hoặc listing ID là null.");
            Toast.makeText(getContext(), "Không thể mở chi tiết sản phẩm.", Toast.LENGTH_SHORT).show();
        }
    }

    private void onFavoriteClick(Listing listing, ImageView favoriteIcon) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(getContext(), "Vui lòng đăng nhập để yêu thích.", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean isCurrentlyFavorited = favoriteIcon.getTag() != null && (boolean) favoriteIcon.getTag();
        boolean newFavoriteState = !isCurrentlyFavorited;

        updateFavoriteIconUI(favoriteIcon, newFavoriteState);
        viewModel.toggleFavorite(listing.getId(), newFavoriteState);
    }

    private void updateFavoriteIconUI(ImageView favoriteIcon, boolean isFavorite) {
        if (getContext() == null) return;
        if (isFavorite) {
            favoriteIcon.setImageResource(R.drawable.ic_favorite_filled);
            favoriteIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.red_error));
            favoriteIcon.setTag(true);
        } else {
            favoriteIcon.setImageResource(R.drawable.ic_favorite_outline);
            favoriteIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.text_secondary));
            favoriteIcon.setTag(false);
        }
    }

    private void navigateTo(int destinationId) {
        if (navController != null && navController.getCurrentDestination() != null && navController.getCurrentDestination().getId() != destinationId) {
            navController.navigate(destinationId);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}