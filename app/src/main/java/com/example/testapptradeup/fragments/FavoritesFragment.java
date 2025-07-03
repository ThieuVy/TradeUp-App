package com.example.testapptradeup.fragments;

import static android.content.ContentValues.TAG;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.testapptradeup.R;
import com.example.testapptradeup.adapters.FavoritesAdapter;
import com.example.testapptradeup.models.Listing;
import com.example.testapptradeup.models.User;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.example.testapptradeup.viewmodels.FavoritesViewModel;

import java.util.ArrayList;
import java.util.List;

public class FavoritesFragment extends Fragment {

    private RecyclerView recyclerView;
    private FavoritesAdapter adapter;
    private List<Listing> favoriteListings;
    private MaterialToolbar toolbar;
    private NavController navController;
    private FavoritesViewModel viewModel;
    // Optional: for loading and empty states
    // private ProgressBar progressBar;
    // private TextView emptyStateText;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(FavoritesViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_favorites, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(view);
        initViews(view);
        setupRecyclerView();
        setupToolbar();
        observeViewModel();
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recycler_view_favorites);
        toolbar = view.findViewById(R.id.toolbar_favorites);
        // progressBar = view.findViewById(R.id.progress_bar_favorites);
        // emptyStateText = view.findViewById(R.id.text_empty_state);
    }

    private void setupToolbar() {
        toolbar.setNavigationOnClickListener(v -> navController.popBackStack());
    }

    private void setupRecyclerView() {
        adapter = new FavoritesAdapter(getContext(), new ArrayList<>());
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    private void observeViewModel() {
        viewModel.getFavoriteListings().observe(getViewLifecycleOwner(), listings -> {
            if (listings != null) {
                adapter = new FavoritesAdapter(getContext(), listings);
                recyclerView.setAdapter(adapter);
                if (listings.isEmpty()) {
                    showEmptyState();
                }
            } else {
                Toast.makeText(getContext(), "Lỗi tải danh sách yêu thích", Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void fetchFavoriteListingIds() {
        FirebaseUser firebaseUser = mAuth.getCurrentUser();
        if (firebaseUser == null) {
            Toast.makeText(getContext(), "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
            // Tùy chọn: Điều hướng về trang đăng nhập
            return;
        }

        // Bước 1: Lấy document của người dùng để lấy danh sách ID yêu thích
        db.collection("users").document(firebaseUser.getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null && user.getFavoriteListingIds() != null && !user.getFavoriteListingIds().isEmpty()) {
                            // Bước 2: Lấy thông tin chi tiết các sản phẩm từ danh sách ID
                            fetchListingsDetails(user.getFavoriteListingIds());
                        } else {
                            // Người dùng chưa thích sản phẩm nào
                            showEmptyState();
                        }
                    } else {
                        showEmptyState();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi lấy thông tin người dùng: ", e);
                    Toast.makeText(getContext(), "Lỗi tải dữ liệu", Toast.LENGTH_SHORT).show();
                });
    }

    @SuppressLint("NotifyDataSetChanged")
    private void fetchListingsDetails(List<String> listingIds) {
        if (listingIds.isEmpty()) {
            showEmptyState();
            return;
        }
        // Firestore có giới hạn 30 phần tử cho truy vấn 'in'. Nếu nhiều hơn, bạn phải chia nhỏ.
        db.collection("listings")
                .whereIn(FieldPath.documentId(), listingIds)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    favoriteListings.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Listing listing = document.toObject(Listing.class);
                        favoriteListings.add(listing);
                    }
                    adapter.notifyDataSetChanged();
                    if (favoriteListings.isEmpty()) {
                        showEmptyState();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi lấy chi tiết tin đăng: ", e);
                    Toast.makeText(getContext(), "Lỗi tải danh sách yêu thích", Toast.LENGTH_SHORT).show();
                });
    }

    @SuppressLint("NotifyDataSetChanged")
    private void showEmptyState() {
        // TODO: Hiển thị một TextView thông báo "Chưa có sản phẩm yêu thích"
        // và ẩn RecyclerView
        Toast.makeText(getContext(), "Bạn chưa có sản phẩm yêu thích nào.", Toast.LENGTH_SHORT).show();
        favoriteListings.clear();
        adapter.notifyDataSetChanged();
    }
}