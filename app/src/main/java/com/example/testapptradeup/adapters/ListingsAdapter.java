package com.example.testapptradeup.adapters;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView; // Giữ import cho ImageView
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.testapptradeup.R;
import com.example.testapptradeup.models.Listing;
import com.google.android.material.button.MaterialButton;

public class ListingsAdapter extends ListAdapter<Listing, ListingsAdapter.ProductViewHolder> {

    private final OnProductClickListener productClickListener;
    private final OnFavoriteClickListener favoriteClickListener;

    public interface OnProductClickListener {
        void onProductClick(Listing listing);
    }

    public interface OnFavoriteClickListener {
        void onFavoriteClick(Listing listing);
    }

    public ListingsAdapter(OnProductClickListener productClickListener, OnFavoriteClickListener favoriteClickListener) {
        super(DIFF_CALLBACK);
        this.productClickListener = productClickListener;
        this.favoriteClickListener = favoriteClickListener;
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

    // Hoàn thiện ViewHolder để ánh xạ đầy đủ các view
    class ProductViewHolder extends RecyclerView.ViewHolder {
        private final ImageView productImage; // SỬA LỖI: Khai báo đúng là ImageView
        private final TextView title;
        private final TextView price;
        private final TextView location;
        private final ImageView favoriteButton;

        @SuppressLint("WrongViewCast")
        ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            // Ánh xạ đúng View với đúng ID từ file XML
            productImage = itemView.findViewById(R.id.listing_image); // Giả sử ID là listing_product_image
            title = itemView.findViewById(R.id.listing_title);
            price = itemView.findViewById(R.id.listing_price); // Giả sử ID là listing_product_price
            location = itemView.findViewById(R.id.listing_distance); // Giả sử ID là listing_product_location
            favoriteButton = itemView.findViewById(R.id.favorite_icon);; // ID này đã đúng

            // Listener không thay đổi
            itemView.setOnClickListener(v -> {
                if (productClickListener != null) {
                    int position = getBindingAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        productClickListener.onProductClick(getItem(position));
                    }
                }
            });

            favoriteButton.setOnClickListener(v -> {
                if (favoriteClickListener != null) {
                    int position = getBindingAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        favoriteClickListener.onFavoriteClick(getItem(position));
                    }
                }
            });
        }

        // Cập nhật hàm bind để hiển thị đầy đủ thông tin
        @SuppressLint("SetTextI18n")
        void bind(Listing listing) {
            title.setText(listing.getTitle());
            // Đổi lại cách format giá cho đúng
            price.setText(listing.getFormattedPrice());
            location.setText(listing.getLocation());

            // Tải ảnh bằng Glide
            if (listing.getPrimaryImageUrl() != null && !listing.getPrimaryImageUrl().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(listing.getPrimaryImageUrl())
                        .placeholder(R.drawable.img_placeholder)
                        .error(R.drawable.img_placeholder)
                        .into(productImage);
            } else {
                productImage.setImageResource(R.drawable.img_placeholder);
            }

            // Cập nhật trạng thái nút yêu thích (dựa vào trường isFavorite trong Product model)
            // Ví dụ:
            // boolean isFavorite = ... ; // Lấy trạng thái yêu thích
            // favoriteButton.setImageResource(isFavorite ? R.drawable.ic_favorite_filled : R.drawable.ic_favorite_border);
        }
    }

    private static final DiffUtil.ItemCallback<Listing> DIFF_CALLBACK = new DiffUtil.ItemCallback<Listing>() {
        @Override
        public boolean areItemsTheSame(@NonNull Listing oldItem, @NonNull Listing newItem) {
            return oldItem.getId().equals(newItem.getId()); // Giả sử Product có getId()
        }

        @Override
        public boolean areContentsTheSame(@NonNull Listing oldItem, @NonNull Listing newItem) {
            // So sánh tất cả các thuộc tính để xác định nội dung có thay đổi không
            return oldItem.equals(newItem); // Yêu cầu Product override equals()
        }
    };
}
