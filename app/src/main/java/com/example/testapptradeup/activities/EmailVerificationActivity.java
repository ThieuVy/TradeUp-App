package com.example.testapptradeup.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.testapptradeup.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Objects;

public class EmailVerificationActivity extends AppCompatActivity {

    private static final String TAG = "EmailVerification";

    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;

    // UI Components
    private TextView tvVerificationMessage, tvResendInfo;
    private Button btnCheckVerification, btnResendEmail;
    private ProgressBar progressBar;
    private LinearLayout backToLoginLink;

    private Handler timerHandler;
    private Runnable timerRunnable;
    private int resendCooldownSeconds = 60;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_email_verification);

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        initViews();
        setupListeners();

        // Kiểm tra ngay lập tức nếu người dùng chưa đăng nhập
        if (currentUser == null) {
            Toast.makeText(this, "Phiên đăng nhập không hợp lệ.", Toast.LENGTH_SHORT).show();
            navigateToLogin();
            return;
        }

        updateUI();
    }

    private void initViews() {
        tvVerificationMessage = findViewById(R.id.verification_message);
        tvResendInfo = findViewById(R.id.resend_info);
        btnCheckVerification = findViewById(R.id.check_verification_button);
        btnResendEmail = findViewById(R.id.resend_email_button);
        progressBar = findViewById(R.id.verification_progress_bar);
        backToLoginLink = findViewById(R.id.back_to_login_link);
    }

    private void setupListeners() {
        btnCheckVerification.setOnClickListener(v -> checkEmailVerificationStatus());
        btnResendEmail.setOnClickListener(v -> resendVerificationEmail());
        backToLoginLink.setOnClickListener(v -> {
            mAuth.signOut(); // Đăng xuất người dùng trước khi quay về trang login
            navigateToLogin();
        });
    }

    @SuppressLint("SetTextI18n")
    private void updateUI() {
        tvVerificationMessage.setText("Một email xác thực đã được gửi đến:\n" + currentUser.getEmail() + "\n\nVui lòng kiểm tra hộp thư và nhấn vào liên kết để kích hoạt tài khoản.");
    }

    private void checkEmailVerificationStatus() {
        showLoading(true);
        // Phải tải lại thông tin người dùng từ Firebase để lấy trạng thái mới nhất
        currentUser.reload().addOnCompleteListener(task -> {
            showLoading(false);
            if (task.isSuccessful()) {
                // Sau khi reload, kiểm tra lại trạng thái isEmailVerified
                if (Objects.requireNonNull(mAuth.getCurrentUser()).isEmailVerified()) {
                    Toast.makeText(this, "Xác thực thành công!", Toast.LENGTH_SHORT).show();
                    // Điều hướng đến MainActivity
                    Intent intent = new Intent(EmailVerificationActivity.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(this, "Email chưa được xác thực. Vui lòng kiểm tra lại hộp thư.", Toast.LENGTH_LONG).show();
                }
            } else {
                Log.e(TAG, "Lỗi khi tải lại thông tin người dùng: ", task.getException());
                Toast.makeText(this, "Lỗi: Không thể kiểm tra trạng thái xác thực.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void resendVerificationEmail() {
        showLoading(true);
        currentUser.sendEmailVerification()
                .addOnCompleteListener(task -> {
                    showLoading(false);
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Email xác thực mới đã được gửi.", Toast.LENGTH_SHORT).show();
                        startResendCooldown(); // Bắt đầu đếm ngược
                    } else {
                        Log.e(TAG, "Lỗi gửi lại email xác thực: ", task.getException());
                        Toast.makeText(this, "Lỗi: Không thể gửi lại email.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void startResendCooldown() {
        btnResendEmail.setEnabled(false);
        tvResendInfo.setVisibility(View.VISIBLE);
        resendCooldownSeconds = 60; // Reset lại thời gian

        timerHandler = new Handler(Looper.getMainLooper());
        timerRunnable = new Runnable() {
            @SuppressLint("SetTextI18n")
            @Override
            public void run() {
                if (resendCooldownSeconds > 0) {
                    tvResendInfo.setText("Bạn có thể gửi lại sau " + resendCooldownSeconds + " giây");
                    resendCooldownSeconds--;
                    timerHandler.postDelayed(this, 1000);
                } else {
                    tvResendInfo.setVisibility(View.GONE);
                    btnResendEmail.setEnabled(true);
                }
            }
        };
        timerHandler.post(timerRunnable);
    }

    private void showLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnCheckVerification.setEnabled(!isLoading);
        // Chỉ bật nút resend nếu không trong thời gian cooldown
        btnResendEmail.setEnabled(!isLoading && (timerHandler == null || resendCooldownSeconds <= 0));
    }

    private void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Dừng Handler để tránh memory leak
        if (timerHandler != null && timerRunnable != null) {
            timerHandler.removeCallbacks(timerRunnable);
        }
    }
}