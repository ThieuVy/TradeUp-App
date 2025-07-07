package com.example.testapptradeup.fragments;

import android.annotation.SuppressLint;
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

        emptyStateText = view.findViewById(R.id.text_empty_history);
        recyclerView = view.findViewById(R.id.recycler_history);

        setupRecyclerView();
        observeViewModel();
    }

    private void setupRecyclerView() {
        adapter = new TransactionHistoryAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    @SuppressLint("SetTextI18n")
    private void observeViewModel() {
        viewModel.getMySales().observe(getViewLifecycleOwner(), transactions -> {
            if (transactions != null && !transactions.isEmpty()) {
                adapter.submitList(transactions);
                recyclerView.setVisibility(View.VISIBLE);
                emptyStateText.setVisibility(View.GONE);
            } else {
                recyclerView.setVisibility(View.GONE);
                emptyStateText.setVisibility(View.VISIBLE);
                emptyStateText.setText("Bạn chưa bán sản phẩm nào.");
            }
        });
    }

    @Override
    public void onReviewClick(Transaction transaction) {
        // Khi bán hàng, người cần đánh giá là người mua (buyer)
        HistoryFragmentDirections.ActionHistoryFragmentToAddReviewFragment action =
                HistoryFragmentDirections.actionHistoryFragmentToAddReviewFragment(
                        transaction.getId(),
                        transaction.getBuyerId() // <<< Truyền ID của người mua
                );
        navController.navigate(action);
    }
}