package com.example.testapptradeup.fragments;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.testapptradeup.R;
import com.example.testapptradeup.adapters.ManageListingsAdapter;
import com.example.testapptradeup.models.Listing;
import com.example.testapptradeup.repositories.ListingRepository;
import com.example.testapptradeup.viewmodels.MainViewModel;
import com.example.testapptradeup.viewmodels.MyListingsViewModel;

import java.util.List;

public class MyListingsFragment extends Fragment implements ManageListingsAdapter.OnListingInteractionListener {

    private MyListingsViewModel viewModel;
    private MainViewModel mainViewModel;
    private ManageListingsAdapter adapter;
    private RecyclerView recyclerView;
    private GridLayoutManager layoutManager;

    private NavController navController;
    private LinearLayout emptyState;
    private ProgressBar loadingState;
    private TextView tabAll, tabActive, tabPaused, tabSold, headerTitle;
    private ImageView btnBack; // Thêm biến cho nút back

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(MyListingsViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_my_listings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        navController = Navigation.findNavController(view);

        // --- SỬA LỖI 1: CẬP NHẬT CÁC ID CHO CHÍNH XÁC ---
        recyclerView = view.findViewById(R.id.recycler_listings);
        emptyState = view.findViewById(R.id.empty_state); // ID đúng là 'empty_state'
        loadingState = view.findViewById(R.id.loading_state); // ID đúng là 'loading_state'
        tabAll = view.findViewById(R.id.tab_all);
        tabActive = view.findViewById(R.id.tab_active);
        tabPaused = view.findViewById(R.id.tab_paused);
        tabSold = view.findViewById(R.id.tab_sold);
        headerTitle = view.findViewById(R.id.header_title); // ID đúng là 'header_title'
        btnBack = view.findViewById(R.id.btn_back);

        // Thêm sự kiện click cho các tab lọc và nút back
        setupClickListeners();
        setupRecyclerView();
        observeViewModel();
        observeSharedViewModel();

        // Cập nhật giao diện bộ lọc lần đầu
        updateFilterButtonUI(R.id.tab_all);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> navController.navigateUp());

        tabAll.setOnClickListener(v -> {
            viewModel.setFilter("all");
            updateFilterButtonUI(R.id.tab_all);
        });
        tabActive.setOnClickListener(v -> {
            viewModel.setFilter("available");
            updateFilterButtonUI(R.id.tab_active);
        });
        tabPaused.setOnClickListener(v -> {
            viewModel.setFilter("paused");
            updateFilterButtonUI(R.id.tab_paused);
        });
        tabSold.setOnClickListener(v -> {
            viewModel.setFilter("sold");
            updateFilterButtonUI(R.id.tab_sold);
        });
    }

    private void setupRecyclerView() {
        adapter = new ManageListingsAdapter(this);
        layoutManager = new GridLayoutManager(getContext(), 1);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (dy > 0) {
                    int visibleItemCount = layoutManager.getChildCount();
                    int totalItemCount = layoutManager.getItemCount();
                    int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                    if (!viewModel.isCurrentlyLoading() && !viewModel.isLastPage()) {
                        if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                                && firstVisibleItemPosition >= 0
                                && totalItemCount >= ListingRepository.PAGE_SIZE) {
                            viewModel.loadNextPage();
                        }
                    }
                }
            }
        });
    }

    private void observeSharedViewModel() {
        mainViewModel.getNewListingPosted().observe(getViewLifecycleOwner(), newListing -> {
            if (newListing != null) {
                Log.d("MyListingsFragment", "Nhận được sự kiện đăng bài mới, đang làm mới danh sách...");
                viewModel.refreshListings();
                mainViewModel.onNewListingEventHandled();
            }
        });
    }

    private void observeViewModel() {
        viewModel.getDisplayedListings().observe(getViewLifecycleOwner(), (List<Listing> listings) -> {
            adapter.submitList(listings);
            if (listings != null) {
                updateHeaderTitle(listings.size());
            }
            checkEmptyState(listings == null || listings.isEmpty());
        });

        viewModel.isLoading().observe(getViewLifecycleOwner(), (Boolean isLoading) -> {
            if (isLoading) {
                loadingState.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
                emptyState.setVisibility(View.GONE);
            } else {
                loadingState.setVisibility(View.GONE);
            }
        });

        viewModel.isLoadingMore().observe(getViewLifecycleOwner(), isLoadingMore -> {
            // TODO: Hiển thị footer loading trong Adapter nếu cần
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
            }
        });
    }

    @SuppressLint("SetTextI18n")
    private void updateHeaderTitle(int count) {
        if (headerTitle != null) {
            headerTitle.setText("Quản lý tin (" + count + ")");
        }
    }

    private void updateFilterButtonUI(int selectedId) {
        updateSingleButtonUI(tabAll, selectedId == R.id.tab_all);
        updateSingleButtonUI(tabActive, selectedId == R.id.tab_active);
        updateSingleButtonUI(tabPaused, selectedId == R.id.tab_paused);
        updateSingleButtonUI(tabSold, selectedId == R.id.tab_sold);
    }

    private void updateSingleButtonUI(TextView button, boolean isSelected) {
        if (button == null || getContext() == null) return;
        button.setBackgroundResource(isSelected ? R.drawable.tab_selected_background : android.R.color.transparent);
        button.setTextColor(ContextCompat.getColor(getContext(), isSelected ? R.color.text_primary_on_dark : R.color.text_secondary));
        button.setTypeface(null, isSelected ? Typeface.BOLD : Typeface.NORMAL);
    }

    private void checkEmptyState(boolean isEmpty) {
        if (emptyState != null) emptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        if (recyclerView != null) recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }


    @Override
    public void onViewDetailsClick(Listing listing) {
        if (listing != null && listing.getId() != null) {
            MyListingsFragmentDirections.ActionMyListingsFragmentToProductDetailFragment action =
                    MyListingsFragmentDirections.actionMyListingsFragmentToProductDetailFragment(listing.getId());
            navController.navigate(action);
        } else {
            Toast.makeText(getContext(), "Không thể mở chi tiết. Dữ liệu không hợp lệ.", Toast.LENGTH_SHORT).show();
            Log.e("MyListingsFragment", "onViewDetailsClick: Listing hoặc Listing ID là null.");
        }
    }

    @Override
    public void onEditClick(Listing listing) {
        if (listing != null && listing.getId() != null) {
            MyListingsFragmentDirections.ActionMyListingsFragmentToEditPostFragment action =
                    MyListingsFragmentDirections.actionMyListingsFragmentToEditPostFragment(listing.getId());
            navController.navigate(action);
        } else {
            Toast.makeText(getContext(), "Không thể sửa. Dữ liệu không hợp lệ.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDeleteClick(Listing listing) {
        if (listing != null && listing.getId() != null) {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Xác nhận xóa")
                    .setMessage("Bạn có chắc chắn muốn xóa tin \"" + listing.getTitle() + "\"?")
                    .setPositiveButton("Xóa", (dialog, which) -> viewModel.deleteListing(listing.getId()).observe(getViewLifecycleOwner(), success -> {
                        if (Boolean.TRUE.equals(success)) {
                            Toast.makeText(getContext(), "Đã xóa tin thành công", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getContext(), "Lỗi khi xóa tin", Toast.LENGTH_SHORT).show();
                        }
                    }))
                    .setNegativeButton("Hủy", null)
                    .show();
        } else {
            Toast.makeText(getContext(), "Không thể xóa. Dữ liệu không hợp lệ.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onViewOffersClick(Listing listing) {
        if (listing != null && listing.getId() != null) {
            if (listing.getOffersCount() > 0) {
                // Tạm thời comment lại để tránh lỗi nếu chưa tạo action này
                // MyListingsFragmentDirections.ActionMyListingsFragmentToOfferListFragment action =
                //        MyListingsFragmentDirections.actionMyListingsFragmentToOfferListFragment(listing.getId(), listing);
                // navController.navigate(action);
                Toast.makeText(getContext(), "Chuyển đến màn hình xem đề nghị", Toast.LENGTH_SHORT).show();

            } else {
                Toast.makeText(getContext(), "Chưa có đề nghị nào cho sản phẩm này.", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(getContext(), "Không thể xem đề nghị. Dữ liệu không hợp lệ.", Toast.LENGTH_SHORT).show();
        }
    }
}