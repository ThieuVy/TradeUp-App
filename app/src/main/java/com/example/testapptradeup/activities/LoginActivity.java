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
import androidx.lifecycle.ViewModelProvider;

import com.example.testapptradeup.R;
import com.example.testapptradeup.models.User;
import com.example.testapptradeup.utils.SharedPrefsHelper;
import com.example.testapptradeup.viewmodels.MainViewModel;
import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Objects;
import java.util.concurrent.Executors;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    private MainViewModel mainViewModel;
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
//        mainViewModel = new ViewModelProvider(this).get(MainViewModel.class);
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
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        // ========== BẮT ĐẦU PHẦN SỬA LỖI ==========
                        if (user != null) {
                            if (user.isEmailVerified()) {
                                // Email đã xác thực -> Tải dữ liệu và vào app
                                Log.d(TAG, "Email đã xác thực, đang tải dữ liệu người dùng.");
                                loadAndNavigateUser(user.getUid());
                            } else {
                                // Email chưa xác thực -> Chuyển đến màn hình xác thực
                                showLoading(false);
                                Toast.makeText(LoginActivity.this, "Vui lòng xác thực email của bạn.", Toast.LENGTH_LONG).show();
                                Intent intent = new Intent(LoginActivity.this, EmailVerificationActivity.class);
                                startActivity(intent);
                                // Không finish() LoginActivity để người dùng có thể quay lại
                            }
                        }
                        // ========== KẾT THÚC PHẦN SỬA LỖI ==========
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
                                // Nếu là người dùng mới, tạo đối tượng User và lưu vào Firestore
                                Log.d(TAG, "New Google user. Saving data to Firestore.");
                                User newUser = new User(
                                        user.getUid(),
                                        user.getDisplayName() != null ? user.getDisplayName() : "Người dùng Google",
                                        user.getEmail(),
                                        "", // phone
                                        "", // bio
                                        user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : "",
                                        "", // address
                                        0.0f, 0, true, "active", false, "not_connected", 0
                                );
                                saveNewUserAndNavigate(newUser);
                            } else {
                                // Nếu là người dùng cũ, tải dữ liệu và điều hướng
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
//                    mainViewModel.setCurrentUser(newUser);
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
                            prefsHelper.saveCurrentUser(user); // Lưu người dùng vào SharedPreferences
//                            mainViewModel.setCurrentUser(user); // Cập nhật ViewModel
                            Log.d(TAG, "Dữ liệu người dùng đã được tải cho UID: " + userId); // Log thông tin tải dữ liệu
                        } else {
                            createFallbackUserAndSave(userId, "Đối tượng người dùng null từ Firestore"); // Tạo người dùng dự phòng nếu đối tượng null
                        }
                    } else {
                        createFallbackUserAndSave(userId, "Không tìm thấy tài liệu Firestore"); // Tạo người dùng dự phòng nếu tài liệu không tồn tại
                    }
                    navigateToMainActivity(); // Điều hướng đến MainActivity
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading user data: " + e.getMessage(), e);
                    Toast.makeText(LoginActivity.this, "Lỗi tải thông tin người dùng.", Toast.LENGTH_SHORT).show();
                    navigateToMainActivity(); // Điều hướng đến MainActivity ngay cả khi có lỗi
                });
    }

    private void createFallbackUserAndSave(String userId, String reason) { // userId là ID người dùng, reason là lý do tạo người dùng dự phòng
        Log.d(TAG, "Tạo người dùng dự phòng: " + reason);
        FirebaseUser firebaseUser = mAuth.getCurrentUser();
        if (firebaseUser != null) {
            User fallbackUser = new User(
                    firebaseUser.getUid(),
                    firebaseUser.getDisplayName(),
                    firebaseUser.getEmail(),
                    "", "", firebaseUser.getPhotoUrl() != null ? firebaseUser.getPhotoUrl().toString() : "", "",
                    0.0f, 0, true, "active", false, "not_connected", 0
            );
            prefsHelper.saveCurrentUser(fallbackUser);
        }
    }

    private String getFirebaseErrorMessage(Exception exception) {
        String defaultMessage = "Xác thực thất bại. Vui lòng thử lại.";
        if (exception == null) return defaultMessage;
        String message = exception.getMessage();
        if (message == null) return defaultMessage;
        if (message.contains("user not found") || message.contains("INVALID_LOGIN_CREDENTIALS")) {
            return "Email hoặc mật khẩu không đúng.";
        }
        return defaultMessage;
    }

    private void showLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
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
        // Cơ chế mới sẽ xử lý việc đăng xuất.
        // Logic này sẽ kiểm tra xem người dùng có còn phiên đăng nhập hợp lệ không
        // (ví dụ: vừa mở lại app trong khoảng thời gian cho phép).
        FirebaseUser firebaseUser = mAuth.getCurrentUser();
        if (firebaseUser != null && firebaseUser.isEmailVerified()) {
            navigateToMainActivity();
        }
    }
}