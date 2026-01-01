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

public class AddBillFragment extends Fragment {

    private AddBillViewModel viewModel;

    private TextInputEditText etTitle;
    private TextInputEditText etAmount;
    private TextInputEditText etDueDate;
    private SwitchMaterial switchRecurring;
    private Spinner spinnerRecurringType;
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
        etDueDate = view.findViewById(R.id.et_due_date);
        switchRecurring = view.findViewById(R.id.switch_recurring);
        spinnerRecurringType = view.findViewById(R.id.spinner_recurring_type);
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

        Bill bill = new Bill(title, amount, selectedDueDate, isRecurring, recurringType, notifyBefore);

        if (editBillId != -1) {
            bill.setId(editBillId);
            // حفظ اطلاعات قبلی که نباید تغییر کنند
            if (existingBill != null) {
                bill.setPaid(existingBill.isPaid());
                bill.setCreatedAt(existingBill.getCreatedAt());
                bill.setCardId(existingBill.getCardId());
            }
            viewModel.updateBill(bill);
        } else {
            viewModel.insertBill(bill);
        }

        Toast.makeText(requireContext(), "قبض ذخیره شد", Toast.LENGTH_SHORT).show();
        Navigation.findNavController(requireView()).popBackStack();
    }
}

