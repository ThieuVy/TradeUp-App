package com.example.testapptradeup.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.example.testapptradeup.R;
import com.example.testapptradeup.viewmodels.AddReviewViewModel;

public class AddReviewFragment extends Fragment {
    private AddReviewViewModel viewModel;
    private RatingBar ratingBar;
    private EditText commentEditText;
    private Button submitButton;
    private String transactionId;
    private String reviewedUserId;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(AddReviewViewModel.class);
        if (getArguments() != null) {
            transactionId = getArguments().getString("transactionId");
            reviewedUserId = getArguments().getString("reviewedUserId");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate layout for adding a review (gồm RatingBar, EditText, Button)
        View view = inflater.inflate(R.layout.fragment_add_review, container, false);

        ratingBar = view.findViewById(R.id.rating_bar);
        commentEditText = view.findViewById(R.id.edit_text_comment);
        submitButton = view.findViewById(R.id.btn_submit_review);

        submitButton.setOnClickListener(v -> submitReview());

        viewModel.getPostStatus().observe(getViewLifecycleOwner(), success -> {
            if (success) {
                Toast.makeText(getContext(), "Đánh giá thành công!", Toast.LENGTH_SHORT).show();
                // Quay lại màn hình trước
                Navigation.findNavController(view).popBackStack();
            } else {
                Toast.makeText(getContext(), "Lỗi khi gửi đánh giá.", Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }

    private void submitReview() {
        float rating = ratingBar.getRating();
        String comment = commentEditText.getText().toString().trim();
        viewModel.postReview(transactionId, reviewedUserId, rating, comment);
    }
}
