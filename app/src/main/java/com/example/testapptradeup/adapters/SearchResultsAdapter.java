package com.example.testapptradeup.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.example.testapptradeup.R;
import com.example.testapptradeup.models.SearchResult;

import java.util.List;

public class SearchResultsAdapter extends RecyclerView.Adapter<SearchResultsAdapter.SearchResultViewHolder> {

    private final List<SearchResult> searchResults;
    private final OnProductClickListener onProductClickListener;
    private final OnFavoriteClickListener onFavoriteClickListener;

    public interface OnProductClickListener {
        void onProductClick(SearchResult product);
    }

    public interface OnFavoriteClickListener {
        void onFavoriteClick(SearchResult product, boolean isFavorite);
    }

    // *** SỬA LỖI 1: Thay đổi kiểu của tham số searchResults ***
    public SearchResultsAdapter(List<SearchResult> searchResults,
                                OnProductClickListener onProductClickListener,
                                OnFavoriteClickListener onFavoriteClickListener) {
        this.searchResults = searchResults;
        this.onProductClickListener = onProductClickListener;
        this.onFavoriteClickListener = onFavoriteClickListener;
    }

    @NonNull
    @Override
    public SearchResultViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_product_search, parent, false);
        return new SearchResultViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SearchResultViewHolder holder, int position) {
        SearchResult result = searchResults.get(position);
        // *** SỬA LỖI: Bỏ tham số không cần thiết ***
        holder.bind(result, onProductClickListener, onFavoriteClickListener);
    }

    @Override
    public int getItemCount() {
        return searchResults.size();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void updateResults(List<SearchResult> newResults) {
        this.searchResults.clear();
        this.searchResults.addAll(newResults);
        notifyDataSetChanged();
    }

    public void addResults(List<SearchResult> moreResults) {
        int startPosition = this.searchResults.size();
        this.searchResults.addAll(moreResults);
        notifyItemRangeInserted(startPosition, moreResults.size());
    }

    static class SearchResultViewHolder extends RecyclerView.ViewHolder {
        private final ImageView productImage;
        private final TextView productTitle;
        private final TextView productPrice;
        private final TextView productCondition;
        private final TextView productLocation;
        private final TextView postedTime;
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

        // *** SỬA LỖI: Tái cấu trúc hàm bind để đơn giản hơn ***
        public void bind(final SearchResult result,
                         final OnProductClickListener productClickListener,
                         final OnFavoriteClickListener favoriteClickListener) {

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

            // Set listeners
            itemView.setOnClickListener(v -> {
                if (productClickListener != null) {
                    productClickListener.onProductClick(result);
                }
            });

            favoriteButton.setOnClickListener(v -> {
                if (favoriteClickListener != null) {
                    boolean newFavoriteState = !result.isFavorite();
                    result.setFavorite(newFavoriteState); // Cập nhật trạng thái trong model
                    updateFavoriteButton(newFavoriteState); // Cập nhật UI ngay lập tức
                    favoriteClickListener.onFavoriteClick(result, newFavoriteState);
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
                favoriteButton.setImageResource(R.drawable.ic_favorite); // Icon trái tim đầy
                favoriteButton.setColorFilter(context.getColor(R.color.red_error)); // Màu đỏ
            } else {
                favoriteButton.setImageResource(R.drawable.ic_favorite_outline); // Icon trái tim rỗng
                favoriteButton.setColorFilter(context.getColor(R.color.text_secondary)); // Màu xám
            }
        }
    }
}