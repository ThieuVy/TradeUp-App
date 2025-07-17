package com.example.testapptradeup.fragments;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.example.testapptradeup.R;
import com.example.testapptradeup.models.Listing;
import com.example.testapptradeup.viewmodels.EditPostViewModel;

public class EditPostFragment extends PostFragment { // Kế thừa từ PostFragment

    private EditPostViewModel editViewModel;
    private String listingIdToEdit;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        editViewModel = new ViewModelProvider(this).get(EditPostViewModel.class);

        if (getArguments() != null) {
            listingIdToEdit = EditPostFragmentArgs.fromBundle(getArguments()).getListingIdToEdit();
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Giờ đây navController có thể truy cập được do là 'protected'
        navController = Navigation.findNavController(view);

        TextView header = view.findViewById(R.id.header_title);
        Button mainButton = view.findViewById(R.id.btn_post_listing);
        Button previewButton = view.findViewById(R.id.btn_preview);

        header.setText("Chỉnh sửa tin đăng");
        mainButton.setText("Lưu thay đổi");
        previewButton.setVisibility(View.GONE);

        mainButton.setOnClickListener(v -> saveChanges());

        // Ghi đè phương thức observeViewModel của cha
        observeViewModel();

        if (listingIdToEdit != null) {
            editViewModel.loadListingData(listingIdToEdit);
        } else {
            Toast.makeText(getContext(), "Lỗi: Không tìm thấy tin đăng để sửa.", Toast.LENGTH_LONG).show();
            navController.popBackStack(); // Dùng biến đã được khởi tạo
        }
    }

    // Ghi đè hàm observeViewModel
    @Override
    protected void observeViewModel() {
        editViewModel.isLoading().observe(getViewLifecycleOwner(), this::showLoading);

        editViewModel.getListingData().observe(getViewLifecycleOwner(), listing -> {
            // Tắt loading khi có dữ liệu (kể cả null)
            showLoading(false);
            if (listing != null) {
                populateForm(listing);
            }
        });

        editViewModel.getUpdateStatus().observe(getViewLifecycleOwner(), success -> {
            if (success != null) {
                showLoading(false);
                if (success) {
                    Toast.makeText(getContext(), "Cập nhật thành công!", Toast.LENGTH_SHORT).show();
                    navController.popBackStack();
                } else {
                    Toast.makeText(getContext(), "Cập nhật thất bại, vui lòng thử lại.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void populateForm(Listing listing) {
        // Các biến UI giờ đã có thể truy cập do là 'protected'
        etProductTitle.setText(listing.getTitle());
        etPrice.setText(String.valueOf(listing.getPrice()));
        spinnerCategory.setText(listing.getCategoryId(), false);
        etDescription.setText(listing.getDescription());
        etLocation.setText(listing.getLocation());

        if (listing.getCondition() != null) {
            switch (listing.getCondition()) {
                case "new": chipGroupCondition.check(R.id.chip_condition_new); break;
                case "like_new": chipGroupCondition.check(R.id.chip_condition_like_new); break;
                case "used": chipGroupCondition.check(R.id.chip_condition_used); break;
            }
        }
        // TODO: Xử lý hiển thị ảnh và tags đã có
    }

    private void saveChanges() {
        // Hàm validateAllFields() giờ đã truy cập được
        if (validateAllFields()) {
            return;
        }

        // Hàm buildListingFromUI() giờ đã truy cập được
        Listing updatedListing = buildListingFromUI(null, null);
        updatedListing.setId(listingIdToEdit);

        editViewModel.saveChanges(updatedListing);
    }
}