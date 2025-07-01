package com.example.testapptradeup.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.testapptradeup.models.User;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class SharedPrefsHelper {
    private static final String PREFS_NAME = "TradeUpPrefs";
    private static final String KEY_USER_DATA = "user_data";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";
    private static final String KEY_FIRST_TIME_LAUNCH = "first_time_launch";

    private final SharedPreferences sharedPreferences;
    private final Gson gson;

    public SharedPrefsHelper(Context context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        gson = new GsonBuilder().create();
    }

    // User data methods
    public void saveCurrentUser(User user) {
        String userJson = gson.toJson(user);
        sharedPreferences.edit()
                .putString(KEY_USER_DATA, userJson)
                .putBoolean(KEY_IS_LOGGED_IN, true)
                .apply();
    }

    public User getCurrentUser() {
        String userJson = sharedPreferences.getString(KEY_USER_DATA, null);
        if (userJson != null) {
            try {
                return gson.fromJson(userJson, User.class);
            } catch (Exception e) {
                // Log the error for debugging
                // Log.e("SharedPrefsHelper", "Error parsing user JSON", e);
                return null;
            }
        }
        return null;
    }

    public boolean isLoggedIn() {
        return sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    // Token management
    public void saveTokens(String accessToken, String refreshToken) {
        sharedPreferences.edit()
                .putString(KEY_ACCESS_TOKEN, accessToken)
                .putString(KEY_REFRESH_TOKEN, refreshToken)
                .apply();
    }

    public String getAccessToken() {
        return sharedPreferences.getString(KEY_ACCESS_TOKEN, null);
    }

    public String getRefreshToken() {
        return sharedPreferences.getString(KEY_REFRESH_TOKEN, null);
    }

    // First time launch
    public boolean isFirstTimeLaunch() {
        return sharedPreferences.getBoolean(KEY_FIRST_TIME_LAUNCH, true);
    }

    public void setFirstTimeLaunch(boolean isFirstTime) {
        sharedPreferences.edit()
                .putBoolean(KEY_FIRST_TIME_LAUNCH, isFirstTime)
                .apply();
    }

    // Clear all user data (for logout)
    public void clearUserData() {
        sharedPreferences.edit()
                .remove(KEY_USER_DATA)
                .remove(KEY_ACCESS_TOKEN)
                .remove(KEY_REFRESH_TOKEN)
                .putBoolean(KEY_IS_LOGGED_IN, false)
                .apply();
    }

    // Clear all preferences
    public void clearAll() {
        sharedPreferences.edit().clear().apply();
    }

    // Update specific user fields
    public void updateUserName(String name) {
        User user = getCurrentUser();
        if (user != null) {
            user.setName(name);
            saveCurrentUser(user);
        }
    }

    public void updateUserRating(float rating, int reviewCount) {
        User user = getCurrentUser();
        if (user != null) {
            user.setRating(rating);
            user.setReviewCount(reviewCount);
            saveCurrentUser(user);
        }
    }

    public void updateNotificationCount(int count) {
        User user = getCurrentUser();
        if (user != null) {
            // Note: The User model now has notificationCount field
            // Ensure you update the User model's constructor and Parcelable methods to include this.
            // If it's already there, great!
            // Assuming `notificationCount` setter exists in User class.
            user.setNotificationCount(count);
            saveCurrentUser(user);
        }
    }

    public void updateProfileImage(String imageUrl) {
        User user = getCurrentUser();
        if (user != null) {
            user.setProfileImageUrl(imageUrl);
            saveCurrentUser(user);
        }
    }

    // Generic preference methods
    public void putString(String key, String value) {
        sharedPreferences.edit().putString(key, value).apply();
    }

    public String getString(String key, String defaultValue) {
        return sharedPreferences.getString(key, defaultValue);
    }

    public void putInt(String key, int value) {
        sharedPreferences.edit().putInt(key, value).apply();
    }

    public int getInt(String key, int defaultValue) {
        return sharedPreferences.getInt(key, defaultValue);
    }

    public void putBoolean(String key, boolean value) {
        sharedPreferences.edit().putBoolean(key, value).apply();
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        return sharedPreferences.getBoolean(key, defaultValue);
    }

    public void putFloat(String key, float value) {
        sharedPreferences.edit().putFloat(key, value).apply();
    }

    public float getFloat(String key, float defaultValue) {
        return sharedPreferences.getFloat(key, defaultValue);
    }

    public void putLong(String key, long value) {
        sharedPreferences.edit().putLong(key, value).apply();
    }

    public long getLong(String key, long defaultValue) {
        return sharedPreferences.getLong(key, defaultValue);
    }

    public void remove(String key) {
        sharedPreferences.edit().remove(key).apply();
    }

    public boolean contains(String key) {
        return sharedPreferences.contains(key);
    }
}