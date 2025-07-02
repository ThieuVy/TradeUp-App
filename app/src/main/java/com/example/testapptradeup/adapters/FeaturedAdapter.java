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

import java.text.NumberFormat;
import java.util.Locale;
import java.util.Objects;

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
            // Kiểm tra danh sách URL có tồn tại và không rỗng
            if (listing.getImageUrls() != null && !listing.getImageUrls().isEmpty()) {
                // Tải ảnh đầu tiên trong danh sách bằng Glide
                // URL này chính là URL từ Cloudinary mà bạn đã upload và lưu vào Firestore
                Glide.with(itemView.getContext())
                        .load(listing.getImageUrls().get(0))
                        .placeholder(R.drawable.img)
                        .error(R.drawable.img)
                        .into(productImage);
            } else {
                // Nếu không có ảnh, hiển thị ảnh mặc định
                productImage.setImageResource(R.drawable.img);
            }
            productTitle.setText(listing.getTitle());
            // Định dạng giá sang VNĐ
            NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
            productPrice.setText(format.format(listing.getPrice()));
        }
    }

    /**
     * DiffUtil.ItemCallback implementation for efficient list updates.
     * This helps in only updating the items that have changed, added, or removed.
     */
    private static final DiffUtil.ItemCallback<Listing> DIFF_CALLBACK = new DiffUtil.ItemCallback<>() {
        @Override
        public boolean areItemsTheSame(@NonNull Listing oldItem, @NonNull Listing newItem) {
            // ID là cách tốt nhất để xác định item có giống nhau không
            return Objects.equals(oldItem.getId(), newItem.getId());
        }

        @SuppressLint("DiffUtilEquals")
        @Override
        public boolean areContentsTheSame(@NonNull Listing oldItem, @NonNull Listing newItem) {
            // *** BẮT ĐẦU SỬA LỖI ***
            // Lấy URL ảnh đầu tiên từ mỗi item để so sánh
            String oldImageUrl = (oldItem.getImageUrls() != null && !oldItem.getImageUrls().isEmpty()) ? oldItem.getImageUrls().get(0) : null;
            String newImageUrl = (newItem.getImageUrls() != null && !newItem.getImageUrls().isEmpty()) ? newItem.getImageUrls().get(0) : null;

            // So sánh các nội dung chính
            return oldItem.getTitle().equals(newItem.getTitle()) &&
                    oldItem.getPrice() == newItem.getPrice() &&
                    Objects.equals(oldImageUrl, newImageUrl);
            // *** KẾT THÚC SỬA LỖI ***
        }
    };
}
