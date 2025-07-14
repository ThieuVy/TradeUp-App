package com.example.testapptradeup.adapters;

import android.annotation.SuppressLint;
import android.text.format.DateUtils;
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

public class ListingsAdapter extends ListAdapter<Listing, ListingsAdapter.ProductViewHolder> {

    private final OnProductClickListener productClickListener;
    private final OnFavoriteClickListener favoriteClickListener;

    public interface OnProductClickListener {
        void onProductClick(Listing listing);
    }

    public interface OnFavoriteClickListener {
        // Interface này yêu cầu một phương thức có 2 tham số: Listing và ImageView
        void onFavoriteClick(Listing listing, ImageView favoriteIcon);
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

    public class ProductViewHolder extends RecyclerView.ViewHolder {
        private final ImageView productImage;
        private final TextView title;
        private final TextView price;
        // <<< SỬA LỖI 1: Khai báo thêm TextView cho thời gian >>>
        private final TextView location;
        private final TextView timePosted;
        private final ImageView favoriteButton;

        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            productImage = itemView.findViewById(R.id.listing_image);
            title = itemView.findViewById(R.id.listing_title);
            price = itemView.findViewById(R.id.listing_price);

            // <<< SỬA LỖI 2: Ánh xạ đúng ID từ XML >>>
            location = itemView.findViewById(R.id.listing_location); // Sửa từ listing_distance
            timePosted = itemView.findViewById(R.id.listing_time_posted);
            favoriteButton = itemView.findViewById(R.id.favorite_icon); // ID này giờ đã tồn tại

            itemView.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION && productClickListener != null) {
                    productClickListener.onProductClick(getItem(position));
                }
            });

            favoriteButton.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION && favoriteClickListener != null) {
                    // Truyền cả icon vào để Fragment/ViewModel có thể cập nhật trạng thái ngay lập tức
                    favoriteClickListener.onFavoriteClick(getItem(position), favoriteButton);
                }
            });
        }

        @SuppressLint("SetTextI18n")
        void bind(Listing listing) {
            title.setText(listing.getTitle());
            price.setText(listing.getFormattedPrice());
            location.setText(listing.getLocation());

            // <<< SỬA LỖI 3: Hiển thị thời gian đăng >>>
            if (listing.getTimePosted() != null) {
                // Sử dụng DateUtils để có định dạng thân thiện "x giờ trước", "Hôm qua", ...
                CharSequence relativeTime = DateUtils.getRelativeTimeSpanString(
                        listing.getTimePosted().getTime(),
                        System.currentTimeMillis(),
                        DateUtils.MINUTE_IN_MILLIS
                );
                timePosted.setText(relativeTime);
            } else {
                timePosted.setText(""); // Ẩn nếu không có dữ liệu
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
            // TODO: Bạn sẽ cần logic để kiểm tra xem sản phẩm này có được yêu thích hay không
            // và cập nhật `favoriteButton` (ví dụ: `favoriteButton.setImageResource(...)`)
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