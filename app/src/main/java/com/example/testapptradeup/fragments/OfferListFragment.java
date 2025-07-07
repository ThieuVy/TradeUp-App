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
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.testapptradeup.R;
import com.example.testapptradeup.adapters.OffersAdapter;
import com.example.testapptradeup.models.Listing;
import com.example.testapptradeup.models.Offer;
import com.example.testapptradeup.viewmodels.OffersViewModel;
import com.google.android.material.appbar.MaterialToolbar;

public class OfferListFragment extends Fragment implements OffersAdapter.OnOfferActionListener {

    private OffersViewModel viewModel;
    private NavController navController;
    private OffersAdapter adapter;

    // Data from previous fragment
    private String listingId;
    private Listing currentListing;

    // UI Components
    private MaterialToolbar toolbar;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView emptyStateText;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(OffersViewModel.class);

        // Lấy arguments được truyền từ Safe Args
        if (getArguments() != null) {
            // Class 'OfferListFragmentArgs' sẽ được tự động tạo ra sau khi bạn build project
            // với file navigation.xml đã được cập nhật.
            OfferListFragmentArgs args = OfferListFragmentArgs.fromBundle(getArguments());
            listingId = args.getListingId();
            currentListing = args.getListing();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_offer_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(view);

        initViews(view);
        setupToolbar();
        setupRecyclerView();

        if (listingId == null || currentListing == null) {
            Toast.makeText(getContext(), "Lỗi: Dữ liệu tin đăng không hợp lệ.", Toast.LENGTH_LONG).show();
            navController.popBackStack();
            return;
        }

        observeViewModel();
    }

    private void initViews(View view) {
        toolbar = view.findViewById(R.id.toolbar_offer_list);
        recyclerView = view.findViewById(R.id.recycler_offers);
        progressBar = view.findViewById(R.id.progress_bar_offers);
        emptyStateText = view.findViewById(R.id.text_empty_offers);
    }

    private void setupToolbar() {
        toolbar.setNavigationOnClickListener(v -> navController.popBackStack());
        if (currentListing != null && currentListing.getTitle() != null) {
            toolbar.setTitle("Đề nghị cho: " + currentListing.getTitle());
        } else {
            toolbar.setTitle("Danh sách đề nghị");
        }
    }

    private void setupRecyclerView() {
        adapter = new OffersAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    private void observeViewModel() {
        showLoading(true);
        viewModel.getOffers(listingId).observe(getViewLifecycleOwner(), offers -> {
            showLoading(false);
            if (offers != null) {
                if (offers.isEmpty()) {
                    showEmptyState(true);
                } else {
                    adapter.submitList(offers);
                    showEmptyState(false);
                }
            } else {
                Toast.makeText(getContext(), "Lỗi khi tải danh sách đề nghị.", Toast.LENGTH_SHORT).show();
                showEmptyState(true);
            }
        });

        viewModel.getOfferActionStatus().observe(getViewLifecycleOwner(), success -> {
            if (success != null) {
                if (success) {
                    Toast.makeText(getContext(), "Chấp nhận đề nghị thành công!", Toast.LENGTH_SHORT).show();
                    // Khi chấp nhận thành công, quay về màn hình quản lý tin
                    navController.popBackStack();
                } else {
                    Toast.makeText(getContext(), "Thao tác thất bại. Vui lòng thử lại.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void showLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        if (isLoading) {
            recyclerView.setVisibility(View.GONE);
            emptyStateText.setVisibility(View.GONE);
        }
    }

    private void showEmptyState(boolean show) {
        recyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
        emptyStateText.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    // --- Implement interface OnOfferActionListener ---

    @Override
    public void onAccept(Offer offer) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Xác nhận chấp nhận")
                .setMessage("Bạn có chắc chắn muốn chấp nhận đề nghị này? Tin đăng sẽ được đánh dấu là 'Đã bán' và các đề nghị khác sẽ bị từ chối.")
                .setPositiveButton("Chấp nhận", (dialog, which) -> viewModel.acceptOffer(offer, currentListing))
                .setNegativeButton("Hủy", null)
                .show();
    }

    @Override
    public void onReject(Offer offer) {
        Toast.makeText(getContext(), "Tính năng từ chối đang phát triển", Toast.LENGTH_SHORT).show();
        // TODO: Gọi viewModel.rejectOffer(offer);
    }

    @Override
    public void onCounter(Offer offer) {
        Toast.makeText(getContext(), "Tính năng phản hồi giá đang phát triển", Toast.LENGTH_SHORT).show();
        // TODO: Mở một dialog để nhập giá phản hồi và gọi viewModel
    }

    @Override
    public void onChat(Offer offer) {
        Toast.makeText(getContext(), "Mở màn hình chat với " + offer.getBuyerName(), Toast.LENGTH_SHORT).show();
        // TODO: Điều hướng đến ChatDetailFragment, truyền vào ID của người mua (offer.getBuyerId())
    }
}