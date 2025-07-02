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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.testapptradeup.R;
import com.example.testapptradeup.adapters.SearchResultsAdapter;
import com.example.testapptradeup.models.Listing;
import com.example.testapptradeup.models.SearchParams;
import com.example.testapptradeup.models.SearchResult;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SearchFragment extends Fragment {

    private static final String TAG = "SearchFragment";
    private static final int DEBOUNCE_DELAY_MS = 500;
    private static final long ITEMS_PER_PAGE = 15;

    // UI Components
    private EditText searchInput;
    private ImageButton clearSearch, filterToggle;
    private ScrollView filtersContainer; // Sửa thành ScrollView
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

    // Adapters and data
    private SearchResultsAdapter searchResultsAdapter;
    private List<SearchResult> searchResultList;
    private List<String> userFavoriteIds = new ArrayList<>();

    // Search
    private SearchParams currentSearchParams;
    private boolean isFiltersVisible = false;
    private boolean isLoading = false;
    private boolean isLastPage = false;
    private DocumentSnapshot lastVisibleDocument;

    // Location
    private FusedLocationProviderClient fusedLocationClient;
    private Location currentLocation;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    private ActivityResultLauncher<String> locationPermissionLauncher;

    // Firebase
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private CollectionReference listingsRef;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        searchResultList = new ArrayList<>();
        currentSearchParams = new SearchParams();

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        listingsRef = db.collection("listings");

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

        fetchUserFavorites();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search, container, false);
        initViews(view);
        setupAdapters();
        setupListeners();
        updateUiState();
        startNewSearch();
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

    private void setupAdapters() {
        searchResultsAdapter = new SearchResultsAdapter(searchResultList, this::onProductClick, this::onFavoriteClick);
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
            updateUiState();
        });

        applyFiltersButton.setOnClickListener(v -> {
            // TODO: Lấy giá trị từ các trường filter và cập nhật vào currentSearchParams
            // currentSearchParams.setMinPrice(...)
            isFiltersVisible = false;
            updateUiState();
            startNewSearch();
        });

        clearFiltersButton.setOnClickListener(v -> {
            // TODO: Xóa trắng các trường filter trên UI và trong currentSearchParams
            isFiltersVisible = false;
            updateUiState();
            startNewSearch();
        });

        loadMoreButton.setOnClickListener(v -> performSearch(false));
        useGpsButton.setOnClickListener(v -> checkLocationPermissionAndFetch());
    }

    @SuppressLint("NotifyDataSetChanged")
    private void startNewSearch() {
        isLastPage = false;
        lastVisibleDocument = null;
        searchResultList.clear();
        searchResultsAdapter.notifyDataSetChanged();
        performSearch(true);
    }

    @SuppressLint("NotifyDataSetChanged")
    private void performSearch(final boolean isNewSearch) {
        if (isLoading) return;
        isLoading = true;
        updateUiState();

        Query query = buildFirestoreQuery();

        query.get().addOnSuccessListener(queryDocumentSnapshots -> {
            isLoading = false;
            if (!queryDocumentSnapshots.isEmpty()) {
                lastVisibleDocument = queryDocumentSnapshots.getDocuments().get(queryDocumentSnapshots.size() - 1);
                isLastPage = queryDocumentSnapshots.size() < ITEMS_PER_PAGE;
            } else {
                isLastPage = true;
            }

            List<SearchResult> newResults = new ArrayList<>();
            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                Listing listing = doc.toObject(Listing.class);
                boolean isFavorite = userFavoriteIds.contains(listing.getId());
                newResults.add(new SearchResult(listing, isFavorite));
            }

            if (isNewSearch) {
                searchResultList.clear();
            }
            searchResultList.addAll(newResults);
            searchResultsAdapter.notifyDataSetChanged();

            updateUiState();
        }).addOnFailureListener(e -> {
            isLoading = false;
            Log.e(TAG, "Error fetching listings", e);
            Toast.makeText(getContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            updateUiState();
        });
    }

    private Query buildFirestoreQuery() {
        Query q = listingsRef.whereEqualTo("status", "available");
        // ... (code xây dựng query dựa trên currentSearchParams giữ nguyên) ...

        // Ví dụ:
        if (currentSearchParams.getQuery() != null && !currentSearchParams.getQuery().isEmpty()) {
            String keyword = currentSearchParams.getQuery();
            // Cần có index cho title để orderBy
            q = q.orderBy("title").startAt(keyword).endAt(keyword + '\uf8ff');
        } else {
            q = q.orderBy("timePosted", Query.Direction.DESCENDING); // Sắp xếp mặc định
        }

        if (lastVisibleDocument != null) {
            q = q.startAfter(lastVisibleDocument);
        }
        return q.limit(ITEMS_PER_PAGE);
    }

    @SuppressLint("DefaultLocale")
    private void updateUiState() {
        // Quản lý trạng thái của toàn bộ màn hình

        // 1. Hiển thị/ẩn vùng filter
        filtersContainer.setVisibility(isFiltersVisible ? View.VISIBLE : View.GONE);
        // Khi filter hiện, ẩn các view khác đi
        sortHeaderCard.setVisibility(isFiltersVisible ? View.GONE : View.VISIBLE);
        searchResultsRecyclerView.setVisibility(isFiltersVisible ? View.GONE : View.VISIBLE);
        emptyState.setVisibility(isFiltersVisible ? View.GONE : View.VISIBLE);

        if (isFiltersVisible) return; // Nếu đang mở filter thì không cần cập nhật các trạng thái bên dưới

        // 2. Quản lý trạng thái loading chính
        loadingState.setVisibility(isLoading && searchResultList.isEmpty() ? View.VISIBLE : View.GONE);

        // 3. Quản lý trạng thái rỗng
        emptyState.setVisibility(!isLoading && searchResultList.isEmpty() ? View.VISIBLE : View.GONE);

        // 4. Quản lý RecyclerView
        searchResultsRecyclerView.setVisibility(!isLoading && !searchResultList.isEmpty() ? View.VISIBLE : View.GONE);

        // 5. Quản lý loading more
        loadMoreProgress.setVisibility(isLoading && !searchResultList.isEmpty() ? View.VISIBLE : View.GONE);
        loadMoreButton.setVisibility(!isLoading && !isLastPage && !searchResultList.isEmpty() ? View.VISIBLE : View.GONE);

        // 6. Cập nhật text số kết quả
        resultsCount.setText(String.format("Tìm thấy %d kết quả", searchResultList.size()));
    }

    private void checkLocationPermissionAndFetch() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fetchCurrentLocation();
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    @SuppressLint("MissingPermission")
    private void fetchCurrentLocation() {
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                currentLocation = location;
                locationInput.setText(String.format(Locale.US, "Vị trí hiện tại (%.4f, %.4f)", location.getLatitude(), location.getLongitude()));
                Toast.makeText(requireContext(), "Đã cập nhật vị trí.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), "Không thể lấy vị trí. Vui lòng bật GPS.", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void onProductClick(SearchResult result) {
        Toast.makeText(requireContext(), "Xem: " + result.getTitle(), Toast.LENGTH_SHORT).show();
    }

    @SuppressLint("NotifyDataSetChanged")
    private void onFavoriteClick(SearchResult result, boolean isFavorite) {
        if (mAuth.getUid() == null) {
            Toast.makeText(requireContext(), "Vui lòng đăng nhập để yêu thích sản phẩm.", Toast.LENGTH_SHORT).show();
            // Đảo ngược lại trạng thái trên UI vì không thể lưu
            result.setFavorite(!isFavorite);
            searchResultsAdapter.notifyDataSetChanged();
            return;
        }

        if (isFavorite) {
            if (!userFavoriteIds.contains(result.getId())) userFavoriteIds.add(result.getId());
        } else {
            userFavoriteIds.remove(result.getId());
        }

        db.collection("users").document(mAuth.getUid())
                .update("favoriteListingIds", userFavoriteIds)
                .addOnSuccessListener(aVoid -> {
                    String message = isFavorite ? "Đã thêm vào yêu thích" : "Đã bỏ yêu thích";
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    result.setFavorite(!isFavorite);
                    searchResultsAdapter.notifyDataSetChanged();
                    Toast.makeText(requireContext(), "Lỗi cập nhật yêu thích.", Toast.LENGTH_SHORT).show();
                });
    }

    private void fetchUserFavorites() {
        if (mAuth.getUid() != null) {
            db.collection("users").document(mAuth.getUid()).get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    Object favoritesObject = documentSnapshot.get("favoriteListingIds");
                    if (favoritesObject instanceof List) {
                        try {
                            @SuppressWarnings("unchecked")
                            List<String> favorites = (List<String>) favoritesObject;
                            userFavoriteIds.clear();
                            userFavoriteIds.addAll(favorites);
                        } catch (ClassCastException e) {
                            Log.e(TAG, "Lỗi ép kiểu danh sách yêu thích.", e);
                        }
                    }
                }
            });
        }
    }
}