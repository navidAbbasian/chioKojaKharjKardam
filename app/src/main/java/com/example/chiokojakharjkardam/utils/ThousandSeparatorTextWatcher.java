package com.example.chiokojakharjkardam.utils;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * TextWatcher برای نمایش خودکار جداکننده هزارگان در فیلدهای ورود مبلغ
 * مثال: ۱۲۳۴۵۶۷ → ۱۲,۳۴۵,۶۷۸
 */
public class ThousandSeparatorTextWatcher implements TextWatcher {

    private final EditText editText;
    private boolean isFormatting = false;

    private static final DecimalFormat FORMATTER;

    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setGroupingSeparator(',');
        FORMATTER = new DecimalFormat("#,###", symbols);
    }

    public ThousandSeparatorTextWatcher(EditText editText) {
        this.editText = editText;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {}

    @Override
    public void afterTextChanged(Editable s) {
        if (isFormatting) return;
        isFormatting = true;

        // تبدیل ارقام فارسی به انگلیسی و حذف کاراکترهای غیر عددی
        String cleaned = PersianDateUtils.toEnglishDigits(s.toString())
                .replaceAll("[^0-9]", "");

        if (!cleaned.isEmpty()) {
            try {
                long number = Long.parseLong(cleaned);
                String formatted = FORMATTER.format(number);
                editText.setText(formatted);
                editText.setSelection(formatted.length());
            } catch (NumberFormatException e) {
                // در صورت خطا، بدون تغییر
            }
        } else {
            // اگر خالی شد، متن را خالی کن
            if (s.length() > 0) {
                editText.setText("");
            }
        }

        isFormatting = false;
    }

    /**
     * مقداردهی اولیه فیلد با عدد فرمت‌شده (برای حالت ویرایش)
     */
    public static void setFormattedAmount(EditText editText, long amount) {
        if (amount <= 0) {
            editText.setText("");
            return;
        }
        String formatted = FORMATTER.format(amount);
        editText.setText(formatted);
        editText.setSelection(formatted.length());
    }
}

