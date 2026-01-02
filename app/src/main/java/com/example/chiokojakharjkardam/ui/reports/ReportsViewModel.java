package com.example.chiokojakharjkardam.ui.reports;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.chiokojakharjkardam.data.database.entity.CategoryReport;
import com.example.chiokojakharjkardam.data.database.entity.CombinedReport;
import com.example.chiokojakharjkardam.data.database.entity.TagReport;
import com.example.chiokojakharjkardam.data.repository.TransactionRepository;

import java.util.Calendar;
import java.util.List;

public class ReportsViewModel extends AndroidViewModel {

    public static final int GROUP_BY_CATEGORY = 0;
    public static final int GROUP_BY_TAG = 1;
    public static final int GROUP_BY_COMBINED = 2;

    public static final int TRANSACTION_TYPE_EXPENSE = 0;
    public static final int TRANSACTION_TYPE_INCOME = 1;

    public static final int DATE_RANGE_THIS_MONTH = 0;
    public static final int DATE_RANGE_LAST_3_MONTHS = 1;
    public static final int DATE_RANGE_LAST_YEAR = 2;
    public static final int DATE_RANGE_CUSTOM = 3;

    private final TransactionRepository repository;

    private final MutableLiveData<Integer> groupBy = new MutableLiveData<>(GROUP_BY_CATEGORY);
    private final MutableLiveData<Integer> transactionType = new MutableLiveData<>(TRANSACTION_TYPE_EXPENSE);
    private final MutableLiveData<Integer> dateRangeType = new MutableLiveData<>(DATE_RANGE_THIS_MONTH);
    private final MutableLiveData<Long> startDate = new MutableLiveData<>();
    private final MutableLiveData<Long> endDate = new MutableLiveData<>();

    private final MediatorLiveData<List<CategoryReport>> categoryReports = new MediatorLiveData<>();
    private final MediatorLiveData<List<TagReport>> tagReports = new MediatorLiveData<>();
    private final MediatorLiveData<List<CombinedReport>> combinedReports = new MediatorLiveData<>();

    private LiveData<List<CategoryReport>> currentCategorySource;
    private LiveData<List<TagReport>> currentTagSource;
    private LiveData<List<CombinedReport>> currentCombinedSource;

    public ReportsViewModel(@NonNull Application application) {
        super(application);
        repository = new TransactionRepository(application);

        // تنظیم بازه زمانی پیش‌فرض (این ماه)
        setDateRangeThisMonth();
    }

    public void setGroupBy(int groupByType) {
        groupBy.setValue(groupByType);
        refreshReports();
    }

    public void setTransactionType(int type) {
        transactionType.setValue(type);
        refreshReports();
    }

    public void setDateRange(int rangeType) {
        dateRangeType.setValue(rangeType);
        switch (rangeType) {
            case DATE_RANGE_THIS_MONTH:
                setDateRangeThisMonth();
                break;
            case DATE_RANGE_LAST_3_MONTHS:
                setDateRangeLast3Months();
                break;
            case DATE_RANGE_LAST_YEAR:
                setDateRangeLastYear();
                break;
        }
    }

    public void setCustomDateRange(long start, long end) {
        dateRangeType.setValue(DATE_RANGE_CUSTOM);
        startDate.setValue(start);
        endDate.setValue(end);
        refreshReports();
    }

    private void setDateRangeThisMonth() {
        Calendar calendar = Calendar.getInstance();
        // پایان ماه (الان)
        long end = calendar.getTimeInMillis();

        // شروع ماه
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long start = calendar.getTimeInMillis();

        startDate.setValue(start);
        endDate.setValue(end);
        refreshReports();
    }

    private void setDateRangeLast3Months() {
        Calendar calendar = Calendar.getInstance();
        // پایان (الان)
        long end = calendar.getTimeInMillis();

        // ۳ ماه قبل
        calendar.add(Calendar.MONTH, -3);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long start = calendar.getTimeInMillis();

        startDate.setValue(start);
        endDate.setValue(end);
        refreshReports();
    }

    private void setDateRangeLastYear() {
        Calendar calendar = Calendar.getInstance();
        // پایان (الان)
        long end = calendar.getTimeInMillis();

        // یک سال قبل
        calendar.add(Calendar.YEAR, -1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long start = calendar.getTimeInMillis();

        startDate.setValue(start);
        endDate.setValue(end);
        refreshReports();
    }

    private void refreshReports() {
        Long start = startDate.getValue();
        Long end = endDate.getValue();
        Integer group = groupBy.getValue();
        Integer type = transactionType.getValue();

        if (start == null || end == null || group == null || type == null) {
            return;
        }

        switch (group) {
            case GROUP_BY_CATEGORY:
                refreshCategoryReports(start, end, type);
                break;
            case GROUP_BY_TAG:
                refreshTagReports(start, end, type);
                break;
            case GROUP_BY_COMBINED:
                refreshCombinedReports(start, end, type);
                break;
        }
    }

    private void refreshCategoryReports(long start, long end, int type) {
        if (currentCategorySource != null) {
            categoryReports.removeSource(currentCategorySource);
        }

        if (type == TRANSACTION_TYPE_EXPENSE) {
            currentCategorySource = repository.getExpenseReportByCategory(start, end);
        } else {
            currentCategorySource = repository.getIncomeReportByCategory(start, end);
        }

        categoryReports.addSource(currentCategorySource, categoryReports::setValue);
    }

    private void refreshTagReports(long start, long end, int type) {
        if (currentTagSource != null) {
            tagReports.removeSource(currentTagSource);
        }

        if (type == TRANSACTION_TYPE_EXPENSE) {
            currentTagSource = repository.getExpenseReportByTag(start, end);
        } else {
            currentTagSource = repository.getIncomeReportByTag(start, end);
        }

        tagReports.addSource(currentTagSource, tagReports::setValue);
    }

    private void refreshCombinedReports(long start, long end, int type) {
        if (currentCombinedSource != null) {
            combinedReports.removeSource(currentCombinedSource);
        }

        if (type == TRANSACTION_TYPE_EXPENSE) {
            currentCombinedSource = repository.getExpenseReportByCategoryAndTag(start, end);
        } else {
            currentCombinedSource = repository.getIncomeReportByCategoryAndTag(start, end);
        }

        combinedReports.addSource(currentCombinedSource, combinedReports::setValue);
    }

    // Getters
    public LiveData<Integer> getGroupBy() {
        return groupBy;
    }

    public LiveData<Integer> getTransactionType() {
        return transactionType;
    }

    public LiveData<Integer> getDateRangeType() {
        return dateRangeType;
    }

    public LiveData<Long> getStartDate() {
        return startDate;
    }

    public LiveData<Long> getEndDate() {
        return endDate;
    }

    public LiveData<List<CategoryReport>> getCategoryReports() {
        return categoryReports;
    }

    public LiveData<List<TagReport>> getTagReports() {
        return tagReports;
    }

    public LiveData<List<CombinedReport>> getCombinedReports() {
        return combinedReports;
    }
}

