package com.example.testapptradeup.adapters;

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
import java.util.Objects;

public class ProductGridAdapter extends ListAdapter<Listing, ProductGridAdapter.ProductGridViewHolder> {

    private final OnProductClickListener listener;

    public interface OnProductClickListener {
        void onProductClick(Listing listing);
    }

    public ProductGridAdapter(OnProductClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ProductGridViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_product_grid, parent, false);
        return new ProductGridViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductGridViewHolder holder, int position) {
        Listing listing = getItem(position);
        holder.bind(listing, listener);
    }

    static class ProductGridViewHolder extends RecyclerView.ViewHolder {
        ImageView productImage, favoriteButton;
        TextView productPrice, productTitle, productLocation;

        ProductGridViewHolder(View itemView) {
            super(itemView);
            productImage = itemView.findViewById(R.id.product_image);
            favoriteButton = itemView.findViewById(R.id.favorite_button);
            productPrice = itemView.findViewById(R.id.product_price);
            productTitle = itemView.findViewById(R.id.product_title);
            productLocation = itemView.findViewById(R.id.product_location);
        }

        void bind(final Listing listing, final OnProductClickListener listener) {
            productPrice.setText(listing.getFormattedPrice());
            productTitle.setText(listing.getTitle());
            productLocation.setText(listing.getLocation());

            Glide.with(itemView.getContext())
                    .load(listing.getPrimaryImageUrl())
                    .placeholder(R.drawable.img_placeholder)
                    .error(R.drawable.img_placeholder)
                    .into(productImage);

            // TODO: Thêm logic cho nút yêu thích
            // favoriteButton.setOnClickListener(...)

            itemView.setOnClickListener(v -> listener.onProductClick(listing));
        }
    }

    private static final DiffUtil.ItemCallback<Listing> DIFF_CALLBACK = new DiffUtil.ItemCallback<Listing>() {
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