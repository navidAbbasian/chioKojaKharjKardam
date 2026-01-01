package com.example.chiokojakharjkardam;

import android.app.Application;

import com.example.chiokojakharjkardam.utils.NotificationHelper;

public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // ایجاد کانال اعلان
        NotificationHelper.createNotificationChannel(this);
    }
}

