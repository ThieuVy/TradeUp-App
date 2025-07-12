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

import java.util.Objects;
import java.util.concurrent.Executors;

public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = "RegisterActivity";

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private SharedPrefsHelper prefsHelper;
    private CredentialManager credentialManager;

    private TextInputEditText editEmail, editPassword, editConfirmPassword;
    private MaterialButton signUpButton;
    private ProgressBar progressBar;
    private TextView loginLink;
    private ImageView googleLogin;

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
        googleLogin = findViewById(R.id.google_login); // Sửa lỗi: gán giá trị cho googleLogin
    }

    private void setupListeners() {
        TextWatcher textWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { checkFieldsForEmptyValues(); }
            @Override public void afterTextChanged(Editable s) {}
        };

        editEmail.addTextChangedListener(textWatcher);
        editPassword.addTextChangedListener(textWatcher);
        editConfirmPassword.addTextChangedListener(textWatcher);

        loginLink.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
        });

        signUpButton.setOnClickListener(v -> performEmailRegistration());
        googleLogin.setOnClickListener(v -> performGoogleSignIn());
    }

    // Đăng ký bằng Email và Mật khẩu
    private void performEmailRegistration() {
        String email = Objects.requireNonNull(editEmail.getText()).toString().trim();
        String password = Objects.requireNonNull(editPassword.getText()).toString();
        String confirmPassword = Objects.requireNonNull(editConfirmPassword.getText()).toString();

        if (!validateInputs(email, password, confirmPassword)) {
            return;
        }

        showLoading(true);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            saveUserToFirestoreAndSendVerification(firebaseUser);
                        }
                    } else {
                        showLoading(false);
                        // Lỗi sẽ được xử lý ở đây
                        String errorMessage = getFirebaseErrorMessage(task.getException());
                        Toast.makeText(RegisterActivity.this, errorMessage, Toast.LENGTH_LONG).show(); // Dùng LONG để người dùng đọc kịp
                    }
                });
    }

    private void saveUserToFirestoreAndSendVerification(FirebaseUser firebaseUser) {
        String name = firebaseUser.getEmail() != null ? firebaseUser.getEmail().split("@")[0] : "Người dùng mới";
        User newUser = new User(firebaseUser.getUid(), name, firebaseUser.getEmail(), "", "", "", "", 0.0f, 0, false, "active", false, "not_connected", 0);

        db.collection("users").document(firebaseUser.getUid())
                .set(newUser)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Lưu người dùng mới vào Firestore thành công.");
                    sendVerificationEmail(firebaseUser);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi lưu người dùng vào Firestore: ", e);
                    showLoading(false);
                    Toast.makeText(RegisterActivity.this, "Lỗi đăng ký. Vui lòng thử lại.", Toast.LENGTH_LONG).show();
                });
    }

    private void sendVerificationEmail(FirebaseUser firebaseUser) {
        firebaseUser.sendEmailVerification()
                .addOnCompleteListener(task -> {
                    showLoading(false);
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Email xác thực đã được gửi.");
                        Toast.makeText(RegisterActivity.this, "Đăng ký thành công! Vui lòng kiểm tra email để xác thực tài khoản.", Toast.LENGTH_LONG).show();
                        // Chuyển người dùng đến màn hình Login để họ đăng nhập lại sau khi xác thực
                        Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        Log.e(TAG, "Lỗi gửi email xác thực.", task.getException());
                        Toast.makeText(RegisterActivity.this, "Lỗi gửi email xác thực.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Đăng nhập/Đăng ký bằng Google
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
                            Toast.makeText(RegisterActivity.this, "Đăng nhập Google thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
                    }
                }
        );
    }

    private void handleGoogleCredentialResponse(@NonNull GetCredentialResponse result) {
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
                                saveNewGoogleUserAndNavigate(user);
                            } else {
                                loadExistingUserAndNavigate(user.getUid());
                            }
                        }
                    } else {
                        showLoading(false);
                        String errorMessage = getFirebaseErrorMessage(task.getException());
                        Toast.makeText(RegisterActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveNewGoogleUserAndNavigate(FirebaseUser firebaseUser) {
        User newUser = new User(
                firebaseUser.getUid(),
                firebaseUser.getDisplayName() != null ? firebaseUser.getDisplayName() : "Người dùng Google",
                firebaseUser.getEmail(),
                "", "",
                firebaseUser.getPhotoUrl() != null ? firebaseUser.getPhotoUrl().toString() : "",
                "", 0.0f, 0, true, "active", false, "not_connected", 0
        );
        db.collection("users").document(newUser.getId()).set(newUser)
                .addOnSuccessListener(aVoid -> {
                    prefsHelper.saveCurrentUser(newUser);
                    navigateToMainActivity();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Lỗi lưu thông tin người dùng.", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadExistingUserAndNavigate(String userId) {
        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            user.setId(userId);
                            prefsHelper.saveCurrentUser(user);
                        }
                    }
                    navigateToMainActivity();
                })
                .addOnFailureListener(e -> navigateToMainActivity());
    }

    // Các hàm Helper
    private boolean validateInputs(String email, String password, String confirmPassword) {
        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password) || TextUtils.isEmpty(confirmPassword)) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Email không hợp lệ", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (password.length() < 6) {
            Toast.makeText(this, "Mật khẩu phải có ít nhất 6 ký tự", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Mật khẩu xác nhận không khớp", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private String getFirebaseErrorMessage(Exception exception) {
        String defaultMessage = "Đăng ký thất bại. Vui lòng thử lại.";
        if (exception == null) return defaultMessage;

        String errorCode = "";
        // FirebaseAuthException cung cấp mã lỗi cụ thể hơn
        if (exception instanceof com.google.firebase.auth.FirebaseAuthUserCollisionException) {
            errorCode = ((com.google.firebase.auth.FirebaseAuthUserCollisionException) exception).getErrorCode();
        }

        // Kiểm tra mã lỗi cụ thể
        if ("ERROR_EMAIL_ALREADY_IN_USE".equals(errorCode)) {
            return "Địa chỉ email này đã được sử dụng bởi một tài khoản khác.";
        }

        // Bạn có thể thêm các trường hợp lỗi khác ở đây
        // Ví dụ:
        if ("ERROR_WEAK_PASSWORD".equals(errorCode)) {
            return "Mật khẩu quá yếu. Vui lòng chọn mật khẩu mạnh hơn.";
        }

        // Trả về thông báo mặc định nếu không khớp mã lỗi nào
        return defaultMessage;
    }

    private void showLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        signUpButton.setEnabled(!isLoading);
        googleLogin.setEnabled(!isLoading);
        loginLink.setEnabled(!isLoading);
    }

    private void navigateToMainActivity() {
        showLoading(false);
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void checkFieldsForEmptyValues() {
        String email = Objects.requireNonNull(editEmail.getText()).toString().trim();
        String password = Objects.requireNonNull(editPassword.getText()).toString();
        String confirmPassword = Objects.requireNonNull(editConfirmPassword.getText()).toString();
        signUpButton.setEnabled(!TextUtils.isEmpty(email) && !TextUtils.isEmpty(password) && !TextUtils.isEmpty(confirmPassword));
    }
}