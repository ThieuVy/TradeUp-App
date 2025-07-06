package com.example.testapptradeup.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.example.testapptradeup.R;
import com.example.testapptradeup.utils.SharedPrefsHelper;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private FirebaseAuth mAuth;
    private SharedPrefsHelper prefsHelper;
    private NavController navController;

    // --- UI Components cho BottomAppBar ---
    private CoordinatorLayout bottomBarContainer;
    private FloatingActionButton fabAdd;
    private LinearLayout menuHome, menuManage, menuNotification, menuProfile;
    private List<LinearLayout> menuItems;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        prefsHelper = new SharedPrefsHelper(this);

        // Kiểm tra người dùng đăng nhập ngay từ đầu
        if (mAuth.getCurrentUser() == null) {
            navigateToLogin();
            return; // Rất quan trọng: Dừng thực thi nếu chưa đăng nhập
        }

        setupUI();
        // Không cần tải lại dữ liệu user ở đây vì LoginActivity đã làm việc đó
    }

    private void setupUI() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        navController = Objects.requireNonNull(navHostFragment).getNavController();

        initCustomBottomBarViews();
        setupCustomBottomBarListeners();
    }

    private void initCustomBottomBarViews() {
        bottomBarContainer = findViewById(R.id.coordinatorLayout);
        fabAdd = findViewById(R.id.fab_add);
        menuHome = findViewById(R.id.menu_home);
        menuManage = findViewById(R.id.menu_manage);
        menuNotification = findViewById(R.id.menu_notification);
        menuProfile = findViewById(R.id.menu_profile);
        menuItems = Arrays.asList(menuHome, menuManage, menuNotification, menuProfile);
    }

    // Trong MainActivity.java

    private void setupCustomBottomBarListeners() {
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            int destinationId = destination.getId();

            // ========== BẮT ĐẦU PHẦN SỬA LỖI ==========
            // Thêm R.id.postFragment vào danh sách các màn hình cấp 1
            List<Integer> topLevelDestinations = Arrays.asList(
                    R.id.navigation_home,
                    R.id.myListingsFragment,
                    R.id.postFragment, // <-- THÊM DÒNG NÀY VÀO ĐÂY
                    R.id.notificationsFragment,
                    R.id.navigation_profile
            );
            // ==========================================

            // Kiểm tra xem destination hiện tại có nằm trong danh sách trên không
            if (topLevelDestinations.contains(destinationId)) {
                bottomBarContainer.setVisibility(View.VISIBLE);

                // Cập nhật trạng thái 'selected' cho các item.
                // Vì PostFragment không phải là một item trên BottomAppBar,
                // chúng ta không cần thêm logic `updateSelectedMenuUI` cho nó.
                // Các màn hình khác vẫn được cập nhật bình thường.
                if (destinationId == R.id.navigation_home) updateSelectedMenuUI(menuHome);
                else if (destinationId == R.id.myListingsFragment) updateSelectedMenuUI(menuManage);
                else if (destinationId == R.id.notificationsFragment) updateSelectedMenuUI(menuNotification);
                else if (destinationId == R.id.navigation_profile) updateSelectedMenuUI(menuProfile);

            } else {
                // Nếu là các màn hình con khác (Chat, Search, Detail...), ẩn BottomAppBar
                bottomBarContainer.setVisibility(View.GONE);
            }
        });

        // Gán sự kiện click để điều hướng (giữ nguyên)
        menuHome.setOnClickListener(v -> navigateTo(R.id.navigation_home));
        menuManage.setOnClickListener(v -> navigateTo(R.id.myListingsFragment));
        menuNotification.setOnClickListener(v -> navigateTo(R.id.notificationsFragment));
        menuProfile.setOnClickListener(v -> navigateTo(R.id.navigation_profile));
        fabAdd.setOnClickListener(v -> navigateTo(R.id.postFragment));
    }

    // Điều hướng an toàn, tránh click lặp lại
    private void navigateTo(int destinationId) {
        if (navController.getCurrentDestination() != null && navController.getCurrentDestination().getId() != destinationId) {
            navController.navigate(destinationId);
        }
    }

    // Cập nhật UI cho item được chọn
    private void updateSelectedMenuUI(View selectedContainer) {
        int selectedColor = ContextCompat.getColor(this, R.color.purple_500);
        int unselectedColor = ContextCompat.getColor(this, R.color.text_secondary);

        for (LinearLayout container : menuItems) {
            ImageView icon = (ImageView) container.getChildAt(0);
            TextView text = (TextView) container.getChildAt(1);

            if (container == selectedContainer) {
                icon.setColorFilter(selectedColor);
                text.setTextColor(selectedColor);
            } else {
                icon.setColorFilter(unselectedColor);
                text.setTextColor(unselectedColor);
            }
        }
    }

    private void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    // Hàm đăng xuất, có thể gọi từ ProfileFragment
    public void performLogout() {
        mAuth.signOut();
        prefsHelper.clearUserData();
        navigateToLogin();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Kiểm tra lại mỗi khi activity quay trở lại foreground
        // Nếu người dùng đã bị đăng xuất do timeout, hãy đưa họ về màn hình login.
        if (mAuth.getCurrentUser() == null) {
            navigateToLogin();
        }
    }
}