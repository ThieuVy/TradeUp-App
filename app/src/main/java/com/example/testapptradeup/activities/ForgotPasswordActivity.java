package com.example.testapptradeup.activities;

import android.os.Bundle;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.testapptradeup.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

import java.util.Objects;

public class ForgotPasswordActivity extends AppCompatActivity {

    private static final String TAG = "ForgotPasswordActivity";

    // UI Components
    private TextInputEditText editEmail;
    private MaterialButton sendRequestButton;
    private ProgressBar progressBar;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        mAuth = FirebaseAuth.getInstance();
        initViews();
        setupListeners();
    }

    private void initViews() {
        editEmail = findViewById(R.id.email);
        sendRequestButton = findViewById(R.id.send_request_button);
        // Giả sử progressBar không tồn tại trong layout này, chúng ta sẽ quản lý trạng thái của button
    }

    private void setupListeners() {
        LinearLayout backLogin = findViewById(R.id.back_to_login_link);
        backLogin.setOnClickListener(v -> finish());

        sendRequestButton.setOnClickListener(v -> handlePasswordReset());
    }

    private void handlePasswordReset() {
        String email = Objects.requireNonNull(editEmail.getText()).toString().trim();

        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            editEmail.setError("Vui lòng nhập email hợp lệ");
            editEmail.requestFocus();
            return;
        }

        showLoading(true);

        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    showLoading(false);
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Email khôi phục mật khẩu đã được gửi.");
                        Toast.makeText(ForgotPasswordActivity.this, "Yêu cầu đã được gửi. Vui lòng kiểm tra email để đặt lại mật khẩu.", Toast.LENGTH_LONG).show();
                        // Quay về màn hình đăng nhập sau 3 giây
                        new android.os.Handler(Looper.getMainLooper()).postDelayed(this::finish, 3000);
                    } else {
                        Log.w(TAG, "Lỗi gửi email khôi phục mật khẩu.", task.getException());
                        String errorMessage = "Gửi yêu cầu thất bại. Vui lòng thử lại.";
                        if (task.getException() != null && task.getException().getMessage() != null) {
                            if (task.getException().getMessage().contains("user-not-found") || task.getException().getMessage().contains("INVALID_RECIPIENT_EMAIL")) {
                                errorMessage = "Email không tồn tại trong hệ thống.";
                            }
                        }
                        Toast.makeText(ForgotPasswordActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void showLoading(boolean isLoading) {
        sendRequestButton.setEnabled(!isLoading);
        sendRequestButton.setText(isLoading ? "Đang gửi..." : "Gửi yêu cầu");
    }
}