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
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Objects;
import java.util.concurrent.Executors;

public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = "RegisterActivity";

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private SharedPrefsHelper prefsHelper;

    private TextInputEditText editEmail;
    private TextInputEditText editPassword;
    private TextInputEditText editConfirmPassword;
    private MaterialButton signUpButton;
    private ProgressBar progressBar;
    private TextView loginLink;
    private ImageView googleLogin;
    private CredentialManager credentialManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        prefsHelper = new SharedPrefsHelper(this);
        credentialManager = CredentialManager.create(this);

        initViews();
        setupListeners();
    }

    private void initViews() {
        editEmail = findViewById(R.id.email);
        editPassword = findViewById(R.id.password);
        editConfirmPassword = findViewById(R.id.confirm_password);
        signUpButton = findViewById(R.id.sign_up_button);
        progressBar = findViewById(R.id.progressBar);
        loginLink = findViewById(R.id.login_link);
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
        editConfirmPassword.addTextChangedListener(textWatcher);

        loginLink.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });

        signUpButton.setOnClickListener(v -> performRegistration());
        googleLogin.setOnClickListener(v -> performGoogleSignIn());
    }

    private void performRegistration() {
        String email = Objects.requireNonNull(editEmail.getText()).toString().trim();
        String password = Objects.requireNonNull(editPassword.getText()).toString();
        String confirmPassword = Objects.requireNonNull(editConfirmPassword.getText()).toString();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password) || TextUtils.isEmpty(confirmPassword)) {
            Toast.makeText(RegisterActivity.this, "Vui lòng nhập đầy đủ email, mật khẩu và xác nhận mật khẩu", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(RegisterActivity.this, "Email không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(RegisterActivity.this, "Mật khẩu phải có ít nhất 6 ký tự", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(RegisterActivity.this, "Mật khẩu xác nhận không khớp", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isPasswordStrong(password)) {
            Toast.makeText(RegisterActivity.this, "Mật khẩu phải chứa ít nhất một chữ cái và một số", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // For email/password registration, default name to email prefix or generic
                            String name = user.getEmail() != null ? user.getEmail().split("@")[0] : "Người dùng mới";
                            saveUserToFirestoreAndNavigate(user.getUid(), name, user.getEmail(), user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : "");
                        }
                    } else {
                        showLoading(false);
                        String errorMessage = getFirebaseErrorMessage(task);
                        Toast.makeText(RegisterActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
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
                            Toast.makeText(RegisterActivity.this, "Đăng nhập Google thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
                                String name = user.getDisplayName() != null ? user.getDisplayName() : "Người dùng Google";
                                String email = user.getEmail();
                                String profileImageUrl = user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : "";
                                saveUserToFirestoreAndNavigate(user.getUid(), name, email, profileImageUrl);
                            } else {
                                Log.d(TAG, "Existing Google user. Navigating.");
                                loadAndNavigateUser(user.getUid());
                            }
                        }
                    } else {
                        String errorMessage = getFirebaseErrorMessage(task);
                        Toast.makeText(RegisterActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveUserToFirestoreAndNavigate(String userId, String name, String email, String profileImageUrl) {
        User newUser = new User(
                userId, name, email,
                "", // phone default
                "", // bio default
                profileImageUrl,
                "", // address default
                0.0f, // rating default
                0,    // reviewCount default
                false, // isVerified default
                "active", // accountStatus default
                false, // isFlagged default
                "not_connected", // walletStatus default
                0    // notificationCount default
        );

        db.collection("users").document(userId)
                .set(newUser)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User data saved to Firestore successfully for UID: " + userId);
                    prefsHelper.saveCurrentUser(newUser);
                    Toast.makeText(RegisterActivity.this, "Đăng ký thành công!", Toast.LENGTH_SHORT).show();
                    navigateToMainActivity();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error saving user data to Firestore: " + e.getMessage(), e);
                    Toast.makeText(RegisterActivity.this, "Lỗi khi lưu thông tin người dùng: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    navigateToMainActivity();
                });
    }

    private void loadAndNavigateUser(String userId) {
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            user.setId(userId);
                            prefsHelper.saveCurrentUser(user);
                            Log.d(TAG, "User data loaded from Firestore and saved to SharedPrefs for UID: " + userId);
                        } else {
                            Log.e(TAG, "User object is null after conversion from Firestore for UID: " + userId);
                            FirebaseUser firebaseUser = mAuth.getCurrentUser();
                            if (firebaseUser != null) {
                                User defaultUser = new User(
                                        firebaseUser.getUid(),
                                        firebaseUser.getDisplayName() != null ? firebaseUser.getDisplayName() : "Người dùng",
                                        firebaseUser.getEmail(),
                                        "", "", // phone, bio
                                        firebaseUser.getPhotoUrl() != null ? firebaseUser.getPhotoUrl().toString() : "",
                                        "", // address
                                        0.0f, 0, false, "active", false, "not_connected", 0 // other defaults
                                );
                                prefsHelper.saveCurrentUser(defaultUser);
                                Log.d(TAG, "Created fallback default user and saved to SharedPrefs.");
                            }
                        }
                    } else {
                        Log.w(TAG, "Firestore document not found for existing user: " + userId + ". Creating fallback default user.");
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            User defaultUser = new User(
                                    firebaseUser.getUid(),
                                    firebaseUser.getDisplayName() != null ? firebaseUser.getDisplayName() : "Người dùng",
                                    firebaseUser.getEmail(),
                                    "", "", // phone, bio
                                    firebaseUser.getPhotoUrl() != null ? firebaseUser.getPhotoUrl().toString() : "",
                                    "", // address
                                    0.0f, 0, false, "active", false, "not_connected", 0 // other defaults
                            );
                            prefsHelper.saveCurrentUser(defaultUser);
                            Log.d(TAG, "Created fallback default user and saved to SharedPrefs.");
                        }
                    }
                    navigateToMainActivity(); // Always navigate after attempting to load/create user data
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading user data from Firestore during navigation: " + e.getMessage(), e);
                    Toast.makeText(RegisterActivity.this, "Lỗi tải thông tin người dùng: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    navigateToMainActivity();
                });
    }

    private boolean isPasswordStrong(String password) {
        boolean hasLetter = false;
        boolean hasDigit = false;

        for (char c : password.toCharArray()) {
            if (Character.isLetter(c)) {
                hasLetter = true;
            } else if (Character.isDigit(c)) {
                hasDigit = true;
            }
            if (hasLetter && hasDigit) {
                return true;
            }
        }
        return false;
    }

    @NonNull
    private String getFirebaseErrorMessage(Task<AuthResult> task) {
        String errorMessage = "Đăng ký thất bại";
        if (task.getException() != null) {
            String error = task.getException().getMessage();
            if (error != null) {
                if (error.contains("email address is already in use") || error.contains("email-already-in-use")) {
                    errorMessage = "Email đã được sử dụng";
                } else if (error.contains("weak password") || error.contains("weak-password")) {
                    errorMessage = "Mật khẩu quá yếu";
                } else if (error.contains("invalid email") || error.contains("invalid-email")) {
                    errorMessage = "Email không hợp lệ";
                } else if (error.contains("operation-not-allowed")) {
                    errorMessage = "Đăng ký không được cho phép. Vui lòng kiểm tra cài đặt Firebase.";
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
            signUpButton.setEnabled(false);
            googleLogin.setEnabled(false);
        } else {
            progressBar.setVisibility(View.GONE);
            googleLogin.setEnabled(true);
            checkFieldsForEmptyValues();
        }
    }

    private void navigateToMainActivity() {
        Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    private void checkFieldsForEmptyValues() {
        String email = Objects.requireNonNull(editEmail.getText()).toString().trim();
        String password = Objects.requireNonNull(editPassword.getText()).toString();
        String confirmPassword = Objects.requireNonNull(editConfirmPassword.getText()).toString();

        boolean fieldsNotEmpty = !TextUtils.isEmpty(email) &&
                !TextUtils.isEmpty(password) &&
                !TextUtils.isEmpty(confirmPassword);

        if (progressBar.getVisibility() != View.VISIBLE) {
            signUpButton.setEnabled(fieldsNotEmpty);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        FirebaseUser firebaseUser = mAuth.getCurrentUser();
        if (firebaseUser != null) {
            if (prefsHelper.getCurrentUser() == null) {
                Log.d(TAG, "User logged in (Firebase Auth), but data not in SharedPrefs. Loading from Firestore.");
                loadAndNavigateUser(firebaseUser.getUid());
            } else {
                Log.d(TAG, "User data already in SharedPrefs. Navigating to MainActivity.");
                navigateToMainActivity();
            }
        }
    }
}
