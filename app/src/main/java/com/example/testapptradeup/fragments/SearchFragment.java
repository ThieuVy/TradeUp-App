// File: com/example/testapptradeup/fragments/SearchFragment.java
package com.example.testapptradeup.fragments;

import android.annotation.SuppressLint;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
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
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.testapptradeup.R;
import com.example.testapptradeup.adapters.SearchResultsAdapter;
import com.example.testapptradeup.models.Listing; // SỬ DỤNG LISTING.JAVA
import com.example.testapptradeup.models.SearchParams;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class SearchFragment extends Fragment {

    private static final String TAG = "SearchFragment";
    private static final int DEBOUNCE_DELAY_MS = 500;
    private static final long ITEMS_PER_PAGE = 15; // Phân trang

    // UI Components
    private EditText searchInput;
    private ImageButton clearSearch, filterToggle;
    private CardView filtersCard, sortHeaderCard;
    private AutoCompleteTextView categoryFilter, conditionFilter, sortFilter;
    private EditText minPriceInput, maxPriceInput, locationInput;
    private SeekBar distanceSeekbar;
    private TextView distanceValue, resultsCount;
    private Button useGpsButton, clearFiltersButton, loadMoreButton, resetSearchButton;
    private RecyclerView searchResultsRecyclerView;
    private LinearLayout emptyState, loadingState;
    private ProgressBar loadMoreProgress;

    // Adapters and data
    private SearchResultsAdapter searchResultsAdapter;
    private List<Listing> listingResults; // Đổi từ SearchResult sang Listing
    private ArrayAdapter<String> categoryAdapter, conditionAdapter, sortAdapter;

    // Search
    private SearchParams currentSearchParams;
    private boolean isFiltersVisible = false;
    private boolean isLoading = false;
    private boolean isLastPage = false;
    private QueryDocumentSnapshot lastVisibleDocument; // Dùng để phân trang

    // Location
    private FusedLocationProviderClient fusedLocationClient;
    private Location currentLocation;

    // Debouncing
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    // Permission launcher
    private ActivityResultLauncher<String> locationPermissionLauncher;

    // Firebase
    private FirebaseFirestore db;
    private CollectionReference listingsRef;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        listingResults = new ArrayList<>();
        currentSearchParams = new SearchParams();

        // ** THAY ĐỔI: KHỞI TẠO FIRESTORE **
        db = FirebaseFirestore.getInstance();
        listingsRef = db.collection("listings"); // THAY ĐỔI: "products" -> "listings"

        locationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) fetchCurrentLocation();
                    else Toast.makeText(requireContext(), "Quyền truy cập vị trí bị từ chối", Toast.LENGTH_SHORT).show();
                }
        );

        if (getArguments() != null) {
            SearchParams params = getArguments().getParcelable("searchParams");
            if (params != null) currentSearchParams = new SearchParams(params);
            isFiltersVisible = getArguments().getBoolean("showFilters", false);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search, container, false);
        initViews(view);
        setupAdapters();
        setupListeners();
        updateFiltersVisibility();
        // Bắt đầu tìm kiếm lần đầu
        startNewSearch();
        return view;
    }

    private void initViews(View view) {
        // (Giữ nguyên phần này)
        searchInput = view.findViewById(R.id.search_input);
        clearSearch = view.findViewById(R.id.clear_search);
        filterToggle = view.findViewById(R.id.filter_toggle);
        filtersCard = view.findViewById(R.id.filters_card);
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
        resultsCount = view.findViewById(R.id.results_count);
        sortFilter = view.findViewById(R.id.sort_filter);
        searchResultsRecyclerView = view.findViewById(R.id.search_results_recycler_view);
        emptyState = view.findViewById(R.id.empty_state);
        loadingState = view.findViewById(R.id.loading_state);
        loadMoreButton = view.findViewById(R.id.load_more_button);
        loadMoreProgress = view.findViewById(R.id.load_more_progress);
        resetSearchButton = view.findViewById(R.id.reset_search_button);
    }

    private void setupAdapters() {
        // (Giữ nguyên phần này, nhưng adapter của bạn cần được cập nhật để nhận `List<Listing>`)
        String[] categories = {"Điện thoại & Phụ kiện", "Máy tính & Laptop", "Thời trang Nam", "Thời trang Nữ", "Khác"};
        categoryAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, categories);
        categoryFilter.setAdapter(categoryAdapter);

        String[] conditions = {"Mới", "Như mới", "Đã sử dụng - Tốt", "Đã sử dụng - Khá"};
        conditionAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, conditions);
        conditionFilter.setAdapter(conditionAdapter);

        String[] sortOptions = {"Liên quan nhất", "Giá thấp đến cao", "Giá cao đến thấp", "Mới nhất"};
        sortAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, sortOptions);
        sortFilter.setAdapter(sortAdapter);

        // ** QUAN TRỌNG: Cập nhật SearchResultsAdapter của bạn để nhận List<Listing> thay vì List<SearchResult> **
        searchResultsAdapter = new SearchResultsAdapter(listingResults,
                listing -> Toast.makeText(requireContext(), "Xem: " + listing.getTitle(), Toast.LENGTH_SHORT).show(),
                (listing, isFavorite) -> Toast.makeText(requireContext(), isFavorite ? "Yêu thích" : "Bỏ yêu thích", Toast.LENGTH_SHORT).show()
        );
        searchResultsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        searchResultsRecyclerView.setAdapter(searchResultsAdapter);
    }

    private void setupListeners() {
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                clearSearch.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
                handler.removeCallbacks(searchRunnable);
                searchRunnable = () -> {
                    currentSearchParams.setQuery(s.toString().trim());
                    startNewSearch();
                };
                handler.postDelayed(searchRunnable, DEBOUNCE_DELAY_MS);
            }
        });
        clearSearch.setOnClickListener(v -> searchInput.setText(""));
        filterToggle.setOnClickListener(v -> {
            isFiltersVisible = !isFiltersVisible;
            updateFiltersVisibility();
        });
        clearFiltersButton.setOnClickListener(v -> {
            currentSearchParams.clearFilters();
            updateFilterUI();
            startNewSearch();
        });
        resetSearchButton.setOnClickListener(v -> {
            currentSearchParams.reset();
            searchInput.setText("");
            updateFilterUI();
            startNewSearch();
        });
        loadMoreButton.setOnClickListener(v -> performSearch(false)); // false = không phải tìm kiếm mới
        // (Thêm các listener khác cho filter, sort tương tự như code cũ của bạn, nhưng gọi startNewSearch())
    }

    private void updateFilterUI() { /* Giữ nguyên code cũ */ }
    private void updateFiltersVisibility() { /* Giữ nguyên code cũ */ }

    // Bắt đầu một truy vấn mới, reset phân trang
    private void startNewSearch() {
        isLastPage = false;
        lastVisibleDocument = null;
        listingResults.clear();
        searchResultsAdapter.notifyDataSetChanged();
        performSearch(true);
    }

    @SuppressLint("StringFormatInvalid")
    private void performSearch(final boolean isNewSearch) {
        if (isLoading) return;
        isLoading = true;

        if (isNewSearch) {
            loadingState.setVisibility(View.VISIBLE);
            emptyState.setVisibility(View.GONE);
            searchResultsRecyclerView.setVisibility(View.GONE);
        } else {
            loadMoreProgress.setVisibility(View.VISIBLE);
        }
        loadMoreButton.setVisibility(View.GONE);

        // 1. Xây dựng Query động cho Firestore
        Query query = buildFirestoreQuery();

        // 2. Thực thi Query
        query.get().addOnSuccessListener(queryDocumentSnapshots -> {
            if (queryDocumentSnapshots.isEmpty()) {
                isLastPage = true;
                if (isNewSearch) {
                    emptyState.setVisibility(View.VISIBLE);
                    resultsCount.setText(getString(R.string.results_count, 0));
                }
            } else {
                // Lấy document cuối cùng cho lần load sau
                lastVisibleDocument = (QueryDocumentSnapshot) queryDocumentSnapshots.getDocuments().get(queryDocumentSnapshots.size() - 1);

                List<Listing> newListings = new ArrayList<>();
                for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                    newListings.add(doc.toObject(Listing.class));
                }

                // ** LƯU Ý: Phần filter theo khoảng cách (distance) phải làm ở client-side **
                // Firestore không hỗ trợ query vị trí địa lý và các filter khác cùng lúc.
                // Nếu người dùng có filter khoảng cách, bạn phải lọc `newListings` ở đây.

                if (isNewSearch) {
                    listingResults.clear();
                }
                listingResults.addAll(newListings);
                searchResultsAdapter.notifyDataSetChanged();

                if (queryDocumentSnapshots.size() < ITEMS_PER_PAGE) {
                    isLastPage = true;
                }
            }

            // 3. Cập nhật UI
            updateUiAfterSearch();
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error fetching listings", e);
            Toast.makeText(getContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            isLoading = false;
            loadingState.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);
        });
    }

    private Query buildFirestoreQuery() {
        Query q = listingsRef.whereEqualTo("status", "available")
                .whereEqualTo("isSold", false);

        // Filter
        if (currentSearchParams.getCategory() != null && !currentSearchParams.getCategory().isEmpty()) {
            q = q.whereEqualTo("categoryId", currentSearchParams.getCategory());
        }
        if (currentSearchParams.getCondition() != null && !currentSearchParams.getCondition().isEmpty()) {
            q = q.whereEqualTo("condition", currentSearchParams.getCondition());
        }
        if (currentSearchParams.getMinPrice() != null) {
            q = q.whereGreaterThanOrEqualTo("price", currentSearchParams.getMinPrice());
        }
        if (currentSearchParams.getMaxPrice() != null) {
            q = q.whereLessThanOrEqualTo("price", currentSearchParams.getMaxPrice());
        }

        // Search Query (dạng prefix search)
        // Firestore không hỗ trợ "contains", đây là cách làm đơn giản nhất
        if (currentSearchParams.getQuery() != null && !currentSearchParams.getQuery().isEmpty()) {
            String keyword = currentSearchParams.getQuery();
            q = q.orderBy("title").startAt(keyword).endAt(keyword + '\uf8ff');
        }

        // Sort
        switch (currentSearchParams.getSortBy()) {
            case 1: // Giá thấp đến cao
                q = q.orderBy("price", Query.Direction.ASCENDING);
                break;
            case 2: // Giá cao đến thấp
                q = q.orderBy("price", Query.Direction.DESCENDING);
                break;
            case 3: // Mới nhất
                // orderBy theo title đã được thêm ở trên cho tìm kiếm, không cần thêm nữa
                if (currentSearchParams.getQuery() == null || currentSearchParams.getQuery().isEmpty()) {
                    q = q.orderBy("timePosted", Query.Direction.DESCENDING);
                }
                break;
            default: // Liên quan nhất (Mặc định sort theo thời gian)
                if (currentSearchParams.getQuery() == null || currentSearchParams.getQuery().isEmpty()) {
                    q = q.orderBy("timePosted", Query.Direction.DESCENDING);
                }
                break;
        }

        // Pagination
        if (lastVisibleDocument != null) {
            q = q.startAfter(lastVisibleDocument);
        }
        return q.limit(ITEMS_PER_PAGE);
    }

    @SuppressLint("StringFormatInvalid")
    private void updateUiAfterSearch() {
        isLoading = false;
        loadingState.setVisibility(View.GONE);
        loadMoreProgress.setVisibility(View.GONE);
        searchResultsRecyclerView.setVisibility(listingResults.isEmpty() ? View.GONE : View.VISIBLE);
        emptyState.setVisibility(listingResults.isEmpty() ? View.VISIBLE : View.GONE);
        loadMoreButton.setVisibility(!isLastPage && !listingResults.isEmpty() ? View.VISIBLE : View.GONE);
        // Cần cách để lấy tổng số kết quả, nhưng Firestore ko hỗ trợ. Có thể hiển thị số lượng đã tải.
        resultsCount.setText(getString(R.string.results_count, listingResults.size()));
    }

    @SuppressLint({"MissingPermission", "SetTextI18n"})
    private void fetchCurrentLocation() {
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                currentLocation = location;
                locationInput.setText("Vị trí hiện tại");
                // TODO: Re-trigger search if distance filter is active
                startNewSearch();
            } else {
                Toast.makeText(requireContext(), "Không thể lấy vị trí", Toast.LENGTH_SHORT).show();
            }
        });
    }
}