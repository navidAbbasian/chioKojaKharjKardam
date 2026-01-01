package com.example.chiokojakharjkardam;

import android.app.Application;

import com.example.chiokojakharjkardam.utils.NotificationHelper;
import com.example.chiokojakharjkardam.utils.ThemeManager;

public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // ایجاد کانال اعلان
        NotificationHelper.createNotificationChannel(this);

        // اعمال تم ذخیره شده
        ThemeManager themeManager = new ThemeManager(this);
        themeManager.applySavedTheme();
    }
}

