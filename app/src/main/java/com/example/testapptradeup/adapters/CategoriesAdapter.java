package com.example.testapptradeup.adapters;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.graphics.PorterDuff;
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

import com.example.testapptradeup.R;
import com.example.testapptradeup.models.Category;
import com.google.android.material.card.MaterialCardView;

public class CategoriesAdapter extends ListAdapter<Category, CategoriesAdapter.CategoryViewHolder> {

    public interface OnCategoryClickListener {
        void onCategoryClick(Category category);
    }

    private final OnCategoryClickListener listener;
    private String selectedCategoryId = null;

    public CategoriesAdapter(OnCategoryClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<Category> DIFF_CALLBACK = new DiffUtil.ItemCallback<>() {
        @Override
        public boolean areItemsTheSame(@NonNull Category oldItem, @NonNull Category newItem) {
            return oldItem.getId().equals(newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Category oldItem, @NonNull Category newItem) {
            return oldItem.equals(newItem) && oldItem.isSelected() == newItem.isSelected();
        }
    };

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_category_chip, parent, false);
        return new CategoryViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        Category category = getItem(position);
        holder.bind(category);
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setSelectedCategory(String categoryId) {
        this.selectedCategoryId = categoryId;
        notifyDataSetChanged();
    }

    class CategoryViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCardView cardView;
        private final ImageView iconImage; // Đổi thành ImageView
        private final TextView nameText;

        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.category_card);
            iconImage = itemView.findViewById(R.id.category_icon); // Ánh xạ ImageView
            nameText = itemView.findViewById(R.id.category_name);
        }

        public void bind(Category category) {
            // SỬA LỖI: Gọi đúng phương thức và dùng setImageResource
            iconImage.setImageResource(category.getIconResId());
            nameText.setText(category.getName());

            boolean isSelected = category.getId().equals(selectedCategoryId);

            // Lấy màu từ resources
            int selectedColor = Color.parseColor(category.getColor());
            int whiteColor = ContextCompat.getColor(itemView.getContext(), R.color.white);
            int grayColor = Color.parseColor("#666666");

            if (isSelected) {
                cardView.setCardBackgroundColor(selectedColor);
                cardView.setStrokeWidth(0); // Không cần stroke khi đã có nền
                nameText.setTextColor(whiteColor);
                iconImage.setColorFilter(whiteColor, PorterDuff.Mode.SRC_IN); // Đổi màu icon
            } else {
                cardView.setCardBackgroundColor(whiteColor);
                cardView.setStrokeColor(Color.parseColor("#E0E0E0"));
                cardView.setStrokeWidth(1);
                nameText.setTextColor(grayColor);
                iconImage.clearColorFilter(); // Bỏ filter màu của icon
            }

            cardView.setOnClickListener(v -> {
                if (listener != null) {
                    if (category.getId().equals(selectedCategoryId)) {
                        setSelectedCategory(null);
                        listener.onCategoryClick(null);
                    } else {
                        setSelectedCategory(category.getId());
                        listener.onCategoryClick(category);
                    }
                }
            });
        }
    }
}