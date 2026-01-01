package com.example.chiokojakharjkardam.utils;

import java.util.Calendar;
import java.util.Locale;

/**
 * کلاس کمکی برای کار با تاریخ شمسی
 */
public class PersianDateUtils {

    private static final String[] PERSIAN_MONTHS = {
            "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
            "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"
    };

    private static final String[] PERSIAN_WEEKDAYS = {
            "شنبه", "یکشنبه", "دوشنبه", "سه‌شنبه", "چهارشنبه", "پنجشنبه", "جمعه"
    };

    private static final String[] PERSIAN_DIGITS = {"۰", "۱", "۲", "۳", "۴", "۵", "۶", "۷", "۸", "۹"};

    /**
     * تبدیل میلادی به شمسی
     */
    public static int[] gregorianToJalali(int gy, int gm, int gd) {
        int[] gdm = {0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334};
        int jy;

        if (gy > 1600) {
            jy = 979;
            gy -= 1600;
        } else {
            jy = 0;
            gy -= 621;
        }

        int gy2 = (gm > 2) ? (gy + 1) : gy;
        int days = (365 * gy) + ((gy2 + 3) / 4) - ((gy2 + 99) / 100) + ((gy2 + 399) / 400) - 80 + gd + gdm[gm - 1];

        jy += 33 * (days / 12053);
        days %= 12053;
        jy += 4 * (days / 1461);
        days %= 1461;

        if (days > 365) {
            jy += (days - 1) / 365;
            days = (days - 1) % 365;
        }

        int jm = (days < 186) ? 1 + (days / 31) : 7 + ((days - 186) / 30);
        int jd = 1 + ((days < 186) ? (days % 31) : ((days - 186) % 30));

        return new int[]{jy, jm, jd};
    }

    /**
     * تبدیل شمسی به میلادی
     */
    public static int[] jalaliToGregorian(int jy, int jm, int jd) {
        int gy;

        if (jy > 979) {
            gy = 1600;
            jy -= 979;
        } else {
            gy = 621;
        }

        int days = (365 * jy) + ((jy / 33) * 8) + (((jy % 33) + 3) / 4) + 78 + jd + ((jm < 7) ? (jm - 1) * 31 : ((jm - 7) * 30) + 186);

        gy += 400 * (days / 146097);
        days %= 146097;

        if (days > 36524) {
            gy += 100 * (--days / 36524);
            days %= 36524;
            if (days >= 365) days++;
        }

        gy += 4 * (days / 1461);
        days %= 1461;

        if (days > 365) {
            gy += (days - 1) / 365;
            days = (days - 1) % 365;
        }

        int[] gdm = {0, 31, (gy % 4 == 0 && gy % 100 != 0) || (gy % 400 == 0) ? 29 : 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
        int gm = 0;

        for (gm = 0; gm < 13 && days >= gdm[gm]; gm++) {
            days -= gdm[gm];
        }

        return new int[]{gy, gm, days + 1};
    }

    /**
     * تبدیل timestamp به تاریخ شمسی
     */
    public static int[] timestampToJalali(long timestamp) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp);

        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) + 1;
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        return gregorianToJalali(year, month, day);
    }

    /**
     * تبدیل تاریخ شمسی به timestamp
     */
    public static long jalaliToTimestamp(int jy, int jm, int jd) {
        int[] gregorian = jalaliToGregorian(jy, jm, jd);

        Calendar calendar = Calendar.getInstance();
        calendar.set(gregorian[0], gregorian[1] - 1, gregorian[2], 0, 0, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        return calendar.getTimeInMillis();
    }

    /**
     * دریافت تاریخ امروز به شمسی
     */
    public static int[] getTodayJalali() {
        return timestampToJalali(System.currentTimeMillis());
    }

    /**
     * فرمت تاریخ شمسی
     */
    public static String formatJalaliDate(int year, int month, int day) {
        return toPersianDigits(String.format(Locale.US, "%d/%02d/%02d", year, month, day));
    }

    /**
     * فرمت تاریخ شمسی با نام ماه
     */
    public static String formatJalaliDateWithMonthName(int year, int month, int day) {
        return toPersianDigits(String.valueOf(day)) + " " + PERSIAN_MONTHS[month - 1] + " " + toPersianDigits(String.valueOf(year));
    }

    /**
     * فرمت timestamp به تاریخ شمسی
     */
    public static String formatTimestamp(long timestamp) {
        int[] jalali = timestampToJalali(timestamp);
        return formatJalaliDate(jalali[0], jalali[1], jalali[2]);
    }

    /**
     * فرمت timestamp به تاریخ شمسی با نام ماه
     */
    public static String formatTimestampWithMonthName(long timestamp) {
        int[] jalali = timestampToJalali(timestamp);
        return formatJalaliDateWithMonthName(jalali[0], jalali[1], jalali[2]);
    }

    /**
     * نام ماه شمسی
     */
    public static String getMonthName(int month) {
        if (month >= 1 && month <= 12) {
            return PERSIAN_MONTHS[month - 1];
        }
        return "";
    }

    /**
     * تبدیل اعداد به فارسی
     */
    public static String toPersianDigits(String input) {
        if (input == null) return "";

        StringBuilder result = new StringBuilder();
        for (char c : input.toCharArray()) {
            if (c >= '0' && c <= '9') {
                result.append(PERSIAN_DIGITS[c - '0']);
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    /**
     * تبدیل اعداد فارسی به انگلیسی
     */
    public static String toEnglishDigits(String input) {
        if (input == null) return "";

        return input
                .replace("۰", "0")
                .replace("۱", "1")
                .replace("۲", "2")
                .replace("۳", "3")
                .replace("۴", "4")
                .replace("۵", "5")
                .replace("۶", "6")
                .replace("۷", "7")
                .replace("۸", "8")
                .replace("۹", "9");
    }

    /**
     * شروع ماه جاری
     */
    public static long getStartOfCurrentMonth() {
        int[] today = getTodayJalali();
        return jalaliToTimestamp(today[0], today[1], 1);
    }

    /**
     * پایان ماه جاری
     */
    public static long getEndOfCurrentMonth() {
        int[] today = getTodayJalali();
        int daysInMonth = today[1] <= 6 ? 31 : (today[1] <= 11 ? 30 : 29);
        return jalaliToTimestamp(today[0], today[1], daysInMonth) + (24 * 60 * 60 * 1000) - 1;
    }

    /**
     * شروع امروز
     */
    public static long getStartOfToday() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    /**
     * پایان امروز
     */
    public static long getEndOfToday() {
        return getStartOfToday() + (24 * 60 * 60 * 1000) - 1;
    }

    /**
     * فرمت ساده تاریخ از timestamp
     */
    public static String formatDate(long timestamp) {
        return formatTimestamp(timestamp);
    }

    /**
     * بازه ماه جاری [start, end]
     */
    public static long[] getCurrentMonthRange() {
        return new long[]{getStartOfCurrentMonth(), getEndOfCurrentMonth()};
    }
}

