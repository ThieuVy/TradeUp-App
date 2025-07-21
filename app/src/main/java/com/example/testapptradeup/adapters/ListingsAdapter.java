package com.example.testapptradeup.adapters;

import android.annotation.SuppressLint;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.testapptradeup.R;
import com.example.testapptradeup.models.Listing;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ListingsAdapter extends ListAdapter<Listing, ListingsAdapter.ProductViewHolder> {

    private final OnProductClickListener productClickListener;
    private final OnFavoriteClickListener favoriteClickListener;
    private List<String> favoriteIds = new ArrayList<>(); // THÊM MỚI

    public interface OnProductClickListener {
        void onProductClick(Listing listing);
    }

    public interface OnFavoriteClickListener {
        void onFavoriteClick(Listing listing, ImageView favoriteIcon);
    }

    public ListingsAdapter(OnProductClickListener productClickListener, OnFavoriteClickListener favoriteClickListener) {
        super(DIFF_CALLBACK);
        this.productClickListener = productClickListener;
        this.favoriteClickListener = favoriteClickListener;
    }

    // THÊM MỚI: Phương thức để cập nhật danh sách ID yêu thích
    @SuppressLint("NotifyDataSetChanged")
    public void setFavoriteIds(List<String> favoriteIds) {
        this.favoriteIds = favoriteIds;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_product_listing, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        Listing listing = getItem(position);
        holder.bind(listing);
    }

    public class ProductViewHolder extends RecyclerView.ViewHolder {
        private final ImageView productImage;
        private final TextView title;
        private final TextView price;
        private final TextView location;
        private final TextView timePosted;
        private final ImageView favoriteButton;

        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            productImage = itemView.findViewById(R.id.listing_image);
            title = itemView.findViewById(R.id.listing_title);
            price = itemView.findViewById(R.id.listing_price);
            location = itemView.findViewById(R.id.listing_location);
            timePosted = itemView.findViewById(R.id.listing_time_posted);
            favoriteButton = itemView.findViewById(R.id.favorite_icon);

            itemView.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION && productClickListener != null) {
                    productClickListener.onProductClick(getItem(position));
                }
            });

            favoriteButton.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION && favoriteClickListener != null) {
                    favoriteClickListener.onFavoriteClick(getItem(position), favoriteButton);
                }
            });
        }

        @SuppressLint("SetTextI18n")
        void bind(Listing listing) {
            title.setText(listing.getTitle());
            price.setText(listing.getFormattedPrice());
            location.setText(listing.getLocation());

            if (listing.getTimePosted() != null) {
                CharSequence relativeTime = DateUtils.getRelativeTimeSpanString(
                        listing.getTimePosted().getTime(),
                        System.currentTimeMillis(),
                        DateUtils.MINUTE_IN_MILLIS
                );
                timePosted.setText(relativeTime);
            } else {
                timePosted.setText("");
            }

            if (listing.getPrimaryImageUrl() != null && !listing.getPrimaryImageUrl().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(listing.getPrimaryImageUrl())
                        .placeholder(R.drawable.img_placeholder)
                        .error(R.drawable.img_placeholder)
                        .into(productImage);
            } else {
                productImage.setImageResource(R.drawable.img_placeholder);
            }

            // SỬA ĐỔI: Logic cập nhật trạng thái icon trái tim
            if (favoriteIds.contains(listing.getId())) {
                favoriteButton.setImageResource(R.drawable.ic_favorite_filled);
                favoriteButton.setColorFilter(ContextCompat.getColor(itemView.getContext(), R.color.red_error));
                favoriteButton.setTag(true);
            } else {
                favoriteButton.setImageResource(R.drawable.ic_favorite_outline);
                favoriteButton.setColorFilter(ContextCompat.getColor(itemView.getContext(), R.color.text_secondary));
                favoriteButton.setTag(false);
            }
        }
    }

    private static final DiffUtil.ItemCallback<Listing> DIFF_CALLBACK = new DiffUtil.ItemCallback<>() {
        @Override
        public boolean areItemsTheSame(@NonNull Listing oldItem, @NonNull Listing newItem) {
            return Objects.equals(oldItem.getId(), newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Listing oldItem, @NonNull Listing newItem) {
            // SỬA ĐỔI: So sánh cả trạng thái yêu thích để DiffUtil hoạt động chính xác
            return oldItem.equals(newItem) && (oldItem.isFavorite() == newItem.isFavorite());
        }
    };
}