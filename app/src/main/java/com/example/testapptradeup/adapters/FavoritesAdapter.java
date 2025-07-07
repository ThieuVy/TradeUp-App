package com.example.testapptradeup.adapters;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
// THÊM IMPORT NÀY
import androidx.recyclerview.widget.DiffUtil;
// THAY ĐỔI IMPORT NÀY
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.testapptradeup.R;
import com.example.testapptradeup.models.Listing;
import java.util.Objects;

// SỬA ĐỔI 1: Kế thừa từ ListAdapter thay vì RecyclerView.Adapter
public class FavoritesAdapter extends ListAdapter<Listing, FavoritesAdapter.FavoriteViewHolder> {

    // SỬA ĐỔI 2: Xóa các biến không cần thiết (Context và List)
    // ListAdapter sẽ tự quản lý danh sách.

    // SỬA ĐỔI 3: Constructor gọi đến constructor của lớp cha với DIFF_CALLBACK
    public FavoritesAdapter() {
        super(DIFF_CALLBACK);
    }

    @NonNull
    @Override
    public FavoriteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_favorite_listing, parent, false);
        return new FavoriteViewHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull FavoriteViewHolder holder, int position) {
        // SỬA ĐỔI 4: Dùng getItem(position) để lấy đối tượng. Phương thức này đã có sẵn trong ListAdapter.
        Listing listing = getItem(position);

        // Logic bind dữ liệu giữ nguyên, nhưng lấy context từ itemView để an toàn hơn
        holder.textName.setText(listing.getTitle());
        holder.textPrice.setText(listing.getFormattedPrice());

        if ("sold".equalsIgnoreCase(listing.getStatus())) {
            holder.textStockStatus.setText("Đã bán");
            // Tùy chọn: đổi màu cho trạng thái đã bán
            holder.textStockStatus.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.red_error));
        } else {
            holder.textStockStatus.setText("Còn hàng");
            holder.textStockStatus.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.success));
        }

        String imageUrl = listing.getPrimaryImageUrl();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(imageUrl)
                    .placeholder(R.drawable.img_placeholder)
                    .error(R.drawable.img_placeholder)
                    .into(holder.imageProduct);
        } else {
            holder.imageProduct.setImageResource(R.drawable.img_placeholder);
        }
    }

    // SỬA ĐỔI 5: Xóa các phương thức không cần thiết (`getItemCount`, `updateListings`)
    // ListAdapter đã xử lý những việc này.

    // Lớp ViewHolder giữ nguyên
    public static class FavoriteViewHolder extends RecyclerView.ViewHolder {
        ImageView imageProduct;
        TextView textName, textPrice, textStockStatus;

        public FavoriteViewHolder(@NonNull View itemView) {
            super(itemView);
            imageProduct = itemView.findViewById(R.id.image_product);
            textName = itemView.findViewById(R.id.text_name);
            textPrice = itemView.findViewById(R.id.text_price);
            textStockStatus = itemView.findViewById(R.id.text_stock_status);
        }
    }

    // SỬA ĐỔI 6: Khai báo DIFF_CALLBACK là static và final
    private static final DiffUtil.ItemCallback<Listing> DIFF_CALLBACK = new DiffUtil.ItemCallback<Listing>() {
        @Override
        public boolean areItemsTheSame(@NonNull Listing oldItem, @NonNull Listing newItem) {
            // So sánh dựa trên ID duy nhất
            return Objects.equals(oldItem.getId(), newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Listing oldItem, @NonNull Listing newItem) {
            // So sánh nội dung để biết có cần vẽ lại item không
            // Đảm bảo Listing có implement equals() một cách chính xác
            return oldItem.equals(newItem);
        }
    };
}