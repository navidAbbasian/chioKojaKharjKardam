package com.example.chiokojakharjkardam.ui.bills;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.chiokojakharjkardam.data.database.entity.Bill;
import com.example.chiokojakharjkardam.data.repository.BillRepository;

import java.util.Calendar;
import java.util.List;
import java.util.stream.Collectors;

public class BillsViewModel extends AndroidViewModel {

    private final BillRepository repository;
    private final LiveData<List<Bill>> allBills;
    private final MutableLiveData<Boolean> showPaid = new MutableLiveData<>(false);
    private final MediatorLiveData<List<Bill>> currentMonthBills = new MediatorLiveData<>();
    private final MediatorLiveData<List<Bill>> futureBills = new MediatorLiveData<>();

    public BillsViewModel(@NonNull Application application) {
        super(application);
        repository = new BillRepository(application);
        allBills = repository.getAllBills();

        currentMonthBills.addSource(allBills, bills -> {
            applyFilter(bills, showPaid.getValue());
        });

        currentMonthBills.addSource(showPaid, paid -> {
            applyFilter(allBills.getValue(), paid);
        });

        futureBills.addSource(allBills, bills -> {
            applyFutureFilter(bills, showPaid.getValue());
        });

        futureBills.addSource(showPaid, paid -> {
            applyFutureFilter(allBills.getValue(), paid);
        });
    }

    private long getEndOfCurrentMonth() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        return calendar.getTimeInMillis();
    }

    private void applyFilter(List<Bill> bills, Boolean showPaid) {
        if (bills == null) {
            currentMonthBills.setValue(null);
            return;
        }

        boolean paid = showPaid != null && showPaid;
        long endOfMonth = getEndOfCurrentMonth();

        currentMonthBills.setValue(
                bills.stream()
                        .filter(b -> b.isPaid() == paid)
                        .filter(b -> b.getDueDate() <= endOfMonth)
                        .collect(Collectors.toList())
        );
    }

    private void applyFutureFilter(List<Bill> bills, Boolean showPaid) {
        if (bills == null) {
            futureBills.setValue(null);
            return;
        }

        boolean paid = showPaid != null && showPaid;
        long endOfMonth = getEndOfCurrentMonth();

        futureBills.setValue(
                bills.stream()
                        .filter(b -> b.isPaid() == paid)
                        .filter(b -> b.getDueDate() > endOfMonth)
                        .collect(Collectors.toList())
        );
    }

    public void setShowPaid(boolean showPaid) {
        this.showPaid.setValue(showPaid);
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

