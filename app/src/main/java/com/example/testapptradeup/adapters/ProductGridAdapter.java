package com.example.testapptradeup.adapters;

import android.annotation.SuppressLint;
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

public class ProductGridAdapter extends ListAdapter<Listing, ProductGridAdapter.ProductGridViewHolder> {

    public interface OnProductClickListener {
        void onProductClick(Listing listing);
    }

    public interface OnFavoriteClickListener {
        void onFavoriteClick(Listing listing, ImageView favoriteIcon);
    }

    private final OnProductClickListener productClickListener;
    private final OnFavoriteClickListener favoriteClickListener;
    private List<String> favoriteIds = new ArrayList<>(); // Danh sách ID yêu thích

    public ProductGridAdapter(@NonNull OnProductClickListener productClickListener, @NonNull OnFavoriteClickListener favoriteClickListener) {
        super(DIFF_CALLBACK);
        this.productClickListener = productClickListener;
        this.favoriteClickListener = favoriteClickListener;
    }

    @NonNull
    @Override
    public ProductGridViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_product_grid, parent, false);
        return new ProductGridViewHolder(view, productClickListener, favoriteClickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductGridViewHolder holder, int position) {
        Listing listing = getItem(position);
        holder.bind(listing);
    }

    // Phương thức mới để cập nhật danh sách ID yêu thích từ Fragment
    @SuppressLint("NotifyDataSetChanged")
    public void setFavoriteIds(List<String> favoriteIds) {
        this.favoriteIds = favoriteIds;
        notifyDataSetChanged(); // Yêu cầu adapter vẽ lại các item hiển thị
    }

    class ProductGridViewHolder extends RecyclerView.ViewHolder {
        ImageView productImage, favoriteButton;
        TextView productPrice, productTitle, productLocation;

        ProductGridViewHolder(View itemView, final OnProductClickListener productClickListener, final OnFavoriteClickListener favoriteClickListener) {
            super(itemView);
            productImage = itemView.findViewById(R.id.product_image);
            favoriteButton = itemView.findViewById(R.id.favorite_button);
            productPrice = itemView.findViewById(R.id.product_price);
            productTitle = itemView.findViewById(R.id.product_title);
            productLocation = itemView.findViewById(R.id.product_location);

            itemView.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    productClickListener.onProductClick(getItem(position));
                }
            });

            favoriteButton.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    favoriteClickListener.onFavoriteClick(getItem(position), favoriteButton);
                }
            });
        }

        void bind(final Listing listing) {
            productPrice.setText(listing.getFormattedPrice());
            productTitle.setText(listing.getTitle());
            productLocation.setText(listing.getLocation());

            Glide.with(itemView.getContext())
                    .load(listing.getPrimaryImageUrl())
                    .placeholder(R.drawable.img_placeholder)
                    .error(R.drawable.img_placeholder)
                    .into(productImage);

            // Logic cập nhật trạng thái icon trái tim
            if (favoriteIds.contains(listing.getId())) {
                favoriteButton.setImageResource(R.drawable.ic_favorite_filled);
                favoriteButton.setColorFilter(ContextCompat.getColor(itemView.getContext(), R.color.red_error));
                favoriteButton.setTag(true); // Đánh dấu là đang được yêu thích
            } else {
                favoriteButton.setImageResource(R.drawable.ic_favorite_outline);
                favoriteButton.setColorFilter(ContextCompat.getColor(itemView.getContext(), R.color.text_secondary));
                favoriteButton.setTag(false); // Đánh dấu là không được yêu thích
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
            return oldItem.equals(newItem);
        }
    };
}