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

import java.util.Collections;

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
        // Khởi tạo adapters ở đây hoặc trong onViewCreated nếu bạn muốn
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
        // Khởi tạo MainViewModel với scope của Activity
        mainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        setupRecyclerViews();
        setupClickListeners();
        observeViewModel();
        observeSharedViewModel(); // Lắng nghe ViewModel chia sẻ
    }

    // Hàm này lắng nghe sự kiện từ MainViewModel
    private void observeSharedViewModel() {
        mainViewModel.getNewListingPosted().observe(getViewLifecycleOwner(), newListing -> {
            // Kiểm tra newListing không null để tránh xử lý sự kiện đã được clear
            if (newListing != null) {
                // 1. Thông báo cho HomeViewModel để xử lý logic thêm vào danh sách
                viewModel.addNewListingToTop(newListing);

                // 2. (UX) Cuộn RecyclerView lên đầu để người dùng thấy ngay bài đăng của mình
                binding.listingsRecycler.scrollToPosition(0);

                // 3. Thông báo cho MainViewModel rằng sự kiện đã được xử lý
                // Điều này ngăn HomeFragment xử lý lại sự kiện này nếu nó xoay màn hình hoặc quay lại.
                mainViewModel.onNewListingEventHandled();

                Toast.makeText(getContext(), "Đã cập nhật tin đăng mới!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupRecyclerViews() {
        // Nổi bật
        featuredAdapter = new FeaturedAdapter(this::navigateToProductDetail);
        binding.featuredRecycler.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.featuredRecycler.setAdapter(featuredAdapter);

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
        // Toolbar and Search Bar
        binding.chatIcon.setOnClickListener(v -> navigateTo(R.id.action_home_to_chatList));
        binding.searchIcon.setOnClickListener(v -> navigateTo(R.id.action_home_to_search));
        binding.searchBarCard.setOnClickListener(v -> navigateTo(R.id.action_home_to_search));

        // "See All" buttons
        binding.seeAllRecommendations.setOnClickListener(v -> Toast.makeText(getContext(), "Xem tất cả Đề xuất", Toast.LENGTH_SHORT).show());

        binding.seeAllRecent.setOnClickListener(v -> Toast.makeText(getContext(), "Xem tất cả Gần đây", Toast.LENGTH_SHORT).show());
    }

    private void observeViewModel() {
        viewModel.getFeaturedItems().observe(getViewLifecycleOwner(), listings -> featuredAdapter.submitList(listings != null ? listings : Collections.emptyList()));
        viewModel.getRecommendations().observe(getViewLifecycleOwner(), listings -> recommendationsAdapter.submitList(listings != null ? listings : Collections.emptyList()));
        viewModel.getListings().observe(getViewLifecycleOwner(), listings -> recentListingsAdapter.submitList(listings != null ? listings : Collections.emptyList()));

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            // TODO: Hiển thị/ẩn ProgressBar hoặc Shimmer effect
        });

        // Hàm này lắng nghe danh sách "Tin rao gần đây" từ HomeViewModel
        // Nó sẽ được kích hoạt cả khi tải dữ liệu ban đầu và khi có bài đăng mới được thêm vào
        viewModel.getListings().observe(getViewLifecycleOwner(), listings -> recentListingsAdapter.submitList(listings != null ? listings : Collections.emptyList()));
    }

    private void navigateToCategoryListings(Category category) {
        Toast.makeText(getContext(), "Xem danh mục: " + category.getName(), Toast.LENGTH_SHORT).show();
        // TODO: Điều hướng tới màn hình danh sách sản phẩm theo danh mục
    }

    private void navigateToProductDetail(Listing listing) {
        Toast.makeText(getContext(), "Xem sản phẩm: " + listing.getTitle(), Toast.LENGTH_SHORT).show();
        // TODO: Điều hướng tới màn hình chi tiết sản phẩm
    }

    private void onFavoriteClick(Listing listing) {
        // TODO: Xử lý logic yêu thích thông qua ViewModel
        Toast.makeText(getContext(), "Yêu thích: " + listing.getTitle(), Toast.LENGTH_SHORT).show();
    }

    private void navigateTo(int destinationId) {
        if (navController.getCurrentDestination() != null && navController.getCurrentDestination().getId() != destinationId) {
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