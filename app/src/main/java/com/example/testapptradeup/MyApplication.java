package com.example.testapptradeup;

import android.app.Application;
import android.util.Log;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.lifecycle.ProcessLifecycleOwner;

import com.example.testapptradeup.utils.CloudinaryManager;
import com.example.testapptradeup.utils.SharedPrefsHelper;
import com.google.firebase.auth.FirebaseAuth;

public class MyApplication extends Application implements LifecycleObserver {

    private static final String TAG = "MyApplication";
    // *** ĐỊNH NGHĨA NGƯỠNG THỜI GIAN (TIMEOUT) ***
    // Ví dụ: 30 phút. Bạn có thể thay đổi giá trị này.
    // 30 phút * 60 giây/phút * 1000 mili-giây/giây
    private static final long TIMEOUT_IN_MS = 30 * 60 * 1000;

    private SharedPrefsHelper prefsHelper;

    @Override
    public void onCreate() {
        super.onCreate();

        // Khởi tạo các thành phần khác
        CloudinaryManager.setup(this);
        prefsHelper = new SharedPrefsHelper(this);

        // *** ĐĂNG KÝ LẮNG NGHE VÒNG ĐỜI CỦA TOÀN BỘ ỨNG DỤNG ***
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
    }

    /**
     * Phương thức này sẽ được gọi khi ứng dụng được đưa LÊN MÀN HÌNH (foreground).
     * Nó được gọi sau onCreate và mỗi khi người dùng mở lại app.
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    public void onAppForegrounded() {
        Log.d(TAG, "App is in FOREGROUND");

        // Kiểm tra xem có cần đăng xuất không
        long lastActiveTime = prefsHelper.getLong("last_active_time", 0);
        long currentTime = System.currentTimeMillis();

        // Chỉ kiểm tra nếu người dùng đã từng hoạt động (lastActiveTime > 0)
        if (lastActiveTime > 0) {
            long timeInBackground = currentTime - lastActiveTime;
            Log.d(TAG, "Time in background: " + timeInBackground / 1000 + " seconds");

            // Nếu thời gian ở dưới nền lớn hơn ngưỡng timeout
            if (timeInBackground > TIMEOUT_IN_MS) {
                Log.d(TAG, "Timeout exceeded. Signing out user.");
                // Thực hiện đăng xuất
                FirebaseAuth.getInstance().signOut();
                prefsHelper.clearUserData();
                // (Không cần điều hướng ở đây, các Activity sẽ tự xử lý)
            }
        }
    }

    /**
     * Phương thức này sẽ được gọi khi ứng dụng bị ĐƯA XUỐNG NỀN (background).
     * Nó được gọi khi người dùng nhấn nút Home, chuyển app, hoặc khóa màn hình.
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public void onAppBackgrounded() {
        Log.d(TAG, "App is in BACKGROUND");

        // Ghi lại thời điểm ứng dụng bị đưa xuống nền
        prefsHelper.putLong("last_active_time", System.currentTimeMillis());
    }
}