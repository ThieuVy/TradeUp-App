package com.example.testapptradeup.fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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

    // UI Views
    private ImageView productImage, btnMoreOptions;
    private TextView productTitle, productPrice, productDescription;
    private CollapsingToolbarLayout collapsingToolbar;
    private MaterialToolbar toolbar;
    private Button btnMakeOffer, btnChat;
    private View bottomActionBar;
    private Listing currentListing;
    private String currentUserId;
    private LinearLayout sellerInfoLayout;
    private ImageView sellerAvatar;
    private TextView sellerName;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(ProductDetailViewModel.class);
        currentUserId = FirebaseAuth.getInstance().getUid(); // Lấy ID người dùng hiện tại một lần

        if (getArguments() != null) {
            listingId = ProductDetailFragmentArgs.fromBundle(getArguments()).getListingId();
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

        initViews(view);
        setupToolbar();
        setupListeners();

        if (currentListing != null) { // Chế độ xem trước
            populateUI(currentListing);
            bottomActionBar.setVisibility(View.GONE);
            if (btnMoreOptions != null) {
                btnMoreOptions.setVisibility(View.GONE);
            }
        } else if (listingId != null) { // Chế độ xem chi tiết thực
            viewModel.loadListingDetail(listingId);
            observeViewModel();
        } else {
            Toast.makeText(getContext(), "Lỗi: Không có dữ liệu sản phẩm.", Toast.LENGTH_LONG).show();
            navController.popBackStack();
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
        btnChat = view.findViewById(R.id.btn_chat);
        bottomActionBar = view.findViewById(R.id.bottom_action_bar);
        sellerInfoLayout = view.findViewById(R.id.seller_info_layout);
        sellerAvatar = view.findViewById(R.id.seller_avatar);
        sellerName = view.findViewById(R.id.seller_name);
        btnMoreOptions = view.findViewById(R.id.btn_more_options);
    }

    private void setupToolbar() {
        if (getActivity() instanceof AppCompatActivity) {
            ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);
            Objects.requireNonNull(((AppCompatActivity) getActivity()).getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
            ((AppCompatActivity) getActivity()).getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
    }

    private void setupListeners() {
        toolbar.setNavigationOnClickListener(v -> navController.popBackStack());

        if (btnMoreOptions != null) {
            btnMoreOptions.setOnClickListener(v -> {
                if (currentListing != null && listingId != null) {
                    showListingOptionsDialog();
                }
            });
        } else if (listingId != null) {
            Log.e("ProductDetailFragment", "Lỗi: btnMoreOptions không được tìm thấy trong layout!");
        }

        btnMakeOffer.setOnClickListener(v -> {
            if (currentListing != null) {
                OfferBottomSheetDialogFragment bottomSheet =
                        OfferBottomSheetDialogFragment.newInstance(currentListing.getId(), currentListing.getSellerId());
                bottomSheet.show(getChildFragmentManager(), bottomSheet.getTag());
            }
        });

        btnChat.setOnClickListener(v -> {
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

        viewModel.getSellerProfile().observe(getViewLifecycleOwner(), seller -> {
            if (seller != null && getContext() != null) {
                sellerName.setText(seller.getName());
                Glide.with(getContext())
                        .load(seller.getProfileImageUrl())
                        .placeholder(R.drawable.ic_profile_placeholder)
                        .circleCrop()
                        .into(sellerAvatar);

                sellerInfoLayout.setOnClickListener(v -> {
                    // Sử dụng action đã được sửa lỗi trong mobile_navigation.xml
                    ProductDetailFragmentDirections.ActionProductDetailFragmentToPublicProfileFragment action =
                            ProductDetailFragmentDirections.actionProductDetailFragmentToPublicProfileFragment(seller.getId());
                    navController.navigate(action);
                });
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

        if (currentUserId != null && currentUserId.equals(listing.getSellerId())) {
            bottomActionBar.setVisibility(View.GONE);
        } else {
            bottomActionBar.setVisibility(View.VISIBLE);
        }
    }

    private void showListingOptionsDialog() {
        if (getContext() == null) return;
        final CharSequence[] options = {"Báo cáo tin đăng", "Hủy"};

        new AlertDialog.Builder(getContext())
                .setTitle("Tùy chọn")
                .setItems(options, (dialog, item) -> {
                    if (options[item].equals("Báo cáo tin đăng")) {
                        showReportReasonDialog();
                    } else {
                        dialog.dismiss();
                    }
                })
                .show();
    }

    private void showReportReasonDialog() {
        if (getContext() == null || currentListing == null) return;
        final String[] reportReasons = {"Thông tin sai sự thật", "Lừa đảo", "Hàng cấm/Bất hợp pháp", "Spam", "Lý do khác"};

        new AlertDialog.Builder(getContext())
                .setTitle("Báo cáo tin đăng")
                .setItems(reportReasons, (dialog, which) -> sendListingReport(reportReasons[which]))
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void sendListingReport(String reason) {
        if (currentUserId == null) {
            Toast.makeText(getContext(), "Bạn cần đăng nhập để thực hiện hành động này.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (currentListing == null || currentListing.getId() == null) {
            Toast.makeText(getContext(), "Lỗi: Không tìm thấy thông tin bài đăng.", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> report = new HashMap<>();
        report.put("reporterId", currentUserId);
        report.put("reportedListingId", currentListing.getId());
        report.put("reportedSellerId", currentListing.getSellerId());
        report.put("reason", reason);
        report.put("timestamp", FieldValue.serverTimestamp());
        report.put("type", "listing");
        report.put("status", "pending");

        FirebaseFirestore.getInstance().collection("reports").add(report)
                .addOnSuccessListener(documentReference ->
                        Toast.makeText(getContext(), "Cảm ơn bạn đã gửi báo cáo. Chúng tôi sẽ xem xét.", Toast.LENGTH_LONG).show()
                )
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Gửi báo cáo thất bại. Vui lòng thử lại.", Toast.LENGTH_SHORT).show()
                );
    }
}