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
import androidx.navigation.Navigation;
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

public class SearchFragment extends Fragment implements SearchResultsAdapter.OnProductClickListener, SearchResultsAdapter.OnFavoriteClickListener {

    private static final int DEBOUNCE_DELAY_MS = 300; // Độ trễ 300ms
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    private SearchViewModel viewModel;

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

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(SearchViewModel.class);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        locationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) fetchCurrentLocation();
                    else Toast.makeText(requireContext(), "Quyền vị trí bị từ chối.", Toast.LENGTH_SHORT).show();
                }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search, container, false);
        initViews(view);
        setupAdaptersAndListeners(); // Gộp 2 hàm
        observeViewModel();
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
        btnBackSearch = view.findViewById(R.id.btn_back_search);
    }

    private void setupAdaptersAndListeners() {
        // Setup RecyclerView Adapter
        searchResultsAdapter = new SearchResultsAdapter(this, this);
        searchResultsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        searchResultsRecyclerView.setAdapter(searchResultsAdapter);

        // Setup Adapters for AutoCompleteTextViews
        if (getContext() != null) {
            ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_dropdown_item_1line, getResources().getStringArray(R.array.category_options));
            categoryFilter.setAdapter(categoryAdapter);

            ArrayAdapter<String> conditionAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_dropdown_item_1line, getResources().getStringArray(R.array.condition_options));
            conditionFilter.setAdapter(conditionAdapter);

            ArrayAdapter<String> sortAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_dropdown_item_1line, getResources().getStringArray(R.array.sort_options));
            sortFilter.setAdapter(sortAdapter);
        }

        // Setup Listeners
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Hủy bỏ runnable cũ mỗi khi người dùng gõ thêm
                if (searchRunnable != null) {
                    handler.removeCallbacks(searchRunnable);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Tạo runnable mới để tìm kiếm sau một khoảng trễ
                searchRunnable = () -> {
                    if (isAdded()) { // Đảm bảo fragment vẫn đang attached
                        viewModel.startNewSearch(collectSearchParamsFromUi());
                    }
                };
                handler.postDelayed(searchRunnable, DEBOUNCE_DELAY_MS);
            }
        });

        clearSearch.setOnClickListener(v -> searchInput.setText(""));
        applyFiltersButton.setOnClickListener(v -> {
            viewModel.startNewSearch(collectSearchParamsFromUi());
            toggleFilterVisibility(false);
        });
        sortFilter.setOnItemClickListener((parent, view, position, id) -> viewModel.startNewSearch(collectSearchParamsFromUi()));
        clearFiltersButton.setOnClickListener(v -> {
            clearFilterInputs();
            viewModel.startNewSearch(new SearchParams());
            toggleFilterVisibility(false);
        });
        filterToggle.setOnClickListener(v -> toggleFilterVisibility(!isFiltersVisible));
        useGpsButton.setOnClickListener(v -> checkLocationPermissionAndFetch());
        distanceSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @SuppressLint("DefaultLocale")
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                distanceValue.setText(getString(R.string.search_distance_in_km, progress));
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        btnBackSearch.setOnClickListener(v -> Navigation.findNavController(v).popBackStack());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Rất quan trọng: Hủy bỏ runnable khi fragment bị hủy để tránh memory leak
        if (searchRunnable != null) {
            handler.removeCallbacks(searchRunnable);
        }
    }

    private void observeViewModel() {
        viewModel.getSearchResults().observe(getViewLifecycleOwner(), results -> {
            searchResultsAdapter.submitList(new ArrayList<>(results)); // Truyền vào một bản sao mới của list
            resultsCount.setText(getString(R.string.search_results_count, results.size()));
        });

        viewModel.getUiState().observe(getViewLifecycleOwner(), this::updateUiState);
        viewModel.getSearchParams().observe(getViewLifecycleOwner(), this::updateUiFromParams);
    }

    @SuppressLint("SetTextI18n")
    private void updateUiFromParams(SearchParams params) {
        if (params == null) return;
        searchInput.setText(params.getQuery());
        // Hiển thị "Tất cả danh mục" nếu category là null
        categoryFilter.setText(params.getCategory() != null ? params.getCategory() : getString(R.string.search_category_all), false);
        minPriceInput.setText(params.getMinPrice() != null ? String.valueOf(params.getMinPrice()) : "");
        maxPriceInput.setText(params.getMaxPrice() != null ? String.valueOf(params.getMaxPrice()) : "");
        conditionFilter.setText(mapValueToCondition(params.getCondition()), false);
        // Cập nhật các UI khác nếu cần...
    }

    private void updateUiState(SearchViewModel.UiState state) {
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
    }

    private SearchParams collectSearchParamsFromUi() {
        SearchParams params = new SearchParams();
        params.setQuery(searchInput.getText().toString().trim());

        String category = categoryFilter.getText().toString();
        if (!category.equals(getString(R.string.search_category_all))) {
            params.setCategory(category);
        }

        try {
            if (!TextUtils.isEmpty(minPriceInput.getText())) {
                params.setMinPrice(Double.parseDouble(minPriceInput.getText().toString()));
            }
            if (!TextUtils.isEmpty(maxPriceInput.getText())) {
                params.setMaxPrice(Double.parseDouble(maxPriceInput.getText().toString()));
            }
        } catch (NumberFormatException e) {
            Log.e("SearchFragment", "Invalid price format", e);
        }

        String condition = conditionFilter.getText().toString();
        // SỬA LỖI: So sánh với R.string
        if (!condition.equals(getString(R.string.search_condition_all))) {
            params.setCondition(mapConditionToValue(condition));
        }

        String[] sortOptions = getResources().getStringArray(R.array.sort_options);
        String selectedSort = sortFilter.getText().toString();
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
        conditionFilter.setText(getString(R.string.search_category_all), false);
        sortFilter.setText(getString(R.string.search_category_all), false);
        locationInput.setText("");
        distanceSeekbar.setProgress(50);
        currentLocation = null;
    }
    private void toggleFilterVisibility(boolean show) {
        isFiltersVisible = show;
        filtersContainer.setVisibility(show ? View.VISIBLE : View.GONE);
        sortHeaderCard.setVisibility(show ? View.GONE : View.VISIBLE);
        searchResultsRecyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
        if (show) {
            emptyState.setVisibility(View.GONE);
            loadingState.setVisibility(View.GONE);
        } else {
            if(viewModel.getUiState().getValue() != null) {
                updateUiState(viewModel.getUiState().getValue());
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
            boolean isCurrentlyFavorite = result.isFavorite();
            result.setFavorite(!isCurrentlyFavorite);
            searchResultsAdapter.notifyItemChanged(position);
            return;
        }

        boolean newFavoriteState = !result.isFavorite();
        result.setFavorite(newFavoriteState);
        searchResultsAdapter.notifyItemChanged(position);

        viewModel.toggleFavorite(result.getId(), newFavoriteState);
    }
    private String mapConditionToValue(String conditionText) {
        String[] conditions = getResources().getStringArray(R.array.condition_options);
        if (conditionText.equals(conditions[1])) return "new";
        if (conditionText.equals(conditions[2])) return "like_new";
        if (conditionText.equals(conditions[3])) return "good";
        if (conditionText.equals(conditions[4])) return "used";
        return null;
    }

    private String mapValueToCondition(String value) {
        if (value == null) return getString(R.string.search_category_all);
        String[] conditions = getResources().getStringArray(R.array.condition_options);
        switch (value) {
            case "new":
                return conditions[1];
            case "like_new":
                return conditions[2];
            case "good":
                return conditions[3];
            case "used":
                return conditions[4];
            default:
                return getString(R.string.search_category_all);
        }
    }
}