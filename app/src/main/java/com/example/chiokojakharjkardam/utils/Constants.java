package com.example.chiokojakharjkardam.utils;

/**
 * ثابت‌های برنامه
 */
public class Constants {

    // Notification
    public static final String NOTIFICATION_CHANNEL_ID = "bill_reminder_channel";
    public static final String NOTIFICATION_CHANNEL_NAME = "یادآوری قبوض";

    // Default colors for members
    public static final String[] MEMBER_COLORS = {
            "#4CAF50", // سبز
            "#2196F3", // آبی
            "#FF9800", // نارنجی
            "#9C27B0", // بنفش
            "#E91E63", // صورتی
            "#00BCD4", // فیروزه‌ای
            "#FF5722", // قرمز نارنجی
            "#3F51B5"  // نیلی
    };

    // Default colors for bank cards
    public static final String[] CARD_COLORS = {
            "#1A237E", // آبی تیره
            "#004D40", // سبز تیره
            "#BF360C", // قرمز تیره
            "#4A148C", // بنفش تیره
            "#263238", // خاکستری تیره
            "#E65100", // نارنجی تیره
            "#1B5E20", // سبز
            "#880E4F"  // صورتی تیره
    };

    // Default colors for tags
    public static final String[] TAG_COLORS = {
            "#F44336", "#E91E63", "#9C27B0", "#673AB7",
            "#3F51B5", "#2196F3", "#03A9F4", "#00BCD4",
            "#009688", "#4CAF50", "#8BC34A", "#CDDC39",
            "#FFEB3B", "#FFC107", "#FF9800", "#FF5722"
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

