package com.example.testapptradeup.fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import com.bumptech.glide.Glide;
import com.example.testapptradeup.R;
import com.example.testapptradeup.models.Listing;
import com.example.testapptradeup.viewmodels.ProductDetailViewModel;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class ProductDetailFragment extends Fragment {
    private ProductDetailViewModel viewModel;
    private NavController navController;
    private String listingId;

    private ImageView productImage, btnMoreOptions;
    private TextView productTitle, productPrice, productDescription;
    private CollapsingToolbarLayout collapsingToolbar;
    private MaterialToolbar toolbar;
    private Button btnMakeOffer;
    private Listing currentListing;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(ProductDetailViewModel.class);
        if (getArguments() != null) {
            listingId = ProductDetailFragmentArgs.fromBundle(getArguments()).getListingId();
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_product_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(view);
        initViews(view);
        setupListeners(); // <<< THÊM DÒNG NÀY

        toolbar.setNavigationOnClickListener(v -> navController.popBackStack());

        // Logic kiểm tra argument mới
        if (getArguments() != null) {
            listingId = ProductDetailFragmentArgs.fromBundle(getArguments()).getListingId();
            currentListing = ProductDetailFragmentArgs.fromBundle(getArguments()).getListingPreview();

            if (currentListing != null) {
                // Chế độ xem trước: Dữ liệu được truyền trực tiếp
                populateUI(currentListing);
                // Ẩn các nút không cần thiết trong chế độ xem trước
                btnMakeOffer.setVisibility(View.GONE);
            } else if (listingId != null) {
                // Chế độ xem tin đã đăng: Tải dữ liệu từ ViewModel
                viewModel.loadListingDetail(listingId);
                observeViewModel();
            }
        }

        btnMakeOffer.setOnClickListener(v -> {
            if (currentListing != null) {
                OfferBottomSheetDialogFragment bottomSheet =
                        OfferBottomSheetDialogFragment.newInstance(currentListing.getId(), currentListing.getSellerId());
                bottomSheet.show(getChildFragmentManager(), bottomSheet.getTag());
            }
        });
    }

    private void setupListeners() {
        toolbar.setNavigationOnClickListener(v -> navController.popBackStack());

        btnMoreOptions.setOnClickListener(v -> {
            // Chỉ hiển thị menu nếu không phải ở chế độ xem trước
            if (currentListing != null && listingId != null) {
                showListingOptionsDialog();
            }
        });

        btnMakeOffer.setOnClickListener(v -> {
            if (currentListing != null) {
                OfferBottomSheetDialogFragment bottomSheet =
                        OfferBottomSheetDialogFragment.newInstance(currentListing.getId(), currentListing.getSellerId());
                bottomSheet.show(getChildFragmentManager(), bottomSheet.getTag());
            }
        });
    }

    private void showListingOptionsDialog() {
        if (getContext() == null) return;
        final CharSequence[] options = {"Báo cáo tin đăng", "Hủy"};

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Tùy chọn");
        builder.setItems(options, (dialog, item) -> {
            if (options[item].equals("Báo cáo tin đăng")) {
                showReportReasonDialog();
            } else if (options[item].equals("Hủy")) {
                dialog.dismiss();
            }
        });
        builder.show();
    }

    private void showReportReasonDialog() {
        if (getContext() == null || currentListing == null) return;
        final String[] reportReasons = {"Thông tin sai sự thật", "Lừa đảo", "Hàng cấm/Bất hợp pháp", "Spam", "Lý do khác"};

        new AlertDialog.Builder(getContext())
                .setTitle("Báo cáo tin đăng")
                .setItems(reportReasons, (dialog, which) -> {
                    String reason = reportReasons[which];
                    sendListingReport(reason);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void sendListingReport(String reason) {
        String currentUserId = FirebaseAuth.getInstance().getUid();
        if (currentUserId == null) {
            Toast.makeText(getContext(), "Bạn cần đăng nhập để thực hiện hành động này.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentListing == null || currentListing.getId() == null) {
            Toast.makeText(getContext(), "Lỗi: Không tìm thấy thông tin bài đăng.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Tạo một đối tượng báo cáo
        Map<String, Object> report = new HashMap<>();
        report.put("reporterId", currentUserId);
        report.put("reportedListingId", currentListing.getId());
        report.put("reportedSellerId", currentListing.getSellerId()); // Lưu cả ID người bán để dễ truy vấn
        report.put("reason", reason);
        report.put("timestamp", FieldValue.serverTimestamp());
        report.put("type", "listing"); // QUAN TRỌNG: Đánh dấu đây là báo cáo bài đăng
        report.put("status", "pending"); // Trạng thái ban đầu của báo cáo

        // Gửi báo cáo lên Firestore
        FirebaseFirestore.getInstance().collection("reports").add(report)
                .addOnSuccessListener(documentReference ->
                        Toast.makeText(getContext(), "Cảm ơn bạn đã gửi báo cáo. Chúng tôi sẽ xem xét.", Toast.LENGTH_LONG).show()
                )
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Gửi báo cáo thất bại. Vui lòng thử lại.", Toast.LENGTH_SHORT).show()
                );
    }

    // Tạo hàm riêng để hiển thị dữ liệu lên UI
    private void populateUI(Listing listing) {
        if (listing == null) return;
        collapsingToolbar.setTitle(listing.getTitle());
        productTitle.setText(listing.getTitle());
        productPrice.setText(listing.getFormattedPrice());
        productDescription.setText(listing.getDescription());
        if (getContext() != null) {
            Glide.with(getContext()).load(listing.getPrimaryImageUrl()).into(productImage);
        }
    }

    private void initViews(View view) {
        productImage = view.findViewById(R.id.product_image);
        productTitle = view.findViewById(R.id.product_title);
        productPrice = view.findViewById(R.id.product_price);
        productDescription = view.findViewById(R.id.product_description);
        collapsingToolbar = view.findViewById(R.id.collapsing_toolbar);
        toolbar = view.findViewById(R.id.toolbar);
        btnMakeOffer = view.findViewById(R.id.btn_make_offer);
        toolbar = view.findViewById(R.id.toolbar);
        btnMakeOffer = view.findViewById(R.id.btn_make_offer);
        btnMoreOptions = view.findViewById(R.id.btn_more_options); // Ánh xạ nút mới
    }

    private void observeViewModel() {
        viewModel.getListingDetail().observe(getViewLifecycleOwner(), listing -> {
            if (listing != null) {
                this.currentListing = listing;
                populateUI(listing); // Gọi hàm populateUI
            }
        });
    }
}