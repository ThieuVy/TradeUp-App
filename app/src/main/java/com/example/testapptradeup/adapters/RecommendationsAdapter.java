package com.example.testapptradeup.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.example.testapptradeup.R;
import com.example.testapptradeup.models.Listing;

public class RecommendationsAdapter extends ListAdapter<Listing, RecommendationsAdapter.ProductViewHolder> {
    private final OnProductClickListener listener;

    public interface OnProductClickListener {
        void onProductClick(Listing listing);
    }

    public RecommendationsAdapter(OnProductClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_product_recommendation, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        Listing listing = getItem(position);
        holder.bind(listing);
    }

    class ProductViewHolder extends RecyclerView.ViewHolder {
        private final TextView title;

        ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.product_title);
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onProductClick(getItem(getAdapterPosition()));
                }
            });
        }

        void bind(Listing listing) {
            title.setText(listing.getTitle());
            // Bind các thuộc tính khác như hình ảnh, giá, v.v.
        }
    }

    private static final DiffUtil.ItemCallback<Listing> DIFF_CALLBACK = new DiffUtil.ItemCallback<Listing>() {
        @Override
        public boolean areItemsTheSame(@NonNull Listing oldItem, @NonNull Listing newItem) {
            return oldItem.getId().equals(newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Listing oldItem, @NonNull Listing newItem) {
            return oldItem.equals(newItem);
        }
    };
}