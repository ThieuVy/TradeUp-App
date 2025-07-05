package com.example.testapptradeup;

import android.app.Application;

import com.example.testapptradeup.utils.CloudinaryManager;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Khởi tạo Cloudinary SDK khi ứng dụng bắt đầu
        CloudinaryManager.setup(this);
    }
}