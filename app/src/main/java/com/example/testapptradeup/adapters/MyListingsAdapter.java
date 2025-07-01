package com.example.testapptradeup.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.testapptradeup.R;
import com.example.testapptradeup.models.Listing; // Đảm bảo bạn có model này

import java.util.List;

public class MyListingsAdapter extends RecyclerView.Adapter<MyListingsAdapter.ListingViewHolder> {

    private final Context context;
    private final List<Listing> listingList;

    public MyListingsAdapter(Context context, List<Listing> listingList) {
        this.context = context;
        this.listingList = listingList;
    }

    @NonNull
    @Override
    public ListingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_my_listing, parent, false);
        return new ListingViewHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull ListingViewHolder holder, int position) {
        Listing listing = listingList.get(position);

        // Đặt thông tin text
        holder.title.setText(listing.getTitle());
        holder.price.setText(listing.getFormattedPrice()); // Dùng hàm định dạng giá
        // Bạn cần thêm hàm getPostTimeAgo() vào model Listing để có "2 days ago"
        // holder.time.setText(listing.getPostTimeAgo());

        // Tải ảnh bằng Glide
        // Giả sử ảnh là Base64
        try {
            byte[] imageBytes = Base64.decode(listing.getPrimaryImageUrl(), Base64.DEFAULT);
            Glide.with(context)
                    .load(imageBytes)
                    .placeholder(R.drawable.img) // Thay bằng ảnh placeholder của bạn
                    .into(holder.image);
        } catch (Exception e) {
            Glide.with(context).load(R.drawable.img).into(holder.image);
        }


        // Xử lý logic cho huy hiệu trạng thái
        String status = listing.getStatus();
        if (status != null && !status.isEmpty()) {
            holder.statusBadge.setVisibility(View.VISIBLE);
            holder.statusBadge.setText(status.substring(0, 1).toUpperCase() + status.substring(1)); // Viết hoa chữ cái đầu

            switch (status.toLowerCase()) {
                case "active":
                    holder.statusBadge.setBackgroundResource(R.drawable.badge_background_active);
                    holder.statusBadge.setTextColor(ContextCompat.getColor(context, R.color.green_dark)); // Cần định nghĩa màu này
                    break;
                case "sold":
                    holder.statusBadge.setBackgroundResource(R.drawable.badge_background_sold);
                    holder.statusBadge.setTextColor(ContextCompat.getColor(context, R.color.grey_dark)); // Cần định nghĩa màu này
                    break;
                case "paused":
                    holder.statusBadge.setBackgroundResource(R.drawable.badge_background_paused);
                    holder.statusBadge.setTextColor(ContextCompat.getColor(context, R.color.yellow_dark)); // Cần định nghĩa màu này
                    break;
                default:
                    holder.statusBadge.setVisibility(View.GONE);
                    break;
            }
        } else {
            holder.statusBadge.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return listingList.size();
    }

    public static class ListingViewHolder extends RecyclerView.ViewHolder {
        ImageView image;
        TextView statusBadge, title, price, time;

        public ListingViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.image_listing);
            statusBadge = itemView.findViewById(R.id.text_status_badge);
            title = itemView.findViewById(R.id.text_listing_title);
            price = itemView.findViewById(R.id.text_listing_price);
            time = itemView.findViewById(R.id.text_listing_time);
        }
    }
}