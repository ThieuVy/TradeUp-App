package com.example.testapptradeup;

import android.app.Application;
import android.util.Log;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.lifecycle.ProcessLifecycleOwner;

import com.example.testapptradeup.utils.CloudinaryManager;
import com.example.testapptradeup.utils.SharedPrefsHelper;
import com.google.firebase.FirebaseApp;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory;
import com.google.firebase.auth.FirebaseAuth;

// ================== THÊM CÁC IMPORT NÀY ==================
import com.vanniktech.emoji.EmojiManager;
import com.vanniktech.emoji.google.GoogleEmojiProvider;
// =======================================================

public class MyApplication extends Application implements LifecycleObserver {

    private static final String TAG = "MyApplication";
    private static final long TIMEOUT_IN_MS = 30 * 60 * 1000;

    private SharedPrefsHelper prefsHelper;

    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseApp.initializeApp(this);

        FirebaseAppCheck firebaseAppCheck = FirebaseAppCheck.getInstance();
        firebaseAppCheck.installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance());

        EmojiManager.install(new GoogleEmojiProvider());

        CloudinaryManager.setup(this);
        prefsHelper = new SharedPrefsHelper(this);
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    public void onAppForegrounded() {
        Log.d(TAG, "App is in FOREGROUND");

        long lastActiveTime = prefsHelper.getLong("last_active_time", 0);
        long currentTime = System.currentTimeMillis();

        if (lastActiveTime > 0) {
            long timeInBackground = currentTime - lastActiveTime;
            Log.d(TAG, "Time in background: " + timeInBackground / 1000 + " seconds");

            if (timeInBackground > TIMEOUT_IN_MS) {
                Log.d(TAG, "Timeout exceeded. Signing out user.");
                FirebaseAuth.getInstance().signOut();
                prefsHelper.clearUserData();
            }
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public void onAppBackgrounded() {
        Log.d(TAG, "App is in BACKGROUND");
        prefsHelper.putLong("last_active_time", System.currentTimeMillis());
    }
}