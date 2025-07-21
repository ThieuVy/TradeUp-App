package com.example.testapptradeup.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.testapptradeup.R;
import com.example.testapptradeup.models.Listing;

import java.util.Objects;

public class ManageListingsAdapter extends ListAdapter<Listing, ManageListingsAdapter.ListingViewHolder> {

    public interface OnListingInteractionListener {
        void onViewDetailsClick(Listing listing);
        void onEditClick(Listing listing);
        void onDeleteClick(Listing listing);
        void onViewOffersClick(Listing listing);
    }

    private final OnListingInteractionListener listener;

    public ManageListingsAdapter(@NonNull OnListingInteractionListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ListingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_post_management, parent, false);
        return new ListingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ListingViewHolder holder, int position) {
        Listing listing = getItem(position);
        holder.bind(listing, listener); // Truyền cả listener vào hàm bind
    }

    // Lớp ViewHolder bây giờ không cần 'static' nữa
    public static class ListingViewHolder extends RecyclerView.ViewHolder {
        ImageView imgPost;
        TextView txtTitle, txtPrice, txtPostedTime, txtStatus, txtViews, txtOffers, txtChats;
        Button btnEdit, btnDelete, btnViewOffers;
        View itemView;

        public ListingViewHolder(@NonNull View itemView) {
            super(itemView);
            this.itemView = itemView;
            imgPost = itemView.findViewById(R.id.img_post);
            txtTitle = itemView.findViewById(R.id.txt_title);
            txtPrice = itemView.findViewById(R.id.txt_price);
            txtPostedTime = itemView.findViewById(R.id.txt_posted_time);
            txtStatus = itemView.findViewById(R.id.txt_status);
            txtViews = itemView.findViewById(R.id.txt_views);
            txtOffers = itemView.findViewById(R.id.txt_offers);
            btnEdit = itemView.findViewById(R.id.btn_edit);
            btnDelete = itemView.findViewById(R.id.btn_delete);
            btnViewOffers = itemView.findViewById(R.id.btn_view_offers);
            txtChats = itemView.findViewById(R.id.txt_chats);
        }

        @SuppressLint({"SetTextI18n", "DefaultLocale"})
        public void bind(final Listing listing, final OnListingInteractionListener listener) {
            Context context = itemView.getContext();

            txtTitle.setText(listing.getTitle());
            txtPrice.setText(listing.getFormattedPrice());
            txtViews.setText(String.valueOf(listing.getViews()));
            txtOffers.setText(String.format("%d đề nghị", listing.getOffersCount()));
            txtChats.setText(String.format("%d tin nhắn", listing.getChatCount()));

            if (listing.getTimePosted() != null) {
                txtPostedTime.setText(android.text.format.DateUtils.getRelativeTimeSpanString(listing.getTimePosted().getTime()));
            } else {
                txtPostedTime.setText("N/A");
            }

            if (listing.getPrimaryImageUrl() != null && !listing.getPrimaryImageUrl().isEmpty()) {
                Glide.with(context).load(listing.getPrimaryImageUrl()).placeholder(R.drawable.img_placeholder).error(R.drawable.img_placeholder).centerCrop().into(imgPost);
            } else {
                imgPost.setImageResource(R.drawable.img_placeholder);
            }
            updateStatusUI(listing.getStatus(), context);

            // SỬA LỖI: Gán listener ở đây, nơi có thể truy cập `listing` một cách an toàn
            itemView.setOnClickListener(v -> listener.onViewDetailsClick(listing));
            btnEdit.setOnClickListener(v -> listener.onEditClick(listing));
            btnDelete.setOnClickListener(v -> listener.onDeleteClick(listing));
            btnViewOffers.setOnClickListener(v -> listener.onViewOffersClick(listing));
        }

        private void updateStatusUI(String status, Context context) {
            if (txtStatus == null) return;
            String safeStatus = status != null ? status : "";
            txtStatus.setText(getStatusDisplayName(safeStatus));
            int statusColor;
            switch (safeStatus) {
                case "available":
                case "pending_payment":
                    statusColor = ContextCompat.getColor(context, R.color.success);
                    break;
                case "paused":
                    statusColor = ContextCompat.getColor(context, R.color.warning);
                    break;
                case "sold":
                    statusColor = ContextCompat.getColor(context, R.color.red_error);
                    break;
                default:
                    statusColor = ContextCompat.getColor(context, R.color.text_secondary);
                    break;
            }
            txtStatus.setTextColor(statusColor);
        }

        private String getStatusDisplayName(String status) {
            switch (status) {
                case "available": return "Đang hiển thị";
                case "pending_payment": return "Chờ thanh toán";
                case "paused": return "Tạm dừng";
                case "sold": return "Đã bán";
                default: return "Không rõ";
            }
        }
    }

    private static final DiffUtil.ItemCallback<Listing> DIFF_CALLBACK = new DiffUtil.ItemCallback<>() {
        @Override
        public boolean areItemsTheSame(@NonNull Listing oldItem, @NonNull Listing newItem) {
            return Objects.equals(oldItem.getId(), newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Listing oldItem, @NonNull Listing newItem) {
            return Objects.equals(oldItem, newItem);
        }
    };
}