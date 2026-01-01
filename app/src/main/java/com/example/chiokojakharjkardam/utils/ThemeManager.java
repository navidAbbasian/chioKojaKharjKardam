package com.example.chiokojakharjkardam.utils;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

/**
 * مدیریت تم برنامه (دارک مد / لایت مد)
 */
public class ThemeManager {

    private static final String PREF_NAME = "theme_preferences";
    private static final String KEY_DARK_MODE = "dark_mode";

    private final SharedPreferences preferences;

    public ThemeManager(Context context) {
        preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    /**
     * بررسی وضعیت دارک مد
     */
    public boolean isDarkMode() {
        return preferences.getBoolean(KEY_DARK_MODE, false);
    }

    /**
     * تنظیم دارک مد
     */
    public void setDarkMode(boolean isDarkMode) {
        preferences.edit().putBoolean(KEY_DARK_MODE, isDarkMode).apply();
        applyTheme(isDarkMode);
    }

    /**
     * اعمال تم به برنامه
     */
    public void applyTheme(boolean isDarkMode) {
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }

    /**
     * اعمال تم ذخیره شده
     */
    public void applySavedTheme() {
        applyTheme(isDarkMode());
    }
}

