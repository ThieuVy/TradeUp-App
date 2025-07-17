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

public class MyListingsFragment extends Fragment implements ManageListingsAdapter.OnListingInteractionListener {

    private MyListingsViewModel viewModel;
    private NavController navController;
    private ManageListingsAdapter adapter;
    private RecyclerView recyclerView;
    private LinearLayout emptyState;
    private ProgressBar loadingState;
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
        mainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        navController = Navigation.findNavController(view);

        adapter = new ManageListingsAdapter(this);
        recyclerView.setAdapter(adapter);

        setupViewsAndListeners(view);
        observeViewModel();
        observeSharedViewModel();
    }

    private void setupViewsAndListeners(View view) {
        // Ánh xạ các Views
        recyclerView = view.findViewById(R.id.recycler_listings);
        emptyState = view.findViewById(R.id.empty_state);
        loadingState = view.findViewById(R.id.loading_state);
        ImageView btnBack = view.findViewById(R.id.btn_back);
        if (btnBack != null) {
            btnBack.setVisibility(View.GONE); // Ẩn nút back vì đây là màn hình cấp cao nhất
        }
        tabAll = view.findViewById(R.id.tab_all);
        tabActive = view.findViewById(R.id.tab_active);
        tabSold = view.findViewById(R.id.tab_sold);
        tabPaused = view.findViewById(R.id.tab_paused);
        sortContainer = view.findViewById(R.id.sort_container);
        sortText = view.findViewById(R.id.sort_text);
        headerTitle = view.findViewById(R.id.header_title);

        // Khởi tạo Adapter MỘT LẦN DUY NHẤT, truyền `this` làm listener
        adapter = new ManageListingsAdapter(this);

        // Setup RecyclerView
        layoutManager = new GridLayoutManager(getContext(), 1);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);

        // Setup Listeners
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (dy > 0) { // Check for scroll down
                    int visibleItemCount = layoutManager.getChildCount();
                    int totalItemCount = layoutManager.getItemCount();
                    int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                    // === BẮT ĐẦU SỬA LỖI: SỬ DỤNG GETTER ===
                    // Thay vì truy cập viewModel.isCurrentlyLoading, hãy gọi phương thức công khai
                    if (!viewModel.isCurrentlyLoading() && !viewModel.isLastPage()) {
                        // === KẾT THÚC SỬA LỖI ===
                        if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                                && firstVisibleItemPosition >= 0
                                && totalItemCount >= ListingRepository.PAGE_SIZE) {
                            viewModel.loadNextPage();
                        }
                    }
                }
            }
        });

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

        sortContainer.setOnClickListener(v ->
                Toast.makeText(getContext(), "Chức năng sắp xếp đang phát triển", Toast.LENGTH_SHORT).show()
        );

        // Đặt tab mặc định
        updateFilterButtonUI(R.id.tab_all);
    }

    // Xóa các phương thức setupRecyclerView() và setupListeners() riêng lẻ

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
        viewModel.getDisplayedListings().observe(getViewLifecycleOwner(), listings -> {
            adapter.submitList(listings);
            checkEmptyState(listings == null || listings.isEmpty());
            if (listings != null) {
                updateHeaderTitle(listings.size());
            }
        });

        viewModel.isLoading().observe(getViewLifecycleOwner(), isLoading -> {
            loadingState.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            if (isLoading) {
                recyclerView.setVisibility(View.GONE);
                emptyState.setVisibility(View.GONE);
            }
        });

        // Bạn có thể thêm logic cho isLoadingMore nếu muốn hiển thị footer
        viewModel.isLoadingMore().observe(getViewLifecycleOwner(), isLoadingMore -> {
            // Ví dụ: adapter.showFooter(isLoadingMore);
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), errorMessage -> {
            if (errorMessage != null && !errorMessage.isEmpty()) {
                Toast.makeText(getContext(), errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    // ... Các hàm helper (updateHeaderTitle, updateFilterButtonUI, checkEmptyState) giữ nguyên ...
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

    private void checkEmptyState(boolean isEmpty) {
        if (emptyState != null) emptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        if (recyclerView != null) recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    // === BƯỚC 3: TRIỂN KHAI ĐẦY ĐỦ CÁC PHƯƠNG THỨC CỦA INTERFACE ===

    @Override
    public void onViewDetailsClick(Listing listing) {
        // SỬA LỖI CRASH: Thêm kiểm tra null
        if (listing != null && listing.getId() != null) {
            // Giả sử bạn đã tạo action này trong mobile_navigation.xml
            MyListingsFragmentDirections.ActionMyListingsFragmentToProductDetailFragment action =
                    MyListingsFragmentDirections.actionMyListingsFragmentToProductDetailFragment(listing.getId());
            action.setListingPreview(null); // Không phải chế độ xem trước
            navController.navigate(action);
        } else {
            Toast.makeText(getContext(), "Không thể mở chi tiết. Dữ liệu không hợp lệ.", Toast.LENGTH_SHORT).show();
            Log.e("MyListingsFragment", "onViewDetailsClick: Listing hoặc Listing ID bị null.");
        }
    }

    @Override
    public void onEditClick(Listing listing) {
        if (listing != null && listing.getId() != null) {
            // SỬA LỖI: Gọi đúng action đã định nghĩa
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
                        if (success != null && success) {
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
                MyListingsFragmentDirections.ActionMyListingsFragmentToOfferListFragment action =
                        MyListingsFragmentDirections.actionMyListingsFragmentToOfferListFragment(listing.getId(), listing);
                navController.navigate(action);
            } else {
                Toast.makeText(getContext(), "Chưa có đề nghị nào cho sản phẩm này.", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(getContext(), "Không thể xem đề nghị. Dữ liệu không hợp lệ.", Toast.LENGTH_SHORT).show();
        }
    }
}