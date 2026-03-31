package com.example.chiokojakharjkardam.ui.reports;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chiokojakharjkardam.R;
import com.example.chiokojakharjkardam.data.database.AppDatabase;
import com.example.chiokojakharjkardam.data.database.entity.Transaction;
import com.example.chiokojakharjkardam.data.database.entity.TransactionDetail;
import com.example.chiokojakharjkardam.ui.adapters.ReportAdapter;
import com.example.chiokojakharjkardam.ui.adapters.TransactionAdapter;
import com.example.chiokojakharjkardam.ui.components.PersianDatePickerDialog;
import com.example.chiokojakharjkardam.utils.PdfExportManager;
import com.example.chiokojakharjkardam.utils.PersianDateUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ReportsFragment extends Fragment {

    private ReportsViewModel viewModel;
    private ReportAdapter adapter;
    private TransactionAdapter transactionAdapter;
    private RecyclerView rvReports;
    private RecyclerView rvTransactions;
    private LinearLayout layoutEmpty;
    private LinearLayout layoutEmptyTransactions;
    private LinearLayout layoutDetailFilters;
    private MaterialCardView cardTransactions;
    private TextView tvTotalAmount;
    private TextView tvSelectedRange;
    private TextView tvTransactionsCount;
    private MaterialButton btnExportPdf;
    private MaterialSwitch switchAllTransactions;
    private MaterialButtonToggleGroup toggleDateRange;
    private MaterialButtonToggleGroup toggleGroupBy;
    private MaterialButtonToggleGroup toggleTransactionType;

    private NumberFormat numberFormat;
    private Long customStartDate;
    private Long customEndDate;
    private List<Transaction> currentTransactions = new ArrayList<>();

    // لانچر برای ذخیره فایل PDF
    private final ActivityResultLauncher<Intent> savePdfLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        performPdfExport(uri);
                    }
                }
            }
    );

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
        layoutDetailFilters = view.findViewById(R.id.layout_detail_filters);
        cardTransactions = view.findViewById(R.id.card_transactions);
        tvTotalAmount = view.findViewById(R.id.tv_total_amount);
        tvSelectedRange = view.findViewById(R.id.tv_selected_range);
        tvTransactionsCount = view.findViewById(R.id.tv_transactions_count);
        btnExportPdf = view.findViewById(R.id.btn_export_pdf);
        switchAllTransactions = view.findViewById(R.id.switch_all_transactions);

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

        // وضعیت اولیه: سوئیچ خاموش، فیلترهای تفصیلی نمایش داده می‌شوند
        switchAllTransactions.setChecked(false);
        layoutDetailFilters.setVisibility(View.VISIBLE);
        btnExportPdf.setVisibility(View.GONE);
        cardTransactions.setVisibility(View.GONE);
    }

    private void setupListeners() {
        // سوئیچ همه تراکنش‌ها
        switchAllTransactions.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                 layoutDetailFilters.setVisibility(View.GONE);
                rvReports.setVisibility(View.GONE);
                layoutEmpty.setVisibility(View.GONE);
                adapter.clearSelection();
                viewModel.clearTransactionFilter();
                viewModel.setTransactionType(ReportsViewModel.TRANSACTION_TYPE_ALL);
            } else {
                layoutDetailFilters.setVisibility(View.VISIBLE);
                adapter.clearSelection();
                viewModel.clearTransactionFilter();
                // بازگشت به نوع خرج
                toggleTransactionType.check(R.id.btn_expense_type);
                viewModel.setTransactionType(ReportsViewModel.TRANSACTION_TYPE_EXPENSE);
            }
        });

        // دکمه PDF
        btnExportPdf.setOnClickListener(v -> startPdfExport());

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

        // بازه سفارشی
        MaterialButton btnCustomRange = requireView().findViewById(R.id.btn_custom_range);
        btnCustomRange.setOnClickListener(v -> showCustomDateRangePicker());

        // انتخاب نوع گروه‌بندی
        toggleGroupBy.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                adapter.clearSelection();
                viewModel.clearTransactionFilter();
                if (checkedId == R.id.btn_by_category) viewModel.setGroupBy(ReportsViewModel.GROUP_BY_CATEGORY);
                else if (checkedId == R.id.btn_by_tag) viewModel.setGroupBy(ReportsViewModel.GROUP_BY_TAG);
                else if (checkedId == R.id.btn_combined) viewModel.setGroupBy(ReportsViewModel.GROUP_BY_COMBINED);
            }
        });

        // انتخاب نوع تراکنش
        toggleTransactionType.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                adapter.clearSelection();
                viewModel.clearTransactionFilter();
                if (checkedId == R.id.btn_expense_type)
                    viewModel.setTransactionType(ReportsViewModel.TRANSACTION_TYPE_EXPENSE);
                else if (checkedId == R.id.btn_income_type)
                    viewModel.setTransactionType(ReportsViewModel.TRANSACTION_TYPE_INCOME);
                else if (checkedId == R.id.btn_transfer_type)
                    viewModel.setTransactionType(ReportsViewModel.TRANSACTION_TYPE_TRANSFER);
            }
        });
    }

    private void startPdfExport() {
        if (currentTransactions.isEmpty()) {
            Toast.makeText(requireContext(), "داده‌ای برای صادر کردن وجود ندارد", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/pdf");
        intent.putExtra(Intent.EXTRA_TITLE, PdfExportManager.generatePdfFileName("report"));
        savePdfLauncher.launch(intent);
    }

    private void performPdfExport(Uri uri) {
        String title = getString(R.string.report_title);
        String dateRange = tvSelectedRange.getText().toString();
        List<Transaction> snapshot = new ArrayList<>(currentTransactions);

        // در background thread اطلاعات category و tag رو می‌گیریم
        AppDatabase.databaseWriteExecutor.execute(() -> {
            AppDatabase db = AppDatabase.getDatabase(requireContext());
            List<TransactionDetail> details = new ArrayList<>();

            for (Transaction t : snapshot) {
                // نام دسته‌بندی
                String catName = "-";
                if (t.getCategoryId() != null && t.getCategoryId() > 0) {
                    var cat = db.categoryDao().getCategoryByIdSync(t.getCategoryId());
                    if (cat != null) catName = cat.getName();
                }

                // نام تگ‌ها
                List<Long> tagIds = db.transactionTagDao().getTagIdsByTransaction(t.getId());
                StringBuilder tagBuilder = new StringBuilder();
                if (tagIds != null && !tagIds.isEmpty()) {
                    var tags = db.tagDao().getTagsByIds(tagIds);
                    for (int i = 0; i < tags.size(); i++) {
                        if (i > 0) tagBuilder.append("، ");
                        tagBuilder.append(tags.get(i).getName());
                    }
                }
                String tagNames = tagBuilder.length() > 0 ? tagBuilder.toString() : "-";

                details.add(new TransactionDetail(t, catName, tagNames));
            }

            PdfExportManager.exportTransactionsToPdf(
                    requireContext(), uri, details, title, dateRange,
                    new PdfExportManager.ExportCallback() {
                        @Override
                        public void onSuccess(String message) {
                            if (isAdded()) requireActivity().runOnUiThread(() ->
                                    Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show());
                        }
                        @Override
                        public void onError(String error) {
                            if (isAdded()) requireActivity().runOnUiThread(() ->
                                    Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show());
                        }
                    });
        });
    }

    private void showCustomDateRangePicker() {
        PersianDatePickerDialog startPicker = new PersianDatePickerDialog(requireContext(),
                (year, month, day, timestamp) -> {
                    customStartDate = timestamp;
                    showEndDatePicker();
                });
        startPicker.show();
    }

    private void showEndDatePicker() {
        PersianDatePickerDialog endPicker = new PersianDatePickerDialog(requireContext(),
                (year, month, day, timestamp) -> {
                    customEndDate = timestamp;
                    if (customStartDate != null) {
                        toggleDateRange.clearChecked();
                        viewModel.setCustomDateRange(customStartDate, customEndDate);
                    }
                });
        endPicker.show();
    }

    private void observeData() {
        viewModel.getStartDate().observe(getViewLifecycleOwner(), s -> updateSelectedRangeText());
        viewModel.getEndDate().observe(getViewLifecycleOwner(), e -> updateSelectedRangeText());
        viewModel.getGroupBy().observe(getViewLifecycleOwner(), this::observeReportsForGroupBy);

        viewModel.getFilteredTransactions().observe(getViewLifecycleOwner(), transactions -> {
            if (transactions != null && !transactions.isEmpty()) {
                currentTransactions = transactions;
                transactionAdapter.submitList(transactions);
                rvTransactions.setVisibility(View.VISIBLE);
                layoutEmptyTransactions.setVisibility(View.GONE);
                cardTransactions.setVisibility(View.VISIBLE);
                btnExportPdf.setVisibility(View.VISIBLE);
                tvTransactionsCount.setText(getString(R.string.transaction_count_format, transactions.size()));
            } else {
                currentTransactions = new ArrayList<>();
                rvTransactions.setVisibility(View.GONE);
                layoutEmptyTransactions.setVisibility(View.VISIBLE);
                cardTransactions.setVisibility(View.VISIBLE);
                btnExportPdf.setVisibility(View.GONE);
                tvTransactionsCount.setText("");
            }
        });
    }

    private void observeReportsForGroupBy(int groupBy) {
        viewModel.getCategoryReports().removeObservers(getViewLifecycleOwner());
        viewModel.getTagReports().removeObservers(getViewLifecycleOwner());
        viewModel.getCombinedReports().removeObservers(getViewLifecycleOwner());

        switch (groupBy) {
            case ReportsViewModel.GROUP_BY_CATEGORY:
                viewModel.getCategoryReports().observe(getViewLifecycleOwner(), reports -> {
                    if (!switchAllTransactions.isChecked()) {
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
                    }
                });
                break;
            case ReportsViewModel.GROUP_BY_TAG:
                viewModel.getTagReports().observe(getViewLifecycleOwner(), reports -> {
                    if (!switchAllTransactions.isChecked()) {
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
                    }
                });
                break;
            case ReportsViewModel.GROUP_BY_COMBINED:
                viewModel.getCombinedReports().observe(getViewLifecycleOwner(), reports -> {
                    if (!switchAllTransactions.isChecked()) {
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
                    }
                });
                break;
        }
    }

    private void updateSelectedRangeText() {
        Long start = viewModel.getStartDate().getValue();
        Long end = viewModel.getEndDate().getValue();

        if (start != null && end != null) {
            tvSelectedRange.setText(PersianDateUtils.formatTimestamp(start) + " تا " + PersianDateUtils.formatTimestamp(end));
        }
    }

    private void updateTotalAmount() {
        long total = adapter.getTotalAmount();
        tvTotalAmount.setText(numberFormat.format(total) + " " + getString(R.string.toman));
    }
}

