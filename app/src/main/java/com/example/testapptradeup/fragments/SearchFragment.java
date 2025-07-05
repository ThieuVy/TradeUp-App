package com.example.testapptradeup.fragments;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.testapptradeup.R;
import com.example.testapptradeup.adapters.SearchResultsAdapter;
import com.example.testapptradeup.models.SearchParams;
import com.example.testapptradeup.models.SearchResult;
import com.example.testapptradeup.viewmodels.SearchViewModel;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.Locale;

public class SearchFragment extends Fragment {

    private static final int DEBOUNCE_DELAY_MS = 500;

    // ViewModel
    private SearchViewModel viewModel;

    // UI Components
    private EditText searchInput;
    private ImageButton clearSearch, filterToggle;
    private ScrollView filtersContainer;
    private LinearLayout sortHeaderCard;
    private AutoCompleteTextView categoryFilter, conditionFilter, sortFilter;
    private EditText minPriceInput, maxPriceInput, locationInput;
    private SeekBar distanceSeekbar;
    private TextView distanceValue, resultsCount;
    private Button useGpsButton, clearFiltersButton, applyFiltersButton;
    private RecyclerView searchResultsRecyclerView;
    private LinearLayout emptyState, loadingState;
    private ProgressBar loadMoreProgress;
    private Button loadMoreButton;

    // Adapter
    private SearchResultsAdapter searchResultsAdapter;

    // Location
    private FusedLocationProviderClient fusedLocationClient;
    private Location currentLocation;
    private ActivityResultLauncher<String> locationPermissionLauncher;

    // Utils
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;
    private boolean isFiltersVisible = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(SearchViewModel.class);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        locationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        fetchCurrentLocation();
                    } else {
                        Toast.makeText(requireContext(), "Quyền vị trí bị từ chối.", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search, container, false);
        initViews(view);
        setupAdaptersAndData();
        setupListeners();
        observeViewModel();

        if (viewModel.getSearchResults().getValue() == null || viewModel.getSearchResults().getValue().isEmpty()) {
            viewModel.startNewSearch(new SearchParams());
        }
        return view;
    }

    private void initViews(View view) {
        searchInput = view.findViewById(R.id.search_input);
        clearSearch = view.findViewById(R.id.clear_search);
        filterToggle = view.findViewById(R.id.filter_toggle);
        filtersContainer = view.findViewById(R.id.filters_container);
        sortHeaderCard = view.findViewById(R.id.sort_header_card);
        categoryFilter = view.findViewById(R.id.category_filter);
        minPriceInput = view.findViewById(R.id.min_price_input);
        maxPriceInput = view.findViewById(R.id.max_price_input);
        conditionFilter = view.findViewById(R.id.condition_filter);
        distanceSeekbar = view.findViewById(R.id.distance_seekbar);
        distanceValue = view.findViewById(R.id.distance_value);
        locationInput = view.findViewById(R.id.location_input);
        useGpsButton = view.findViewById(R.id.use_gps_button);
        clearFiltersButton = view.findViewById(R.id.clear_filters_button);
        applyFiltersButton = view.findViewById(R.id.apply_filters_button);
        resultsCount = view.findViewById(R.id.results_count);
        sortFilter = view.findViewById(R.id.sort_filter);
        searchResultsRecyclerView = view.findViewById(R.id.search_results_recycler_view);
        emptyState = view.findViewById(R.id.empty_state);
        loadingState = view.findViewById(R.id.loading_state);
        loadMoreButton = view.findViewById(R.id.load_more_button);
        loadMoreProgress = view.findViewById(R.id.load_more_progress);
    }

    private void setupAdaptersAndData() {
        searchResultsAdapter = new SearchResultsAdapter(new ArrayList<>(), this::onProductClick, this::onFavoriteClick);
        searchResultsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        searchResultsRecyclerView.setAdapter(searchResultsAdapter);

        String[] categories = {"Tất cả", "Điện thoại", "Laptop", "Thời trang", "Đồ gia dụng", "Khác"};
        categoryFilter.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, categories));

        String[] conditions = {"Tất cả", "Mới", "Như mới", "Tốt", "Đã dùng"};
        conditionFilter.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, conditions));

        String[] sortOptions = {"Liên quan nhất", "Mới nhất", "Giá: Thấp đến cao", "Giá: Cao đến thấp"};
        sortFilter.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, sortOptions));
    }

    private void setupAdapters() {
        searchResultsAdapter = new SearchResultsAdapter(new ArrayList<>(), this::onProductClick, this::onFavoriteClick);
        searchResultsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        searchResultsRecyclerView.setAdapter(searchResultsAdapter);

        // Setup các dropdown adapters
        String[] categories = {"Điện thoại", "Laptop", "Thời trang", "Đồ gia dụng", "Khác"};
        categoryFilter.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, categories));
        String[] conditions = {"Mới", "Như mới", "Tốt", "Đã dùng"};
        conditionFilter.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, conditions));
        String[] sortOptions = {"Liên quan nhất", "Mới nhất", "Giá: Thấp đến cao", "Giá: Cao đến thấp"};
        sortFilter.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, sortOptions));
    }

    private void setupListeners() {
        // Debounce cho ô tìm kiếm
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                clearSearch.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
                handler.removeCallbacks(searchRunnable);
                searchRunnable = () -> {
                    SearchParams params = viewModel.getSearchParams().getValue();
                    if(params == null) params = new SearchParams();
                    params.setQuery(s.toString().trim());
                    viewModel.startNewSearch(params);
                };
                handler.postDelayed(searchRunnable, DEBOUNCE_DELAY_MS);
            }
        });

        clearSearch.setOnClickListener(v -> searchInput.setText(""));

        // Nút bật/tắt bộ lọc
        filterToggle.setOnClickListener(v -> {
            isFiltersVisible = !isFiltersVisible;
            filtersContainer.setVisibility(isFiltersVisible ? View.VISIBLE : View.GONE);
            // Ẩn các view chính khi bộ lọc hiện ra
            sortHeaderCard.setVisibility(isFiltersVisible ? View.GONE : View.VISIBLE);
            searchResultsRecyclerView.setVisibility(isFiltersVisible ? View.GONE : View.VISIBLE);
        });

        // Áp dụng bộ lọc
        applyFiltersButton.setOnClickListener(v -> {
            // ========== SỬA LỖI Ở ĐÂY ==========
            SearchParams params = collectSearchParamsFromUi();
            viewModel.startNewSearch(params);
            // ===================================
            toggleFilterVisibility(false);
        });

        //Xóa bộ lọc
        clearFiltersButton.setOnClickListener(v -> {
            clearFilterInputs();
            viewModel.startNewSearch(new SearchParams());
            toggleFilterVisibility(false);
        });

        filterToggle.setOnClickListener(v -> toggleFilterVisibility(!isFiltersVisible));
        useGpsButton.setOnClickListener(v -> checkLocationPermissionAndFetch());
    }

    private void toggleFilterVisibility(boolean show) {
        isFiltersVisible = show;
        filtersContainer.setVisibility(isFiltersVisible ? View.VISIBLE : View.GONE);
        sortHeaderCard.setVisibility(isFiltersVisible ? View.GONE : View.VISIBLE);
        searchResultsRecyclerView.setVisibility(isFiltersVisible ? View.GONE : View.VISIBLE);
        if(isFiltersVisible) {
            emptyState.setVisibility(View.GONE);
            loadingState.setVisibility(View.GONE);
        }
    }

    @SuppressLint("DefaultLocale")
    private void observeViewModel() {
        // Lắng nghe kết quả tìm kiếm
        viewModel.getSearchResults().observe(getViewLifecycleOwner(), results -> {
            searchResultsAdapter.updateResults(results);
            resultsCount.setText(String.format("Tìm thấy %d kết quả", results.size()));
        });

        // Lắng nghe trạng thái UI (loading, empty, success...)
        viewModel.getUiState().observe(getViewLifecycleOwner(), this::updateUiState);
    }

    // Cập nhật giao diện dựa trên trạng thái từ ViewModel
    private void updateUiState(SearchViewModel.UiState state) {
        // Ẩn/hiện bộ lọc
        if (isFiltersVisible) {
            loadingState.setVisibility(View.GONE);
            emptyState.setVisibility(View.GONE);
            searchResultsRecyclerView.setVisibility(View.GONE);
            return;
        }

        // Quản lý trạng thái loading chính
        loadingState.setVisibility(state == SearchViewModel.UiState.LOADING ? View.VISIBLE : View.GONE);

        // Quản lý trạng thái rỗng
        emptyState.setVisibility(state == SearchViewModel.UiState.EMPTY ? View.VISIBLE : View.GONE);

        // Quản lý RecyclerView
        searchResultsRecyclerView.setVisibility(state == SearchViewModel.UiState.SUCCESS || state == SearchViewModel.UiState.LOADING_MORE ? View.VISIBLE : View.GONE);

        // Quản lý loading more
        loadMoreProgress.setVisibility(state == SearchViewModel.UiState.LOADING_MORE ? View.VISIBLE : View.GONE);
        loadMoreButton.setVisibility(state == SearchViewModel.UiState.SUCCESS && !viewModel.isLastPage() ? View.VISIBLE : View.GONE);
    }

    private void checkLocationPermissionAndFetch() {
        if (getContext() != null && ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fetchCurrentLocation();
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    @SuppressLint("MissingPermission")
    private void fetchCurrentLocation() {
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                this.currentLocation = location;
                locationInput.setText(String.format(Locale.US, "Vị trí hiện tại (%.4f, %.4f)", location.getLatitude(), location.getLongitude()));
                Toast.makeText(requireContext(), "Đã cập nhật vị trí.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), "Không thể lấy vị trí. Vui lòng bật GPS.", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void onProductClick(SearchResult result) {
        Toast.makeText(requireContext(), "Xem: " + result.getTitle(), Toast.LENGTH_SHORT).show();
        // TODO: Điều hướng đến trang chi tiết sản phẩm
    }

    private void onFavoriteClick(SearchResult result, boolean isFavorite) {
        if (FirebaseAuth.getInstance().getUid() == null) {
            Toast.makeText(requireContext(), "Vui lòng đăng nhập để yêu thích.", Toast.LENGTH_SHORT).show();
            // Yêu cầu adapter vẽ lại item này để trả lại trạng thái cũ
            int position = searchResultsAdapter.getResults().indexOf(result);
            if (position != -1) {
                result.setFavorite(!isFavorite); // Đảo ngược lại
                searchResultsAdapter.notifyItemChanged(position);
            }
            return;
        }
        // Gọi ViewModel để xử lý logic
        viewModel.toggleFavorite(result.getId(), isFavorite);
    }

    // Thu thập dữ liệu từ các trường filter trên UI
    private SearchParams collectSearchParamsFromUi() {
        SearchParams params = new SearchParams();
        params.setQuery(searchInput.getText().toString().trim());

        // Lấy giá trị từ các trường lọc và set vào params
        String category = categoryFilter.getText().toString();
        if (!category.isEmpty() && !category.equals("Tất cả")) {
            params.setCategory(category);
        }

        try {
            double minPrice = Double.parseDouble(minPriceInput.getText().toString());
            params.setMinPrice(minPrice);
        } catch (NumberFormatException e) { /* Bỏ qua */ }

        try {
            double maxPrice = Double.parseDouble(maxPriceInput.getText().toString());
            params.setMaxPrice(maxPrice);
        } catch (NumberFormatException e) { /* Bỏ qua */ }

        String condition = conditionFilter.getText().toString();
        // Ánh xạ từ text trên UI sang giá trị lưu trong DB (ví dụ: "Mới" -> "new")
        if (!condition.isEmpty() && !condition.equals("Tất cả")) {
            params.setCondition(mapConditionToValue(condition));
        }

        // Trong phương thức collectSearchParamsFromUi()
        String selectedSort = sortFilter.getText().toString();

        // Dòng này sẽ không còn báo lỗi
        String[] sortOptions = getResources().getStringArray(R.array.sort_options);

        if (selectedSort.equals(sortOptions[1])) { // Mới nhất
            params.setSortBy("timePosted");
            params.setSortAscending(false);
        } else if (selectedSort.equals(sortOptions[2])) { // Giá: Thấp đến cao
            params.setSortBy("price");
            params.setSortAscending(true);
        } else if (selectedSort.equals(sortOptions[3])) { // Giá: Cao đến thấp
            params.setSortBy("price");
            params.setSortAscending(false);
        }

        // Xử lý logic cho Vị trí và Khoảng cách
        if (currentLocation != null) {
            params.setUserLocation(currentLocation);
            params.setMaxDistance(distanceSeekbar.getProgress());
        } else {
            params.setLocation(locationInput.getText().toString().trim());
        }

        return params;
    }

    private String mapConditionToValue(String conditionText) {
        switch (conditionText) {
            case "Mới": return "new";
            case "Như mới": return "like_new";
            case "Tốt": return "good";
            case "Đã dùng": return "used";
            default: return null;
        }
    }

    // Xóa trắng các trường input trong bộ lọc
    private void clearFilterInputs() {
        categoryFilter.setText("", false);
        minPriceInput.setText("");
        maxPriceInput.setText("");
        conditionFilter.setText("", false);
        sortFilter.setText("", false);
        locationInput.setText("");
        distanceSeekbar.setProgress(distanceSeekbar.getMax() / 2); // Reset về giữa
        currentLocation = null;
    }
}