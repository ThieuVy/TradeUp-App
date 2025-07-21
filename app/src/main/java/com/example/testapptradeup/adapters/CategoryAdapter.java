package com.example.testapptradeup.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.testapptradeup.R;
import com.example.testapptradeup.models.Category;

public class CategoryAdapter extends ListAdapter<Category, CategoryAdapter.CategoryViewHolder> {

    private final OnCategoryClickListener listener;

    public interface OnCategoryClickListener {
        void onCategoryClick(Category category);
    }

    public CategoryAdapter(OnCategoryClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Sửa lại để inflate layout mới
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.categories_section_layout, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        Category category = getItem(position);
        holder.bind(category, listener);
    }

    static class CategoryViewHolder extends RecyclerView.ViewHolder {
        ImageView categoryIcon;
        TextView categoryName;

        CategoryViewHolder(View itemView) {
            super(itemView);
            // SỬA LỖI: Ánh xạ lại đúng ID từ layout `categories_section_layout.xml`
            // Layout này có nhiều ImageView và TextView, bạn cần đảm bảo ánh xạ đúng.
            // Ví dụ, nếu bạn đang bind cho danh mục "Electronics":
            LinearLayout electronicsLayout = itemView.findViewById(R.id.category_electronics);
            categoryIcon = electronicsLayout.findViewById(R.id.img_home); // Giả sử ID icon là đây
            categoryName = electronicsLayout.findViewById(R.id.tv_home); // Giả sử ID text là đây
        }

        void bind(final Category category, final OnCategoryClickListener listener) {
            categoryName.setText(category.getName());

            // === SỬA LỖI GỌI PHƯƠNG THỨC ===
            // Gọi đúng phương thức getIconResId()
            Glide.with(itemView.getContext())
                    .load(category.getIconResId()) // <<< SỬA ĐỔI QUAN TRỌNG
                    .placeholder(R.drawable.ic_category_placeholder)
                    .error(R.drawable.ic_category_placeholder)
                    .into(categoryIcon);

            itemView.setOnClickListener(v -> listener.onCategoryClick(category));
        }
    }

    private static final DiffUtil.ItemCallback<Category> DIFF_CALLBACK = new DiffUtil.ItemCallback<>() {
        @Override
        public boolean areItemsTheSame(@NonNull Category oldItem, @NonNull Category newItem) {
            return oldItem.getId().equals(newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Category oldItem, @NonNull Category newItem) {
            return oldItem.equals(newItem);
        }
    };
}