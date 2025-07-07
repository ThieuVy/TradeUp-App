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
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import com.example.testapptradeup.R;
import com.example.testapptradeup.viewmodels.PaymentSettingsViewModel;
import com.google.android.material.appbar.MaterialToolbar;
import com.stripe.android.PaymentConfiguration;
import com.stripe.android.paymentsheet.PaymentSheet;
import com.stripe.android.paymentsheet.PaymentSheetResult;

public class PaymentSettingsFragment extends Fragment {
    private PaymentSettingsViewModel viewModel;
    private PaymentSheet paymentSheet;
    private Button managePaymentsButton;
    private ProgressBar progressBar;
    private NavController navController;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(PaymentSettingsViewModel.class);
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
        navController = Navigation.findNavController(view);

        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        managePaymentsButton = view.findViewById(R.id.btn_manage_payments);
        progressBar = view.findViewById(R.id.progress_bar);

        toolbar.setNavigationOnClickListener(v -> navController.popBackStack());
        managePaymentsButton.setOnClickListener(v -> viewModel.fetchStripeKeys());

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
                // PublishableKey cần được cấu hình một lần, thường là trong Application class
                // nhưng để đơn giản, ta đặt ở đây.
                // THAY THẾ BẰNG PUBLISHABLE KEY CỦA BẠN
                PaymentConfiguration.init(requireContext(), "pk_test_YOUR_PUBLISHABLE_KEY");
                presentPaymentSheet(keys);
            }
        });
    }

    private void presentPaymentSheet(PaymentSettingsViewModel.StripeKeys keys) {
        final PaymentSheet.Configuration configuration = new PaymentSheet.Configuration.Builder("TradeUp, Inc.")
                .customer(new PaymentSheet.CustomerConfiguration(keys.customerId, keys.ephemeralKeySecret))
                .allowsDelayedPaymentMethods(true)
                .build();

        paymentSheet.presentWithSetupIntent(keys.setupIntentClientSecret, configuration);
    }

    private void onPaymentSheetResult(final PaymentSheetResult paymentSheetResult) {
        if (paymentSheetResult instanceof PaymentSheetResult.Completed) {
            Toast.makeText(getContext(), "Thiết lập thành công!", Toast.LENGTH_SHORT).show();
        } else if (paymentSheetResult instanceof PaymentSheetResult.Canceled) {
            Toast.makeText(getContext(), "Đã hủy", Toast.LENGTH_SHORT).show();
        } else if (paymentSheetResult instanceof PaymentSheetResult.Failed) {
            Toast.makeText(getContext(), "Lỗi: " + ((PaymentSheetResult.Failed) paymentSheetResult).getError().getLocalizedMessage(), Toast.LENGTH_LONG).show();
        }
    }
}