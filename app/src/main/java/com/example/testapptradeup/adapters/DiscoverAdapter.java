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
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.testapptradeup.R;
import com.example.testapptradeup.models.Listing;

import java.util.Objects;

public class DiscoverAdapter extends ListAdapter<Listing, DiscoverAdapter.ViewHolder> {

    private final OnProductClickListener listener;

    public interface OnProductClickListener {
        void onProductClick(Listing listing);
    }

    public DiscoverAdapter(OnProductClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_product_discover, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Listing listing = getItem(position);
        holder.bind(listing, listener);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView userAvatar, postImage;
        TextView userName, postTimestamp, postTitle, postPrice, postLocation;
        Button followButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            userAvatar = itemView.findViewById(R.id.user_avatar);
            postImage = itemView.findViewById(R.id.post_image);
            userName = itemView.findViewById(R.id.user_name);
            postTimestamp = itemView.findViewById(R.id.post_timestamp);
            postTitle = itemView.findViewById(R.id.post_title);
            postPrice = itemView.findViewById(R.id.post_price);
            postLocation = itemView.findViewById(R.id.post_location);
            followButton = itemView.findViewById(R.id.follow_button);
        }

        @SuppressLint("SetTextI18n")
        public void bind(final Listing listing, final OnProductClickListener listener) {
            Context context = itemView.getContext();
            userName.setText(listing.getSellerName());
            postTitle.setText(listing.getTitle());
            postPrice.setText(listing.getFormattedPrice());
            postLocation.setText(listing.getLocation());

            if (listing.getTimePosted() != null) {
                postTimestamp.setText(android.text.format.DateUtils.getRelativeTimeSpanString(listing.getTimePosted().getTime()));
            } else {
                postTimestamp.setText("");
            }

            Glide.with(context)
                    .load(listing.getPrimaryImageUrl())
                    .placeholder(R.drawable.img_placeholder)
                    .error(R.drawable.img_placeholder)
                    .centerCrop()
                    .into(postImage);

            // TODO: Cần lấy URL avatar của người bán và hiển thị
            Glide.with(context)
                    .load(R.drawable.ic_profile_placeholder) // Ảnh đại diện mặc định
                    .circleCrop()
                    .into(userAvatar);

            itemView.setOnClickListener(v -> listener.onProductClick(listing));
            followButton.setOnClickListener(v -> Toast.makeText(context, "Chức năng theo dõi đang phát triển", Toast.LENGTH_SHORT).show());
        }
    }

    private static final DiffUtil.ItemCallback<Listing> DIFF_CALLBACK = new DiffUtil.ItemCallback<>() {
        @Override
        public boolean areItemsTheSame(@NonNull Listing oldItem, @NonNull Listing newItem) {
            return Objects.equals(oldItem.getId(), newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Listing oldItem, @NonNull Listing newItem) {
            return oldItem.equals(newItem);
        }
    };
}