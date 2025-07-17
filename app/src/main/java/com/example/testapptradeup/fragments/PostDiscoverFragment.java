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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.testapptradeup.R;
import com.example.testapptradeup.adapters.DiscoverAdapter;
import com.example.testapptradeup.models.Listing;
import com.example.testapptradeup.viewmodels.DiscoverViewModel;
import com.google.android.material.tabs.TabLayout;

import java.util.Objects;

public class PostDiscoverFragment extends Fragment {

    private DiscoverViewModel viewModel;
    private NavController navController;
    private DiscoverAdapter adapter;

    // UI Components
    private RecyclerView feedRecyclerView;
    private TabLayout tabLayout;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(DiscoverViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_discover, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(view);

        initViews(view);
        setupRecyclerView();
        setupTabLayoutListener();
        observeViewModel();

        // Tải dữ liệu cho tab mặc định khi fragment được tạo
        viewModel.setFilter("Gần bạn");
    }

    private void initViews(View view) {
        feedRecyclerView = view.findViewById(R.id.feed_recycler_view);
        tabLayout = view.findViewById(R.id.tab_layout);
    }

    private void setupRecyclerView() {
        adapter = new DiscoverAdapter(this::onProductClick);
        feedRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        feedRecyclerView.setAdapter(adapter);
    }

    private void setupTabLayoutListener() {
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                String selectedCategory = Objects.requireNonNull(tab.getText()).toString();
                viewModel.setFilter(selectedCategory);
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void observeViewModel() {
        // Lắng nghe danh sách tin đăng từ ViewModel
        viewModel.discoverListings.observe(getViewLifecycleOwner(), listings -> {
            if (listings != null) {
                adapter.submitList(listings);
            }
        });

        // Bạn có thể thêm LiveData cho trạng thái loading/error trong ViewModel nếu cần
    }

    private void onProductClick(Listing listing) {
        // TODO: Điều hướng đến trang chi tiết sản phẩm bằng NavController
        Toast.makeText(getContext(), "Mở chi tiết: " + listing.getTitle(), Toast.LENGTH_SHORT).show();
    }
}