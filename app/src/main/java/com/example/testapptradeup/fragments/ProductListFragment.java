package com.example.testapptradeup.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView; // <<< THÊM MỚI: Import ImageView
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat; // <<< THÊM MỚI: Import để lấy màu
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.testapptradeup.R;
import com.example.testapptradeup.adapters.ListingsAdapter;
import com.example.testapptradeup.models.Listing;
import com.example.testapptradeup.viewmodels.ProductListViewModel;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth; // <<< THÊM MỚI: Import để kiểm tra đăng nhập

public class ProductListFragment extends Fragment {

    private ProductListViewModel viewModel;
    private NavController navController;

    private String filterType;
    private String categoryId;

    private MaterialToolbar toolbar;
    private RecyclerView recyclerView;
    private ListingsAdapter adapter;
    private ProgressBar loadingIndicator;
    private TextView emptyListText;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // SỬA LỖI: Khởi tạo ViewModel từ Fragment, không phải từ Activity
        // để đảm bảo vòng đời của ViewModel gắn với Fragment này.
        viewModel = new ViewModelProvider(this).get(ProductListViewModel.class);

        if (getArguments() != null) {
            ProductListFragmentArgs args = ProductListFragmentArgs.fromBundle(getArguments());
            filterType = args.getFilterType();
            categoryId = args.getCategoryId();
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_product_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(view);

        initViews(view);
        setupRecyclerView();
        setupToolbar(); // Gọi setupToolbar

        // Gọi các phương thức của ViewModel
        viewModel.loadProducts(filterType, categoryId);
        // Yêu cầu ViewModel cập nhật tiêu đề
        viewModel.updateToolbarTitle(filterType, categoryId);
        observeViewModel();
    }

    private void initViews(View view) {
        toolbar = view.findViewById(R.id.toolbar_product_list);
        recyclerView = view.findViewById(R.id.recycler_product_list);
        loadingIndicator = view.findViewById(R.id.loading_indicator);
        emptyListText = view.findViewById(R.id.empty_list_text);
    }

    private void setupRecyclerView() {
        // Dòng này bây giờ sẽ không còn lỗi
        adapter = new ListingsAdapter(this::onProductClick, this::onFavoriteClick);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    private void setupToolbar() {
        toolbar.setNavigationOnClickListener(v -> navController.popBackStack());

        // <<< SỬA LỖI: Lắng nghe LiveData tiêu đề từ ViewModel >>>
        // Fragment không còn tự quyết định tiêu đề nữa.
        viewModel.getToolbarTitle().observe(getViewLifecycleOwner(), title -> {
            if (title != null) {
                toolbar.setTitle(title);
            }
        });
    }

    private void observeViewModel() {
        viewModel.productList.observe(getViewLifecycleOwner(), listings -> {
            boolean isEmpty = listings == null || listings.isEmpty();
            emptyListText.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
            if (!isEmpty) {
                adapter.submitList(listings);
            }
        });

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            loadingIndicator.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            if (isLoading) {
                recyclerView.setVisibility(View.GONE);
                emptyListText.setVisibility(View.GONE);
            }
        });
    }

    private void onProductClick(Listing listing) {
        ProductListFragmentDirections.ActionProductListToProductDetail action =
                ProductListFragmentDirections.actionProductListToProductDetail(listing.getId());
        navController.navigate(action);
    }

    // <<< BƯỚC 1: Sửa lại chữ ký phương thức để khớp với interface >>>
    private void onFavoriteClick(Listing listing, ImageView favoriteIcon) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(getContext(), "Vui lòng đăng nhập để yêu thích sản phẩm.", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean isCurrentlyFavorited = (favoriteIcon.getTag() != null && (boolean) favoriteIcon.getTag());
        boolean newFavoriteState = !isCurrentlyFavorited;

        updateFavoriteIconUI(favoriteIcon, newFavoriteState);

        // <<< SỬA LỖI: Gọi ViewModel để xử lý logic >>>
        viewModel.toggleFavorite(listing.getId(), newFavoriteState);

        String message = newFavoriteState ? "Đã thêm vào yêu thích" : "Đã xóa khỏi yêu thích";
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }

    // <<< BƯỚC 3 (Tùy chọn nhưng khuyến khích): Hàm helper để cập nhật UI >>>
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
}