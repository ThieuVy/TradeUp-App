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

import java.util.Date;
import java.util.Objects;
import java.util.concurrent.Executors;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private SharedPrefsHelper prefsHelper;
    private CredentialManager credentialManager;

    // UI Components
    private TextInputEditText editEmail, editPassword;
    private MaterialButton loginButton;
    private ProgressBar progressBar;
    private TextView registerLink;
    private ImageView googleLogin;
    private TextView forgotPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

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
        loginButton = findViewById(R.id.login_button);
        progressBar = findViewById(R.id.progressBar);
        registerLink = findViewById(R.id.sign_up_link);
        googleLogin = findViewById(R.id.google_login);
        forgotPassword = findViewById(R.id.forgot_password);
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
        });

        loginButton.setOnClickListener(v -> performEmailPasswordLogin());
        googleLogin.setOnClickListener(v -> performGoogleSignIn());
        forgotPassword.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, ForgotPasswordActivity.class);
            startActivity(intent);
        });
    }

    private void performEmailPasswordLogin() {
        Editable emailEditable = editEmail.getText();
        Editable passwordEditable = editPassword.getText();
        String email = (emailEditable != null) ? emailEditable.toString().trim() : "";
        String password = (passwordEditable != null) ? passwordEditable.toString() : "";

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(LoginActivity.this, "Vui lòng điền vào tất cả các trường", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(LoginActivity.this, "Email không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            if (user.isEmailVerified()) {
                                Log.d(TAG, "Email đã được xác minh, đang tải dữ liệu người dùng.");
                                loadAndNavigateUser(user.getUid());
                            } else {
                                showLoading(false);
                                Toast.makeText(LoginActivity.this, "Vui lòng xác minh địa chỉ email của bạn.", Toast.LENGTH_LONG).show();
                                Intent intent = new Intent(LoginActivity.this, EmailVerificationActivity.class);
                                startActivity(intent);
                            }
                        }
                    } else {
                        showLoading(false);
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

        credentialManager.getCredentialAsync(this, request, null, Executors.newSingleThreadExecutor(),
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
        try {
            GoogleIdTokenCredential googleIdTokenCredential = GoogleIdTokenCredential.createFrom(result.getCredential().getData());
            String idToken = googleIdTokenCredential.getIdToken();
            firebaseAuthWithGoogle(idToken);
        } catch (Exception e) {
            showLoading(false);
            Log.e(TAG, "handleGoogleCredentialResponse failed", e);
            Toast.makeText(this, "Lỗi xử lý thông tin Google.", Toast.LENGTH_SHORT).show();
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            boolean isNewUser = task.getResult().getAdditionalUserInfo() != null && task.getResult().getAdditionalUserInfo().isNewUser();

                            if (isNewUser) {
                                Log.d(TAG, "New Google user. Saving data to Firestore.");
                                User newUser = new User();
                                newUser.setId(user.getUid());
                                newUser.setName(user.getDisplayName() != null ? user.getDisplayName() : "Người dùng Google");
                                newUser.setEmail(user.getEmail());
                                newUser.setPhone("");
                                newUser.setBio("");
                                newUser.setProfileImageUrl(user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : "");
                                newUser.setAddress("");
                                newUser.setRating(0.0f);
                                newUser.setReviewCount(0);
                                newUser.setVerified(true);
                                newUser.setAccountStatus("active");
                                newUser.setFlagged(false);
                                newUser.setWalletStatus("not_connected");
                                newUser.setNotificationCount(0);
                                newUser.setMemberSince(new Date());

                                saveNewUserAndNavigate(newUser);
                            } else {
                                Log.d(TAG, "Existing Google user. Loading data.");
                                Toast.makeText(LoginActivity.this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();
                                loadAndNavigateUser(user.getUid());
                            }
                        }
                    } else {
                        showLoading(false);
                        String errorMessage = getFirebaseErrorMessage(task.getException());
                        Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveNewUserAndNavigate(User newUser) {
        db.collection("users").document(newUser.getId())
                .set(newUser)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(LoginActivity.this, "Chào mừng bạn đến với TradeUp!", Toast.LENGTH_SHORT).show();
                    prefsHelper.saveCurrentUser(newUser);
                    navigateToMainActivity();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error saving new Google user to Firestore: " + e.getMessage(), e);
                    Toast.makeText(LoginActivity.this, "Lỗi khi lưu thông tin người dùng.", Toast.LENGTH_SHORT).show();
                    navigateToMainActivity();
                });
    }

    private void loadAndNavigateUser(String userId) {
        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            user.setId(userId);
                            prefsHelper.saveCurrentUser(user);
                            Log.d(TAG, "Dữ liệu người dùng cho UID đã được tải: " + userId);
                        } else {
                            createFallbackUserAndSave(userId, "Đối tượng người dùng từ Firestore là null");
                        }
                    } else {
                        createFallbackUserAndSave(userId, "Không tìm thấy tài liệu Firestore");
                    }
                    navigateToMainActivity();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi khi tải dữ liệu người dùng: " + e.getMessage(), e);
                    Toast.makeText(LoginActivity.this, "Lỗi khi tải dữ liệu người dùng.", Toast.LENGTH_SHORT).show();
                    navigateToMainActivity();
                });
    }

    private void createFallbackUserAndSave(String userId, String reason) {
        Log.d(TAG, "Đang tạo người dùng dự phòng: " + reason);
        FirebaseUser firebaseUser = mAuth.getCurrentUser();
        if (firebaseUser != null) {
            User fallbackUser = new User();
            fallbackUser.setId(firebaseUser.getUid());
            fallbackUser.setName(firebaseUser.getDisplayName());
            fallbackUser.setEmail(firebaseUser.getEmail());
            fallbackUser.setPhone("");
            fallbackUser.setBio("");
            fallbackUser.setProfileImageUrl(firebaseUser.getPhotoUrl() != null ? firebaseUser.getPhotoUrl().toString() : "");
            fallbackUser.setAddress("");
            fallbackUser.setRating(0.0f);
            fallbackUser.setReviewCount(0);
            fallbackUser.setVerified(firebaseUser.isEmailVerified());
            fallbackUser.setAccountStatus("active");
            fallbackUser.setFlagged(false);
            fallbackUser.setWalletStatus("not_connected");
            fallbackUser.setNotificationCount(0);
            fallbackUser.setMemberSince(new Date());
            prefsHelper.saveCurrentUser(fallbackUser);
        }
    }

    private String getFirebaseErrorMessage(Exception exception) {
        String defaultMessage = "Xác thực thất bại. Vui lòng thử lại.";
        if (exception == null) return defaultMessage;
        String message = exception.getMessage();
        if (message == null) return defaultMessage;
        if (message.contains("user not found") || message.contains("INVALID_LOGIN_CREDENTIALS") || message.contains("INVALID_PASSWORD")) {
            return "Email hoặc mật khẩu không đúng.";
        }
        return defaultMessage;
    }

    private void showLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        loginButton.setText(isLoading ? "" : getString(R.string.auth_login));
        loginButton.setEnabled(!isLoading);
        googleLogin.setEnabled(!isLoading);
        registerLink.setEnabled(!isLoading);
        forgotPassword.setEnabled(!isLoading);
    }

    private void navigateToMainActivity() {
        showLoading(false);
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
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
        FirebaseUser firebaseUser = mAuth.getCurrentUser();
        if (firebaseUser != null && firebaseUser.isEmailVerified()) {
            navigateToMainActivity();
        }
    }
}