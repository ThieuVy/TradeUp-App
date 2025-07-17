package com.example.testapptradeup.activities;

import android.Manifest;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.content.Intent;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.example.testapptradeup.R;
import com.example.testapptradeup.models.User;
import com.example.testapptradeup.utils.SharedPrefsHelper;
import com.example.testapptradeup.viewmodels.MainViewModel;
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
    private MainViewModel mainViewModel;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Log.d(TAG, "Quyền POST_NOTIFICATIONS đã được cấp.");
                } else {
                    Toast.makeText(this, "Bạn sẽ không nhận được thông báo quan trọng.", Toast.LENGTH_SHORT).show();
                }
            });

    private void askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                    new AlertDialog.Builder(this)
                            .setTitle("Cần quyền gửi thông báo")
                            .setMessage("Chúng tôi cần quyền này để gửi cho bạn các cập nhật quan trọng về tin nhắn và ưu đãi. Vui lòng cấp quyền.")
                            .setPositiveButton("OK", (dialog, which) -> requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS))
                            .setNegativeButton("Hủy", null)
                            .show();
                } else {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                }
            } else {
                Log.d(TAG, "Quyền POST_NOTIFICATIONS đã được cấp trước đó.");
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        prefsHelper = new SharedPrefsHelper(this);
        mainViewModel = new ViewModelProvider(this).get(MainViewModel.class);

        if (mAuth.getCurrentUser() == null) {
            navigateToLogin();
            return;
        }

        askNotificationPermission();

        loadUserIntoViewModel();
        setupUI();
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

    private void setupCustomBottomBarListeners() {
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            int destinationId = destination.getId();

            List<Integer> topLevelDestinations = Arrays.asList(
                    R.id.navigation_home,
                    R.id.myListingsFragment,
                    R.id.postFragment,
                    R.id.notificationsFragment,
                    R.id.navigation_profile
            );

            if (topLevelDestinations.contains(destinationId)) {
                bottomBarContainer.setVisibility(View.VISIBLE);

                if (destinationId == R.id.navigation_home) updateSelectedMenuUI(menuHome);
                else if (destinationId == R.id.myListingsFragment) updateSelectedMenuUI(menuManage);
                else if (destinationId == R.id.notificationsFragment) updateSelectedMenuUI(menuNotification);
                else if (destinationId == R.id.navigation_profile) updateSelectedMenuUI(menuProfile);

            } else {
                bottomBarContainer.setVisibility(View.GONE);
            }
        });

        menuHome.setOnClickListener(v -> navigateTo(R.id.navigation_home));
        menuManage.setOnClickListener(v -> navigateTo(R.id.myListingsFragment));
        menuNotification.setOnClickListener(v -> navigateTo(R.id.notificationsFragment));
        menuProfile.setOnClickListener(v -> navigateTo(R.id.navigation_profile));
        fabAdd.setOnClickListener(v -> navigateTo(R.id.postFragment));
    }

    private void navigateTo(int destinationId) {
        if (navController.getCurrentDestination() != null && navController.getCurrentDestination().getId() != destinationId) {
            navController.navigate(destinationId);
        }
    }

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

    private void loadUserIntoViewModel() {
        User user = prefsHelper.getCurrentUser();
        if (user != null) {
            mainViewModel.setCurrentUser(user);
        } else {
            performLogout();
        }
    }

    private void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    public void performLogout() {
        mAuth.signOut();
        prefsHelper.clearUserData();

        mainViewModel.setCurrentUser(null);

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