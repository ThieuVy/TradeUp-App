package com.example.testapptradeup.activities;

import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.example.testapptradeup.R;
import com.example.testapptradeup.models.User;
import com.example.testapptradeup.utils.SharedPrefsHelper;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private SharedPrefsHelper prefsHelper;
    private NavController navController;

    // --- UI Components cho BottomAppBar ---
    private FloatingActionButton fabAdd;
    private LinearLayout menuHome, menuManage, menuNotification, menuProfile;
    private List<LinearLayout> menuContainers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FirebaseApp.initializeApp(this);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        prefsHelper = new SharedPrefsHelper(this);

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Log.d(TAG, "No user logged in. Redirecting to LoginActivity.");
            navigateToLogin();
            return;
        }

        if (prefsHelper.getCurrentUser() == null) {
            loadUserDataFromFirestore(currentUser.getUid());
        } else {
            // Chỉ gọi một hàm setupUI duy nhất
            setupUI();
        }
    }

    // ========== PHẦN CODE ĐÃ ĐƯỢC HỢP NHẤT VÀ SỬA LỖI ==========
    private void setupUI() {
        setContentView(R.layout.activity_main);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Khởi tạo NavController
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        navController = Objects.requireNonNull(navHostFragment).getNavController();

        // Ánh xạ và cài đặt cho BottomAppBar
        initCustomBottomBarViews();
        setupCustomBottomBarListeners();

        // Cài đặt listener để ẩn/hiện BottomAppBar khi bàn phím xuất hiện
        setupKeyboardListener();
    }

    private void initCustomBottomBarViews() {
        fabAdd = findViewById(R.id.fab_add);

        menuHome = findViewById(R.id.menu_home);
        menuManage = findViewById(R.id.menu_manage);
        menuNotification = findViewById(R.id.menu_notification);
        menuProfile = findViewById(R.id.menu_profile);

        // Gom các container vào một list để dễ quản lý
        menuContainers = new ArrayList<>();
        menuContainers.add(menuHome);
        menuContainers.add(menuManage);
        menuContainers.add(menuNotification);
        menuContainers.add(menuProfile);
    }

    private void setupCustomBottomBarListeners() {
        // Lắng nghe sự thay đổi màn hình (fragment) để cập nhật UI cho thanh nav
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            int destinationId = destination.getId();
            if (destinationId == R.id.navigation_home) {
                updateSelectedMenuUI(menuHome);
            } else if (destinationId == R.id.myListingsFragment) {
                updateSelectedMenuUI(menuManage);
            } else if (destinationId == R.id.notificationsFragment) {
                updateSelectedMenuUI(menuNotification);
            } else if (destinationId == R.id.navigation_profile) {
                updateSelectedMenuUI(menuProfile);
            } else {
                // Nếu là màn hình khác không có trong bottom bar (ví dụ PostFragment), bỏ chọn tất cả
                updateSelectedMenuUI(null);
            }
        });

        // Gán sự kiện click để điều hướng
        menuHome.setOnClickListener(v -> navigateTo(R.id.navigation_home));
        menuManage.setOnClickListener(v -> navigateTo(R.id.myListingsFragment));
        menuNotification.setOnClickListener(v -> navigateTo(R.id.notificationsFragment));
        menuProfile.setOnClickListener(v -> navigateTo(R.id.navigation_profile));

        fabAdd.setOnClickListener(v -> {
            navigateTo(R.id.postFragment);
            Toast.makeText(this, "Mở màn hình đăng tin", Toast.LENGTH_SHORT).show();
        });
    }

    /**
     * Hàm điều hướng, kiểm tra để tránh click liên tục vào cùng một item
     * @param destinationId ID của destination cần điều hướng đến
     */
    private void navigateTo(int destinationId) {
        if (navController.getCurrentDestination() != null && navController.getCurrentDestination().getId() != destinationId) {
            navController.navigate(destinationId);
        }
    }

    private void updateSelectedMenuUI(View selectedContainer) {
        int selectedColor = ContextCompat.getColor(this, R.color.charcoal_black);
        int unselectedColor = ContextCompat.getColor(this, R.color.charcoal_black);

        for (View container : menuContainers) {
            LinearLayout linearLayout = (LinearLayout) container;
            ImageView icon = (ImageView) linearLayout.getChildAt(0);
            TextView text = (TextView) linearLayout.getChildAt(1);

            if (container == selectedContainer) {
                icon.setColorFilter(selectedColor);
                text.setTextColor(selectedColor);
            } else {
                icon.setColorFilter(unselectedColor);
                text.setTextColor(unselectedColor);
            }
        }
    }

    private void setupKeyboardListener() {
        final View rootView = getWindow().getDecorView().getRootView();
        final View bottomBarContainer = findViewById(R.id.coordinatorLayout);

        rootView.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            Rect r = new Rect();
            rootView.getWindowVisibleDisplayFrame(r);
            int screenHeight = rootView.getHeight();
            int keypadHeight = screenHeight - r.bottom;

            if (keypadHeight > screenHeight * 0.15) {
                if (bottomBarContainer.getVisibility() == View.VISIBLE) {
                    bottomBarContainer.setVisibility(View.GONE);
                }
            } else {
                if (bottomBarContainer.getVisibility() == View.GONE) {
                    bottomBarContainer.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    // ========== CÁC HÀM XỬ LÝ USER VÀ LOGOUT (KHÔNG THAY ĐỔI) ==========

    private void loadUserDataFromFirestore(String userId) {
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            user.setId(userId);
                            prefsHelper.saveCurrentUser(user);
                        } else {
                            createFallbackUserAndSave(userId, "Firestore conversion failed");
                        }
                    } else {
                        createFallbackUserAndSave(userId, "Firestore document not found");
                    }
                    setupUI();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading user data from Firestore: " + e.getMessage(), e);
                    setupUI();
                });
    }

    private void createFallbackUserAndSave(String userId, String reason) {
        FirebaseUser firebaseUser = mAuth.getCurrentUser();
        if (firebaseUser != null) {
            User fallbackUser = new User(
                    firebaseUser.getUid(),
                    firebaseUser.getDisplayName() != null ? firebaseUser.getDisplayName() : "Người dùng",
                    firebaseUser.getEmail(), "", "",
                    firebaseUser.getPhotoUrl() != null ? firebaseUser.getPhotoUrl().toString() : "",
                    "", 0.0f, 0, false, "active", false, "not_connected", 0
            );
            fallbackUser.setBankAccount("");
            prefsHelper.saveCurrentUser(fallbackUser);
        } else {
            performLogout();
        }
    }

    private void navigateToLogin() {
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    public void performLogout() {
        mAuth.signOut();
        prefsHelper.clearUserData();
        navigateToLogin();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mAuth.getCurrentUser() == null) {
            navigateToLogin();
        }
    }
}