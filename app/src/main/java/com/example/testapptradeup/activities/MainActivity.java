package com.example.testapptradeup.activities;

import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View; // Import View
import android.view.ViewTreeObserver; // Import ViewTreeObserver

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.example.testapptradeup.R;
import com.example.testapptradeup.models.User;
import com.example.testapptradeup.utils.SharedPrefsHelper;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private SharedPrefsHelper prefsHelper;
    private NavController navController;
    private BottomNavigationView navView; // Khai báo BottomNavigationView

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FirebaseApp.initializeApp(this);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        prefsHelper = new SharedPrefsHelper(this);

        // IMMEDIATE CHECK: If user is NOT logged in with Firebase Auth, redirect to LoginActivity.
        // This is the very first check to prevent unauthenticated access.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Log.d(TAG, "No user logged in (Firebase Auth). Redirecting to LoginActivity.");
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
            return; // IMPORTANT: Stop onCreate execution here if not logged in.
        }

        // If user is authenticated via Firebase Auth, ensure their data is in SharedPrefs.
        // This handles cases where app process was killed, but auth token is still valid.
        if (prefsHelper.getCurrentUser() == null) {
            Log.d(TAG, "User logged in (Auth), but data not in SharedPrefs. Loading from Firestore.");
            loadUserDataFromFirestore(currentUser.getUid());
            // The rest of onCreate will not execute immediately.
            // setupMainUI() will be called after Firestore data is loaded/fallback created.
        } else {
            Log.d(TAG, "User data found in SharedPrefs. Proceeding with UI setup.");
            setupMainUI(); // User data is already cached, proceed to set up UI.
        }
    }

    private void loadUserDataFromFirestore(String userId) {
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            user.setId(userId);
                            prefsHelper.saveCurrentUser(user);
                            Log.d(TAG, "User data loaded from Firestore and saved to SharedPrefs successfully.");
                        } else {
                            Log.e(TAG, "User object is null after conversion from Firestore for UID: " + userId);
                            createFallbackUserAndSave(userId, "Firestore conversion failed");
                        }
                    } else {
                        Log.w(TAG, "Firestore document not found for user: " + userId + ". Creating fallback default user.");
                        createFallbackUserAndSave(userId, "Firestore document not found");
                    }
                    // AFTER loading/creating user data, setup the UI.
                    setupMainUI();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading user data from Firestore in MainActivity: " + e.getMessage(), e);
                    // If Firestore load fails (e.g., network issue), still try to setup UI.
                    // ProfileFragment will attempt to load data or display default.
                    // For persistent network issues, consider a retry mechanism or redirect to Login.
                    setupMainUI();
                });
    }

    private void createFallbackUserAndSave(String userId, String reason) {
        Log.d(TAG, "Attempting to create fallback user: " + reason);
        FirebaseUser firebaseUser = mAuth.getCurrentUser();
        if (firebaseUser != null) {
            User fallbackUser = new User(
                    firebaseUser.getUid(),
                    firebaseUser.getDisplayName() != null ? firebaseUser.getDisplayName() : "Người dùng",
                    firebaseUser.getEmail(),
                    "", // phone default
                    "", // bio default
                    firebaseUser.getPhotoUrl() != null ? firebaseUser.getPhotoUrl().toString() : "",
                    "", // address default
                    0.0f, // rating default
                    0,    // reviewCount default
                    false, // isVerified default
                    "active", // accountStatus default
                    false, // isFlagged default
                    "not_connected", // walletStatus default
                    0    // notificationCount default
            );
            fallbackUser.setBankAccount(""); // Đảm bảo bankAccount được khởi tạo
            prefsHelper.saveCurrentUser(fallbackUser);
            Log.d(TAG, "Fallback user created and saved to SharedPrefs for UID: " + userId);
        } else {
            Log.e(TAG, "FirebaseUser is null when trying to create fallback user for " + userId + ". Cannot create fallback.");
            // If FirebaseUser is null here, it means we somehow lost auth state, best to re-authenticate.
            // This case should ideally be handled by the initial currentUser == null check.
            performLogout(); // Force logout and redirect to Login.
        }
    }


    private void setupMainUI() {
        setContentView(R.layout.activity_main);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        navView = findViewById(R.id.nav_view); // Ánh xạ BottomNavigationView

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        navController = Objects.requireNonNull(navHostFragment).getNavController();

        NavigationUI.setupWithNavController(navView, navController);

        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            if (getSupportActionBar() != null) {
                if (destination.getId() == R.id.navigation_home) {
                    getSupportActionBar().hide();
                } else {
                    getSupportActionBar().show();
                    getSupportActionBar().setTitle(destination.getLabel());
                }
            }
        });

        // Add a global layout listener to detect keyboard visibility changes
        final View rootView = getWindow().getDecorView().getRootView();
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            private final Rect r = new Rect();
            private int previousKeyboardHeight = 0;

            @Override
            public void onGlobalLayout() {
                // Determine the visible area of the screen.
                rootView.getWindowVisibleDisplayFrame(r);
                int screenHeight = rootView.getHeight();
                int keyboardHeight = screenHeight - r.bottom;

                // Check if keyboard is visible and if its height has changed significantly
                if (keyboardHeight > screenHeight * 0.15 && keyboardHeight != previousKeyboardHeight) { // Assume keyboard if height > 15% of screen
                    // Keyboard is visible
                    if (navView.getVisibility() == View.VISIBLE) {
                        navView.setVisibility(View.GONE);
                        Log.d(TAG, "Keyboard is visible, hiding BottomNavigationView.");
                    }
                } else if (keyboardHeight < screenHeight * 0.15 && previousKeyboardHeight > screenHeight * 0.15) {
                    // Keyboard is hidden
                    if (navView.getVisibility() == View.GONE) {
                        navView.setVisibility(View.VISIBLE);
                        Log.d(TAG, "Keyboard is hidden, showing BottomNavigationView.");
                    }
                }
                previousKeyboardHeight = keyboardHeight;
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (navController != null &&
                navController.getCurrentDestination() != null &&
                navController.getCurrentDestination().getId() != R.id.navigation_home) {
            getMenuInflater().inflate(R.menu.main_menu, menu);
            return true;
        }
        return false;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (navController != null &&
                navController.getCurrentDestination() != null &&
                navController.getCurrentDestination().getId() == R.id.navigation_home) {
            return false;
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_logout) {
            performLogout();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        return navController.navigateUp() || super.onSupportNavigateUp();
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        // This onStart check acts as a safeguard.
        // If currentUser is null (meaning logged out), redirect.
        // If currentUser is NOT null, but prefsHelper.getCurrentUser() IS null,
        // it means app was killed and restarted. In this case, load user data and setup UI.
        if (currentUser == null) {
            Log.d(TAG, "onStart: User logged out. Redirecting to LoginActivity.");
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        } else if (prefsHelper.getCurrentUser() == null) {
            Log.d(TAG, "onStart: User logged in, but data not in SharedPrefs. Re-loading.");
            loadUserDataFromFirestore(currentUser.getUid());
        } else {
            Log.d(TAG, "onStart: User logged in and data in SharedPrefs. UI should be ready.");
            // If already set up, do nothing. If onCreate skipped setup because of loading,
            // this might re-trigger it, but it's safe if setupMainUI handles multiple calls.
            // For now, let's assume onCreate handles initial setup.
        }
    }

    /**
     * Method to handle logout from any fragment or ActionBar menu.
     * Clears Firebase session and local user data, then redirects to LoginActivity.
     */
    public void performLogout() {
        Log.d(TAG, "Performing logout.");
        mAuth.signOut();
        prefsHelper.clearUserData();

        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
