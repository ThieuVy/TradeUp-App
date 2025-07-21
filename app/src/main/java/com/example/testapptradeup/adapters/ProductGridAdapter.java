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

    // BƯỚC 1: Định nghĩa một interface để làm "khuôn mẫu" cho sự kiện click
    public interface OnProductClickListener {
        void onProductClick(Listing listing);
    }

    // BƯỚC 2: Khai báo một biến để lưu trữ listener
    private final OnProductClickListener listener;

    // BƯỚC 3: Sửa lại constructor để nó nhận vào một OnProductClickListener
    public ProductGridAdapter(@NonNull OnProductClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener; // Lưu lại listener được truyền vào
    }

    @NonNull
    @Override
    public ProductGridViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_product_grid, parent, false);
        // BƯỚC 4: Truyền listener vào trong ViewHolder khi nó được tạo ra
        return new ProductGridViewHolder(view, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductGridViewHolder holder, int position) {
        Listing listing = getItem(position);
        holder.bind(listing);
    }

    // BỎ TỪ KHÓA 'static' ĐỂ VIEWHOLDER CÓ THỂ TRUY CẬP PHƯƠNG THỨC getItem() CỦA ADAPTER
    class ProductGridViewHolder extends RecyclerView.ViewHolder {
        ImageView productImage, favoriteButton;
        TextView productPrice, productTitle, productLocation;

        // BƯỚC 5: Constructor của ViewHolder cũng nhận listener
        ProductGridViewHolder(View itemView, final OnProductClickListener listener) {
            super(itemView);
            productImage = itemView.findViewById(R.id.product_image);
            favoriteButton = itemView.findViewById(R.id.favorite_button);
            productPrice = itemView.findViewById(R.id.product_price);
            productTitle = itemView.findViewById(R.id.product_title);
            productLocation = itemView.findViewById(R.id.product_location);

            // BƯỚC 6: Gán sự kiện click cho toàn bộ item view
            itemView.setOnClickListener(v -> {
                int position = getBindingAdapterPosition(); // Lấy vị trí item một cách an toàn
                // Luôn kiểm tra vị trí hợp lệ trước khi thực hiện hành động
                if (position != RecyclerView.NO_POSITION) {
                    // Gọi phương thức trong interface đã được truyền vào
                    listener.onProductClick(getItem(position));
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

            // TODO: Thêm logic cho nút yêu thích nếu cần
        }
    }

    private static final DiffUtil.ItemCallback<Listing> DIFF_CALLBACK = new DiffUtil.ItemCallback<>() {
        @Override
        public boolean areItemsTheSame(@NonNull Listing oldItem, @NonNull Listing newItem) {
            return Objects.equals(oldItem.getId(), newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Listing oldItem, @NonNull Listing newItem) {
            // Đảm bảo bạn đã override hàm equals() trong model Listing
            return oldItem.equals(newItem);
        }
    };
}