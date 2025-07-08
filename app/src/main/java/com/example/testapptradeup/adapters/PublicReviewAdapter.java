package com.example.testapptradeup.adapters;

import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.testapptradeup.R;
import com.example.testapptradeup.models.Review;
import java.util.Objects;

public class PublicReviewAdapter extends ListAdapter<Review, PublicReviewAdapter.ReviewViewHolder> {

    public PublicReviewAdapter() {
        super(DIFF_CALLBACK);
    }

    @NonNull
    @Override
    public ReviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_public_review, parent, false);
        return new ReviewViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReviewViewHolder holder, int position) {
        Review review = getItem(position);
        holder.bind(review);
        // Ẩn đường kẻ ở item cuối cùng
        holder.divider.setVisibility(position == getItemCount() - 1 ? View.GONE : View.VISIBLE);
    }

    static class ReviewViewHolder extends RecyclerView.ViewHolder {
        ImageView reviewerImage;
        TextView reviewerName, reviewDate, reviewRatingStars, reviewComment;
        View divider;

        public ReviewViewHolder(@NonNull View itemView) {
            super(itemView);
            reviewerImage = itemView.findViewById(R.id.reviewer_image);
            reviewerName = itemView.findViewById(R.id.reviewer_name);
            reviewDate = itemView.findViewById(R.id.review_date);
            reviewRatingStars = itemView.findViewById(R.id.review_rating_stars);
            reviewComment = itemView.findViewById(R.id.review_comment);
            divider = itemView.findViewById(R.id.divider);
        }

        void bind(final Review review) {
            reviewerName.setText(review.getReviewerName());
            reviewComment.setText(review.getComment());
            reviewRatingStars.setText(getStarString(review.getRating()));

            if (review.getReviewDate() != null) {
                CharSequence relativeTime = DateUtils.getRelativeTimeSpanString(
                        review.getReviewDate().getTime(), System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS);
                reviewDate.setText(relativeTime);
            }

            Glide.with(itemView.getContext())
                    .load(review.getReviewerImageUrl())
                    .placeholder(R.drawable.ic_profile_placeholder)
                    .error(R.drawable.ic_profile_placeholder)
                    .circleCrop()
                    .into(reviewerImage);
        }

        private String getStarString(float rating) {
            int fullStars = Math.round(rating);
            StringBuilder stars = new StringBuilder();
            for (int i = 0; i < 5; i++) {
                stars.append(i < fullStars ? "★" : "☆");
            }
            return stars.toString();
        }
    }

    private static final DiffUtil.ItemCallback<Review> DIFF_CALLBACK = new DiffUtil.ItemCallback<Review>() {
        @Override
        public boolean areItemsTheSame(@NonNull Review oldItem, @NonNull Review newItem) {
            return Objects.equals(oldItem.getReviewId(), newItem.getReviewId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Review oldItem, @NonNull Review newItem) {
            return oldItem.equals(newItem);
        }
    };
}