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

public class EmailVerificationActivity extends AppCompatActivity {

    private static final String TAG = "EmailVerification";
    // Tần suất kiểm tra lại trạng thái xác thực (5000ms = 5 giây)
    private static final long AUTO_CHECK_INTERVAL = 5000;

    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;

    // UI Components
    private TextView tvVerificationMessage, tvResendInfo;
    private Button btnCheckVerification, btnResendEmail;
    private ProgressBar progressBar;
    private LinearLayout backToLoginLink;

    // Timer cho việc khóa nút "Gửi lại"
    private Handler resendCooldownHandler;
    private Runnable resendCooldownRunnable;
    private int resendCooldownSeconds = 60;

    private Handler autoCheckHandler;
    private Runnable autoCheckRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_email_verification);

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        initViews();
        setupListeners();

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
        btnCheckVerification.setOnClickListener(v -> checkEmailVerificationStatus(true)); // Kiểm tra thủ công thì hiện Toast
        btnResendEmail.setOnClickListener(v -> resendVerificationEmail());
        backToLoginLink.setOnClickListener(v -> {
            mAuth.signOut();
            navigateToLogin();
        });
    }

    @SuppressLint("SetTextI18n")
    private void updateUI() {
        tvVerificationMessage.setText("Một email xác thực đã được gửi đến:\n" + currentUser.getEmail() + "\n\nVui lòng kiểm tra hộp thư và nhấn vào liên kết để kích hoạt tài khoản.");
    }

    /**
     * Kiểm tra trạng thái xác thực email.
     * @param showToastIfUnverified true nếu muốn hiển thị Toast khi chưa xác thực (cho việc nhấn nút thủ công).
     */
    private void checkEmailVerificationStatus(boolean showToastIfUnverified) {
        if (currentUser == null) return;
        showLoading(true);
        currentUser.reload().addOnCompleteListener(task -> {
            showLoading(false);
            if (task.isSuccessful()) {
                currentUser = mAuth.getCurrentUser(); // Lấy lại user mới nhất
                if (currentUser != null && currentUser.isEmailVerified()) {
                    Toast.makeText(this, "Xác thực thành công!", Toast.LENGTH_SHORT).show();
                    navigateToMainActivity();
                } else if (showToastIfUnverified) {
                    Toast.makeText(this, "Email chưa được xác thực. Vui lòng kiểm tra lại hộp thư.", Toast.LENGTH_LONG).show();
                }
            } else {
                Log.e(TAG, "Lỗi khi tải lại thông tin người dùng: ", task.getException());
                if (showToastIfUnverified) {
                    Toast.makeText(this, "Lỗi: Không thể kiểm tra trạng thái xác thực.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void resendVerificationEmail() {
        showLoading(true);
        currentUser.sendEmailVerification().addOnCompleteListener(task -> {
            showLoading(false);
            if (task.isSuccessful()) {
                Toast.makeText(this, "Email xác thực mới đã được gửi.", Toast.LENGTH_SHORT).show();
                startResendCooldown();
            } else {
                Log.e(TAG, "Lỗi gửi lại email xác thực: ", task.getException());
                Toast.makeText(this, "Lỗi: Không thể gửi lại email.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void startResendCooldown() {
        btnResendEmail.setEnabled(false);
        tvResendInfo.setVisibility(View.VISIBLE);
        resendCooldownSeconds = 60;

        resendCooldownHandler = new Handler(Looper.getMainLooper());
        resendCooldownRunnable = new Runnable() {
            @SuppressLint("SetTextI18n")
            @Override
            public void run() {
                if (resendCooldownSeconds > 0) {
                    tvResendInfo.setText("Bạn có thể gửi lại sau " + resendCooldownSeconds + " giây");
                    resendCooldownSeconds--;
                    resendCooldownHandler.postDelayed(this, 1000);
                } else {
                    tvResendInfo.setVisibility(View.GONE);
                    btnResendEmail.setEnabled(true);
                }
            }
        };
        resendCooldownHandler.post(resendCooldownRunnable);
    }

    private void startAutoVerificationCheck() {
        if (autoCheckHandler != null) return; // Đã chạy rồi thì thôi
        Log.d(TAG, "Bắt đầu tự động kiểm tra xác thực...");
        autoCheckHandler = new Handler(Looper.getMainLooper());
        autoCheckRunnable = new Runnable() {
            @Override
            public void run() {
                // Tải lại thông tin người dùng một cách "thầm lặng"
                // và kiểm tra trạng thái mà không hiển thị Toast cho người dùng
                Log.d(TAG, "Tự động kiểm tra trạng thái email...");
                currentUser = mAuth.getCurrentUser();
                if (currentUser == null) {
                    stopAutoVerificationCheck();
                    return;
                }
                currentUser.reload().addOnSuccessListener(aVoid -> {
                    if (mAuth.getCurrentUser() != null && mAuth.getCurrentUser().isEmailVerified()) {
                        stopAutoVerificationCheck();
                        Toast.makeText(getApplicationContext(), "Tài khoản đã được xác thực!", Toast.LENGTH_SHORT).show();
                        navigateToMainActivity();
                    } else {
                        // Nếu chưa xác thực, lên lịch chạy lại sau một khoảng thời gian
                        if (autoCheckHandler != null) {
                            autoCheckHandler.postDelayed(this, AUTO_CHECK_INTERVAL);
                        }
                    }
                }).addOnFailureListener(e -> {
                    // Nếu có lỗi, vẫn tiếp tục thử lại
                    if (autoCheckHandler != null) {
                        autoCheckHandler.postDelayed(this, AUTO_CHECK_INTERVAL);
                    }
                });
            }
        };
        // Lên lịch chạy lần đầu
        autoCheckHandler.postDelayed(autoCheckRunnable, AUTO_CHECK_INTERVAL);
    }

    private void stopAutoVerificationCheck() {
        if (autoCheckHandler != null) {
            Log.d(TAG, "Dừng tự động kiểm tra xác thực.");
            autoCheckHandler.removeCallbacks(autoCheckRunnable);
            autoCheckHandler = null;
            autoCheckRunnable = null;
        }
    }

    private void showLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnCheckVerification.setEnabled(!isLoading);
        boolean isResendButtonEnabled = btnResendEmail.isEnabled();
        btnResendEmail.setEnabled(!isLoading && isResendButtonEnabled);
    }

    private void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void navigateToMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Bắt đầu kiểm tra khi người dùng quay lại màn hình
        startAutoVerificationCheck();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Dừng kiểm tra khi người dùng rời khỏi màn hình
        stopAutoVerificationCheck();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Dừng tất cả các timer để tránh rò rỉ bộ nhớ
        if (resendCooldownHandler != null && resendCooldownRunnable != null) {
            resendCooldownHandler.removeCallbacks(resendCooldownRunnable);
        }
        stopAutoVerificationCheck();
    }
}