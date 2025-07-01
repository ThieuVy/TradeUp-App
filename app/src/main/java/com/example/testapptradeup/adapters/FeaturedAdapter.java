package com.example.testapptradeup.adapters;

import android.annotation.SuppressLint;
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
import com.example.testapptradeup.models.Listing;

/**
 * Adapter for displaying featured products in a RecyclerView.
 * This adapter uses DiffUtil for efficient updates.
 */
public class FeaturedAdapter extends ListAdapter<Listing, FeaturedAdapter.FeaturedViewHolder> {

    // Interface for handling product click events
    public interface OnProductClickListener {
        void onProductClick(Listing listing);
    }

    private final OnProductClickListener listener;

    /**
     * Constructor for FeaturedAdapter.
     * @param listener Listener for product click events.
     */
    public FeaturedAdapter(OnProductClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    @NonNull
    @Override
    public FeaturedViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the layout for a single featured product item
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_featured_product, parent, false);
        return new FeaturedViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull FeaturedViewHolder holder, int position) {
        // Get the Product object at the current position
        Listing currentListing = getItem(position);
        // Bind the product data to the ViewHolder's views
        holder.bind(currentListing);
    }

    /**
     * ViewHolder class for individual featured product items.
     */
    class FeaturedViewHolder extends RecyclerView.ViewHolder {
        private final ImageView productImage;
        private final TextView productTitle;
        private final TextView productPrice;

        @SuppressLint("CutPasteId")
        public FeaturedViewHolder(@NonNull View itemView) {
            super(itemView);
            // Initialize UI components from the item layout
            productImage = itemView.findViewById(R.id.featured_image);
            productTitle = itemView.findViewById(R.id.featured_title);
            productPrice = itemView.findViewById(R.id.featured_price);

            // Set an OnClickListener for the entire item view
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                // Ensure the position is valid before calling the listener
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onProductClick(getItem(position));
                }
            });
        }

        /**
         * Binds the product data to the views in the ViewHolder.
         * @param listing The Product object containing the data to display.
         */
        @SuppressLint("DefaultLocale")
        public void bind(Listing listing) {
            // Load product image using Glide
            if (listing.getImageUrl() != null && !listing.getImageUrl().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(listing.getImageUrl())
                        .placeholder(R.drawable.img) // Placeholder image
                        .error(R.drawable.img)       // Error image
                        .into(productImage);
            } else {
                productImage.setImageResource(R.drawable.img); // Default image if no URL
            }

            // Set product title and price
            productTitle.setText(listing.getTitle());
            productPrice.setText(String.format("$%.2f", listing.getPrice())); // Format price
        }
    }

    /**
     * DiffUtil.ItemCallback implementation for efficient list updates.
     * This helps in only updating the items that have changed, added, or removed.
     */
    private static final DiffUtil.ItemCallback<Listing> DIFF_CALLBACK = new DiffUtil.ItemCallback<Listing>() {
        @Override
        public boolean areItemsTheSame(@NonNull Listing oldItem, @NonNull Listing newItem) {
            // Check if the items represent the same product (e.g., by their ID)
            return oldItem.getId().equals(newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Listing oldItem, @NonNull Listing newItem) {
            // Check if the contents of the items are the same
            // This is a shallow comparison; consider deep comparison if product has complex objects
            return oldItem.getTitle().equals(newItem.getTitle()) &&
                    oldItem.getPrice() == newItem.getPrice() &&
                    oldItem.getImageUrl().equals(newItem.getImageUrl());
        }
    };
}
