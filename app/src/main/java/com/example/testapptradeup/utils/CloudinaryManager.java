package com.example.testapptradeup.utils;

import android.content.Context;
import com.cloudinary.android.MediaManager;
import java.util.HashMap;
import java.util.Map;

public class CloudinaryManager {
    private static boolean isConfigured = false;

    public static void setup(Context context) {
        if (isConfigured) {
            return;
        }

        // **THAY THÔNG TIN CỦA BẠN VÀO ĐÂY**
        Map<String, String> config = new HashMap<>();
        config.put("cloud_name", "dqpyrygyu");
        config.put("api_key", "625242223375512");
        config.put("api_secret", "bNd07UKRjs0JHcAXXTiedsT6RYc");
        // Bạn có thể thêm các tùy chọn khác ở đây
        // config.put("secure", "true");

        MediaManager.init(context, config);
        isConfigured = true;
    }
}