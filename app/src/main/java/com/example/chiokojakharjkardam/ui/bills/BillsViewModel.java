package com.example.chiokojakharjkardam.ui.bills;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.chiokojakharjkardam.data.database.entity.Bill;
import com.example.chiokojakharjkardam.data.repository.BillRepository;
import com.example.chiokojakharjkardam.utils.PersianDateUtils;

import java.util.List;
import java.util.stream.Collectors;

public class BillsViewModel extends AndroidViewModel {

    public static final int VIEW_MODE_CURRENT_MONTH = 0;  // ماه جاری شمسی
    public static final int VIEW_MODE_NEXT_30_DAYS = 1;   // ۳۰ روز آینده

    private final BillRepository repository;
    private final LiveData<List<Bill>> allBills;
    private final MutableLiveData<Boolean> showPaid = new MutableLiveData<>(false);
    private final MutableLiveData<Integer> viewMode = new MutableLiveData<>(VIEW_MODE_CURRENT_MONTH);
    private final MediatorLiveData<List<Bill>> currentMonthBills = new MediatorLiveData<>();
    private final MediatorLiveData<List<Bill>> futureBills = new MediatorLiveData<>();

    public BillsViewModel(@NonNull Application application) {
        super(application);
        repository = new BillRepository(application);
        allBills = repository.getAllBills();

        currentMonthBills.addSource(allBills, bills -> {
            applyFilter(bills, showPaid.getValue(), viewMode.getValue());
        });

        currentMonthBills.addSource(showPaid, paid -> {
            applyFilter(allBills.getValue(), paid, viewMode.getValue());
        });

        currentMonthBills.addSource(viewMode, mode -> {
            applyFilter(allBills.getValue(), showPaid.getValue(), mode);
        });

        futureBills.addSource(allBills, bills -> {
            applyFutureFilter(bills, showPaid.getValue(), viewMode.getValue());
        });

        futureBills.addSource(showPaid, paid -> {
            applyFutureFilter(allBills.getValue(), paid, viewMode.getValue());
        });

        futureBills.addSource(viewMode, mode -> {
            applyFutureFilter(allBills.getValue(), showPaid.getValue(), mode);
        });
    }

    private long getEndOfCurrentPersianMonth() {
        return PersianDateUtils.getEndOfCurrentMonth();
    }

    private long getStartOfCurrentPersianMonth() {
        return PersianDateUtils.getStartOfCurrentMonth();
    }

    private void applyFilter(List<Bill> bills, Boolean showPaid, Integer mode) {
        if (bills == null) {
            currentMonthBills.setValue(null);
            return;
        }

        boolean paid = showPaid != null && showPaid;
        int viewModeValue = mode != null ? mode : VIEW_MODE_CURRENT_MONTH;

        long now = System.currentTimeMillis();
        long startDate;
        long endDate;

        if (viewModeValue == VIEW_MODE_NEXT_30_DAYS) {
            // ۳۰ روز آینده
            startDate = now;
            endDate = PersianDateUtils.getThirtyDaysFromNow();
        } else {
            // ماه جاری شمسی
            startDate = getStartOfCurrentPersianMonth();
            endDate = getEndOfCurrentPersianMonth();
        }

        currentMonthBills.setValue(
                bills.stream()
                        .filter(b -> b.isPaid() == paid)
                        .filter(b -> b.getDueDate() >= startDate && b.getDueDate() <= endDate)
                        .collect(Collectors.toList())
        );
    }

    private void applyFutureFilter(List<Bill> bills, Boolean showPaid, Integer mode) {
        if (bills == null) {
            futureBills.setValue(null);
            return;
        }

        boolean paid = showPaid != null && showPaid;
        int viewModeValue = mode != null ? mode : VIEW_MODE_CURRENT_MONTH;

        long endDate;
        if (viewModeValue == VIEW_MODE_NEXT_30_DAYS) {
            endDate = PersianDateUtils.getThirtyDaysFromNow();
        } else {
            endDate = getEndOfCurrentPersianMonth();
        }

        futureBills.setValue(
                bills.stream()
                        .filter(b -> b.isPaid() == paid)
                        .filter(b -> b.getDueDate() > endDate)
                        .collect(Collectors.toList())
        );
    }

    public void setShowPaid(boolean showPaid) {
        this.showPaid.setValue(showPaid);
    }

    public void setViewMode(int mode) {
        this.viewMode.setValue(mode);
    }

    public LiveData<Integer> getViewMode() {
        return viewMode;
    }

    public LiveData<List<Bill>> getCurrentMonthBills() {
        return currentMonthBills;
    }

    public LiveData<List<Bill>> getFutureBills() {
        return futureBills;
    }

    public void markAsPaid(Bill bill) {
        bill.setPaid(true);
        repository.update(bill);
    }

    public void deleteBill(Bill bill) {
        repository.delete(bill);
    }
}

