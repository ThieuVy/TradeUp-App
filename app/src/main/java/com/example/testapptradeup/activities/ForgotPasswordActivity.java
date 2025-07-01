package com.example.testapptradeup.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.example.testapptradeup.R;

public class ForgotPasswordActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        // Ẩn ActionBar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Khởi tạo các thành phần giao diện

        // Xử lý sự kiện khi người dùng nhấn nút Send Request

        // Xử lý sự kiện khi người dùng nhấn nút Back to Login
        LinearLayout backLogin = findViewById(R.id.back_to_login_link);
        backLogin.setOnClickListener(v -> {
            // Chuyển hướng đến màn hình đăng nhập
            Intent intent = new Intent(ForgotPasswordActivity.this, LoginActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
            finish();
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_left);
    }
}
