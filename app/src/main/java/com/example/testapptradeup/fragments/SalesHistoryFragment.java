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

public class SalesHistoryFragment extends Fragment implements TransactionHistoryAdapter.OnReviewButtonClickListener {

    private HistoryViewModel viewModel;
    private TransactionHistoryAdapter adapter;
    private TextView emptyStateText;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private NavController navController;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(HistoryViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_history_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
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
        // 1. Lắng nghe trạng thái Loading
        viewModel.isLoading().observe(getViewLifecycleOwner(), isLoading -> {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            if (isLoading) {
                recyclerView.setVisibility(View.GONE);
                emptyStateText.setVisibility(View.GONE);
            }
        });

        // 2. Lắng nghe thông báo lỗi
        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                recyclerView.setVisibility(View.GONE);
                emptyStateText.setVisibility(View.VISIBLE);
                emptyStateText.setText(error); // Hiển thị lỗi
            }
        });

        // 3. Lắng nghe danh sách giao dịch
        viewModel.getMyPurchases().observe(getViewLifecycleOwner(), transactions -> {
            // Chỉ xử lý danh sách khi không loading và không có lỗi
            if (viewModel.isLoading().getValue() != null && Boolean.TRUE.equals(viewModel.isLoading().getValue())) {
                return; // Đang loading, không làm gì cả
            }
            if (viewModel.getErrorMessage().getValue() != null && !viewModel.getErrorMessage().getValue().isEmpty()) {
                return; // Đang có lỗi, không làm gì cả
            }

            if (transactions != null && !transactions.isEmpty()) {
                adapter.submitList(transactions);
                recyclerView.setVisibility(View.VISIBLE);
                emptyStateText.setVisibility(View.GONE);
            } else {
                // Đây là trường hợp đã tải xong, không lỗi, nhưng danh sách rỗng
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
        // Dùng NavController của NavHostFragment chính để điều hướng
        Navigation.findNavController(requireActivity(), R.id.nav_host_fragment).navigate(action);
    }
}