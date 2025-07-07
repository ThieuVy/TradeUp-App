package com.example.testapptradeup.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment; // QUAN TRỌNG: Import từ androidx
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import com.example.testapptradeup.R;
import com.google.android.material.appbar.MaterialToolbar;

public class ReviewsFragment extends Fragment {

    // Thêm các phương thức vòng đời cơ bản cho một Fragment
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate layout của bạn
        return inflater.inflate(R.layout.fragment_reviews, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Thiết lập sự kiện cho nút back trên toolbar
        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> {
            NavController navController = Navigation.findNavController(v);
            navController.popBackStack();
        });

        // TODO: Thêm logic để tải và hiển thị danh sách tất cả các đánh giá ở đây
        // Ví dụ: setup RecyclerView, observe ViewModel...
    }
}