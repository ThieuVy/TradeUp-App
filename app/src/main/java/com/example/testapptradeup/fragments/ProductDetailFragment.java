package com.example.testapptradeup.fragments;

import android.app.AlertDialog;
import android.os.Bundle;
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
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.example.testapptradeup.R;
import com.example.testapptradeup.adapters.ImagePagerAdapter;
import com.example.testapptradeup.adapters.PublicReviewAdapter;
import com.example.testapptradeup.models.Listing;
import com.example.testapptradeup.viewmodels.ProductDetailViewModel;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import me.relex.circleindicator.CircleIndicator3;

public class ProductDetailFragment extends Fragment {
    private ProductDetailViewModel viewModel;
    private NavController navController;
    private String listingId;
    private Listing currentListing;
    private String currentUserId;

    // UI Views
    private ViewPager2 productImagePager;
    private ImagePagerAdapter pagerAdapter;
    private CircleIndicator3 pagerIndicator;
    private TextView productTitle, productPrice, productDescription;
    private CollapsingToolbarLayout collapsingToolbar;
    private MaterialToolbar toolbar;
    private Button btnMakeOffer, btnChat;
    private View bottomActionBar;
    private LinearLayout sellerInfoLayout;
    private ImageView sellerAvatar;
    private TextView sellerName;
    private LinearLayout productRatingLayout;
    private TextView productRatingStars, productReviewCount;
    private RecyclerView recyclerSellerReviews;
    private PublicReviewAdapter reviewAdapter;
    private TextView emptyReviewsText;
    private Button btnBuyNow;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(ProductDetailViewModel.class);
        currentUserId = FirebaseAuth.getInstance().getUid();

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
        setupViewPager();
        setupReviewRecyclerView();
        setupListeners();

        if (currentListing != null) { // Chế độ xem trước
            populateUI(currentListing);
            bottomActionBar.setVisibility(View.GONE);
            toolbar.getMenu().findItem(R.id.action_more_options).setVisible(false);
        } else if (listingId != null) { // Chế độ xem chi tiết
            observeViewModel();
            viewModel.loadListingDetail(listingId);
        } else {
            Toast.makeText(getContext(), "Lỗi: Không có dữ liệu sản phẩm.", Toast.LENGTH_LONG).show();
            navController.popBackStack();
        }
    }

    private void initViews(View view) {
        productImagePager = view.findViewById(R.id.product_image_pager);
        pagerIndicator = view.findViewById(R.id.pager_indicator);
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
        productRatingLayout = view.findViewById(R.id.product_rating_layout);
        productRatingStars = view.findViewById(R.id.product_rating_stars);
        productReviewCount = view.findViewById(R.id.product_review_count);
        recyclerSellerReviews = view.findViewById(R.id.recycler_seller_reviews);
        emptyReviewsText = view.findViewById(R.id.empty_reviews_text);
        btnBuyNow = view.findViewById(R.id.btn_buy_now);
    }

    private void setupReviewRecyclerView() {
        reviewAdapter = new PublicReviewAdapter();
        recyclerSellerReviews.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerSellerReviews.setNestedScrollingEnabled(false); // Quan trọng cho NestedScrollView
        recyclerSellerReviews.setAdapter(reviewAdapter);
    }

    private void setupViewPager() {
        pagerAdapter = new ImagePagerAdapter(new ArrayList<>());
        productImagePager.setAdapter(pagerAdapter);
        pagerIndicator.setViewPager(productImagePager);
    }

    // <<< SỬA LỖI 1 & 3: Hợp nhất tất cả các listener vào một nơi >>>
    private void setupListeners() {
        // Cài đặt cho Toolbar
        toolbar.setNavigationOnClickListener(v -> navController.popBackStack());

        // Cài đặt cho Menu
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_more_options) {
                showListingOptionsDialog();
                return true;
            }
            return false;
        });

        // Cài đặt cho nút Trả giá
        btnMakeOffer.setOnClickListener(v -> {
            if (currentListing != null) {
                OfferBottomSheetDialogFragment bottomSheet =
                        OfferBottomSheetDialogFragment.newInstance(currentListing.getId(), currentListing.getSellerId());
                bottomSheet.show(getChildFragmentManager(), bottomSheet.getTag());
            }
        });

        // Cài đặt cho nút Chat
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
        btnBuyNow.setOnClickListener(v -> {
            if (currentListing != null) {
                showBuyNowConfirmationDialog();
            } else {
                Toast.makeText(getContext(), "Dữ liệu sản phẩm chưa sẵn sàng.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Hiển thị dialog xác nhận trước khi chuyển đến màn hình thanh toán.
     */
    private void showBuyNowConfirmationDialog() {
        if (getContext() == null || currentListing == null) return;

        new AlertDialog.Builder(getContext())
                .setTitle("Xác nhận mua hàng")
                .setMessage("Bạn có chắc chắn muốn mua sản phẩm \"" + currentListing.getTitle() + "\" với giá " + currentListing.getFormattedPrice() + "?")
                .setPositiveButton("Mua ngay", (dialog, which) -> {
                    // Người dùng đã xác nhận, điều hướng đến màn hình thanh toán
                    navigateToPayment();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    /**
     * Điều hướng đến màn hình PaymentFragment, truyền các dữ liệu cần thiết.
     */
    private void navigateToPayment() {
        if (navController != null && currentListing != null) {
            // Tạo một "offer" ảo với giá gốc của sản phẩm để luồng thanh toán có thể xử lý
            // ID của offer ảo này có thể là một chuỗi đặc biệt, ví dụ: "BUY_NOW_" + listingId
            String virtualOfferId = "BUY_NOW_" + currentListing.getId();

            ProductDetailFragmentDirections.ActionProductDetailFragmentToPaymentFragment action =
                    ProductDetailFragmentDirections.actionProductDetailFragmentToPaymentFragment(
                            currentListing.getId(),
                            currentListing.getSellerId(),
                            (float) currentListing.getPrice(),
                            virtualOfferId
                    );
            navController.navigate(action);
        } else {
            Toast.makeText(getContext(), "Lỗi: Không thể xử lý thanh toán.", Toast.LENGTH_SHORT).show();
        }
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
                    ProductDetailFragmentDirections.ActionProductDetailFragmentToPublicProfileFragment action =
                            ProductDetailFragmentDirections.actionProductDetailFragmentToPublicProfileFragment(seller.getId());
                    navController.navigate(action);
                });
            }
        });
        viewModel.getSellerReviews().observe(getViewLifecycleOwner(), reviews -> {
            if (reviews != null && !reviews.isEmpty()) {
                reviewAdapter.submitList(reviews);
                recyclerSellerReviews.setVisibility(View.VISIBLE);
                emptyReviewsText.setVisibility(View.GONE);
            } else {
                recyclerSellerReviews.setVisibility(View.GONE);
                emptyReviewsText.setVisibility(View.VISIBLE);
            }
        });
    }

    private void populateUI(Listing listing) {
        if (listing == null || getContext() == null) return;

        collapsingToolbar.setTitle(listing.getTitle());
        productTitle.setText(listing.getTitle());
        productPrice.setText(listing.getFormattedPrice());
        productDescription.setText(listing.getDescription());

        if (listing.getReviewCount() > 0) {
            productRatingLayout.setVisibility(View.VISIBLE);
            productRatingStars.setText(getStarString(listing.getRating()));
            productReviewCount.setText(String.format(Locale.getDefault(), "(%d đánh giá)", listing.getReviewCount()));
        } else {
            productRatingLayout.setVisibility(View.GONE);
        }

        if (listing.getImageUrls() != null && !listing.getImageUrls().isEmpty()) {
            pagerAdapter.setImageUrls(listing.getImageUrls());
            pagerIndicator.setVisibility(listing.getImageUrls().size() > 1 ? View.VISIBLE : View.GONE);
        }

        boolean isOwner = currentUserId != null && currentUserId.equals(listing.getSellerId());
        bottomActionBar.setVisibility(isOwner ? View.GONE : View.VISIBLE);
        toolbar.getMenu().findItem(R.id.action_more_options).setVisible(!isOwner);
    }

    /**
     * Hàm helper để chuyển đổi số rating thành chuỗi các ngôi sao.
     * @param rating Điểm đánh giá (ví dụ: 4.7f)
     * @return Chuỗi các ngôi sao (ví dụ: "★★★★★")
     */
    private String getStarString(float rating) {
        int fullStars = Math.round(rating);
        StringBuilder stars = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            stars.append(i < fullStars ? "★" : "☆");
        }
        return stars.toString();
    }

    // Các hàm dialog và báo cáo giữ nguyên không thay đổi
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