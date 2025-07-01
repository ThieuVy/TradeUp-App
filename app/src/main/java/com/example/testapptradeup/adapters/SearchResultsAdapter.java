package com.example.testapptradeup.adapters;

import android.annotation.SuppressLint;
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
import com.example.testapptradeup.models.Listing;
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

    public SearchResultsAdapter(List<Listing> searchResults,
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
        holder.bind(result, onProductClickListener, onFavoriteClickListener, searchResults);
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

    // Made static to fix visibility scope warning
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

        public void bind(SearchResult result,
                         OnProductClickListener onProductClickListener,
                         OnFavoriteClickListener onFavoriteClickListener,
                         List<SearchResult> searchResults) {

            productTitle.setText(result.getTitle());
            productPrice.setText(result.getPrice());
            productCondition.setText(result.getCondition());

            // Format location with distance
            String locationText = result.getLocation();
            if (result.getDistance() != null && !result.getDistance().isEmpty()) {
                locationText += " • " + result.getDistance();
            }
            productLocation.setText(locationText);

            postedTime.setText(result.getPostedTime());

            // Update favorite button state
            updateFavoriteButton(result.isFavorite());

            // Load product image
            loadProductImage(result);

            // Set click listeners
            setupClickListeners(onProductClickListener, onFavoriteClickListener, searchResults);
        }

        private void setupClickListeners(OnProductClickListener onProductClickListener,
                                         OnFavoriteClickListener onFavoriteClickListener,
                                         List<SearchResult> searchResults) {
            // Product click listener
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && onProductClickListener != null) {
                    onProductClickListener.onProductClick(searchResults.get(position));
                }
            });

            // Favorite button click listener
            favoriteButton.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && onFavoriteClickListener != null) {
                    SearchResult result = searchResults.get(position);
                    boolean newFavoriteState = !result.isFavorite();
                    result.setFavorite(newFavoriteState);
                    updateFavoriteButton(newFavoriteState);
                    onFavoriteClickListener.onFavoriteClick(result, newFavoriteState);
                }
            });
        }

        private void loadProductImage(SearchResult result) {
            if (result.getImageUrl() != null && !result.getImageUrl().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(result.getImageUrl())
                        .apply(new RequestOptions()
                                .placeholder(R.drawable.ic_image_placeholder)
                                .error(R.drawable.ic_image_placeholder)
                                .transform(new RoundedCorners(16)))
                        .into(productImage);
            } else {
                // Set placeholder image
                Glide.with(itemView.getContext())
                        .load(R.drawable.ic_image_placeholder)
                        .apply(new RequestOptions()
                                .transform(new RoundedCorners(16)))
                        .into(productImage);
            }
        }

        private void updateFavoriteButton(boolean isFavorite) {
            if (isFavorite) {
                favoriteButton.setImageResource(R.drawable.ic_favorite);
                favoriteButton.setColorFilter(itemView.getContext().getColor(R.color.favorite_color));
            } else {
                favoriteButton.setImageResource(R.drawable.ic_favorite_outline);
                favoriteButton.setColorFilter(itemView.getContext().getColor(R.color.text_secondary));
            }
        }
    }
}