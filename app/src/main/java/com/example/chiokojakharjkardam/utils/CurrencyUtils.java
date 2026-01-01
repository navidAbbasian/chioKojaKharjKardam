package com.example.chiokojakharjkardam.utils;

import java.text.DecimalFormat;

/**
 * کلاس کمکی برای فرمت کردن مبالغ پولی
 */
public class CurrencyUtils {

    private static final DecimalFormat formatter = new DecimalFormat("#,###");

    /**
     * فرمت مبلغ با جداکننده هزارگان
     * @param amount مبلغ به ریال
     * @return مبلغ فرمت شده با فارسی
     */
    public static String formatAmount(long amount) {
        String formatted = formatter.format(Math.abs(amount));
        return PersianDateUtils.toPersianDigits(formatted);
    }

    /**
     * فرمت مبلغ با واحد تومان
     * @param amount مبلغ به ریال
     * @return مبلغ فرمت شده با واحد تومان
     */
    public static String formatAmountWithToman(long amount) {
        // تبدیل ریال به تومان
        long toman = amount / 10;
        return formatAmount(toman) + " تومان";
    }

    /**
     * فرمت مبلغ با واحد ریال
     * @param amount مبلغ به ریال
     * @return مبلغ فرمت شده با واحد ریال
     */
    public static String formatAmountWithRial(long amount) {
        return formatAmount(amount) + " ریال";
    }

    /**
     * فرمت مبلغ با علامت مثبت یا منفی
     * @param amount مبلغ
     * @param isExpense آیا خرج است
     * @return مبلغ با علامت
     */
    public static String formatAmountWithSign(long amount, boolean isExpense) {
        String sign = isExpense ? "-" : "+";
        return sign + formatAmountWithToman(amount);
    }

    /**
     * تبدیل رشته به عدد
     * @param text رشته حاوی عدد (فارسی یا انگلیسی)
     * @return عدد
     */
    public static long parseAmount(String text) {
        if (text == null || text.isEmpty()) return 0;

        // حذف کاراکترهای اضافی
        String cleaned = PersianDateUtils.toEnglishDigits(text)
                .replaceAll("[^0-9]", "");

        try {
            return Long.parseLong(cleaned);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * تبدیل تومان به ریال
     */
    public static long tomanToRial(long toman) {
        return toman * 10;
    }

    /**
     * تبدیل ریال به تومان
     */
    public static long rialToToman(long rial) {
        return rial / 10;
    }

    /**
     * فرمت خلاصه مبلغ (مثل ۱.۲ میلیون)
     */
    public static String formatCompactAmount(long amount) {
        long toman = amount / 10;

        if (toman >= 1_000_000_000) {
            double value = toman / 1_000_000_000.0;
            return PersianDateUtils.toPersianDigits(String.format("%.1f", value)) + " میلیارد";
        } else if (toman >= 1_000_000) {
            double value = toman / 1_000_000.0;
            return PersianDateUtils.toPersianDigits(String.format("%.1f", value)) + " میلیون";
        } else if (toman >= 1_000) {
            double value = toman / 1_000.0;
            return PersianDateUtils.toPersianDigits(String.format("%.0f", value)) + " هزار";
        } else {
            return formatAmountWithToman(amount);
        }
    }

    /**
     * فرمت مبلغ با واحد تومان (مستقیماً تومان است نه ریال)
     * @param amount مبلغ به تومان
     * @return مبلغ فرمت شده با واحد تومان
     */
    public static String formatToman(long amount) {
        return formatAmount(amount) + " تومان";
    }
}

