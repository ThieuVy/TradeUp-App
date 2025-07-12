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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.testapptradeup.R;
import com.example.testapptradeup.adapters.MyOffersAdapter;
import com.example.testapptradeup.models.OfferWithListing;
import com.example.testapptradeup.viewmodels.MyOffersViewModel;
import com.google.android.material.appbar.MaterialToolbar;

// ======================= THAY ĐỔI Ở DÒNG NÀY =======================
public class MyOffersFragment extends Fragment implements MyOffersAdapter.OnOfferInteractionListener {

    private MyOffersViewModel viewModel;
    private NavController navController;
    private MyOffersAdapter adapter;

    // UI Components
    private MaterialToolbar toolbar;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView emptyStateText;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(MyOffersViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_my_offers, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(view);

        initViews(view);
        setupToolbar();
        setupRecyclerView();
        observeViewModel();
    }

    private void initViews(View view) {
        toolbar = view.findViewById(R.id.toolbar_my_offers);
        recyclerView = view.findViewById(R.id.recycler_my_offers);
        progressBar = view.findViewById(R.id.progress_bar);
        emptyStateText = view.findViewById(R.id.text_empty_state);
    }

    private void setupToolbar() {
        toolbar.setNavigationOnClickListener(v -> navController.popBackStack());
    }

    private void setupRecyclerView() {
        // Giờ dòng này sẽ không còn báo lỗi
        adapter = new MyOffersAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    private void observeViewModel() {
        viewModel.isLoading().observe(getViewLifecycleOwner(), isLoading -> {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            if (isLoading) {
                recyclerView.setVisibility(View.GONE);
                emptyStateText.setVisibility(View.GONE);
            }
        });

        viewModel.getMyOffers().observe(getViewLifecycleOwner(), offers -> {
            if (offers != null) {
                if (offers.isEmpty()) {
                    recyclerView.setVisibility(View.GONE);
                    emptyStateText.setVisibility(View.VISIBLE);
                } else {
                    adapter.submitList(offers);
                    recyclerView.setVisibility(View.VISIBLE);
                    emptyStateText.setVisibility(View.GONE);
                }
            } else {
                Toast.makeText(getContext(), "Lỗi khi tải lịch sử đề nghị.", Toast.LENGTH_SHORT).show();
                emptyStateText.setVisibility(View.VISIBLE);
            }
        });
    }

    // Giờ annotation @Override sẽ không còn báo lỗi
    @Override
    public void onPayNowClick(OfferWithListing item) {
        if (navController != null && item != null && item.getListing() != null && item.getOffer() != null) {
            MyOffersFragmentDirections.ActionMyOffersFragmentToPaymentFragment action =
                    MyOffersFragmentDirections.actionMyOffersFragmentToPaymentFragment(
                            item.getListing().getId(),
                            item.getListing().getSellerId(),
                            (float) item.getOffer().getOfferPrice()
                    );
            navController.navigate(action);
        } else {
            Toast.makeText(getContext(), "Lỗi: Không thể xử lý thanh toán.", Toast.LENGTH_SHORT).show();
        }
    }
}