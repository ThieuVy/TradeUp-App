package com.example.testapptradeup.fragments.admin;

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
import com.example.testapptradeup.adapters.AdminReviewAdapter;
import com.example.testapptradeup.models.Review;
import com.example.testapptradeup.viewmodels.AdminViewModel;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;

public class ReviewModerationFragment extends Fragment implements AdminReviewAdapter.OnReviewActionListener {

    private AdminViewModel viewModel;
    private NavController navController;

    // UI Components
    private MaterialToolbar toolbar;
    private RecyclerView recyclerView;
    private AdminReviewAdapter adapter;
    private ProgressBar progressBar;
    private TextView emptyStateText;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Lấy ViewModel từ Activity để chia sẻ, hoặc từ Fragment nếu chỉ dùng ở đây
        // Trong trường hợp này, lấy từ Fragment là đủ
        viewModel = new ViewModelProvider(this).get(AdminViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_review_moderation, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(view);

        initViews(view);
        setupRecyclerView();
        setupListeners();
        observeViewModel();
    }

    private void initViews(View view) {
        toolbar = view.findViewById(R.id.toolbar);
        recyclerView = view.findViewById(R.id.recycler_reviews);
        progressBar = view.findViewById(R.id.progress_bar);
        emptyStateText = view.findViewById(R.id.text_empty_state);
    }

    private void setupRecyclerView() {
        adapter = new AdminReviewAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    private void setupListeners() {
        toolbar.setNavigationOnClickListener(v -> navController.popBackStack());
    }

    private void observeViewModel() {
        // Bắt đầu tải dữ liệu
        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        emptyStateText.setVisibility(View.GONE);

        viewModel.getPendingReviews().observe(getViewLifecycleOwner(), reviews -> {
            progressBar.setVisibility(View.GONE);
            if (reviews != null && !reviews.isEmpty()) {
                adapter.submitList(new ArrayList<>(reviews)); // Tạo list mới để DiffUtil hoạt động
                recyclerView.setVisibility(View.VISIBLE);
                emptyStateText.setVisibility(View.GONE);
            } else {
                recyclerView.setVisibility(View.GONE);
                emptyStateText.setVisibility(View.VISIBLE);
            }
        });

        viewModel.getActionStatusMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null) {
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                // Tải lại danh sách sau khi thực hiện hành động
                viewModel.getPendingReviews();
                viewModel.clearActionStatus(); // Reset thông báo
            }
        });
    }

    @Override
    public void onApprove(Review review) {
        viewModel.moderateReview(review.getReviewId(), "approved");
    }

    @Override
    public void onReject(Review review) {
        viewModel.moderateReview(review.getReviewId(), "rejected");
    }
}