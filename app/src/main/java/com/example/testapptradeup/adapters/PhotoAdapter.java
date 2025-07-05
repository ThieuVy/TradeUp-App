package com.example.testapptradeup.adapters;

import android.annotation.SuppressLint;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.testapptradeup.R;

import java.util.ArrayList;
import java.util.List;

public class PhotoAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int VIEW_TYPE_IMAGE = 1;
    private static final int VIEW_TYPE_ADD = 2;
    private static final int MAX_IMAGES = 10;
    private List<Uri> imageUris = new ArrayList<>();
    private final Runnable onAddPhotoClick;
    private final OnRemovePhotoListener onRemovePhotoListener;

    public interface OnRemovePhotoListener { void onRemove(Uri uri); }

    public PhotoAdapter(Runnable onAddPhotoClick, OnRemovePhotoListener onRemovePhotoListener) {
        this.onAddPhotoClick = onAddPhotoClick;
        this.onRemovePhotoListener = onRemovePhotoListener;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setImageUris(List<Uri> uris) {
        this.imageUris = uris;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        if (position < imageUris.size()) {
            return VIEW_TYPE_IMAGE;
        } else {
            return VIEW_TYPE_ADD;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_TYPE_IMAGE) {
            View view = inflater.inflate(R.layout.item_photo_thumbnail, parent, false);
            return new ImageViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_add_photo, parent, false);
            return new AddViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder.getItemViewType() == VIEW_TYPE_IMAGE) {
            Uri uri = imageUris.get(position);
            // Chỉ cần bind Uri, không cần state
            ((ImageViewHolder) holder).bind(uri);
        }
    }

    @Override
    public int getItemCount() {
        if (imageUris.size() < MAX_IMAGES) {
            return imageUris.size() + 1;
        }
        return imageUris.size();
    }

    class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView thumbnailImage, removeButton;
        // Xóa các View không còn dùng đến
        // ImageView errorIcon;
        // ProgressBar loadingIndicator;

        ImageViewHolder(View itemView) {
            super(itemView);
            thumbnailImage = itemView.findViewById(R.id.thumbnail_image);
            removeButton = itemView.findViewById(R.id.thumbnail_remove_button);
            // Xóa ánh xạ không còn dùng đến
            // errorIcon = itemView.findViewById(R.id.thumbnail_error_icon);
            // loadingIndicator = itemView.findViewById(R.id.thumbnail_loading_indicator);
        }

        // Đơn giản hóa hàm bind
        void bind(Uri uri) {
            Glide.with(itemView.getContext()).load(uri).centerCrop().into(thumbnailImage);

            removeButton.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                if(position != RecyclerView.NO_POSITION) {
                    onRemovePhotoListener.onRemove(imageUris.get(position));
                }
            });

            // Xóa toàn bộ logic xử lý state
            thumbnailImage.setAlpha(1.0f);
            // loadingIndicator.setVisibility(View.GONE);
            // errorIcon.setVisibility(View.GONE);
        }
    }

    class AddViewHolder extends RecyclerView.ViewHolder {
        AddViewHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(v -> onAddPhotoClick.run());
        }
    }
}