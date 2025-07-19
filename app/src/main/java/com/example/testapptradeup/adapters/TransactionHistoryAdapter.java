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
import com.example.testapptradeup.models.Transaction;
import com.google.firebase.auth.FirebaseAuth;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Objects;

public class TransactionHistoryAdapter extends ListAdapter<Transaction, TransactionHistoryAdapter.TransactionViewHolder> {

    private final String currentUserId;
    private final OnReviewButtonClickListener listener;

    public interface OnReviewButtonClickListener {
        void onReviewClick(Transaction transaction);
    }

    public TransactionHistoryAdapter(@NonNull OnReviewButtonClickListener listener) {
        super(DIFF_CALLBACK);
        this.currentUserId = FirebaseAuth.getInstance().getUid();
        this.listener = listener;
    }

    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_transaction_history, parent, false);
        return new TransactionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TransactionViewHolder holder, int position) {
        Transaction transaction = getItem(position);
        holder.bind(transaction, currentUserId, listener);
    }

    static class TransactionViewHolder extends RecyclerView.ViewHolder {
        ImageView listingImage;
        TextView listingTitle, partnerLabel, transactionPrice;
        Button btnReview;

        public TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            listingImage = itemView.findViewById(R.id.listing_image);
            listingTitle = itemView.findViewById(R.id.listing_title);
            partnerLabel = itemView.findViewById(R.id.partner_label);
            transactionPrice = itemView.findViewById(R.id.transaction_price);
            btnReview = itemView.findViewById(R.id.btn_review);
        }

        @SuppressLint("SetTextI18n")
        public void bind(final Transaction transaction, String currentUserId, final OnReviewButtonClickListener listener) {
            Context context = itemView.getContext();
            listingTitle.setText(transaction.getListingTitle());

            NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
            transactionPrice.setText(currencyFormatter.format(transaction.getFinalPrice()));

            Glide.with(context)
                    .load(transaction.getListingImageUrl())
                    .placeholder(R.drawable.img_placeholder)
                    .error(R.drawable.img_placeholder)
                    .centerCrop()
                    .into(listingImage);

            if (currentUserId == null) return;

            boolean amIBuyer = currentUserId.equals(transaction.getBuyerId());

            if (amIBuyer) {
                partnerLabel.setText("Đã mua từ: " + transaction.getSellerName());
                transactionPrice.setTextColor(ContextCompat.getColor(context, R.color.red_error));
                if (!transaction.isBuyerReviewed()) {
                    btnReview.setVisibility(View.VISIBLE);
                    btnReview.setText("Đánh giá người bán");
                    btnReview.setOnClickListener(v -> listener.onReviewClick(transaction));
                } else {
                    btnReview.setVisibility(View.GONE);
                }
            } else {
                partnerLabel.setText("Đã bán cho: " + transaction.getBuyerName());
                transactionPrice.setTextColor(ContextCompat.getColor(context, R.color.success));
                if (!transaction.isSellerReviewed()) {
                    btnReview.setVisibility(View.VISIBLE);
                    btnReview.setText("Đánh giá người mua");
                    btnReview.setOnClickListener(v -> listener.onReviewClick(transaction));
                } else {
                    btnReview.setVisibility(View.GONE);
                }
            }
        }
    }

    private static final DiffUtil.ItemCallback<Transaction> DIFF_CALLBACK = new DiffUtil.ItemCallback<>() {
        @Override
        public boolean areItemsTheSame(@NonNull Transaction oldItem, @NonNull Transaction newItem) {
            return Objects.equals(oldItem.getId(), newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Transaction oldItem, @NonNull Transaction newItem) {
            return oldItem.isBuyerReviewed() == newItem.isBuyerReviewed() &&
                    oldItem.isSellerReviewed() == newItem.isSellerReviewed();
        }
    };
}