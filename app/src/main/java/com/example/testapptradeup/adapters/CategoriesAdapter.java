package com.example.testapptradeup.adapters;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.testapptradeup.models.Category;
import com.google.android.material.card.MaterialCardView;
import com.example.testapptradeup.R;

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

    private static final DiffUtil.ItemCallback<Category> DIFF_CALLBACK = new DiffUtil.ItemCallback<Category>() {
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

    public void setSelectedCategory(String categoryId) {
        this.selectedCategoryId = categoryId;
        notifyDataSetChanged();
    }

    class CategoryViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCardView cardView;
        private final TextView iconText;
        private final TextView nameText;

        @SuppressLint("WrongViewCast")
        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.category_card);
            iconText = itemView.findViewById(R.id.category_icon);
            nameText = itemView.findViewById(R.id.category_name);
        }

        public void bind(Category category) {
            iconText.setText(category.getIcon());
            nameText.setText(category.getName());

            // Kiểm tra trạng thái được chọn
            boolean isSelected = category.getId().equals(selectedCategoryId);

            if (isSelected) {
                // Trạng thái được chọn
                cardView.setCardBackgroundColor(Color.parseColor(category.getColor()));
                cardView.setStrokeColor(Color.parseColor(category.getColor()));
                cardView.setStrokeWidth(3);
                nameText.setTextColor(Color.WHITE);
                iconText.setTextColor(Color.WHITE);
            } else {
                // Trạng thái bình thường
                cardView.setCardBackgroundColor(Color.WHITE);
                cardView.setStrokeColor(Color.parseColor("#E0E0E0"));
                cardView.setStrokeWidth(1);
                nameText.setTextColor(Color.parseColor("#666666"));
                iconText.setTextColor(Color.parseColor("#666666"));
            }

            // Xử lý click
            cardView.setOnClickListener(v -> {
                if (listener != null) {
                    // Nếu click vào category đã chọn thì bỏ chọn
                    if (category.getId().equals(selectedCategoryId)) {
                        setSelectedCategory(null);
                        listener.onCategoryClick(null); // Hiển thị tất cả
                    } else {
                        setSelectedCategory(category.getId());
                        listener.onCategoryClick(category);
                    }
                }
            });

            // Hiệu ứng ripple
            cardView.setClickable(true);
            cardView.setFocusable(true);
        }
    }
}