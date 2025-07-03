package com.example.testapptradeup.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import com.bumptech.glide.Glide;
import com.example.testapptradeup.R;
import com.example.testapptradeup.viewmodels.ProductDetailViewModel;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.appbar.MaterialToolbar;

public class ProductDetailFragment extends Fragment {
    private ProductDetailViewModel viewModel;
    private NavController navController;
    private String listingId;

    private ImageView productImage;
    private TextView productTitle, productPrice, productDescription;
    private CollapsingToolbarLayout collapsingToolbar;
    private MaterialToolbar toolbar;

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

        toolbar.setNavigationOnClickListener(v -> navController.popBackStack());

        if (listingId != null) {
            viewModel.loadListingDetail(listingId);
            observeViewModel();
        }
    }

    private void initViews(View view) {
        productImage = view.findViewById(R.id.product_image);
        productTitle = view.findViewById(R.id.product_title);
        productPrice = view.findViewById(R.id.product_price);
        productDescription = view.findViewById(R.id.product_description);
        collapsingToolbar = view.findViewById(R.id.collapsing_toolbar);
        toolbar = view.findViewById(R.id.toolbar);
    }

    private void observeViewModel() {
        viewModel.listingDetail.observe(getViewLifecycleOwner(), listing -> {
            if (listing != null) {
                collapsingToolbar.setTitle(listing.getTitle());
                productTitle.setText(listing.getTitle());
                productPrice.setText(listing.getFormattedPrice());
                productDescription.setText(listing.getDescription());
                if (getContext() != null) {
                    Glide.with(getContext()).load(listing.getPrimaryImageUrl()).into(productImage);
                }
            }
        });
    }
}