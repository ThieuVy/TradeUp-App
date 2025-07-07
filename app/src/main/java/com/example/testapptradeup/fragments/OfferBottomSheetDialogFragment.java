package com.example.testapptradeup.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.example.testapptradeup.R;
import com.example.testapptradeup.viewmodels.ProductDetailViewModel;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.textfield.TextInputEditText;

public class OfferBottomSheetDialogFragment extends BottomSheetDialogFragment {

    private ProductDetailViewModel viewModel;
    private TextInputEditText editOfferPrice, editOfferMessage;
    private Button btnSendOffer;
    private String listingId;
    private String sellerId;

    public static OfferBottomSheetDialogFragment newInstance(String listingId, String sellerId) {
        OfferBottomSheetDialogFragment fragment = new OfferBottomSheetDialogFragment();
        Bundle args = new Bundle();
        args.putString("listingId", listingId);
        args.putString("sellerId", sellerId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            listingId = getArguments().getString("listingId");
            sellerId = getArguments().getString("sellerId");
        }
        // Lấy ViewModel từ Fragment cha (ProductDetailFragment)
        viewModel = new ViewModelProvider(requireParentFragment()).get(ProductDetailViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_make_offer, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        editOfferPrice = view.findViewById(R.id.edit_offer_price);
        editOfferMessage = view.findViewById(R.id.edit_offer_message);
        btnSendOffer = view.findViewById(R.id.btn_send_offer);

        btnSendOffer.setOnClickListener(v -> sendOffer());
    }

    private void sendOffer() {
        String priceStr = editOfferPrice.getText().toString();
        if (priceStr.isEmpty()) {
            Toast.makeText(getContext(), "Vui lòng nhập giá bạn đề nghị", Toast.LENGTH_SHORT).show();
            return;
        }
        double price = Double.parseDouble(priceStr);
        String message = editOfferMessage.getText().toString().trim();

        viewModel.makeOffer(listingId, sellerId, price, message).observe(getViewLifecycleOwner(), success -> {
            if (success) {
                Toast.makeText(getContext(), "Gửi đề nghị thành công", Toast.LENGTH_SHORT).show();
                dismiss();
            } else {
                Toast.makeText(getContext(), "Gửi đề nghị thất bại", Toast.LENGTH_SHORT).show();
            }
        });
    }
}