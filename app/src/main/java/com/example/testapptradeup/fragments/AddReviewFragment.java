package com.example.testapptradeup.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.testapptradeup.R;
import com.example.testapptradeup.viewmodels.AddReviewViewModel;
import com.google.android.material.appbar.MaterialToolbar;

public class AddReviewFragment extends Fragment {
    private AddReviewViewModel viewModel;
    private NavController navController;

    // UI Components
    private RatingBar ratingBar;
    private EditText commentEditText;
    private Button submitButton;
    private ProgressBar progressBar;
    private MaterialToolbar toolbar;

    // Data from arguments
    private String transactionId;
    private String reviewedUserId;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(AddReviewViewModel.class);
        // Lấy tham số từ navigation
        if (getArguments() != null) {
            // Sử dụng class Args được tạo tự động để an toàn hơn
            transactionId = AddReviewFragmentArgs.fromBundle(getArguments()).getTransactionId();
            reviewedUserId = AddReviewFragmentArgs.fromBundle(getArguments()).getReviewedUserId();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_add_review, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(view);

        initViews(view);
        setupListeners();
        observeViewModel();
    }

    private void initViews(View view) {
        toolbar = view.findViewById(R.id.toolbar_add_review);
        ratingBar = view.findViewById(R.id.rating_bar);
        commentEditText = view.findViewById(R.id.edit_text_comment);
        submitButton = view.findViewById(R.id.btn_submit_review);
        progressBar = view.findViewById(R.id.submit_progress); // Ánh xạ ProgressBar
    }

    private void setupListeners() {
        toolbar.setNavigationOnClickListener(v -> navController.popBackStack());
        submitButton.setOnClickListener(v -> submitReview());
    }

    private void observeViewModel() {
        // SỬA LỖI: Sử dụng lớp PostResult đã được định nghĩa trong ViewModel
        viewModel.getPostStatus().observe(getViewLifecycleOwner(), result -> {
            if (result == null) return; // Trạng thái ban đầu, không làm gì cả

            showLoading(false);
            if (result.isSuccess()) {
                Toast.makeText(getContext(), "Đánh giá thành công!", Toast.LENGTH_SHORT).show();
                navController.popBackStack(); // Quay lại màn hình trước
            } else {
                Toast.makeText(getContext(), "Lỗi khi gửi đánh giá: " + result.getErrorMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void submitReview() {
        float rating = ratingBar.getRating();
        if (rating == 0) {
            Toast.makeText(getContext(), "Vui lòng chọn số sao đánh giá", Toast.LENGTH_SHORT).show();
            return;
        }
        String comment = commentEditText.getText().toString().trim();

        showLoading(true);
        viewModel.postReview(transactionId, reviewedUserId, rating, comment);
    }

    private void showLoading(boolean isLoading) {
        if (isLoading) {
            submitButton.setVisibility(View.INVISIBLE);
            progressBar.setVisibility(View.VISIBLE);
        } else {
            submitButton.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.GONE);
        }
    }
}