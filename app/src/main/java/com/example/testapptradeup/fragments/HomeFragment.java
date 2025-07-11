package com.example.testapptradeup.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import com.example.testapptradeup.models.Listing;
import com.example.testapptradeup.viewmodels.HomeViewModel;
import com.example.testapptradeup.viewmodels.MainViewModel;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private HomeViewModel viewModel;
    private MainViewModel mainViewModel;
    private NavController navController;

    // Adapters
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
        mainViewModel.getNewListingPosted().observe(getViewLifecycleOwner(), newListing -> {
            if (newListing != null) {
                Log.d("HomeFragment", "Nhận được sự kiện đăng bài mới, đang thêm vào đầu danh sách...");
                viewModel.addNewListingToTop(newListing);
                binding.listingsRecycler.scrollToPosition(0);
                mainViewModel.onNewListingEventHandled();
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

        binding.categoriesSectionLayout.categoryElectronics.setOnClickListener(v -> navigateToCategory("electronics"));
        binding.categoriesSectionLayout.categoryFashion.setOnClickListener(v -> navigateToCategory("fashion"));
        binding.categoriesSectionLayout.categoryHome.setOnClickListener(v -> navigateToCategory("home_goods"));
        binding.categoriesSectionLayout.categoryCars.setOnClickListener(v -> navigateToCategory("cars"));
        binding.categoriesSectionLayout.categorySports.setOnClickListener(v -> navigateToCategory("sports"));
        binding.categoriesSectionLayout.categoryBooks.setOnClickListener(v -> navigateToCategory("books"));
        binding.categoriesSectionLayout.categoryLaptop.setOnClickListener(v -> navigateToCategory("laptops"));
        binding.categoriesSectionLayout.categoryOther.setOnClickListener(v -> navigateToCategory("other"));
    }

    private void observeViewModel() {
        viewModel.getFeaturedItems().observe(getViewLifecycleOwner(), listings -> featuredAdapter.submitList(listings));

        viewModel.getRecommendations().observe(getViewLifecycleOwner(), listings -> recommendationsAdapter.submitList(listings));

        viewModel.getListings().observe(getViewLifecycleOwner(), listings -> recentListingsAdapter.submitList(listings));

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            // Handle loading state
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
            }
        });
        viewModel.getPrioritizedRecentListings().observe(getViewLifecycleOwner(), listings -> recentListingsAdapter.submitList(listings));
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
            Log.e("HomeFragment", "Cannot navigate: listing or listing ID is null.");
            Toast.makeText(getContext(), "Không thể mở chi tiết sản phẩm.", Toast.LENGTH_SHORT).show();
        }
    }

    private void onFavoriteClick(Listing listing) {
        Toast.makeText(getContext(), "Yêu thích: " + listing.getTitle(), Toast.LENGTH_SHORT).show();
    }

    private void navigateTo(int destinationId) {
        if (navController != null && navController.getCurrentDestination() != null && navController.getCurrentDestination().getId() != destinationId) {
            navController.navigate(destinationId);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        viewModel.refreshData();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}