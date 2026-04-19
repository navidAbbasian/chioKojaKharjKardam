package com.example.chiokojakharjkardam;

import android.app.Application;

import com.example.chiokojakharjkardam.utils.NetworkMonitor;
import com.example.chiokojakharjkardam.utils.NotificationHelper;
import com.example.chiokojakharjkardam.utils.SessionManager;
import com.example.chiokojakharjkardam.utils.SyncLogger;
import com.example.chiokojakharjkardam.utils.SyncManager;
import com.example.chiokojakharjkardam.utils.ThemeManager;

public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // Supabase session + network monitor (must be first)
        SessionManager.init(this);
        NetworkMonitor.init(this);

        // Background sync manager
        SyncManager.init(this);

        // Sync logger (file-based logging for sync operations)
        SyncLogger.init(this);

        // ایجاد کانال اعلان
        NotificationHelper.createNotificationChannel(this);

        // اعمال تم ذخیره شده
        ThemeManager themeManager = new ThemeManager(this);
        themeManager.applySavedTheme();
    }
}
