package com.example.testapptradeup.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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

public class ProductListFragment extends Fragment {

    private ProductListViewModel viewModel;
    private NavController navController;

    // Arguments
    private String filterType;
    private String categoryId;

    // UI Components
    private MaterialToolbar toolbar;
    private RecyclerView recyclerView;
    private ListingsAdapter adapter;
    private ProgressBar loadingIndicator;
    private TextView emptyListText;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(ProductListViewModel.class);

        // Lấy arguments từ Safe Args
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
        setupToolbar();

        // Tải dữ liệu dựa trên arguments đã nhận
        viewModel.loadProducts(filterType, categoryId);
        observeViewModel();
    }

    private void initViews(View view) {
        toolbar = view.findViewById(R.id.toolbar_product_list);
        recyclerView = view.findViewById(R.id.recycler_product_list);
        loadingIndicator = view.findViewById(R.id.loading_indicator);
        emptyListText = view.findViewById(R.id.empty_list_text);
    }

    private void setupRecyclerView() {
        // Sử dụng lại ListingsAdapter vì nó phù hợp
        adapter = new ListingsAdapter(this::onProductClick, this::onFavoriteClick);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    private void setupToolbar() {
        toolbar.setNavigationOnClickListener(v -> navController.popBackStack());
        // Cập nhật tiêu đề toolbar
        if ("recommended".equals(filterType)) {
            toolbar.setTitle("Sản phẩm đề xuất");
        } else if ("recent".equals(filterType)) {
            toolbar.setTitle("Sản phẩm gần đây");
        } else if ("category".equals(filterType)) {
            // TODO: Lấy tên category từ categoryId và hiển thị
            toolbar.setTitle("Danh mục");
        }
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
        // Điều hướng đến chi tiết sản phẩm
        ProductListFragmentDirections.ActionProductListToProductDetail action =
                ProductListFragmentDirections.actionProductListToProductDetail(listing.getId());
        navController.navigate(action);
    }

    private void onFavoriteClick(Listing listing) {
        Toast.makeText(getContext(), "Yêu thích: " + listing.getTitle(), Toast.LENGTH_SHORT).show();
    }
}