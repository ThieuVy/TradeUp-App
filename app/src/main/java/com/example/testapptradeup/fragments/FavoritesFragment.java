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

public class FavoritesFragment extends Fragment {

    private RecyclerView recyclerView;
    private FavoritesAdapter adapter;
    private MaterialToolbar toolbar;
    private NavController navController;
    private FavoritesViewModel viewModel;
    private ProgressBar progressBar;
    private TextView emptyStateText;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(FavoritesViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_favorites, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(view);
        initViews(view);
        setupToolbar();
        setupRecyclerView();

        // Kiểm tra người dùng đã đăng nhập chưa
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(getContext(), "Vui lòng đăng nhập để xem danh sách yêu thích.", Toast.LENGTH_LONG).show();
            showEmptyState("Bạn cần đăng nhập để xem mục này.");
            return;
        }

        observeViewModel();
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recycler_view_favorites);
        toolbar = view.findViewById(R.id.toolbar_favorites);
        progressBar = view.findViewById(R.id.progress_bar_favorites);
        emptyStateText = view.findViewById(R.id.text_empty_state_favorites);
    }

    private void setupToolbar() {
        toolbar.setNavigationOnClickListener(v -> navController.popBackStack());
    }

    private void setupRecyclerView() {
        // CHỈNH SỬA: Khởi tạo adapter không cần tham số vì nó hiện là ListAdapter
        adapter = new FavoritesAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    private void observeViewModel() {
        showLoading(true);
        viewModel.getFavoriteListings().observe(getViewLifecycleOwner(), listings -> {
            showLoading(false);
            if (listings != null) {
                if (listings.isEmpty()) {
                    showEmptyState("Bạn chưa có mục yêu thích nào.");
                } else {
                    // CHỈNH SỬA: dùng submitList() thay vì phương thức tùy chỉnh
                    adapter.submitList(listings);
                    recyclerView.setVisibility(View.VISIBLE);
                    emptyStateText.setVisibility(View.GONE);
                }
            } else {
                Toast.makeText(getContext(), "Lỗi khi tải danh sách yêu thích", Toast.LENGTH_SHORT).show();
                showEmptyState("Không thể tải dữ liệu. Vui lòng thử lại.");
            }
        });
    }

    private void showLoading(boolean isLoading) {
        if (progressBar != null) {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
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
