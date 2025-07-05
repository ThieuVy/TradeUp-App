package com.example.testapptradeup.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.testapptradeup.R;
import com.example.testapptradeup.viewmodels.PaymentSettingsViewModel;
import com.stripe.android.paymentsheet.PaymentSheet;
import com.stripe.android.paymentsheet.PaymentSheetResult;

public class PaymentSettingsFragment extends Fragment {

    private PaymentSettingsViewModel viewModel;
    private PaymentSheet paymentSheet;

    private Button managePaymentsButton;
    private ProgressBar progressBar;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(PaymentSettingsViewModel.class);

        // Khởi tạo PaymentSheet, truyền this (fragment) và callback
        paymentSheet = new PaymentSheet(this, this::onPaymentSheetResult);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_payment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        managePaymentsButton = view.findViewById(R.id.btn_manage_payments);
        progressBar = view.findViewById(R.id.progress_bar);

        managePaymentsButton.setOnClickListener(v -> {
            // Yêu cầu ViewModel lấy các khóa từ backend
            viewModel.fetchStripeKeys();
        });

        observeViewModel();
    }

    private void observeViewModel() {
        viewModel.isLoading().observe(getViewLifecycleOwner(), isLoading -> {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            managePaymentsButton.setEnabled(!isLoading);
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
            }
        });

        viewModel.getStripeKeys().observe(getViewLifecycleOwner(), keys -> {
            if (keys != null) {
                // Khi nhận được các khóa, cấu hình và hiển thị PaymentSheet
                presentPaymentSheet(keys);
            }
        });
    }

    private void presentPaymentSheet(PaymentSettingsViewModel.StripeKeys keys) {
        // Cấu hình PaymentSheet
        PaymentSheet.Configuration configuration = new PaymentSheet.Configuration(
                "TradeUp", // Tên công ty của bạn
                new PaymentSheet.CustomerConfiguration(
                        keys.customerId,
                        keys.ephemeralKeySecret
                )
        );

        // *** XÓA DÒNG GÂY LỖI NÀY ĐI ***
        // configuration.setAllowsDelayedPaymentMethods(true);

        // Hiển thị PaymentSheet
        // Dùng `setupIntentClientSecret` vì chúng ta chỉ đang lưu thẻ, không thu tiền
        paymentSheet.presentWithSetupIntent(
                keys.setupIntentClientSecret,
                configuration
        );
    }

    // Callback này sẽ được gọi khi người dùng đóng PaymentSheet
    private void onPaymentSheetResult(final PaymentSheetResult paymentSheetResult) {
        if (paymentSheetResult instanceof PaymentSheetResult.Completed) {
            Toast.makeText(getContext(), "Thiết lập thành công!", Toast.LENGTH_SHORT).show();
            // Người dùng đã thêm/chọn một phương thức thanh toán thành công.
        } else if (paymentSheetResult instanceof PaymentSheetResult.Canceled) {
            Toast.makeText(getContext(), "Đã hủy", Toast.LENGTH_SHORT).show();
        } else if (paymentSheetResult instanceof PaymentSheetResult.Failed) {
            PaymentSheetResult.Failed result = (PaymentSheetResult.Failed) paymentSheetResult;
            Toast.makeText(getContext(), "Lỗi: " + result.getError().getLocalizedMessage(), Toast.LENGTH_LONG).show();
        }
    }
}