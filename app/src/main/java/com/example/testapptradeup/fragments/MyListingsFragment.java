package com.example.testapptradeup.fragments;

import android.annotation.SuppressLint;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.testapptradeup.R;
import com.example.testapptradeup.adapters.MyListingsAdapter;
import com.example.testapptradeup.models.Listing;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class MyListingsFragment extends Fragment {

    private static final String TAG = "MyListingsFragment";

    private RecyclerView recyclerViewMyListings;
    private MyListingsAdapter adapter;
    private LinearLayout emptyState;

    // Components from the new layout
    private ImageView btnBack;
    private TextView tabAll, tabActive, tabSold, tabPaused;
    private LinearLayout sortContainer;
    private TextView sortText;

    private List<Listing> allMyListings;
    private List<Listing> displayedListings;
    private List<TextView> tabViews;

    private String currentFilterStatus = "all";
    private int currentSortOptionId = R.id.sort_most_recent;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private NavController navController;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_my_listings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        navController = Navigation.findNavController(view);

        initViews(view);
        setupRecyclerView();
        setupListeners();

        fetchMyListings();
    }

    private void initViews(View view) {
        // Sử dụng ID từ layout XML mới `fragment_my_listings.xml`
        recyclerViewMyListings = view.findViewById(R.id.recycler_listings);
        emptyState = view.findViewById(R.id.empty_state);

        btnBack = view.findViewById(R.id.btn_back);
        tabAll = view.findViewById(R.id.tab_all);
        tabActive = view.findViewById(R.id.tab_active);
        tabSold = view.findViewById(R.id.tab_sold);
        tabPaused = view.findViewById(R.id.tab_paused);
        sortContainer = view.findViewById(R.id.sort_container);
        sortText = view.findViewById(R.id.sort_text);

        tabViews = new ArrayList<>();
        tabViews.add(tabAll);
        tabViews.add(tabActive);
        tabViews.add(tabSold);
        tabViews.add(tabPaused);
    }

    private void setupRecyclerView() {
        allMyListings = new ArrayList<>();
        displayedListings = new ArrayList<>();

        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 2);
        recyclerViewMyListings.setLayoutManager(layoutManager);

        adapter = new MyListingsAdapter(getContext(), displayedListings);
        recyclerViewMyListings.setAdapter(adapter);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> navController.navigateUp());

        // Tab listeners
        tabAll.setOnClickListener(v -> {
            currentFilterStatus = "all";
            updateTabUI(v);
            applyFiltersAndSort();
        });
        tabActive.setOnClickListener(v -> {
            currentFilterStatus = "active";
            updateTabUI(v);
            applyFiltersAndSort();
        });
        tabSold.setOnClickListener(v -> {
            currentFilterStatus = "sold";
            updateTabUI(v);
            applyFiltersAndSort();
        });
        tabPaused.setOnClickListener(v -> {
            currentFilterStatus = "paused";
            updateTabUI(v);
            applyFiltersAndSort();
        });

        // Sort listener
        sortContainer.setOnClickListener(this::showSortMenu);
    }

    private void showSortMenu(View v) {
        PopupMenu popup = new PopupMenu(getContext(), v);
        popup.getMenuInflater().inflate(R.menu.sort_listings_menu, popup.getMenu());

        popup.setOnMenuItemClickListener(item -> {
            currentSortOptionId = item.getItemId();
            sortText.setText(item.getTitle());
            applyFiltersAndSort();
            return true;
        });

        popup.show();
    }


    private void fetchMyListings() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), "Vui lòng đăng nhập để xem tin đăng.", Toast.LENGTH_SHORT).show();
            return;
        }
        String currentUserId = currentUser.getUid();

        // TODO: Hiển thị ProgressBar
        db.collection("listings")
                .whereEqualTo("sellerId", currentUserId)
                .orderBy("timePosted", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    // TODO: Ẩn ProgressBar
                    if (task.isSuccessful()) {
                        allMyListings.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Listing listing = document.toObject(Listing.class);
                            listing.setId(document.getId()); // Quan trọng để có ID cho các thao tác sau này
                            allMyListings.add(listing);
                        }
                        // Sau khi lấy xong, áp dụng bộ lọc và sắp xếp mặc định
                        updateTabUI(tabAll); // Mặc định chọn tab "All"
                        applyFiltersAndSort();
                    } else {
                        Log.w(TAG, "Lỗi khi lấy dữ liệu: ", task.getException());
                        Toast.makeText(getContext(), "Không thể tải danh sách tin đăng.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @SuppressLint("NotifyDataSetChanged")
    private void applyFiltersAndSort() {
        // 1. Filter
        displayedListings.clear();
        if (currentFilterStatus.equalsIgnoreCase("all")) {
            displayedListings.addAll(allMyListings);
        } else {
            for (Listing listing : allMyListings) {
                if (listing.getStatus() != null && listing.getStatus().equalsIgnoreCase(currentFilterStatus)) {
                    displayedListings.add(listing);
                }
            }
        }

        // 2. Sort
        Comparator<Listing> comparator = null;
        if (currentSortOptionId == R.id.sort_most_recent) {
            comparator = (l1, l2) -> l2.getTimePosted().compareTo(l1.getTimePosted());
        } else if (currentSortOptionId == R.id.sort_oldest) {
            comparator = (l1, l2) -> l1.getTimePosted().compareTo(l2.getTimePosted());
        } else if (currentSortOptionId == R.id.sort_price_high_to_low) {
            comparator = Comparator.comparing(Listing::getPrice).reversed();
        } else if (currentSortOptionId == R.id.sort_price_low_to_high) {
            comparator = Comparator.comparing(Listing::getPrice);
        }

        if (comparator != null) {
            Collections.sort(displayedListings, comparator);
        }

        // 3. Update UI
        adapter.notifyDataSetChanged();
        checkEmptyState();
    }

    private void updateTabUI(View selectedTab) {
        for (TextView tab : tabViews) {
            if (tab == selectedTab) {
                // Style cho tab được chọn
                tab.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.charcoal_black));
                tab.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white));
                tab.setTypeface(null, Typeface.BOLD);
            } else {
                // Style cho các tab không được chọn
                tab.setBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.white));
                tab.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
                tab.setTypeface(null, Typeface.NORMAL);
            }
        }
    }

    private void checkEmptyState() {
        if (displayedListings.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            recyclerViewMyListings.setVisibility(View.GONE);
        } else {
            emptyState.setVisibility(View.GONE);
            recyclerViewMyListings.setVisibility(View.VISIBLE);
        }
    }
}