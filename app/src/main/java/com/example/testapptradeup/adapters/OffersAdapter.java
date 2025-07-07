package com.example.testapptradeup.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.testapptradeup.R;
import com.example.testapptradeup.models.Offer;
import java.text.NumberFormat;
import java.util.Locale;
import de.hdodenhof.circleimageview.CircleImageView;

public class OffersAdapter extends ListAdapter<Offer, OffersAdapter.OfferViewHolder> {

    private final OnOfferActionListener listener;

    public interface OnOfferActionListener {
        void onAccept(Offer offer);
        void onReject(Offer offer);
        void onCounter(Offer offer);
        void onChat(Offer offer);
    }

    public OffersAdapter(@NonNull OnOfferActionListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    @NonNull
    @Override
    public OfferViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_offer, parent, false);
        return new OfferViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OfferViewHolder holder, int position) {
        Offer offer = getItem(position);
        holder.bind(offer, listener);
    }

    static class OfferViewHolder extends RecyclerView.ViewHolder {
        CircleImageView buyerAvatar;
        TextView buyerName, offerPrice, offerMessage, offerStatus;
        Button btnAccept, btnReject, btnCounter, btnChat;
        View actionContainer;

        public OfferViewHolder(@NonNull View itemView) {
            super(itemView);
            buyerAvatar = itemView.findViewById(R.id.buyer_avatar);
            buyerName = itemView.findViewById(R.id.buyer_name);
            offerPrice = itemView.findViewById(R.id.text_offer_price);
            offerMessage = itemView.findViewById(R.id.text_offer_message);
            offerStatus = itemView.findViewById(R.id.text_offer_status);
            btnAccept = itemView.findViewById(R.id.btn_accept);
            btnReject = itemView.findViewById(R.id.btn_reject);
            btnCounter = itemView.findViewById(R.id.btn_counter);
            btnChat = itemView.findViewById(R.id.btn_chat_with_buyer);
            actionContainer = itemView.findViewById(R.id.offer_action_container);
        }

        void bind(final Offer offer, final OnOfferActionListener listener) {
            buyerName.setText(offer.getBuyerName());
            NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
            offerPrice.setText(format.format(offer.getOfferPrice()));

            if (offer.getMessage() != null && !offer.getMessage().isEmpty()) {
                offerMessage.setText("\"" + offer.getMessage() + "\"");
                offerMessage.setVisibility(View.VISIBLE);
            } else {
                offerMessage.setVisibility(View.GONE);
            }

            Glide.with(itemView.getContext())
                    .load(offer.getBuyerAvatarUrl())
                    .placeholder(R.drawable.ic_profile_placeholder)
                    .into(buyerAvatar);

            // Cập nhật UI dựa trên trạng thái của offer
            switch (offer.getStatus()) {
                case "accepted":
                    offerStatus.setText("Đã chấp nhận");
                    offerStatus.setVisibility(View.VISIBLE);
                    actionContainer.setVisibility(View.GONE);
                    break;
                case "rejected":
                    offerStatus.setText("Đã từ chối");
                    offerStatus.setVisibility(View.VISIBLE);
                    actionContainer.setVisibility(View.GONE);
                    break;
                case "pending":
                default:
                    offerStatus.setVisibility(View.GONE);
                    actionContainer.setVisibility(View.VISIBLE);
                    break;
            }

            // Gán sự kiện cho các nút
            btnAccept.setOnClickListener(v -> listener.onAccept(offer));
            btnReject.setOnClickListener(v -> listener.onReject(offer));
            btnCounter.setOnClickListener(v -> listener.onCounter(offer));
            btnChat.setOnClickListener(v -> listener.onChat(offer));
        }
    }

    private static final DiffUtil.ItemCallback<Offer> DIFF_CALLBACK = new DiffUtil.ItemCallback<Offer>() {
        @Override
        public boolean areItemsTheSame(@NonNull Offer oldItem, @NonNull Offer newItem) {
            return oldItem.getId().equals(newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Offer oldItem, @NonNull Offer newItem) {
            return oldItem.getStatus().equals(newItem.getStatus());
        }
    };
}