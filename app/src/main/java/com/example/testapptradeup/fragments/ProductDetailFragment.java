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
// <<< THÊM MỚI: Import lớp AppCompatActivity để thiết lập Toolbar >>>
import androidx.appcompat.app.AppCompatActivity;
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
import java.util.Objects;

public class ProductDetailFragment extends Fragment {
    private ProductDetailViewModel viewModel;
    private NavController navController;
    private String listingId;

    // --- Khai báo UI Views ---
    private ImageView productImage, btnMoreOptions;
    private TextView productTitle, productPrice, productDescription;
    private CollapsingToolbarLayout collapsingToolbar;
    private MaterialToolbar toolbar;
    private Button btnMakeOffer;
    // <<< THÊM MỚI: Tham chiếu đến nút Chat và thanh action dưới cùng >>>
    private Button btnChat;
    private View bottomActionBar; // Dùng View hoặc LinearLayout

    private Listing currentListing;
    private String currentUserId;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(ProductDetailViewModel.class);
        if (getArguments() != null) {
            listingId = ProductDetailFragmentArgs.fromBundle(getArguments()).getListingId();
            // Lấy cả listing preview nếu có
            currentListing = ProductDetailFragmentArgs.fromBundle(getArguments()).getListingPreview();
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

        // <<< TỐI ƯU: Gọi các hàm thiết lập một lần >>>
        initViews(view);
        setupToolbar(); // Thiết lập toolbar trước
        setupListeners();

        // Xử lý logic hiển thị dựa trên arguments
        if (currentListing != null) {
            // Chế độ xem trước: Dữ liệu đã có sẵn
            populateUI(currentListing);
            btnMakeOffer.setVisibility(View.GONE);
            btnChat.setVisibility(View.GONE);
            btnMoreOptions.setVisibility(View.GONE); // Ẩn nút "..." khi xem trước
        } else if (listingId != null) {
            // Chế độ xem tin đã đăng: Tải dữ liệu
            viewModel.loadListingDetail(listingId);
            observeViewModel();
        } else {
            // Trường hợp lỗi: không có dữ liệu để hiển thị
            Toast.makeText(getContext(), "Lỗi: Không có dữ liệu sản phẩm.", Toast.LENGTH_LONG).show();
            navController.popBackStack();
        }
    }

    // <<< TỐI ƯU: Ánh xạ view tập trung tại một nơi >>>
    private void initViews(View view) {
        productImage = view.findViewById(R.id.product_image);
        productTitle = view.findViewById(R.id.product_title);
        productPrice = view.findViewById(R.id.product_price);
        productDescription = view.findViewById(R.id.product_description);
        collapsingToolbar = view.findViewById(R.id.collapsing_toolbar);
        toolbar = view.findViewById(R.id.toolbar);
        btnMakeOffer = view.findViewById(R.id.btn_make_offer);
        btnChat = view.findViewById(R.id.btn_chat); // Ánh xạ nút chat mới
        bottomActionBar = view.findViewById(R.id.bottom_action_bar);
        btnMoreOptions = view.findViewById(R.id.btn_more_options);
    }

    // <<< SỬA LỖI QUAN TRỌNG: Thiết lập Toolbar đúng cách >>>
    private void setupToolbar() {
        if (getActivity() instanceof AppCompatActivity) {
            ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);
            Objects.requireNonNull(((AppCompatActivity) getActivity()).getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
            ((AppCompatActivity) getActivity()).getSupportActionBar().setDisplayShowTitleEnabled(false); // Ẩn tiêu đề mặc định của Toolbar
        }
    }

    // <<< TỐI ƯU: Gom tất cả các listener vào một chỗ >>>
    private void setupListeners() {
        toolbar.setNavigationOnClickListener(v -> navController.popBackStack());

        btnMoreOptions.setOnClickListener(v -> {
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

        btnChat.setOnClickListener(v -> {
            // <<< TỐI ƯU: Đã có sẵn currentUserId >>>
            if (currentListing == null) {
                Toast.makeText(getContext(), "Dữ liệu sản phẩm chưa sẵn sàng.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (currentUserId == null) {
                Toast.makeText(getContext(), "Vui lòng đăng nhập để bắt đầu trò chuyện.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (currentUserId.equals(currentListing.getSellerId())) {
                Toast.makeText(getContext(), "Bạn không thể tự chat với mình.", Toast.LENGTH_SHORT).show();
                return;
            }

            // ... (code gọi viewModel và điều hướng giữ nguyên)
            viewModel.findOrCreateChat(currentListing.getSellerId()).observe(getViewLifecycleOwner(), chatId -> {
                if (chatId != null && !chatId.isEmpty()) {
                    ProductDetailFragmentDirections.ActionProductDetailToChatDetail action =
                            ProductDetailFragmentDirections.actionProductDetailToChatDetail(chatId, currentListing.getSellerName());
                    action.setListingId(currentListing.getId());
                    navController.navigate(action);
                } else {
                    Toast.makeText(getContext(), "Không thể bắt đầu cuộc trò chuyện. Vui lòng thử lại.", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void observeViewModel() {
        viewModel.getListingDetail().observe(getViewLifecycleOwner(), listing -> {
            if (listing != null) {
                this.currentListing = listing;
                populateUI(listing);
            } else {
                Toast.makeText(getContext(), "Không tìm thấy sản phẩm.", Toast.LENGTH_SHORT).show();
                navController.popBackStack();
            }
        });
    }

    private void populateUI(Listing listing) {
        if (listing == null || getContext() == null) return;

        collapsingToolbar.setTitle(listing.getTitle());
        productTitle.setText(listing.getTitle());
        productPrice.setText(listing.getFormattedPrice());
        productDescription.setText(listing.getDescription());

        Glide.with(getContext())
                .load(listing.getPrimaryImageUrl())
                .placeholder(R.drawable.img_placeholder)
                .into(productImage);

        // <<< SỬA LỖI 3: Dùng biến thành viên đã được khởi tạo >>>
        // Thay vì gọi FirebaseAuth.getInstance().getUid() một lần nữa.
        if (currentUserId != null && currentUserId.equals(listing.getSellerId())) {
            bottomActionBar.setVisibility(View.GONE);
        } else {
            bottomActionBar.setVisibility(View.VISIBLE);
        }
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
}