package com.example.testapptradeup.adapters;

import android.content.Context;
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

public class PublicListingAdapter extends ListAdapter<Listing, PublicListingAdapter.ListingViewHolder> {

    // Bỏ Context và List, ListAdapter sẽ tự quản lý

    public PublicListingAdapter() {
        super(DIFF_CALLBACK);
    }

    @NonNull
    @Override
    public ListingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_public_listing, parent, false);
        return new ListingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ListingViewHolder holder, int position) {
        Listing listing = getItem(position); // Lấy item từ ListAdapter
        holder.bind(listing);
    }

    static class ListingViewHolder extends RecyclerView.ViewHolder {
        ImageView listingImage;
        TextView listingTitle, listingPrice, listingStatus;

        public ListingViewHolder(@NonNull View itemView) {
            super(itemView);
            listingImage = itemView.findViewById(R.id.listing_image);
            listingTitle = itemView.findViewById(R.id.listing_title);
            listingPrice = itemView.findViewById(R.id.listing_price);
            listingStatus = itemView.findViewById(R.id.listing_status);
        }

        void bind(final Listing listing) {
            Context context = itemView.getContext();
            listingTitle.setText(listing.getTitle());
            listingPrice.setText(listing.getFormattedPrice());

            if ("available".equalsIgnoreCase(listing.getStatus())) {
                listingStatus.setText(R.string.listing_status_available);
                listingStatus.setTextColor(context.getResources().getColor(R.color.success, null));
                listingStatus.setVisibility(View.VISIBLE);
            } else {
                listingStatus.setVisibility(View.GONE);
            }

            if (listing.getPrimaryImageUrl() != null && !listing.getPrimaryImageUrl().isEmpty()) {
                Glide.with(context)
                        .load(listing.getPrimaryImageUrl())
                        .placeholder(R.drawable.img_placeholder)
                        .error(R.drawable.img_placeholder)
                        .centerCrop()
                        .into(listingImage);
            } else {
                listingImage.setImageResource(R.drawable.img_placeholder);
            }
        }
    }

    // DiffUtil giúp RecyclerView cập nhật hiệu quả
    private static final DiffUtil.ItemCallback<Listing> DIFF_CALLBACK = new DiffUtil.ItemCallback<>() {
        @Override
        public boolean areItemsTheSame(@NonNull Listing oldItem, @NonNull Listing newItem) {
            return Objects.equals(oldItem.getId(), newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Listing oldItem, @NonNull Listing newItem) {
            // So sánh nội dung để biết có cần vẽ lại item không
            return oldItem.equals(newItem);
        }
    };
}