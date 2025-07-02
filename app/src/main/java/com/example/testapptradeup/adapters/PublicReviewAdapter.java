package com.example.testapptradeup.adapters;

import android.content.Context;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.testapptradeup.R;
import com.example.testapptradeup.models.Review;
import java.util.List;

public class PublicReviewAdapter extends RecyclerView.Adapter<PublicReviewAdapter.ReviewViewHolder> {

    private final Context context;
    private final List<Review> reviewList;

    public PublicReviewAdapter(Context context, List<Review> reviewList) {
        this.context = context;
        this.reviewList = reviewList;
    }

    @NonNull
    @Override
    public ReviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_public_review, parent, false);
        return new ReviewViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReviewViewHolder holder, int position) {
        Review review = reviewList.get(position);
        holder.bind(review);

        // Ẩn đường kẻ ở item cuối cùng
        if (position == reviewList.size() - 1) {
            holder.divider.setVisibility(View.GONE);
        } else {
            holder.divider.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public int getItemCount() {
        return reviewList.size();
    }

    class ReviewViewHolder extends RecyclerView.ViewHolder {
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

            // Hiển thị thời gian tương đối (e.g., "2 days ago")
            if (review.getReviewDate() != null) {
                long now = System.currentTimeMillis();
                CharSequence relativeTime = DateUtils.getRelativeTimeSpanString(
                        review.getReviewDate().getTime(), now, DateUtils.MINUTE_IN_MILLIS);
                reviewDate.setText(relativeTime);
            }

            // Tải ảnh người đánh giá
            Glide.with(context)
                    .load(review.getReviewerImageUrl())
                    .placeholder(R.drawable.img)
                    .error(R.drawable.img)
                    .circleCrop()
                    .into(reviewerImage);
        }

        private String getStarString(float rating) {
            int fullStars = Math.round(rating);
            StringBuilder stars = new StringBuilder();
            for (int i = 0; i < 5; i++) {
                if (i < fullStars) {
                    stars.append("★");
                } else {
                    stars.append("☆");
                }
            }
            return stars.toString();
        }
    }
}