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
                adapter.submitList(offers);
                showEmptyState(offers.isEmpty());
            } else {
                Toast.makeText(getContext(), "Lỗi khi tải danh sách đề nghị.", Toast.LENGTH_SHORT).show();
                showEmptyState(true);
            }
        });

        viewModel.getActionStatus().observe(getViewLifecycleOwner(), status -> {
            if (status != null) {
                Toast.makeText(getContext(), status.message, Toast.LENGTH_SHORT).show();
                if (status.isSuccess) {
                    // Nếu chấp nhận thành công, quay về màn hình trước
                    if (status.message.contains("chấp nhận")) {
                        navController.popBackStack();
                    }
                }
                viewModel.clearActionStatus(); // Reset trạng thái để không hiển thị lại
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
                .setMessage("Bạn có chắc chắn muốn chấp nhận đề nghị này? Tin đăng sẽ được đánh dấu là 'Đã bán'.")
                .setPositiveButton("Chấp nhận", (dialog, which) -> viewModel.acceptOffer(offer, currentListing))
                .setNegativeButton("Hủy", null)
                .show();
    }

    @Override
    public void onReject(Offer offer) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Xác nhận từ chối")
                .setMessage("Bạn có chắc chắn muốn từ chối đề nghị này không?")
                .setPositiveButton("Từ chối", (dialog, which) -> viewModel.rejectOffer(offer))
                .setNegativeButton("Hủy", null)
                .show();
    }

    @Override
    public void onCounter(Offer offer) {
        // Luồng đơn giản nhất cho "Counter Offer" là bắt đầu một cuộc trò chuyện.
        Toast.makeText(getContext(), "Vui lòng sử dụng chức năng Chat để thương lượng giá.", Toast.LENGTH_LONG).show();
        onChat(offer);
    }

    @Override
    public void onChat(Offer offer) {
        // TODO: Điều hướng đến ChatDetailFragment, truyền vào ID của người mua (offer.getBuyerId()) và tên người mua
        Toast.makeText(getContext(), "Mở màn hình chat với " + offer.getBuyerName(), Toast.LENGTH_SHORT).show();
    }
}