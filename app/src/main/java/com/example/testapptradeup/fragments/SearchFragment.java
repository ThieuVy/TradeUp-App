package com.example.testapptradeup.fragments;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
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
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.testapptradeup.R;
import com.example.testapptradeup.adapters.SearchResultsAdapter;
import com.example.testapptradeup.models.Category;
import com.example.testapptradeup.models.SearchParams;
import com.example.testapptradeup.models.SearchResult;
import com.example.testapptradeup.viewmodels.SearchViewModel;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.Locale;

/**
 * Fragment chịu trách nhiệm cho chức năng tìm kiếm, lọc và hiển thị kết quả.
 * Sử dụng SearchViewModel để quản lý logic và trạng thái tìm kiếm.
 */
public class SearchFragment extends Fragment implements SearchResultsAdapter.OnProductClickListener, SearchResultsAdapter.OnFavoriteClickListener {

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;
    private static final int DEBOUNCE_DELAY_MS = 300; // 300ms delay
    private SearchViewModel viewModel;
    private NavController navController;

    // UI Components
    private EditText searchInput;
    private ImageButton clearSearch, filterToggle, btnBackSearch;
    private ScrollView filtersContainer;
    private LinearLayout sortHeaderCard, emptyState, loadingState;
    private AutoCompleteTextView categoryFilter, conditionFilter, sortFilter;
    private EditText minPriceInput, maxPriceInput, locationInput;
    private SeekBar distanceSeekbar;
    private TextView distanceValue, resultsCount;
    private Button useGpsButton, clearFiltersButton, applyFiltersButton, loadMoreButton;
    private RecyclerView searchResultsRecyclerView;
    private ProgressBar loadMoreProgress;
    private SearchResultsAdapter searchResultsAdapter;

    // Location & Utils
    private FusedLocationProviderClient fusedLocationClient;
    private Location currentLocation;
    private ActivityResultLauncher<String> locationPermissionLauncher;
    private boolean isFiltersVisible = false;
    private TextWatcher searchInputWatcher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(SearchViewModel.class);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        // Khởi tạo launcher để yêu cầu quyền truy cập vị trí
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
        return inflater.inflate(R.layout.fragment_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(view);

        initViews(view);
        setupAdaptersAndListeners();
        observeViewModel();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Hủy bỏ mọi runnable đang chờ để tránh memory leak
        if (searchRunnable != null) {
            handler.removeCallbacks(searchRunnable);
        }
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
        btnBackSearch = view.findViewById(R.id.btn_back_search);
    }

    private void setupAdaptersAndListeners() {
        // Setup RecyclerView
        searchResultsAdapter = new SearchResultsAdapter(this, this);
        searchResultsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        searchResultsRecyclerView.setAdapter(searchResultsAdapter);

        // Setup Adapters cho các ô AutoComplete
        if (getContext() != null) {
            ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_dropdown_item_1line, getResources().getStringArray(R.array.category_options_display));
            categoryFilter.setAdapter(categoryAdapter);

            ArrayAdapter<String> conditionAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_dropdown_item_1line, getResources().getStringArray(R.array.condition_options_display));
            conditionFilter.setAdapter(conditionAdapter);

            ArrayAdapter<String> sortAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_dropdown_item_1line, getResources().getStringArray(R.array.sort_options_display));
            sortFilter.setAdapter(sortAdapter);
        }

        // Setup Listeners
        searchInputWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (searchRunnable != null) {
                    handler.removeCallbacks(searchRunnable);
                }
                // Hiển thị/ẩn nút clear
                clearSearch.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
            }

            @Override
            public void afterTextChanged(Editable s) {
                searchRunnable = () -> {
                    if (isAdded()) {
                        viewModel.startNewSearch(collectSearchParamsFromUi());
                    }
                };
                handler.postDelayed(searchRunnable, DEBOUNCE_DELAY_MS);
            }
        };
        searchInput.addTextChangedListener(searchInputWatcher);
        clearSearch.setOnClickListener(v -> searchInput.setText(""));
        btnBackSearch.setOnClickListener(v -> navController.popBackStack());
        applyFiltersButton.setOnClickListener(v -> {
            viewModel.startNewSearch(collectSearchParamsFromUi());
            toggleFilterVisibility(false);
        });
        clearFiltersButton.setOnClickListener(v -> {
            clearFilterInputs();
            viewModel.startNewSearch(new SearchParams()); // Thực hiện tìm kiếm lại với tham số rỗng
            toggleFilterVisibility(false);
        });
        filterToggle.setOnClickListener(v -> toggleFilterVisibility(!isFiltersVisible));
        useGpsButton.setOnClickListener(v -> checkLocationPermissionAndFetch());
        loadMoreButton.setOnClickListener(v -> viewModel.loadMore());
        sortFilter.setOnItemClickListener((parent, view, position, id) -> viewModel.startNewSearch(collectSearchParamsFromUi()));

        distanceSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @SuppressLint("DefaultLocale")
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                distanceValue.setText(getString(R.string.search_distance_in_km, progress));
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void observeViewModel() {
        viewModel.getSearchResults().observe(getViewLifecycleOwner(), results -> {
            searchResultsAdapter.submitList(new ArrayList<>(results)); // Luôn tạo list mới cho ListAdapter
            resultsCount.setText(getString(R.string.search_results_count, results.size()));
        });

        viewModel.getUiState().observe(getViewLifecycleOwner(), this::updateUiForState);
        viewModel.getSearchParams().observe(getViewLifecycleOwner(), this::updateUiFromParams);
    }

    @SuppressLint("SetTextI18n")
    private void updateUiFromParams(SearchParams params) {
        if (params == null || getContext() == null) return;

        // --- PHÁ VỠ VÒNG LẶP CHO Ô TÌM KIẾM ---
        // Chỉ cập nhật text nếu nó khác với những gì đang hiển thị
        if (!searchInput.getText().toString().equals(params.getQuery())) {
            // Tạm thời gỡ bỏ watcher để tránh nó bị kích hoạt lại
            searchInput.removeTextChangedListener(searchInputWatcher);
            searchInput.setText(params.getQuery());
            // Gắn lại watcher sau khi đã set text xong
            searchInput.addTextChangedListener(searchInputWatcher);
        }

        // --- LÀM TƯƠNG TỰ CHO CÁC TRƯỜNG LỌC KHÁC ---

        String categoryText = params.getCategory() != null ? params.getCategory() : getString(R.string.search_category_all);
        if (!categoryFilter.getText().toString().equals(categoryText)) {
            categoryFilter.setText(categoryText, false);
        }

        String minPriceText = params.getMinPrice() != null ? String.valueOf(params.getMinPrice().intValue()) : "";
        if (!minPriceInput.getText().toString().equals(minPriceText)) {
            minPriceInput.setText(minPriceText);
        }

        String maxPriceText = params.getMaxPrice() != null ? String.valueOf(params.getMaxPrice().intValue()) : "";
        if (!maxPriceInput.getText().toString().equals(maxPriceText)) {
            maxPriceInput.setText(maxPriceText);
        }

        // (Bạn có thể thêm các kiểm tra tương tự cho conditionFilter và sortFilter nếu cần)
        String conditionText = mapValueToDisplayText(params.getCondition(), R.array.condition_options_display, R.array.condition_options_values);
        if (!conditionFilter.getText().toString().equals(conditionText)) {
            conditionFilter.setText(conditionText, false);
        }

        // --- SỬA DÒNG NÀY ---
        String sortText = mapValueToDisplayText(params.getSortBy(), R.array.sort_options_display, R.array.sort_options_values);
        if (!sortFilter.getText().toString().equals(sortText)) {
            sortFilter.setText(sortText, false);
        }
    }

    private void updateUiForState(SearchViewModel.UiState state) {
        if (isFiltersVisible) {
            loadingState.setVisibility(View.GONE);
            emptyState.setVisibility(View.GONE);
            searchResultsRecyclerView.setVisibility(View.GONE);
            return;
        }

        loadingState.setVisibility(state == SearchViewModel.UiState.LOADING ? View.VISIBLE : View.GONE);
        emptyState.setVisibility(state == SearchViewModel.UiState.EMPTY ? View.VISIBLE : View.GONE);
        searchResultsRecyclerView.setVisibility(state == SearchViewModel.UiState.SUCCESS || state == SearchViewModel.UiState.LOADING_MORE ? View.VISIBLE : View.GONE);
        loadMoreProgress.setVisibility(state == SearchViewModel.UiState.LOADING_MORE ? View.VISIBLE : View.GONE);
        loadMoreButton.setVisibility(state == SearchViewModel.UiState.SUCCESS && !viewModel.isLastPage() ? View.VISIBLE : View.GONE);

        if (state == SearchViewModel.UiState.ERROR) {
            Toast.makeText(getContext(), "Đã xảy ra lỗi, vui lòng thử lại.", Toast.LENGTH_SHORT).show();
            emptyState.setVisibility(View.VISIBLE);
        }
    }

    private SearchParams collectSearchParamsFromUi() {
        SearchParams params = new SearchParams();
        params.setQuery(searchInput.getText().toString().trim());

        // --- SỬA LỖI CHO DANH MỤC ---
        String selectedCategoryName = categoryFilter.getText().toString();
        // Chỉ áp dụng bộ lọc nếu người dùng không chọn "Tất cả danh mục"
        if (!selectedCategoryName.equals(getString(R.string.search_category_all)) && !selectedCategoryName.isEmpty()) {
            // "Phiên dịch" tên tiếng Việt thành ID hệ thống (ví dụ: "Đồ điện tử" -> "electronics")
            String categoryId = Category.getCategoryIdByName(selectedCategoryName);
            params.setCategory(categoryId);
        }

        // Xử lý giá (giữ nguyên)
        try {
            if (!TextUtils.isEmpty(minPriceInput.getText())) params.setMinPrice(Double.parseDouble(minPriceInput.getText().toString()));
            if (!TextUtils.isEmpty(maxPriceInput.getText())) params.setMaxPrice(Double.parseDouble(maxPriceInput.getText().toString()));
        } catch (NumberFormatException e) {
            Log.e("SearchFragment", "Invalid price format", e);
        }

        // --- SỬA LỖI CHO TÌNH TRẠNG ---
        String selectedConditionName = conditionFilter.getText().toString();
        if (!selectedConditionName.equals(getString(R.string.search_condition_all)) && !selectedConditionName.isEmpty()) {
            // "Phiên dịch" tên tiếng Việt thành ID hệ thống (ví dụ: "Như mới" -> "like_new")
            String conditionValue = mapDisplayToValue(selectedConditionName, R.array.condition_options_display, R.array.condition_options_values);
            params.setCondition(conditionValue);
        }

        // --- SỬA LỖI CHO SẮP XẾP ---
        String selectedSortName = sortFilter.getText().toString();
        if (!selectedSortName.isEmpty()) {
            String sortByValue = mapDisplayToValue(selectedSortName, R.array.sort_options_display, R.array.sort_options_values);
            // Logic sắp xếp cần tách trường và hướng
            if (sortByValue != null) {
                switch (sortByValue) {
                    case "price_asc":
                        params.setSortBy("price");
                        params.setSortAscending(true);
                        break;
                    case "price_desc":
                        params.setSortBy("price");
                        params.setSortAscending(false);
                        break;
                    case "time_desc":
                    default:
                        params.setSortBy("timePosted");
                        params.setSortAscending(false);
                        break;
                }
            }
        }


        if (currentLocation != null) {
            params.setUserLocation(currentLocation);
            params.setMaxDistance(distanceSeekbar.getProgress());
        } else {
            params.setLocation(locationInput.getText().toString().trim());
        }

        return params;
    }

    private void clearFilterInputs() {
        categoryFilter.setText(getString(R.string.search_category_all), false);
        minPriceInput.setText("");
        maxPriceInput.setText("");
        conditionFilter.setText(getString(R.string.search_condition_all), false);
        sortFilter.setText(getString(R.string.search_sort_relevance), false);
        locationInput.setText("");
        distanceSeekbar.setProgress(50);
        currentLocation = null;
    }

    private void toggleFilterVisibility(boolean show) {
        isFiltersVisible = show;
        filtersContainer.setVisibility(show ? View.VISIBLE : View.GONE);
        // Khi bộ lọc hiện, các thành phần khác phải ẩn
        sortHeaderCard.setVisibility(show ? View.GONE : View.VISIBLE);
        searchResultsRecyclerView.setVisibility(show ? View.GONE : View.VISIBLE);

        if (show) {
            emptyState.setVisibility(View.GONE);
            loadingState.setVisibility(View.GONE);
        } else {
            // Khi ẩn bộ lọc, cập nhật lại trạng thái giao diện chính
            if (viewModel.getUiState().getValue() != null) {
                updateUiForState(viewModel.getUiState().getValue());
            }
        }
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
                locationInput.setText(String.format(Locale.US, "Vị trí GPS (%.4f, %.4f)", location.getLatitude(), location.getLongitude()));
                Toast.makeText(requireContext(), "Đã cập nhật vị trí.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), "Không thể lấy vị trí. Vui lòng bật GPS.", Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onProductClick(SearchResult result) {
        if (result == null || result.getId() == null) {
            Toast.makeText(getContext(), "Không thể mở sản phẩm này.", Toast.LENGTH_SHORT).show();
            return;
        }
        SearchFragmentDirections.ActionSearchFragmentToProductDetailFragment action =
                SearchFragmentDirections.actionSearchFragmentToProductDetailFragment(result.getId());
        Navigation.findNavController(requireView()).navigate(action);
    }

    @Override
    public void onFavoriteClick(SearchResult result, int position) {
        if (FirebaseAuth.getInstance().getUid() == null) {
            Toast.makeText(requireContext(), "Vui lòng đăng nhập để yêu thích.", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean newFavoriteState = !result.isFavorite();
        // Cập nhật ngay trên UI để người dùng thấy phản hồi
        result.setFavorite(newFavoriteState);
        searchResultsAdapter.notifyItemChanged(position);

        // Gọi ViewModel để xử lý logic ở backend
        viewModel.toggleFavorite(result.getId(), newFavoriteState);
    }

    // Hàm tiện ích để ánh xạ giữa giá trị lưu trữ và giá trị hiển thị
    private String mapDisplayToValue(String displayText, int displayArrayResId, int valueArrayResId) {
        String[] displayTexts = getResources().getStringArray(displayArrayResId);
        String[] values = getResources().getStringArray(valueArrayResId);
        for (int i = 0; i < displayTexts.length; i++) {
            if (displayTexts[i].equals(displayText)) {
                // Đảm bảo không truy cập ngoài phạm vi của mảng values
                if (i < values.length) {
                    return values[i];
                }
            }
        }
        return null; // Trả về null nếu không tìm thấy
    }

    private String mapValueToDisplayText(String value, int displayArrayResId, int valueArrayResId) {
        String[] displayTexts = getResources().getStringArray(displayArrayResId);
        String[] values = getResources().getStringArray(valueArrayResId);
        if (value == null || value.isEmpty()) {
            return displayTexts.length > 0 ? displayTexts[0] : ""; // Trả về giá trị đầu tiên (vd: "Mọi tình trạng")
        }
        for (int i = 0; i < values.length; i++) {
            if (values[i].equals(value)) {
                if (i < displayTexts.length) {
                    return displayTexts[i];
                }
            }
        }
        return displayTexts.length > 0 ? displayTexts[0] : ""; // Mặc định nếu không tìm thấy
    }
}