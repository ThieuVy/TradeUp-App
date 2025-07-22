package com.example.testapptradeup.fragments.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import com.example.testapptradeup.R;
import com.example.testapptradeup.viewmodels.AdminViewModel;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;

public class AdminPanelFragment extends Fragment {

    private AdminViewModel viewModel;
    private NavController navController;

    // UI Components
    private TextView textReviewCount, textReportCount;
    private MaterialToolbar toolbar;
    private MaterialCardView cardModerateReviews, cardResolveReports;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(AdminViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_panel, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(view);

        initViews(view);
        setupListeners();
        observeViewModel();
    }

    private void initViews(View view) {
        toolbar = view.findViewById(R.id.toolbar);
        textReviewCount = view.findViewById(R.id.text_review_count);
        textReportCount = view.findViewById(R.id.text_report_count);
        cardModerateReviews = view.findViewById(R.id.card_moderate_reviews);
        cardResolveReports = view.findViewById(R.id.card_resolve_reports);
    }

    private void setupListeners() {
        toolbar.setNavigationOnClickListener(v -> navController.popBackStack());

        cardModerateReviews.setOnClickListener(v -> navController.navigate(R.id.action_adminPanelFragment_to_reviewModerationFragment));

        cardResolveReports.setOnClickListener(v -> navController.navigate(R.id.action_adminPanelFragment_to_reportListFragment));
    }

    private void observeViewModel() {
        // Lắng nghe số lượng review đang chờ duyệt
        viewModel.getPendingReviewCount().observe(getViewLifecycleOwner(), count -> {
            if (count != null && count > 0) {
                textReviewCount.setText(String.valueOf(count));
                textReviewCount.setVisibility(View.VISIBLE);
            } else {
                textReviewCount.setVisibility(View.GONE);
            }
        });

        // Lắng nghe số lượng báo cáo đang chờ xử lý
        viewModel.getPendingReportCount().observe(getViewLifecycleOwner(), count -> {
            if (count != null && count > 0) {
                textReportCount.setText(String.valueOf(count));
                textReportCount.setVisibility(View.VISIBLE);
            } else {
                textReportCount.setVisibility(View.GONE);
            }
        });
    }
}