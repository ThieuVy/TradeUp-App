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
import com.example.testapptradeup.R;
import com.example.testapptradeup.models.Listing;
import java.util.List;

public class FavoritesAdapter extends RecyclerView.Adapter<FavoritesAdapter.FavoriteViewHolder> {

    private final Context context;
    private final List<Listing> favoriteListings;

    public FavoritesAdapter(Context context, List<Listing> favoriteListings) {
        this.context = context;
        this.favoriteListings = favoriteListings;
    }

    @NonNull
    @Override
    public FavoriteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_favorite_listing, parent, false);
        return new FavoriteViewHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull FavoriteViewHolder holder, int position) {
        Listing listing = favoriteListings.get(position);

        holder.textName.setText(listing.getTitle());
        holder.textPrice.setText(listing.getFormattedPrice());

        if ("sold".equalsIgnoreCase(listing.getStatus())) {
            holder.textStockStatus.setText("Đã bán");
        } else {
            holder.textStockStatus.setText("Còn hàng");
        }

        // <<--- LOGIC ĐƯỢC ĐƠN GIẢN HÓA
        String imageUrl = listing.getPrimaryImageUrl();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            // Luôn tải ảnh từ URL (Cloudinary hoặc bất kỳ host nào khác)
            Glide.with(context)
                    .load(imageUrl)
                    .placeholder(R.drawable.img_placeholder)
                    .error(R.drawable.img_placeholder)
                    .into(holder.imageProduct);
        } else {
            // Nếu không có ảnh, hiển thị placeholder
            holder.imageProduct.setImageResource(R.drawable.img_placeholder);
        }
    }

    @Override
    public int getItemCount() {
        return favoriteListings.size();
    }

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
    @SuppressLint("NotifyDataSetChanged")
    public void updateListings(List<Listing> newListings) {
        this.favoriteListings.clear();
        this.favoriteListings.addAll(newListings);
        notifyDataSetChanged();
    }
}