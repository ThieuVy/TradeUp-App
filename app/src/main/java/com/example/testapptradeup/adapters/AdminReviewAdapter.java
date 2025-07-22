package com.example.testapptradeup.adapters;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.example.testapptradeup.R;
import com.example.testapptradeup.models.Review;

public class AdminReviewAdapter extends ListAdapter<Review, AdminReviewAdapter.ReviewViewHolder> {

    public interface OnReviewActionListener {
        void onApprove(Review review);
        void onReject(Review review);
    }

    private final OnReviewActionListener listener;

    public AdminReviewAdapter(@NonNull OnReviewActionListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ReviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_review, parent, false);
        return new ReviewViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReviewViewHolder holder, int position) {
        holder.bind(getItem(position), listener);
    }

    static class ReviewViewHolder extends RecyclerView.ViewHolder {
        TextView textReviewer, textReviewed, textRating, textComment;
        Button btnApprove, btnReject;

        public ReviewViewHolder(@NonNull View itemView) {
            super(itemView);
            textReviewer = itemView.findViewById(R.id.text_reviewer_info);
            textReviewed = itemView.findViewById(R.id.text_reviewed_user_info);
            textRating = itemView.findViewById(R.id.text_rating);
            textComment = itemView.findViewById(R.id.text_comment);
            btnApprove = itemView.findViewById(R.id.btn_approve);
            btnReject = itemView.findViewById(R.id.btn_reject);
        }

        @SuppressLint("SetTextI18n")
        public void bind(final Review review, final OnReviewActionListener listener) {
            textReviewer.setText("Người đánh giá: " + review.getReviewerName());
            textReviewed.setText("Được đánh giá: " + review.getReviewedUserId());
            textComment.setText("\"" + review.getComment() + "\"");

            StringBuilder stars = new StringBuilder();
            for (int i = 0; i < 5; i++) {
                stars.append(i < review.getRating() ? "★" : "☆");
            }
            textRating.setText(stars.toString());

            btnApprove.setOnClickListener(v -> listener.onApprove(review));
            btnReject.setOnClickListener(v -> listener.onReject(review));
        }
    }

    private static final DiffUtil.ItemCallback<Review> DIFF_CALLBACK = new DiffUtil.ItemCallback<>() {
        @Override
        public boolean areItemsTheSame(@NonNull Review oldItem, @NonNull Review newItem) {
            return oldItem.getReviewId().equals(newItem.getReviewId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Review oldItem, @NonNull Review newItem) {
            return oldItem.equals(newItem);
        }
    };
}