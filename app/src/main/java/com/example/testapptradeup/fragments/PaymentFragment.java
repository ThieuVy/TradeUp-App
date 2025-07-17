package com.example.testapptradeup.fragments;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import java.util.Objects;

public class PaymentFragment extends Fragment {

    private PaymentViewModel viewModel;
    private NavController navController;
    private PaymentSheet paymentSheet;

    // Arguments
    private String listingId, sellerId, offerId;
    private float offerPrice;

    // UI
    private ProgressBar progressBar;
    private TextView statusText;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(PaymentViewModel.class);
        paymentSheet = new PaymentSheet(this, this::onPaymentSheetResult);

        // Lấy arguments một cách an toàn
        if (getArguments() != null) {
            PaymentFragmentArgs args = PaymentFragmentArgs.fromBundle(getArguments());
            listingId = args.getListingId();
            sellerId = args.getSellerId();
            offerPrice = args.getOfferPrice();
            offerId = args.getOfferId();
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

        // Bắt đầu quá trình thanh toán ngay khi màn hình được tạo
        if (listingId != null && sellerId != null && offerId != null) {
            viewModel.startEscrowPayment(listingId, sellerId, offerPrice);
        } else {
            Toast.makeText(getContext(), "Lỗi: Thiếu thông tin thanh toán quan trọng.", Toast.LENGTH_LONG).show();
            navController.popBackStack();
        }
    }

    private void observeViewModel() {
        viewModel.isLoading().observe(getViewLifecycleOwner(), isLoading -> {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            statusText.setText(isLoading ? "Đang chuẩn bị thanh toán an toàn..." : "Sẵn sàng để thanh toán.");
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                statusText.setText(error);
                Toast.makeText(getContext(), "Lỗi: " + error, Toast.LENGTH_LONG).show();
                // Cho phép người dùng quay lại sau khi có lỗi
                new Handler(Looper.getMainLooper()).postDelayed(() -> navController.popBackStack(), 3000);
            }
        });

        // Lắng nghe khi các khóa từ Stripe đã sẵn sàng
        viewModel.getPaymentKeys().observe(getViewLifecycleOwner(), keys -> {
            if (keys != null && getContext() != null) {
                // QUAN TRỌNG: Thay thế bằng Publishable Key của bạn từ Stripe Dashboard
                String stripePublishableKey = "pk_test_51RhD48PPXaf7jnxkz9AGkIEOJFaO3lymdKq9kFR82MpO0F8WIWjsmGS9DCW7ixsMjnHMiypKb9Jm5ePclVm53PtU00t0ZU99U3";
                PaymentConfiguration.init(requireContext(), stripePublishableKey);

                String customerId = keys.get("customerId");
                String ephemeralKey = keys.get("ephemeralKeySecret");
                String clientSecret = keys.get("clientSecret");

                if (customerId != null && ephemeralKey != null && clientSecret != null) {
                    presentStripeSheet(clientSecret, customerId, ephemeralKey);
                } else {
                    viewModel.postErrorMessage("Lỗi cấu hình thanh toán. Thiếu khóa cần thiết.");
                }
            }
        });
    }

    private void presentStripeSheet(String clientSecret, String customerId, String ephemeralKey) {
        paymentSheet.presentWithPaymentIntent(clientSecret,
                new PaymentSheet.Configuration.Builder("TradeUp Inc.")
                        .customer(new PaymentSheet.CustomerConfiguration(customerId, ephemeralKey))
                        .allowsDelayedPaymentMethods(true)
                        .build()
        );
    }

    @SuppressLint("SetTextI18n")
    private void onPaymentSheetResult(final PaymentSheetResult paymentSheetResult) {
        if (paymentSheetResult instanceof PaymentSheetResult.Completed) {
            statusText.setText("Thanh toán thành công! Đang hoàn tất giao dịch...");
            progressBar.setVisibility(View.VISIBLE);

            // BẮT ĐẦU GỌI HÀM HOÀN TẤT GIAO DỊCH
            viewModel.finalizeTransaction(listingId, offerId).observe(getViewLifecycleOwner(), success -> {
                progressBar.setVisibility(View.GONE);
                if (Boolean.TRUE.equals(success)) {
                    Toast.makeText(getContext(), "Giao dịch đã được hoàn tất!", Toast.LENGTH_LONG).show();
                    // Điều hướng về màn hình chính, xóa các màn hình trung gian
                    navController.popBackStack(R.id.navigation_home, false);
                } else {
                    Toast.makeText(getContext(), "Lỗi khi cập nhật trạng thái giao dịch. Vui lòng liên hệ hỗ trợ.", Toast.LENGTH_LONG).show();
                }
            });

        } else if (paymentSheetResult instanceof PaymentSheetResult.Canceled) {
            Toast.makeText(getContext(), "Đã hủy thanh toán.", Toast.LENGTH_SHORT).show();
            navController.popBackStack();
        } else if (paymentSheetResult instanceof PaymentSheetResult.Failed) {
            String errorMessage = Objects.requireNonNull(((PaymentSheetResult.Failed) paymentSheetResult).getError().getMessage());
            Toast.makeText(getContext(), "Thanh toán thất bại: " + errorMessage, Toast.LENGTH_LONG).show();
            navController.popBackStack();
        }
    }
}