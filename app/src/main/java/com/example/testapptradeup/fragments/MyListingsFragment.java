package com.example.testapptradeup.fragments;

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
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.testapptradeup.R;
import com.example.testapptradeup.adapters.MyListingsAdapter; // Import Adapter bạn đã tạo
import com.example.testapptradeup.models.Listing;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

// THAY ĐỔI 1: Kế thừa từ androidx.fragment.app.Fragment
public class MyListingsFragment extends Fragment {

    private static final String TAG = "MyListingsFragment";

    private RecyclerView recyclerViewMyListings;
    private ChipGroup chipGroupFilters;
    private MyListingsAdapter adapter;

    private List<Listing> allMyListings; // Danh sách chứa tất cả tin đăng
    private List<Listing> displayedListings; // Danh sách hiển thị sau khi lọc

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // THAY ĐỔI 2: Gắn (inflate) layout cho Fragment
        return inflater.inflate(R.layout.fragment_my_listings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Khởi tạo Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Ánh xạ các view từ layout
        initViews(view);

        // Thiết lập RecyclerView và Adapter
        setupRecyclerView();

        // Thiết lập listener cho các chip lọc
        setupFilterListeners();

        // Lấy dữ liệu từ Firestore
        fetchMyListings();
    }

    private void initViews(View view) {
        recyclerViewMyListings = view.findViewById(R.id.recycler_view_my_listings);
        chipGroupFilters = view.findViewById(R.id.chip_group_filters);
        // Bạn có thể thêm xử lý cho Toolbar ở đây nếu cần
        // Toolbar toolbar = view.findViewById(R.id.toolbar);
    }

    private void setupRecyclerView() {
        allMyListings = new ArrayList<>();
        displayedListings = new ArrayList<>();

        // Sử dụng GridLayout với 2 cột
        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 2);
        recyclerViewMyListings.setLayoutManager(layoutManager);

        // Khởi tạo và gán adapter
        adapter = new MyListingsAdapter(getContext(), displayedListings);
        recyclerViewMyListings.setAdapter(adapter);
    }

    private void setupFilterListeners() {
        chipGroupFilters.setOnCheckedStateChangeListener((group, checkedIds) -> {
            // 1. Kiểm tra nếu không có gì được chọn -> An toàn
            if (checkedIds.isEmpty()) {
                return;
            }

            // 2. Lấy ID đầu tiên vì là single-selection -> Chính xác
            int checkedId = checkedIds.get(0);

            // 3. Xử lý logic dựa trên ID -> Rõ ràng, dễ đọc
            if (checkedId == R.id.chip_all) {
                filterAndDisplayListings("all");
            } else if (checkedId == R.id.chip_active) {
                filterAndDisplayListings("active");
            } else if (checkedId == R.id.chip_sold) {
                filterAndDisplayListings("sold");
            } else if (checkedId == R.id.chip_paused) {
                filterAndDisplayListings("paused");
            }
        });
    }

    private void fetchMyListings() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), "Vui lòng đăng nhập để xem tin đăng.", Toast.LENGTH_SHORT).show();
            return; // Người dùng chưa đăng nhập, không làm gì cả
        }
        String currentUserId = currentUser.getUid();

        // TODO: Hiển thị một ProgressBar (vòng xoay loading)

        db.collection("listings")
                .whereEqualTo("sellerId", currentUserId) // Lọc các tin đăng của người dùng hiện tại
                .orderBy("timePosted", Query.Direction.DESCENDING) // Sắp xếp theo mới nhất
                .get()
                .addOnCompleteListener(task -> {
                    // TODO: Ẩn ProgressBar

                    if (task.isSuccessful()) {
                        allMyListings.clear(); // Xóa dữ liệu cũ
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Listing listing = document.toObject(Listing.class);
                            allMyListings.add(listing);
                        }
                        // Sau khi lấy xong, hiển thị tất cả (mặc định)
                        filterAndDisplayListings("all");
                    } else {
                        Log.w(TAG, "Lỗi khi lấy dữ liệu: ", task.getException());
                        Toast.makeText(getContext(), "Không thể tải danh sách tin đăng.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @SuppressLint("NotifyDataSetChanged")
    private void filterAndDisplayListings(String status) {
        displayedListings.clear(); // Xóa danh sách hiển thị hiện tại

        if (status.equalsIgnoreCase("all")) {
            displayedListings.addAll(allMyListings);
        } else {
            for (Listing listing : allMyListings) {
                // So sánh trạng thái (không phân biệt hoa thường)
                if (listing.getStatus() != null && listing.getStatus().equalsIgnoreCase(status)) {
                    displayedListings.add(listing);
                }
            }
        }

        // Cập nhật lại RecyclerView
        adapter.notifyDataSetChanged();

        // TODO: Hiển thị một thông báo nếu danh sách rỗng
        // if (displayedListings.isEmpty()) { ... }
    }
}