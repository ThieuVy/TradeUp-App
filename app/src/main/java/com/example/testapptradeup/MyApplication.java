package com.example.testapptradeup;

import android.app.Application;
import com.cloudinary.android.MediaManager;
import java.util.HashMap;
import java.util.Map;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // Cấu hình Cloudinary
        Map<String, String> config = new HashMap<>();
        config.put("cloud_name", "dqpyrygyu");
        config.put("api_key", "625242223375512");
        config.put("api_secret", "bNd07UKRjs0JHcAXXTiedsT6RYc");
        // config.put("secure", "true"); // Tùy chọn: luôn sử dụng https

        MediaManager.init(this, config);
    }
}