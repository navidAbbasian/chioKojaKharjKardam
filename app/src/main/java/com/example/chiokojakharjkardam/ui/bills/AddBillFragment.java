package com.example.chiokojakharjkardam.ui.bills;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.example.chiokojakharjkardam.R;
import com.example.chiokojakharjkardam.data.database.entity.Bill;
import com.example.chiokojakharjkardam.ui.components.PersianDatePickerDialog;
import com.example.chiokojakharjkardam.utils.CurrencyUtils;
import com.example.chiokojakharjkardam.utils.PersianDateUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.slider.Slider;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class AddBillFragment extends Fragment {

    private AddBillViewModel viewModel;

    private TextInputEditText etTitle;
    private TextInputEditText etAmount;
    private TextInputLayout tilDueDate;
    private TextInputEditText etDueDate;
    private TextInputLayout tilDayOfMonth;
    private TextInputEditText etDayOfMonth;
    private SwitchMaterial switchRecurring;
    private Spinner spinnerRecurringType;
    private TextInputLayout tilRecurrenceCount;
    private TextInputEditText etRecurrenceCount;
    private Slider sliderNotify;
    private TextView tvNotifyDays;
    private MaterialButton btnSave;

    private long editBillId = -1;
    private long selectedDueDate = System.currentTimeMillis();
    private Bill existingBill = null; // برای حفظ اطلاعات قبض در حالت ویرایش
    private boolean isDataLoaded = false; // جلوگیری از لود چندباره

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_add_bill, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(AddBillViewModel.class);

        if (getArguments() != null) {
            editBillId = getArguments().getLong("billId", -1);
        }

        initViews(view);
        setupSpinner();
        setupListeners();

        if (editBillId != -1) {
            loadBill();
        } else {
            updateDateDisplay();
        }
    }

    private void initViews(View view) {
        etTitle = view.findViewById(R.id.et_title);
        etAmount = view.findViewById(R.id.et_amount);
        tilDueDate = view.findViewById(R.id.til_due_date);
        etDueDate = view.findViewById(R.id.et_due_date);
        tilDayOfMonth = view.findViewById(R.id.til_day_of_month);
        etDayOfMonth = view.findViewById(R.id.et_day_of_month);
        switchRecurring = view.findViewById(R.id.switch_recurring);
        spinnerRecurringType = view.findViewById(R.id.spinner_recurring_type);
        tilRecurrenceCount = view.findViewById(R.id.til_recurrence_count);
        etRecurrenceCount = view.findViewById(R.id.et_recurrence_count);
        sliderNotify = view.findViewById(R.id.slider_notify);
        tvNotifyDays = view.findViewById(R.id.tv_notify_days);
        btnSave = view.findViewById(R.id.btn_save);
    }

    private void setupSpinner() {
        String[] recurringTypes = {"ماهانه", "سالانه"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, recurringTypes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRecurringType.setAdapter(adapter);
    }

    private void setupListeners() {
        etDueDate.setOnClickListener(v -> showDatePicker());

        switchRecurring.setOnCheckedChangeListener((buttonView, isChecked) -> {
            spinnerRecurringType.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            // فقط در حالت ایجاد جدید
            if (editBillId == -1) {
                tilRecurrenceCount.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                // تغییر بین دیت پیکر و روز ماه
                tilDueDate.setVisibility(isChecked ? View.GONE : View.VISIBLE);
                tilDayOfMonth.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            }
        });

        sliderNotify.addOnChangeListener((slider, value, fromUser) -> {
            int days = (int) value;
            if (days == 0) {
                tvNotifyDays.setText("بدون یادآوری");
            } else {
                tvNotifyDays.setText(PersianDateUtils.toPersianDigits(String.valueOf(days)) + " روز قبل از سررسید");
            }
        });

        btnSave.setOnClickListener(v -> saveBill());
    }

    private void showDatePicker() {
        PersianDatePickerDialog dialog = PersianDatePickerDialog.fromTimestamp(
                requireContext(),
                selectedDueDate,
                (year, month, day, timestamp) -> {
                    selectedDueDate = timestamp;
                    updateDateDisplay();
                }
        );
        dialog.show();
    }

    private void updateDateDisplay() {
        etDueDate.setText(PersianDateUtils.formatDate(selectedDueDate));
    }

    private void loadBill() {
        viewModel.getBillById(editBillId).observe(getViewLifecycleOwner(), bill -> {
            if (bill != null && !isDataLoaded) {
                isDataLoaded = true;
                existingBill = bill; // ذخیره قبض برای استفاده در هنگام ذخیره
                etTitle.setText(bill.getTitle());
                etAmount.setText(String.valueOf(bill.getAmount()));
                selectedDueDate = bill.getDueDate();
                updateDateDisplay();
                switchRecurring.setChecked(bill.isRecurring());
                if (bill.isRecurring()) {
                    spinnerRecurringType.setVisibility(View.VISIBLE);
                    spinnerRecurringType.setSelection(bill.getRecurringType() == Bill.RECURRING_YEARLY ? 1 : 0);
                }
                // اطمینان از محدوده مجاز slider
                int notifyValue = Math.max(0, Math.min(7, bill.getNotifyBefore()));
                sliderNotify.setValue(notifyValue);
            }
        });
    }

    private void saveBill() {
        String title = etTitle.getText().toString().trim();
        String amountStr = etAmount.getText().toString().trim();

        if (title.isEmpty()) {
            etTitle.setError("عنوان را وارد کنید");
            return;
        }

        if (amountStr.isEmpty()) {
            etAmount.setError("مبلغ را وارد کنید");
            return;
        }

        long amount = CurrencyUtils.parseAmount(amountStr);
        boolean isRecurring = switchRecurring.isChecked();
        int recurringType = isRecurring ?
                (spinnerRecurringType.getSelectedItemPosition() == 1 ? Bill.RECURRING_YEARLY : Bill.RECURRING_MONTHLY)
                : Bill.RECURRING_NONE;
        int notifyBefore = (int) sliderNotify.getValue();

        if (editBillId != -1) {
            // حالت ویرایش - فقط یک قبض آپدیت می‌شود
            Bill bill = new Bill(title, amount, selectedDueDate, isRecurring, recurringType, notifyBefore);
            bill.setId(editBillId);
            // حفظ اطلاعات قبلی که نباید تغییر کنند
            if (existingBill != null) {
                bill.setPaid(existingBill.isPaid());
                bill.setCreatedAt(existingBill.getCreatedAt());
                bill.setCardId(existingBill.getCardId());
            }
            viewModel.updateBill(bill);
            Toast.makeText(requireContext(), "قبض ذخیره شد", Toast.LENGTH_SHORT).show();
        } else {
            // حالت ایجاد جدید
            if (isRecurring) {
                // ایجاد چندین قبض بر اساس تعداد تکرار
                int recurrenceCount = getRecurrenceCount();
                if (recurrenceCount <= 0) {
                    etRecurrenceCount.setError("تعداد تکرار را وارد کنید");
                    return;
                }

                int dayOfMonth = getDayOfMonth();
                if (dayOfMonth <= 0 || dayOfMonth > 31) {
                    etDayOfMonth.setError("روز معتبر وارد کنید (۱ تا ۳۱)");
                    return;
                }

                List<Bill> bills = createRecurringBillsFromDay(title, amount, dayOfMonth,
                        recurringType, notifyBefore, recurrenceCount);
                viewModel.insertBills(bills);
                Toast.makeText(requireContext(),
                        PersianDateUtils.toPersianDigits(String.valueOf(recurrenceCount)) + " قبض ایجاد شد",
                        Toast.LENGTH_SHORT).show();
            } else {
                // یک قبض ساده ایجاد می‌شود
                Bill bill = new Bill(title, amount, selectedDueDate, isRecurring, recurringType, notifyBefore);
                viewModel.insertBill(bill);
                Toast.makeText(requireContext(), "قبض ذخیره شد", Toast.LENGTH_SHORT).show();
            }
        }

        Navigation.findNavController(requireView()).popBackStack();
    }

    private int getDayOfMonth() {
        String dayStr = etDayOfMonth.getText().toString().trim();
        if (dayStr.isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(dayStr);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private int getRecurrenceCount() {
        String countStr = etRecurrenceCount.getText().toString().trim();
        if (countStr.isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(countStr);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private List<Bill> createRecurringBillsFromDay(String title, long amount, int dayOfMonth,
                                                    int recurringType, int notifyBefore, int count) {
        List<Bill> bills = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();

        // شروع از ماه جاری با روز مشخص شده
        int maxDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
        calendar.set(Calendar.DAY_OF_MONTH, Math.min(dayOfMonth, maxDay));
        calendar.set(Calendar.HOUR_OF_DAY, 12);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        // اگر روز گذشته، از ماه بعد شروع کن
        if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
            if (recurringType == Bill.RECURRING_MONTHLY) {
                calendar.add(Calendar.MONTH, 1);
                maxDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
                calendar.set(Calendar.DAY_OF_MONTH, Math.min(dayOfMonth, maxDay));
            } else if (recurringType == Bill.RECURRING_YEARLY) {
                calendar.add(Calendar.YEAR, 1);
                maxDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
                calendar.set(Calendar.DAY_OF_MONTH, Math.min(dayOfMonth, maxDay));
            }
        }

        for (int i = 0; i < count; i++) {
            Bill bill = new Bill(title, amount, calendar.getTimeInMillis(), true, recurringType, notifyBefore);
            bills.add(bill);

            // محاسبه تاریخ سررسید بعدی
            if (recurringType == Bill.RECURRING_MONTHLY) {
                calendar.add(Calendar.MONTH, 1);
                // تنظیم روز ماه به روز اصلی
                maxDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
                calendar.set(Calendar.DAY_OF_MONTH, Math.min(dayOfMonth, maxDay));
            } else if (recurringType == Bill.RECURRING_YEARLY) {
                calendar.add(Calendar.YEAR, 1);
                maxDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
                calendar.set(Calendar.DAY_OF_MONTH, Math.min(dayOfMonth, maxDay));
            }
        }

        return bills;
    }
}

