package com.example.testapptradeup.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

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

    private final OnItemInteractionListener viewListener;
    private final OnItemInteractionListener editListener;
    private final OnItemInteractionListener deleteListener;

    public interface OnItemInteractionListener {
        void onItemClick(Listing listing);
    }

    public ManageListingsAdapter(OnItemInteractionListener viewListener, OnItemInteractionListener editListener, OnItemInteractionListener deleteListener) {
        super(DIFF_CALLBACK);
        this.viewListener = viewListener;
        this.editListener = editListener;
        this.deleteListener = deleteListener;
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
        holder.bind(listing, viewListener, editListener, deleteListener);
    }

    static class ListingViewHolder extends RecyclerView.ViewHolder {
        ImageView imgPost;
        TextView txtTitle, txtPrice, txtPostedTime, txtStatus, txtViews, txtOffers;
        Button btnEdit, btnDelete;
        ImageView btnMoreOptions;

        public ListingViewHolder(@NonNull View itemView) {
            super(itemView);
            imgPost = itemView.findViewById(R.id.img_post);
            txtTitle = itemView.findViewById(R.id.txt_title);
            txtPrice = itemView.findViewById(R.id.txt_price);
            txtPostedTime = itemView.findViewById(R.id.txt_posted_time);
            txtStatus = itemView.findViewById(R.id.txt_status);
            // SỬA LỖI: Xóa txtLocation vì nó không có trong layout mới
            // txtLocation = itemView.findViewById(R.id.txt_location);
            txtViews = itemView.findViewById(R.id.txt_views);
            txtOffers = itemView.findViewById(R.id.txt_offers);
            btnEdit = itemView.findViewById(R.id.btn_edit);
            btnDelete = itemView.findViewById(R.id.btn_delete);
            btnMoreOptions = itemView.findViewById(R.id.btn_more_options);
        }

        @SuppressLint({"SetTextI18n", "DefaultLocale"})
        public void bind(final Listing listing, final OnItemInteractionListener viewListener, final OnItemInteractionListener editListener, final OnItemInteractionListener deleteListener) {
            Context context = itemView.getContext();

            txtTitle.setText(listing.getTitle());
            txtPrice.setText(listing.getFormattedPrice());

            // SỬA LỖI: Bind dữ liệu views và offers
            txtViews.setText(String.valueOf(listing.getViews()));
            txtOffers.setText(String.format("%d đề nghị", listing.getOffersCount()));

            if (listing.getTimePosted() != null) {
                txtPostedTime.setText(android.text.format.DateUtils.getRelativeTimeSpanString(listing.getTimePosted().getTime()));
            } else {
                txtPostedTime.setText("N/A");
            }

            if (listing.getPrimaryImageUrl() != null && !listing.getPrimaryImageUrl().isEmpty()) {
                Glide.with(context)
                        .load(listing.getPrimaryImageUrl())
                        .placeholder(R.drawable.img_placeholder)
                        .error(R.drawable.img_placeholder)
                        .centerCrop()
                        .into(imgPost);
            } else {
                imgPost.setImageResource(R.drawable.img_placeholder);
            }

            updateStatusUI(listing.getStatus(), context);

            itemView.setOnClickListener(v -> viewListener.onItemClick(listing));
            btnEdit.setOnClickListener(v -> editListener.onItemClick(listing));
            btnDelete.setOnClickListener(v -> deleteListener.onItemClick(listing));
            btnMoreOptions.setOnClickListener(v -> Toast.makeText(context, "Thêm tùy chọn...", Toast.LENGTH_SHORT).show());
        }

        private void updateStatusUI(String status, Context context) {
            if (txtStatus == null) return;
            String safeStatus = status != null ? status : "";
            txtStatus.setText(getStatusDisplayName(safeStatus));
            int statusColor;
            switch (safeStatus) {
                case "available":
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
                case "paused": return "Tạm dừng";
                case "sold": return "Đã bán";
                default: return "Không rõ";
            }
        }
    }

    private static final DiffUtil.ItemCallback<Listing> DIFF_CALLBACK = new DiffUtil.ItemCallback<>() {
        @Override
        public boolean areItemsTheSame(@NonNull Listing oldItem, @NonNull Listing newItem) {
            return oldItem.getId().equals(newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Listing oldItem, @NonNull Listing newItem) {
            return Objects.equals(oldItem, newItem);
        }
    };
}