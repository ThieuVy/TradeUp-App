package com.example.testapptradeup.fragments;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.testapptradeup.R;
import com.example.testapptradeup.adapters.TransactionHistoryAdapter;
import com.example.testapptradeup.models.Transaction;
import com.example.testapptradeup.viewmodels.HistoryViewModel;

public class PurchaseHistoryFragment extends Fragment implements TransactionHistoryAdapter.OnReviewButtonClickListener {

    private HistoryViewModel viewModel;
    private TransactionHistoryAdapter adapter;
    private TextView emptyStateText;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private NavController navController;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Lấy ViewModel được chia sẻ từ Activity cha hoặc Fragment cha (HistoryFragment)
        viewModel = new ViewModelProvider(requireActivity()).get(HistoryViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Sử dụng lại layout chung cho danh sách
        return inflater.inflate(R.layout.fragment_history_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Lấy NavController từ NavHostFragment của Activity để đảm bảo điều hướng đúng
        navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment);

        initViews(view);
        setupRecyclerView();
        observeViewModel();
    }

    private void initViews(View view) {
        emptyStateText = view.findViewById(R.id.text_empty_history);
        recyclerView = view.findViewById(R.id.recycler_history);
        progressBar = view.findViewById(R.id.progress_bar_history);
    }

    private void setupRecyclerView() {
        adapter = new TransactionHistoryAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    @SuppressLint("SetTextI18n")
    private void observeViewModel() {
        // Hiển thị loading ban đầu
        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        emptyStateText.setVisibility(View.GONE);

        viewModel.getMyPurchases().observe(getViewLifecycleOwner(), transactions -> {
            progressBar.setVisibility(View.GONE); // Ẩn loading khi có dữ liệu (kể cả rỗng)
            if (transactions != null && !transactions.isEmpty()) {
                adapter.submitList(transactions);
                recyclerView.setVisibility(View.VISIBLE);
                emptyStateText.setVisibility(View.GONE);
            } else {
                recyclerView.setVisibility(View.GONE);
                emptyStateText.setVisibility(View.VISIBLE);
                emptyStateText.setText("Bạn chưa mua sản phẩm nào.");
            }
        });
    }

    @Override
    public void onReviewClick(Transaction transaction) {
        // Khi mua hàng, người được đánh giá là người bán (seller)
        HistoryFragmentDirections.ActionHistoryFragmentToAddReviewFragment action =
                HistoryFragmentDirections.actionHistoryFragmentToAddReviewFragment(
                        transaction.getId(),
                        transaction.getSellerId() // Truyền ID của người bán
                );
        navController.navigate(action);
    }
}