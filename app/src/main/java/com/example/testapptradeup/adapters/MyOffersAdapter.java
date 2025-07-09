package com.example.testapptradeup.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.testapptradeup.R;
import com.example.testapptradeup.models.OfferWithListing;

import java.text.NumberFormat;
import java.util.Locale;

public class MyOffersAdapter extends ListAdapter<OfferWithListing, MyOffersAdapter.MyOfferViewHolder> {

    public MyOffersAdapter() {
        super(DIFF_CALLBACK);
    }

    @NonNull
    @Override
    public MyOfferViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_my_offer, parent, false);
        return new MyOfferViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyOfferViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    static class MyOfferViewHolder extends RecyclerView.ViewHolder {
        ImageView listingImage;
        TextView listingTitle, offerPrice, offerStatus;

        public MyOfferViewHolder(@NonNull View itemView) {
            super(itemView);
            listingImage = itemView.findViewById(R.id.listing_image);
            listingTitle = itemView.findViewById(R.id.listing_title);
            offerPrice = itemView.findViewById(R.id.offer_price);
            offerStatus = itemView.findViewById(R.id.offer_status);
        }

        @SuppressLint("SetTextI18n")
        public void bind(OfferWithListing item) {
            Context context = itemView.getContext();

            // Lấy thông tin từ Listing
            if (item.getListing() != null) {
                listingTitle.setText(item.getListing().getTitle());
                Glide.with(context)
                        .load(item.getListing().getPrimaryImageUrl())
                        .placeholder(R.drawable.img_placeholder)
                        .error(R.drawable.img_placeholder)
                        .into(listingImage);
            }

            // Lấy thông tin từ Offer
            if (item.getOffer() != null) {
                NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
                offerPrice.setText("Đề nghị: " + format.format(item.getOffer().getOfferPrice()));

                // Cập nhật trạng thái
                switch (item.getOffer().getStatus()) {
                    case "accepted":
                        offerStatus.setText("Đã chấp nhận");
                        offerStatus.setTextColor(ContextCompat.getColor(context, R.color.success));
                        break;
                    case "rejected":
                        offerStatus.setText("Đã từ chối");
                        offerStatus.setTextColor(ContextCompat.getColor(context, R.color.red_error));
                        break;
                    case "pending":
                    default:
                        offerStatus.setText("Đang chờ");
                        offerStatus.setTextColor(ContextCompat.getColor(context, R.color.warning));
                        break;
                }
            }
        }
    }

    private static final DiffUtil.ItemCallback<OfferWithListing> DIFF_CALLBACK = new DiffUtil.ItemCallback<>() {
        @Override
        public boolean areItemsTheSame(@NonNull OfferWithListing oldItem, @NonNull OfferWithListing newItem) {
            return oldItem.getOffer().getId().equals(newItem.getOffer().getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull OfferWithListing oldItem, @NonNull OfferWithListing newItem) {
            return oldItem.getOffer().getStatus().equals(newItem.getOffer().getStatus());
        }
    };
}