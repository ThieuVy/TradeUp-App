package com.example.testapptradeup.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import com.example.testapptradeup.R;
import com.example.testapptradeup.viewmodels.PaymentViewModel;
import com.google.android.material.appbar.MaterialToolbar;
import com.stripe.android.PaymentConfiguration;
import com.stripe.android.paymentsheet.PaymentSheet;
import com.stripe.android.paymentsheet.PaymentSheetResult;

public class PaymentFragment extends Fragment {

    private PaymentViewModel viewModel;
    private NavController navController;
    private PaymentSheet paymentSheet;

    // Data from arguments
    private String listingId, sellerId;
    private float offerPrice;

    // UI
    private ProgressBar progressBar;
    private TextView statusText;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(PaymentViewModel.class);
        paymentSheet = new PaymentSheet(this, this::onPaymentSheetResult);

        if (getArguments() != null) {
            PaymentFragmentArgs args = PaymentFragmentArgs.fromBundle(getArguments());
            listingId = args.getListingId();
            sellerId = args.getSellerId();
            offerPrice = args.getOfferPrice();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_payment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(view);

        progressBar = view.findViewById(R.id.progress_bar);
        statusText = view.findViewById(R.id.status_text);
        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> navController.popBackStack());

        observeViewModel();

        if (listingId != null) {
            viewModel.startEscrowPayment(listingId, sellerId, offerPrice);
        } else {
            Toast.makeText(getContext(), "Lỗi: Thiếu thông tin thanh toán.", Toast.LENGTH_LONG).show();
            navController.popBackStack();
        }
    }

    private void observeViewModel() {
        viewModel.isLoading().observe(getViewLifecycleOwner(), isLoading -> progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE));

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                statusText.setText(error);
                Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
            }
        });

        viewModel.getPaymentKeys().observe(getViewLifecycleOwner(), keys -> {
            if (keys != null && getContext() != null) {
                // Thay thế bằng Publishable Key của bạn
                String stripePublishableKey = "pk_test_YOUR_PUBLISHABLE_KEY";
                PaymentConfiguration.init(requireContext(), stripePublishableKey);

                String customerId = keys.get("customerId");
                String ephemeralKey = keys.get("ephemeralKeySecret"); // Tên key từ function v1
                String clientSecret = keys.get("clientSecret");

                if (customerId != null && ephemeralKey != null && clientSecret != null) {
                    paymentSheet.presentWithPaymentIntent(clientSecret,
                            new PaymentSheet.Configuration.Builder("TradeUp Inc.")
                                    .customer(new PaymentSheet.CustomerConfiguration(customerId, ephemeralKey))
                                    .allowsDelayedPaymentMethods(true)
                                    .build()
                    );
                }
            }
        });
    }

    private void onPaymentSheetResult(final PaymentSheetResult paymentSheetResult) {
        if (paymentSheetResult instanceof PaymentSheetResult.Completed) {
            Toast.makeText(getContext(), "Thanh toán thành công! Vui lòng chờ người bán giao hàng.", Toast.LENGTH_LONG).show();
            // TODO: Cập nhật trạng thái tin đăng/offer thành "pending_confirmation"
            navController.popBackStack();
        } else if (paymentSheetResult instanceof PaymentSheetResult.Canceled) {
            Toast.makeText(getContext(), "Đã hủy thanh toán.", Toast.LENGTH_SHORT).show();
            navController.popBackStack();
        } else if (paymentSheetResult instanceof PaymentSheetResult.Failed) {
            Toast.makeText(getContext(), ((PaymentSheetResult.Failed) paymentSheetResult).getError().getMessage(), Toast.LENGTH_LONG).show();
            navController.popBackStack();
        }
    }
}