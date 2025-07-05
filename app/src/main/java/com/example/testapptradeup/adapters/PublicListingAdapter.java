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

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class PublicListingAdapter extends RecyclerView.Adapter<PublicListingAdapter.ListingViewHolder> {

    private final Context context;
    private final List<Listing> listingList;

    public PublicListingAdapter(Context context, List<Listing> listingList) {
        this.context = context;
        this.listingList = listingList;
    }

    @NonNull
    @Override
    public ListingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_public_listing, parent, false);
        return new ListingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ListingViewHolder holder, int position) {
        Listing listing = listingList.get(position);
        holder.bind(listing);
    }

    @Override
    public int getItemCount() {
        return listingList.size();
    }

    class ListingViewHolder extends RecyclerView.ViewHolder {
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
            listingTitle.setText(listing.getTitle());

            // Định dạng giá tiền theo đơn vị VNĐ
            NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
            listingPrice.setText(format.format(listing.getPrice()));

            // Hiển thị trạng thái (giả sử có trường status trong model Listing)
            if ("active".equalsIgnoreCase(listing.getStatus())) {
                listingStatus.setText(R.string.listing_status_available);
                listingStatus.setTextColor(context.getResources().getColor(R.color.success));
            } else {
                listingStatus.setText(listing.getStatus());
            }

            // Tải ảnh bằng Glide
            if (listing.getImageUrls() != null && !listing.getImageUrls().isEmpty()) {
                Glide.with(context)
                        .load(listing.getImageUrls().get(0)) // Lấy ảnh đầu tiên
                        .placeholder(R.drawable.img)
                        .error(R.drawable.img)
                        .centerCrop()
                        .into(listingImage);
            }
        }
    }
    @SuppressLint("NotifyDataSetChanged")
    public void updateData(List<Listing> newListings) {
        listingList.clear();
        listingList.addAll(newListings);
        notifyDataSetChanged();
    }
}