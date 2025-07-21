package com.example.testapptradeup.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.testapptradeup.R;
import com.example.testapptradeup.models.Review;
import java.util.List;

public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder> {
    private List<Review> reviews;

    public ReviewAdapter(List<Review> reviews) {
        this.reviews = reviews;
    }

    public List<Review> getReviews() {
        return reviews;
    }

    public void setReviews(List<Review> reviews) {
        this.reviews = reviews;
    }

    @NonNull
    @Override
    public ReviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_review, parent, false);
        return new ReviewViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReviewViewHolder holder, int position) {
        holder.bind(reviews.get(position));
    }

    @Override
    public int getItemCount() {
        return reviews.size();
    }

    static class ReviewViewHolder extends RecyclerView.ViewHolder {
        // Ánh xạ các view từ item_review.xml
        TextView reviewerName, reviewComment;

        ReviewViewHolder(View itemView) {
            super(itemView);
            // =======================================================
            // SỬA LỖI: DÙNG ĐÚNG ID TỪ FILE LAYOUT XML
            // =======================================================
            reviewerName = itemView.findViewById(R.id.review_user_name); // Sửa ID
            reviewComment = itemView.findViewById(R.id.review_content); // Sửa ID
        }

        void bind(Review review) {
            // Bây giờ các biến này sẽ không còn null nữa
            if (reviewerName != null) {
                reviewerName.setText(review.getReviewerName());
            }
            if (reviewComment != null) {
                reviewComment.setText(review.getComment());
            }
        }
    }
}