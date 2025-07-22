package com.example.testapptradeup.fragments.admin;

import android.app.AlertDialog;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.testapptradeup.R;
import com.example.testapptradeup.adapters.AdminReportAdapter;
import com.example.testapptradeup.models.Report;
import com.example.testapptradeup.viewmodels.AdminViewModel;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;

public class ReportListFragment extends Fragment implements AdminReportAdapter.OnReportActionListener {

    private AdminViewModel viewModel;
    private NavController navController;

    private MaterialToolbar toolbar;
    private RecyclerView recyclerView;
    private AdminReportAdapter adapter;
    private ProgressBar progressBar;
    private TextView emptyStateText;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(AdminViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_report_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(view);

        initViews(view);
        setupRecyclerView();
        setupListeners();
        observeViewModel();
    }

    private void initViews(View view) {
        toolbar = view.findViewById(R.id.toolbar);
        recyclerView = view.findViewById(R.id.recycler_reports);
        progressBar = view.findViewById(R.id.progress_bar);
        emptyStateText = view.findViewById(R.id.text_empty_state);
    }

    private void setupRecyclerView() {
        adapter = new AdminReportAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    private void setupListeners() {
        toolbar.setNavigationOnClickListener(v -> navController.popBackStack());
    }

    private void observeViewModel() {
        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        emptyStateText.setVisibility(View.GONE);

        viewModel.getPendingReports().observe(getViewLifecycleOwner(), reports -> {
            progressBar.setVisibility(View.GONE);
            if (reports != null && !reports.isEmpty()) {
                adapter.submitList(new ArrayList<>(reports));
                recyclerView.setVisibility(View.VISIBLE);
                emptyStateText.setVisibility(View.GONE);
            } else {
                recyclerView.setVisibility(View.GONE);
                emptyStateText.setVisibility(View.VISIBLE);
            }
        });

        viewModel.getActionStatusMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null) {
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                viewModel.getPendingReports(); // Tải lại danh sách
                viewModel.clearActionStatus();
            }
        });
    }

    @Override
    public void onDismiss(Report report) {
        // "Dismiss" nghĩa là giải quyết báo cáo nhưng không treo tài khoản
        viewModel.resolveReport(report.getId(), report.getReportedUserId(), false);
    }

    @Override
    public void onSuspend(Report report) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Xác nhận Treo tài khoản")
                .setMessage("Bạn có chắc muốn treo tài khoản của người dùng: " + report.getReportedUserId() + "?")
                .setPositiveButton("Treo", (dialog, which) -> viewModel.resolveReport(report.getId(), report.getReportedUserId(), true))
                .setNegativeButton("Hủy", null)
                .show();
    }
}