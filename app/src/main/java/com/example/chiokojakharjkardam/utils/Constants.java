package com.example.chiokojakharjkardam.utils;

/**
 * ثابت‌های برنامه
 */
public class Constants {

    // Notification
    public static final String NOTIFICATION_CHANNEL_ID = "bill_reminder_channel";
    public static final String NOTIFICATION_CHANNEL_NAME = "یادآوری قبوض";

    // Default colors for members - Modern 2026 palette
    public static final String[] MEMBER_COLORS = {
            "#3D7A5F", // سبز نعنایی
            "#5B9BD5", // آبی ملایم
            "#E07A5F", // مرجانی
            "#8B7EC8", // لوندر
            "#E8A0BF", // صورتی ملایم
            "#5BBFBA", // فیروزه‌ای
            "#F0A868", // نارنجی ملایم
            "#7A8B99"  // خاکستری آبی
    };

    // Default colors for bank cards - 8 Main Colors (2026)
    public static final String[] CARD_COLORS = {
            "#DC2626", // قرمز (Red)
            "#EAB308", // زرد (Yellow)
            "#16A34A", // سبز (Green)
            "#7C3AED", // بنفش (Purple)
            "#EC4899", // صورتی (Pink)
            "#1F2937", // سیاه (Black)
            "#2563EB", // آبی (Blue)
            "#D97706"  // طلایی (Gold)
    };

    // Default colors for tags - Modern soft palette
    public static final String[] TAG_COLORS = {
            "#E35555", "#E07A5F", "#8B7EC8", "#5B9BD5",
            "#3D7A5F", "#5BBFBA", "#F0A868", "#E8A0BF",
            "#7A8B99", "#3D9970", "#9F7AEA", "#48BB78",
            "#ED8936", "#667EEA", "#38B2AC", "#FC8181"
    };

    // Iranian banks
    public static final String[] BANK_NAMES = {
            "ملی",
            "سپه",
            "ملت",
            "تجارت",
            "صادرات",
            "مسکن",
            "کشاورزی",
            "رفاه",
            "پاسارگاد",
            "پارسیان",
            "سامان",
            "اقتصاد نوین",
            "شهر",
            "آینده",
            "دی",
            "سینا",
            "کارآفرین",
            "گردشگری",
            "ایران زمین",
            "سایر"
    };

    // Shared Preferences keys
    public static final String PREF_NAME = "chiokojakharjkardam_prefs";
    public static final String PREF_FIRST_RUN = "first_run";
    public static final String PREF_FAMILY_CREATED = "family_created";
    public static final String PREF_DARK_MODE = "dark_mode";

    // Request codes
    public static final int REQUEST_NOTIFICATION_PERMISSION = 1001;

    // Date formats
    public static final String DATE_FORMAT_PERSIAN = "yyyy/MM/dd";
}

