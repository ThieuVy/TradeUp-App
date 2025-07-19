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

import com.example.testapptradeup.R;
import com.example.testapptradeup.models.StripeTransaction;

public class PaymentHistoryAdapter extends ListAdapter<StripeTransaction, PaymentHistoryAdapter.ViewHolder> {

    public PaymentHistoryAdapter() {
        super(DIFF_CALLBACK);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_payment_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        StripeTransaction transaction = getItem(position);
        holder.bind(transaction);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView icon;
        private final TextView description, date, amount, status; // <<< SỬA ĐỔI: Đổi tên biến cho rõ ràng

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.transaction_icon);
            // <<< SỬA ĐỔI 1: Ánh xạ đúng ID từ file XML >>>
            description = itemView.findViewById(R.id.transaction_description);
            date = itemView.findViewById(R.id.transaction_date);
            amount = itemView.findViewById(R.id.transaction_amount);
            status = itemView.findViewById(R.id.transaction_status);
        }

        @SuppressLint("SetTextI18n")
        void bind(StripeTransaction transaction) {
            Context context = itemView.getContext();
            description.setText(transaction.getDescription());
            date.setText(transaction.getFormattedDate());
            amount.setText("- " + transaction.getFormattedAmount());

            switch (transaction.getStatus()) {
                case "succeeded":
                    status.setText("Thành công");
                    status.setTextColor(ContextCompat.getColor(context, R.color.success));
                    icon.setImageResource(R.drawable.ic_check_circle);
                    break;
                case "canceled":
                    status.setText("Đã hủy");
                    status.setTextColor(ContextCompat.getColor(context, R.color.text_secondary));
                    icon.setImageResource(R.drawable.ic_cancel); // Icon dấu X
                    amount.setTextColor(ContextCompat.getColor(context, R.color.text_secondary));
                    break;
                default:
                    status.setText("Đang chờ");
                    status.setTextColor(ContextCompat.getColor(context, R.color.warning));
                    icon.setImageResource(R.drawable.ic_time); // Icon đồng hồ
                    amount.setTextColor(ContextCompat.getColor(context, R.color.text_secondary));
                    break;
            }
        }
    }

    private static final DiffUtil.ItemCallback<StripeTransaction> DIFF_CALLBACK = new DiffUtil.ItemCallback<>() {
        @Override
        public boolean areItemsTheSame(@NonNull StripeTransaction oldItem, @NonNull StripeTransaction newItem) {
            return oldItem.getId().equals(newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull StripeTransaction oldItem, @NonNull StripeTransaction newItem) {
            // So sánh thêm các trường khác nếu cần để cập nhật UI chính xác
            return oldItem.getStatus().equals(newItem.getStatus()) &&
                    oldItem.getDescription().equals(newItem.getDescription());
        }
    };
}