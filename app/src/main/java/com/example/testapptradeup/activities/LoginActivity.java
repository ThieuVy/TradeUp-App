package com.example.testapptradeup.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialException;

import com.example.testapptradeup.R;
import com.example.testapptradeup.models.User;
import com.example.testapptradeup.utils.SharedPrefsHelper;
import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity"; // For logging

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private SharedPrefsHelper prefsHelper;

    private TextInputEditText editEmail, editPassword;
    private MaterialButton loginButton;
    private ProgressBar progressBar;
    private TextView registerLink;
    private ImageView googleLogin;
    private CredentialManager credentialManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        // Initialize Firebase Firestore
        db = FirebaseFirestore.getInstance();
        // Initialize SharedPrefsHelper
        prefsHelper = new SharedPrefsHelper(this);
        // Initialize Credential Manager
        credentialManager = CredentialManager.create(this);

        // Initialize views
        initViews();

        // Setup listeners
        setupListeners();
    }

    private void initViews() {
        editEmail = findViewById(R.id.email);
        editPassword = findViewById(R.id.password);
        loginButton = findViewById(R.id.login_button);
        progressBar = findViewById(R.id.progressBar);
        registerLink = findViewById(R.id.sign_up_link);
        googleLogin = findViewById(R.id.google_login);
    }

    private void setupListeners() {
        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                checkFieldsForEmptyValues();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };

        editEmail.addTextChangedListener(textWatcher);
        editPassword.addTextChangedListener(textWatcher);

        registerLink.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
            finish();
        });

        loginButton.setOnClickListener(v -> performEmailPasswordLogin());
        googleLogin.setOnClickListener(v -> performGoogleSignIn());
    }

    private void performEmailPasswordLogin() {
        String email = Objects.requireNonNull(editEmail.getText()).toString().trim();
        String password = Objects.requireNonNull(editPassword.getText()).toString();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(LoginActivity.this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(LoginActivity.this, "Email không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    showLoading(false);

                    if (task.isSuccessful()) {
                        Toast.makeText(LoginActivity.this, "Đăng nhập thành công", Toast.LENGTH_SHORT).show();
                        loadAndNavigateUser(Objects.requireNonNull(mAuth.getCurrentUser()).getUid());
                    } else {
                        String errorMessage = getFirebaseErrorMessage(task.getException());
                        Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void performGoogleSignIn() {
        showLoading(true);

        GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(getString(R.string.default_web_client_id))
                .build();

        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build();

        credentialManager.getCredentialAsync(
                this,
                request,
                null,
                Executors.newSingleThreadExecutor(),
                new CredentialManagerCallback<>() {
                    @Override
                    public void onResult(GetCredentialResponse result) {
                        runOnUiThread(() -> handleGoogleCredentialResponse(result));
                    }

                    @Override
                    public void onError(@NonNull GetCredentialException e) {
                        runOnUiThread(() -> {
                            showLoading(false);
                            Log.e(TAG, "Google Sign-In failed", e);
                            Toast.makeText(LoginActivity.this, "Đăng nhập Google thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
                    }
                }
        );
    }

    private void handleGoogleCredentialResponse(GetCredentialResponse result) {
        GoogleIdTokenCredential googleIdTokenCredential = GoogleIdTokenCredential
                .createFrom(result.getCredential().getData());

        String idToken = googleIdTokenCredential.getIdToken();
        firebaseAuthWithGoogle(idToken);
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    showLoading(false);

                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            if (task.getResult().getAdditionalUserInfo() != null && task.getResult().getAdditionalUserInfo().isNewUser()) {
                                Log.d(TAG, "New Google user. Saving data to Firestore.");
                                // Use the User model constructor for consistency and completeness
                                User newUser = new User(
                                        user.getUid(),
                                        user.getDisplayName() != null ? user.getDisplayName() : "Người dùng Google",
                                        user.getEmail(),
                                        "", // phone default
                                        "", // bio default
                                        user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : "",
                                        "", // address default
                                        0.0f, // rating default
                                        0,    // reviewCount default
                                        false, // isVerified default
                                        "active", // accountStatus default
                                        false, // isFlagged default
                                        "not_connected", // walletStatus default
                                        0    // notificationCount default
                                );

                                // Save to Firestore
                                db.collection("users").document(user.getUid())
                                        .set(newUser) // Save the full User object
                                        .addOnSuccessListener(aVoid -> {
                                            Toast.makeText(LoginActivity.this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();
                                            prefsHelper.saveCurrentUser(newUser); // Save to SharedPrefs
                                            navigateToMainActivity();
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e(TAG, "Error saving new Google user to Firestore: " + e.getMessage(), e);
                                            Toast.makeText(LoginActivity.this, "Lỗi khi lưu thông tin người dùng: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                            // Even if Firestore save fails, try to load/navigate based on current Auth state
                                            loadAndNavigateUser(user.getUid()); // This will likely create a fallback user
                                        });
                            } else {
                                Toast.makeText(LoginActivity.this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();
                                loadAndNavigateUser(user.getUid());
                            }
                        }
                    } else {
                        String errorMessage = getFirebaseErrorMessage(task.getException());
                        Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadAndNavigateUser(String userId) {
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            user.setId(userId); // Ensure ID is set as it's excluded from Firestore mapping
                            prefsHelper.saveCurrentUser(user);
                            Log.d(TAG, "User data loaded from Firestore and saved to SharedPrefs for UID: " + userId);
                        } else {
                            Log.e(TAG, "User object is null after conversion from Firestore for UID: " + userId);
                            // Fallback: Create a basic user object from FirebaseUser if Firestore object is null
                            createFallbackUserAndNavigate(userId, "User object null from Firestore");
                        }
                    } else {
                        // User in Auth but no Firestore document (e.g., deleted document, or race condition on first login)
                        Log.w(TAG, "Firestore document not found for user: " + userId + ". Creating fallback default user.");
                        createFallbackUserAndNavigate(userId, "Firestore document not found");
                    }
                    navigateToMainActivity(); // Always navigate after attempting to load/create user data
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading user data from Firestore during navigation: " + e.getMessage(), e);
                    Toast.makeText(LoginActivity.this, "Lỗi tải thông tin người dùng: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    // Still navigate even if user data couldn't be loaded, MainActivity will re-check auth
                    navigateToMainActivity();
                });
    }

    // Helper method to create a fallback user and navigate
    private void createFallbackUserAndNavigate(String userId, String reason) {
        Log.d(TAG, "Creating fallback user: " + reason);
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
            prefsHelper.saveCurrentUser(fallbackUser);
            Log.d(TAG, "Created fallback user and saved to SharedPrefs.");
        } else {
            Log.e(TAG, "FirebaseUser is null when trying to create fallback user for " + userId);
        }
        // No explicit navigateToMainActivity() call here, as loadAndNavigateUser already calls it
    }


    private String getFirebaseErrorMessage(Exception exception) {
        String errorMessage = "Đăng nhập thất bại";
        if (exception != null) {
            String error = exception.getMessage();
            if (error != null) {
                if (error.contains("user not found") || error.contains("invalid-user") || error.contains("no user record")) {
                    errorMessage = "Tài khoản không tồn tại";
                } else if (error.contains("wrong-password") || error.contains("invalid-credential")) {
                    errorMessage = "Mật khẩu không đúng";
                } else if (error.contains("invalid-email")) {
                    errorMessage = "Email không hợp lệ";
                } else if (error.contains("user-disabled")) {
                    errorMessage = "Tài khoản đã bị khóa";
                } else if (error.contains("too-many-requests")) {
                    errorMessage = "Quá nhiều lần thử. Vui lòng thử lại sau";
                } else if (error.contains("network-request-failed")) {
                    errorMessage = "Lỗi kết nối mạng. Vui lòng kiểm tra internet";
                } else {
                    errorMessage = "Lỗi không xác định: " + error;
                }
            }
        }
        return errorMessage;
    }

    private void showLoading(boolean isLoading) {
        if (isLoading) {
            progressBar.setVisibility(View.VISIBLE);
            loginButton.setEnabled(false);
            googleLogin.setEnabled(false);
        } else {
            progressBar.setVisibility(View.GONE);
            loginButton.setEnabled(true);
            googleLogin.setEnabled(true);
            checkFieldsForEmptyValues(); // Re-check fields for login button
        }
    }

    private void navigateToMainActivity() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    private void checkFieldsForEmptyValues() {
        String email = Objects.requireNonNull(editEmail.getText()).toString().trim();
        String password = Objects.requireNonNull(editPassword.getText()).toString();

        loginButton.setEnabled(!TextUtils.isEmpty(email) && !TextUtils.isEmpty(password));
    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is already signed in on activity start
        FirebaseUser firebaseUser = mAuth.getCurrentUser();
        if (firebaseUser != null) {
            // User is signed in with Firebase Auth. Now check if their data is in SharedPrefs.
            if (prefsHelper.getCurrentUser() == null) {
                Log.d(TAG, "User logged in (Firebase Auth), but data not in SharedPrefs. Loading from Firestore.");
                // Load user data from Firestore and then navigate
                loadAndNavigateUser(firebaseUser.getUid());
            } else {
                Log.d(TAG, "User data already in SharedPrefs. Navigating to MainActivity.");
                // User data is already cached, just navigate
                navigateToMainActivity();
            }
        }
        // If firebaseUser is null, user is not logged in, remain on LoginActivity.
    }
}
