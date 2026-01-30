package com.example.chiokojakharjkardam.ui.reports;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chiokojakharjkardam.R;
import com.example.chiokojakharjkardam.ui.adapters.ReportAdapter;
import com.example.chiokojakharjkardam.ui.adapters.TransactionAdapter;
import com.example.chiokojakharjkardam.ui.components.PersianDatePickerDialog;
import com.example.chiokojakharjkardam.utils.PersianDateUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.card.MaterialCardView;

import java.text.NumberFormat;
import java.util.Locale;

public class ReportsFragment extends Fragment {

    private ReportsViewModel viewModel;
    private ReportAdapter adapter;
    private TransactionAdapter transactionAdapter;
    private RecyclerView rvReports;
    private RecyclerView rvTransactions;
    private LinearLayout layoutEmpty;
    private LinearLayout layoutEmptyTransactions;
    private MaterialCardView cardTransactions;
    private TextView tvTotalAmount;
    private TextView tvSelectedRange;
    private TextView tvTransactionsCount;
    private MaterialButtonToggleGroup toggleDateRange;
    private MaterialButtonToggleGroup toggleGroupBy;
    private MaterialButtonToggleGroup toggleTransactionType;

    private NumberFormat numberFormat;
    private Long customStartDate;
    private Long customEndDate;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_reports, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        numberFormat = NumberFormat.getNumberInstance(new Locale("fa", "IR"));
        viewModel = new ViewModelProvider(this).get(ReportsViewModel.class);

        initViews(view);
        setupListeners();
        observeData();
    }

    private void initViews(View view) {
        rvReports = view.findViewById(R.id.rv_reports);
        rvTransactions = view.findViewById(R.id.rv_transactions);
        layoutEmpty = view.findViewById(R.id.layout_empty);
        layoutEmptyTransactions = view.findViewById(R.id.layout_empty_transactions);
        cardTransactions = view.findViewById(R.id.card_transactions);
        tvTotalAmount = view.findViewById(R.id.tv_total_amount);
        tvSelectedRange = view.findViewById(R.id.tv_selected_range);
        tvTransactionsCount = view.findViewById(R.id.tv_transactions_count);

        toggleDateRange = view.findViewById(R.id.toggle_date_range);
        toggleGroupBy = view.findViewById(R.id.toggle_group_by);
        toggleTransactionType = view.findViewById(R.id.toggle_transaction_type);

        // تنظیم RecyclerView گزارش‌ها
        adapter = new ReportAdapter(requireContext());
        adapter.setOnReportItemClickListener(new ReportAdapter.OnReportItemClickListener() {
            @Override
            public void onCategoryClick(long categoryId, String categoryName) {
                if (categoryId < 0) {
                    viewModel.clearTransactionFilter();
                } else {
                    viewModel.setCategoryFilter(categoryId);
                }
            }

            @Override
            public void onTagClick(long tagId, String tagName) {
                if (tagId < 0) {
                    viewModel.clearTransactionFilter();
                } else {
                    viewModel.setTagFilter(tagId);
                }
            }

            @Override
            public void onCombinedClick(long categoryId, long tagId, String categoryName, String tagName) {
                if (categoryId < 0 || tagId < 0) {
                    viewModel.clearTransactionFilter();
                } else {
                    viewModel.setCombinedFilter(categoryId, tagId);
                }
            }
        });
        rvReports.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvReports.setAdapter(adapter);

        // تنظیم RecyclerView تراکنش‌ها
        transactionAdapter = new TransactionAdapter(transaction -> {
            Bundle args = new Bundle();
            args.putLong("transactionId", transaction.getId());
            Navigation.findNavController(requireView())
                    .navigate(R.id.transactionDetailFragment, args);
        });
        rvTransactions.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvTransactions.setAdapter(transactionAdapter);

        // تنظیم پیش‌فرض‌ها
        toggleDateRange.check(R.id.btn_this_month);
        toggleGroupBy.check(R.id.btn_by_category);
        toggleTransactionType.check(R.id.btn_expense_type);
    }

    private void setupListeners() {
        // انتخاب بازه زمانی
        toggleDateRange.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                adapter.clearSelection();
                viewModel.clearTransactionFilter();
                if (checkedId == R.id.btn_this_month) {
                    viewModel.setDateRange(ReportsViewModel.DATE_RANGE_THIS_MONTH);
                } else if (checkedId == R.id.btn_last_3_months) {
                    viewModel.setDateRange(ReportsViewModel.DATE_RANGE_LAST_3_MONTHS);
                } else if (checkedId == R.id.btn_last_year) {
                    viewModel.setDateRange(ReportsViewModel.DATE_RANGE_LAST_YEAR);
                }
            }
        });

        // دکمه بازه سفارشی
        MaterialButton btnCustomRange = requireView().findViewById(R.id.btn_custom_range);
        btnCustomRange.setOnClickListener(v -> showCustomDateRangePicker());

        // انتخاب نوع گروه‌بندی
        toggleGroupBy.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                adapter.clearSelection();
                viewModel.clearTransactionFilter();
                if (checkedId == R.id.btn_by_category) {
                    viewModel.setGroupBy(ReportsViewModel.GROUP_BY_CATEGORY);
                } else if (checkedId == R.id.btn_by_tag) {
                    viewModel.setGroupBy(ReportsViewModel.GROUP_BY_TAG);
                } else if (checkedId == R.id.btn_combined) {
                    viewModel.setGroupBy(ReportsViewModel.GROUP_BY_COMBINED);
                }
            }
        });

        // انتخاب نوع تراکنش
        toggleTransactionType.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                adapter.clearSelection();
                viewModel.clearTransactionFilter();
                if (checkedId == R.id.btn_expense_type) {
                    viewModel.setTransactionType(ReportsViewModel.TRANSACTION_TYPE_EXPENSE);
                } else if (checkedId == R.id.btn_income_type) {
                    viewModel.setTransactionType(ReportsViewModel.TRANSACTION_TYPE_INCOME);
                }
            }
        });
    }

    private void showCustomDateRangePicker() {
        // نمایش دیالوگ انتخاب تاریخ شروع
        PersianDatePickerDialog startPicker = new PersianDatePickerDialog(requireContext(),
                (year, month, day, timestamp) -> {
                    customStartDate = timestamp;
                    // نمایش دیالوگ انتخاب تاریخ پایان
                    showEndDatePicker();
                });
        startPicker.show();
    }

    private void showEndDatePicker() {
        PersianDatePickerDialog endPicker = new PersianDatePickerDialog(requireContext(),
                (year, month, day, timestamp) -> {
                    customEndDate = timestamp;
                    // به‌روزرسانی بازه سفارشی
                    if (customStartDate != null && customEndDate != null) {
                        // لغو انتخاب در toggle
                        toggleDateRange.clearChecked();
                        viewModel.setCustomDateRange(customStartDate, customEndDate);
                    }
                });
        endPicker.show();
    }

    private void observeData() {
        // گوش دادن به تغییرات بازه زمانی
        viewModel.getStartDate().observe(getViewLifecycleOwner(), startDate -> {
            updateSelectedRangeText();
        });

        viewModel.getEndDate().observe(getViewLifecycleOwner(), endDate -> {
            updateSelectedRangeText();
        });

        // گوش دادن به نوع گروه‌بندی برای تعویض داده‌ها
        viewModel.getGroupBy().observe(getViewLifecycleOwner(), groupBy -> {
            observeReportsForGroupBy(groupBy);
        });

        // گوش دادن به لیست تراکنش‌ها
        viewModel.getFilteredTransactions().observe(getViewLifecycleOwner(), transactions -> {
            if (transactions != null && !transactions.isEmpty()) {
                transactionAdapter.submitList(transactions);
                rvTransactions.setVisibility(View.VISIBLE);
                layoutEmptyTransactions.setVisibility(View.GONE);
                cardTransactions.setVisibility(View.VISIBLE);
                tvTransactionsCount.setText(getString(R.string.transaction_count_format, transactions.size()));
            } else {
                rvTransactions.setVisibility(View.GONE);
                layoutEmptyTransactions.setVisibility(View.VISIBLE);
                tvTransactionsCount.setText("");
            }
        });
    }

    private void observeReportsForGroupBy(int groupBy) {
        // حذف observer های قبلی
        viewModel.getCategoryReports().removeObservers(getViewLifecycleOwner());
        viewModel.getTagReports().removeObservers(getViewLifecycleOwner());
        viewModel.getCombinedReports().removeObservers(getViewLifecycleOwner());

        switch (groupBy) {
            case ReportsViewModel.GROUP_BY_CATEGORY:
                viewModel.getCategoryReports().observe(getViewLifecycleOwner(), reports -> {
                    if (reports != null && !reports.isEmpty()) {
                        adapter.setCategoryReports(reports);
                        rvReports.setVisibility(View.VISIBLE);
                        layoutEmpty.setVisibility(View.GONE);
                        updateTotalAmount();
                    } else {
                        rvReports.setVisibility(View.GONE);
                        layoutEmpty.setVisibility(View.VISIBLE);
                        tvTotalAmount.setText(numberFormat.format(0));
                    }
                });
                break;

            case ReportsViewModel.GROUP_BY_TAG:
                viewModel.getTagReports().observe(getViewLifecycleOwner(), reports -> {
                    if (reports != null && !reports.isEmpty()) {
                        adapter.setTagReports(reports);
                        rvReports.setVisibility(View.VISIBLE);
                        layoutEmpty.setVisibility(View.GONE);
                        updateTotalAmount();
                    } else {
                        rvReports.setVisibility(View.GONE);
                        layoutEmpty.setVisibility(View.VISIBLE);
                        tvTotalAmount.setText(numberFormat.format(0));
                    }
                });
                break;

            case ReportsViewModel.GROUP_BY_COMBINED:
                viewModel.getCombinedReports().observe(getViewLifecycleOwner(), reports -> {
                    if (reports != null && !reports.isEmpty()) {
                        adapter.setCombinedReports(reports);
                        rvReports.setVisibility(View.VISIBLE);
                        layoutEmpty.setVisibility(View.GONE);
                        updateTotalAmount();
                    } else {
                        rvReports.setVisibility(View.GONE);
                        layoutEmpty.setVisibility(View.VISIBLE);
                        tvTotalAmount.setText(numberFormat.format(0));
                    }
                });
                break;
        }
    }

    private void updateSelectedRangeText() {
        Long start = viewModel.getStartDate().getValue();
        Long end = viewModel.getEndDate().getValue();

        if (start != null && end != null) {
            String startStr = PersianDateUtils.formatTimestamp(start);
            String endStr = PersianDateUtils.formatTimestamp(end);
            tvSelectedRange.setText(startStr + " تا " + endStr);
        }
    }

    private void updateTotalAmount() {
        long total = adapter.getTotalAmount();
        tvTotalAmount.setText(numberFormat.format(total) + " " + getString(R.string.toman));
    }
}

