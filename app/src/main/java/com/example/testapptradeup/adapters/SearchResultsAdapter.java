package com.example.testapptradeup.adapters;

import android.content.Context;
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
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.example.testapptradeup.R;
import com.example.testapptradeup.models.SearchResult;

import java.util.Objects;

public class SearchResultsAdapter extends ListAdapter<SearchResult, SearchResultsAdapter.SearchResultViewHolder> {

    private final OnProductClickListener onProductClickListener;
    private final OnFavoriteClickListener onFavoriteClickListener;

    public interface OnProductClickListener { void onProductClick(SearchResult product); }
    public interface OnFavoriteClickListener { void onFavoriteClick(SearchResult product, int position); }

    public SearchResultsAdapter(OnProductClickListener onProductClickListener, OnFavoriteClickListener onFavoriteClickListener) {
        super(DIFF_CALLBACK);
        this.onProductClickListener = onProductClickListener;
        this.onFavoriteClickListener = onFavoriteClickListener;
    }

    @NonNull
    @Override
    public SearchResultViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_product_search, parent, false);
        // SỬA LỖI: Không truyền listener vào ViewHolder nữa vì nó sẽ được gán trong onBindViewHolder
        return new SearchResultViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SearchResultViewHolder holder, int position) {
        SearchResult result = getItem(position);
        // SỬA LỖI: Truyền đối tượng `result` vào hàm bind.
        // Đây là cách làm đúng đắn, đảm bảo ViewHolder luôn có dữ liệu chính xác.
        holder.bind(result, onProductClickListener, onFavoriteClickListener);
    }

    static class SearchResultViewHolder extends RecyclerView.ViewHolder {
        private final ImageView productImage;
        private final TextView productTitle, productPrice, productCondition, productLocation, postedTime;
        private final ImageView favoriteButton;

        public SearchResultViewHolder(@NonNull View itemView) {
            super(itemView);
            productImage = itemView.findViewById(R.id.product_image);
            productTitle = itemView.findViewById(R.id.product_title);
            productPrice = itemView.findViewById(R.id.product_price);
            productCondition = itemView.findViewById(R.id.product_condition);
            productLocation = itemView.findViewById(R.id.product_location);
            postedTime = itemView.findViewById(R.id.posted_time);
            favoriteButton = itemView.findViewById(R.id.favorite_button);
        }

        // Hàm bind giờ đây nhận cả listener
        public void bind(final SearchResult result,
                         final OnProductClickListener productClickListener,
                         final OnFavoriteClickListener favoriteClickListener) {

            // Logic bind dữ liệu vào UI giữ nguyên
            productTitle.setText(result.getTitle());
            productPrice.setText(result.getPrice());
            productCondition.setText(result.getCondition());
            String locationText = result.getLocation();
            if (result.getDistance() != null && !result.getDistance().isEmpty()) {
                locationText += " • " + result.getDistance();
            }
            productLocation.setText(locationText);
            postedTime.setText(result.getPostedTime());
            updateFavoriteButton(result.isFavorite());
            loadProductImage(result);

            // SỬA LỖI: Gán listener ở đây, nơi `result` luôn hợp lệ
            itemView.setOnClickListener(v -> {
                if (productClickListener != null) {
                    productClickListener.onProductClick(result);
                }
            });

            favoriteButton.setOnClickListener(v -> {
                if (favoriteClickListener != null) {
                    // Lấy vị trí hiện tại của ViewHolder
                    int position = getBindingAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        favoriteClickListener.onFavoriteClick(result, position);
                    }
                }
            });
        }

        private void loadProductImage(SearchResult result) {
            Context context = itemView.getContext();
            RequestOptions requestOptions = new RequestOptions()
                    .placeholder(R.drawable.img) // Dùng placeholder chung
                    .error(R.drawable.img)
                    .transform(new RoundedCorners(16));

            if (result.getImageUrl() != null && !result.getImageUrl().isEmpty()) {
                Glide.with(context)
                        .load(result.getImageUrl())
                        .apply(requestOptions)
                        .into(productImage);
            } else {
                Glide.with(context)
                        .load(R.drawable.img) // Load placeholder mặc định
                        .apply(requestOptions)
                        .into(productImage);
            }
        }

        private void updateFavoriteButton(boolean isFavorite) {
            Context context = itemView.getContext();
            if (isFavorite) {
                favoriteButton.setImageResource(R.drawable.ic_favorite_filled);
                favoriteButton.setColorFilter(ContextCompat.getColor(context, R.color.red_error));
            } else {
                favoriteButton.setImageResource(R.drawable.ic_favorite_outline);
                favoriteButton.setColorFilter(ContextCompat.getColor(context, R.color.text_secondary));
            }
        }
    }

    private static final DiffUtil.ItemCallback<SearchResult> DIFF_CALLBACK = new DiffUtil.ItemCallback<>() {
        @Override
        public boolean areItemsTheSame(@NonNull SearchResult oldItem, @NonNull SearchResult newItem) {
            return oldItem.getId().equals(newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull SearchResult oldItem, @NonNull SearchResult newItem) {
            return oldItem.getTitle().equals(newItem.getTitle()) &&
                    oldItem.getPrice().equals(newItem.getPrice()) &&
                    oldItem.isFavorite() == newItem.isFavorite() &&
                    Objects.equals(oldItem.getImageUrl(), newItem.getImageUrl());
        }
    };
}