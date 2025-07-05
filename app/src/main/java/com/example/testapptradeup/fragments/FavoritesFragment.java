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
import com.example.testapptradeup.adapters.FavoritesAdapter;
import com.example.testapptradeup.viewmodels.FavoritesViewModel;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;

public class FavoritesFragment extends Fragment {

    private RecyclerView recyclerView;
    private FavoritesAdapter adapter;
    private MaterialToolbar toolbar;
    private NavController navController;
    private FavoritesViewModel viewModel;

    // Các View cho trạng thái loading và empty
    private ProgressBar progressBar;
    private TextView emptyStateText;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Khởi tạo ĐÚNG ViewModel
        viewModel = new ViewModelProvider(this).get(FavoritesViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Chỉ inflate layout của FavoritesFragment
        return inflater.inflate(R.layout.fragment_favorites, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(view);
        initViews(view);
        setupToolbar();
        setupRecyclerView();

        // Kiểm tra đăng nhập trước khi làm bất cứ điều gì
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(getContext(), "Vui lòng đăng nhập để xem mục yêu thích.", Toast.LENGTH_LONG).show();
            showEmptyState("Bạn cần đăng nhập để xem mục này.");
            return;
        }

        observeViewModel();
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recycler_view_favorites);
        toolbar = view.findViewById(R.id.toolbar_favorites);
        // Giả sử bạn có các View này trong R.layout.fragment_favorites
        progressBar = view.findViewById(R.id.progress_bar_favorites);
        emptyStateText = view.findViewById(R.id.text_empty_state_favorites);
    }

    private void setupToolbar() {
        toolbar.setNavigationOnClickListener(v -> navController.popBackStack());
    }

    private void setupRecyclerView() {
        // Khởi tạo Adapter với một danh sách rỗng ban đầu
        adapter = new FavoritesAdapter(getContext(), new ArrayList<>());
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    // ========== PHẦN CODE ĐÚNG CHO FavoritesFragment ==========
    private void observeViewModel() {
        // Bắt đầu quá trình tải dữ liệu
        showLoading(true);

        // Lắng nghe dữ liệu danh sách yêu thích từ ViewModel
        viewModel.getFavoriteListings().observe(getViewLifecycleOwner(), listings -> {
            showLoading(false); // Dữ liệu đã về, ẩn loading
            if (listings != null) {
                if (listings.isEmpty()) {
                    showEmptyState("Bạn chưa có sản phẩm yêu thích nào.");
                } else {
                    // Cập nhật dữ liệu cho adapter và hiển thị RecyclerView
                    adapter.updateListings(listings); // Cần có phương thức này trong Adapter
                    recyclerView.setVisibility(View.VISIBLE);
                    emptyStateText.setVisibility(View.GONE);
                }
            } else {
                // Xử lý trường hợp lỗi
                Toast.makeText(getContext(), "Lỗi tải danh sách yêu thích", Toast.LENGTH_SHORT).show();
                showEmptyState("Không thể tải dữ liệu. Vui lòng thử lại.");
            }
        });
    }

    private void showLoading(boolean isLoading) {
        if (progressBar != null) {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
        // Ẩn các view khác khi đang loading
        if (isLoading) {
            recyclerView.setVisibility(View.GONE);
            emptyStateText.setVisibility(View.GONE);
        }
    }

    private void showEmptyState(String message) {
        recyclerView.setVisibility(View.GONE);
        if (emptyStateText != null) {
            emptyStateText.setVisibility(View.VISIBLE);
            emptyStateText.setText(message);
        }
    }
}