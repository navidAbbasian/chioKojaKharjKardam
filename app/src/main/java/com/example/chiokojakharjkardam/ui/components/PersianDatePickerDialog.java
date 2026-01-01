package com.example.chiokojakharjkardam.ui.components;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Window;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.TextView;

import com.example.chiokojakharjkardam.R;
import com.example.chiokojakharjkardam.utils.PersianDateUtils;

/**
 * دیالوگ انتخاب تاریخ شمسی
 */
public class PersianDatePickerDialog extends Dialog {

    private static final String[] PERSIAN_MONTHS = {
            "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
            "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"
    };

    private NumberPicker yearPicker;
    private NumberPicker monthPicker;
    private NumberPicker dayPicker;

    private OnDateSelectedListener listener;

    private int selectedYear;
    private int selectedMonth;
    private int selectedDay;

    private int minYear = 1350;
    private int maxYear = 1450;

    public interface OnDateSelectedListener {
        void onDateSelected(int year, int month, int day, long timestamp);
    }

    public PersianDatePickerDialog(Context context, OnDateSelectedListener listener) {
        super(context);
        this.listener = listener;

        // تاریخ امروز به عنوان پیش‌فرض
        int[] today = PersianDateUtils.getTodayJalali();
        this.selectedYear = today[0];
        this.selectedMonth = today[1];
        this.selectedDay = today[2];
    }

    public PersianDatePickerDialog(Context context, int year, int month, int day, OnDateSelectedListener listener) {
        super(context);
        this.listener = listener;
        this.selectedYear = year;
        this.selectedMonth = month;
        this.selectedDay = day;
    }

    public static PersianDatePickerDialog fromTimestamp(Context context, long timestamp, OnDateSelectedListener listener) {
        int[] jalali = PersianDateUtils.timestampToJalali(timestamp);
        return new PersianDatePickerDialog(context, jalali[0], jalali[1], jalali[2], listener);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_persian_date_picker);

        if (getWindow() != null) {
            getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        initViews();
        setupPickers();
        setupButtons();
        updatePreview();
    }

    private void initViews() {
        yearPicker = findViewById(R.id.year_picker);
        monthPicker = findViewById(R.id.month_picker);
        dayPicker = findViewById(R.id.day_picker);
    }

    private void setupPickers() {
        // تنظیم انتخابگر سال
        yearPicker.setMinValue(minYear);
        yearPicker.setMaxValue(maxYear);
        yearPicker.setValue(selectedYear);
        yearPicker.setWrapSelectorWheel(false);
        yearPicker.setFormatter(value -> PersianDateUtils.toPersianDigits(String.valueOf(value)));

        // تنظیم انتخابگر ماه
        monthPicker.setMinValue(1);
        monthPicker.setMaxValue(12);
        monthPicker.setValue(selectedMonth);
        monthPicker.setDisplayedValues(PERSIAN_MONTHS);

        // تنظیم انتخابگر روز
        updateDayPicker();

        // شنوندگان تغییر
        yearPicker.setOnValueChangedListener((picker, oldVal, newVal) -> {
            selectedYear = newVal;
            updateDayPicker();
            updatePreview();
        });

        monthPicker.setOnValueChangedListener((picker, oldVal, newVal) -> {
            selectedMonth = newVal;
            updateDayPicker();
            updatePreview();
        });

        dayPicker.setOnValueChangedListener((picker, oldVal, newVal) -> {
            selectedDay = newVal;
            updatePreview();
        });
    }

    private void updateDayPicker() {
        int maxDay = getDaysInMonth(selectedYear, selectedMonth);
        dayPicker.setMinValue(1);
        dayPicker.setMaxValue(maxDay);

        if (selectedDay > maxDay) {
            selectedDay = maxDay;
        }
        dayPicker.setValue(selectedDay);
        dayPicker.setFormatter(value -> PersianDateUtils.toPersianDigits(String.valueOf(value)));
    }

    private int getDaysInMonth(int year, int month) {
        if (month <= 6) {
            return 31;
        } else if (month <= 11) {
            return 30;
        } else {
            // اسفند - بررسی کبیسه
            return isLeapYear(year) ? 30 : 29;
        }
    }

    private boolean isLeapYear(int year) {
        int[] leapYears = {1, 5, 9, 13, 17, 22, 26, 30};
        int cycle = year % 33;
        for (int leap : leapYears) {
            if (cycle == leap) return true;
        }
        return false;
    }

    private void setupButtons() {
        Button btnConfirm = findViewById(R.id.btn_confirm);
        Button btnCancel = findViewById(R.id.btn_cancel);

        btnConfirm.setOnClickListener(v -> {
            if (listener != null) {
                long timestamp = PersianDateUtils.jalaliToTimestamp(selectedYear, selectedMonth, selectedDay);
                listener.onDateSelected(selectedYear, selectedMonth, selectedDay, timestamp);
            }
            dismiss();
        });

        btnCancel.setOnClickListener(v -> dismiss());
    }

    private void updatePreview() {
        TextView tvPreview = findViewById(R.id.tv_date_preview);
        if (tvPreview != null) {
            String preview = PersianDateUtils.formatJalaliDateWithMonthName(selectedYear, selectedMonth, selectedDay);
            tvPreview.setText(preview);
        }
    }

    public void setMinYear(int minYear) {
        this.minYear = minYear;
    }

    public void setMaxYear(int maxYear) {
        this.maxYear = maxYear;
    }
}

