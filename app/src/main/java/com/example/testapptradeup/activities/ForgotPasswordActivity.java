package com.example.testapptradeup.activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
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
    private ProgressBar progressBar; // Thêm ProgressBar để hiển thị trạng thái chờ

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Khởi tạo Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Ánh xạ các thành phần giao diện
        initViews();

        // Cài đặt listeners
        setupListeners();
    }

    private void initViews() {
        editEmail = findViewById(R.id.email);
        sendRequestButton = findViewById(R.id.send_request_button);
        // Giả sử bạn có một ProgressBar trong activity_forgot_password.xml
        // Nếu không, bạn cần thêm nó vào file layout.
        // progressBar = findViewById(R.id.progressBar);

        // Nếu không có ProgressBar trong layout, tạm thời bỏ qua
        // progressBar = new ProgressBar(this);
    }

    private void setupListeners() {
        LinearLayout backLogin = findViewById(R.id.back_to_login_link);
        backLogin.setOnClickListener(v -> {
            finish(); // Chỉ cần đóng Activity hiện tại để quay lại LoginActivity
        });

        sendRequestButton.setOnClickListener(v -> handlePasswordReset());
    }

    // <<< BẮT ĐẦU THÊM MỚI >>>
    private void handlePasswordReset() {
        String email = Objects.requireNonNull(editEmail.getText()).toString().trim();

        if (TextUtils.isEmpty(email)) {
            editEmail.setError("Vui lòng nhập email");
            editEmail.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            editEmail.setError("Email không hợp lệ");
            editEmail.requestFocus();
            return;
        }

        showLoading(true);

        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    showLoading(false);
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Email khôi phục mật khẩu đã được gửi.");
                        Toast.makeText(ForgotPasswordActivity.this,
                                "Yêu cầu đã được gửi. Vui lòng kiểm tra email của bạn để đặt lại mật khẩu.",
                                Toast.LENGTH_LONG).show();

                        // Tùy chọn: Tự động quay về màn hình đăng nhập sau vài giây
                        new android.os.Handler().postDelayed(
                                this::finish,
                                3000 // 3 giây
                        );

                    } else {
                        Log.w(TAG, "Lỗi gửi email khôi phục mật khẩu.", task.getException());
                        String errorMessage = "Gửi yêu cầu thất bại. Vui lòng thử lại.";
                        if (task.getException() != null && task.getException().getMessage() != null) {
                            if (task.getException().getMessage().contains("user-not-found")) {
                                errorMessage = "Email không tồn tại trong hệ thống.";
                            }
                        }
                        Toast.makeText(ForgotPasswordActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void showLoading(boolean isLoading) {
        // Nếu bạn có ProgressBar, hãy quản lý visibility của nó ở đây
        // if (progressBar != null) {
        //     progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        // }
        sendRequestButton.setEnabled(!isLoading);
        sendRequestButton.setText(isLoading ? "Đang gửi..." : "Gửi");
    }
    // <<< KẾT THÚC THÊM MỚI >>>


    @Override
    protected void onPause() {
        super.onPause();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }
}