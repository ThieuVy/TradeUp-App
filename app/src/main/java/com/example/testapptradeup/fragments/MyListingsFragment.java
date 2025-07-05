package com.example.testapptradeup.fragments;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
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

public class MyListingsFragment extends Fragment implements ManageListingsAdapter.OnItemInteractionListener {

    private MyListingsViewModel viewModel;
    private NavController navController;

    // --- UI Components ---
    private RecyclerView recyclerView;
    private ManageListingsAdapter adapter;
    private LinearLayout emptyState;
    private ProgressBar loadingState;
    private ImageView btnBack;
    private TextView tabAll, tabActive, tabPaused, tabSold, sortText, headerTitle;
    private LinearLayout sortContainer;

    private GridLayoutManager layoutManager;
    private MainViewModel mainViewModel;

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
        mainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class); // Khởi tạo
        navController = Navigation.findNavController(view);
        initViews(view);
        setupRecyclerView();
        setupListeners();
        observeViewModel();
        // Set tab mặc định
        updateFilterButtonUI(R.id.tab_all);
        observeSharedViewModel();
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recycler_listings);
        emptyState = view.findViewById(R.id.empty_state);
        loadingState = view.findViewById(R.id.loading_state);
        btnBack = view.findViewById(R.id.btn_back);
        if (btnBack != null) {
            btnBack.setVisibility(View.GONE); // Ẩn nút back
        }
        tabAll = view.findViewById(R.id.tab_all);
        tabActive = view.findViewById(R.id.tab_active);
        tabSold = view.findViewById(R.id.tab_sold);
        tabPaused = view.findViewById(R.id.tab_paused);
        sortContainer = view.findViewById(R.id.sort_container);
        sortText = view.findViewById(R.id.sort_text);
        headerTitle = view.findViewById(R.id.header_title);
    }

    private void setupRecyclerView() {
        adapter = new ManageListingsAdapter(this, this, this);
        layoutManager = new GridLayoutManager(getContext(), 1); // Sử dụng 1 cột cho dễ nhìn
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

                    if (Boolean.FALSE.equals(viewModel.isLoadingMore().getValue()) && !viewModel.isLastPage()) {
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
                // Khi có bài đăng mới, yêu cầu ViewModel tải lại từ đầu
                viewModel.refreshListings();
                // Đánh dấu sự kiện đã được xử lý để không trigger lại
                mainViewModel.onNewListingEventHandled();
            }
        });
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> navController.popBackStack());

        View.OnClickListener filterClickListener = v -> {
            int id = v.getId();
            updateFilterButtonUI(id);
            if (id == R.id.tab_all) viewModel.setFilter("all");
            else if (id == R.id.tab_active) viewModel.setFilter("available");
            else if (id == R.id.tab_paused) viewModel.setFilter("paused");
            else if (id == R.id.tab_sold) viewModel.setFilter("sold");
        };
        tabAll.setOnClickListener(filterClickListener);
        tabActive.setOnClickListener(filterClickListener);
        tabPaused.setOnClickListener(filterClickListener);
        tabSold.setOnClickListener(filterClickListener);

        sortContainer.setOnClickListener(this::showSortMenu);
    }

    private void observeViewModel() {
        viewModel.getDisplayedListings().observe(getViewLifecycleOwner(), listings -> {
            adapter.submitList(listings);
            if (Boolean.FALSE.equals(viewModel.isLoading().getValue())) {
                checkEmptyState(listings.isEmpty());
            }
            updateHeaderTitle(listings.size());
        });

        viewModel.isLoading().observe(getViewLifecycleOwner(), isLoading -> {
            loadingState.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            if (isLoading) {
                recyclerView.setVisibility(View.GONE);
                emptyState.setVisibility(View.GONE);
            }
        });

        viewModel.isLoadingMore().observe(getViewLifecycleOwner(), isLoadingMore -> {
            // Có thể hiển thị một footer progress bar trong RecyclerView nếu muốn
        });
    }

    @SuppressLint("SetTextI18n")
    private void updateHeaderTitle(int count) {
        if(headerTitle != null) {
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

    private void showSortMenu(View v) {
        PopupMenu popup = new PopupMenu(getContext(), v);
        Toast.makeText(getContext(), "Chức năng sắp xếp đang phát triển", Toast.LENGTH_SHORT).show();
    }

    private void checkEmptyState(boolean isEmpty) {
        if (emptyState != null) emptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        if (recyclerView != null) recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    // --- Triển khai các phương thức của OnItemInteractionListener ---

    @Override
    public void onItemClick(Listing listing) {
        Toast.makeText(getContext(), "Xem chi tiết: " + listing.getTitle(), Toast.LENGTH_SHORT).show();
    }

    public void onEditClick(Listing listing) {
        Toast.makeText(getContext(), "Chỉnh sửa: " + listing.getTitle(), Toast.LENGTH_SHORT).show();
    }

    public void onDeleteClick(Listing listing) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Xác nhận xóa")
                .setMessage("Bạn có chắc chắn muốn xóa tin \"" + listing.getTitle() + "\"?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    viewModel.deleteListing(listing.getId()).observe(getViewLifecycleOwner(), success -> {
                        if (success != null && success) {
                            Toast.makeText(getContext(), "Đã xóa tin thành công", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getContext(), "Lỗi khi xóa tin", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
}