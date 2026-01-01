package com.example.chiokojakharjkardam.ui.bills;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.chiokojakharjkardam.data.database.entity.Bill;
import com.example.chiokojakharjkardam.data.repository.BillRepository;

import java.util.List;
import java.util.stream.Collectors;

public class BillsViewModel extends AndroidViewModel {

    private final BillRepository repository;
    private final LiveData<List<Bill>> allBills;
    private final MutableLiveData<Boolean> showPaid = new MutableLiveData<>(false);
    private final MediatorLiveData<List<Bill>> filteredBills = new MediatorLiveData<>();

    public BillsViewModel(@NonNull Application application) {
        super(application);
        repository = new BillRepository(application);
        allBills = repository.getAllBills();

        filteredBills.addSource(allBills, bills -> {
            applyFilter(bills, showPaid.getValue());
        });

        filteredBills.addSource(showPaid, paid -> {
            applyFilter(allBills.getValue(), paid);
        });
    }

    private void applyFilter(List<Bill> bills, Boolean showPaid) {
        if (bills == null) {
            filteredBills.setValue(null);
            return;
        }

        boolean paid = showPaid != null && showPaid;
        filteredBills.setValue(
                bills.stream()
                        .filter(b -> b.isPaid() == paid)
                        .collect(Collectors.toList())
        );
    }

    public void setShowPaid(boolean showPaid) {
        this.showPaid.setValue(showPaid);
    }

    public LiveData<List<Bill>> getFilteredBills() {
        return filteredBills;
    }

    public void markAsPaid(Bill bill) {
        bill.setPaid(true);
        repository.update(bill);
    }

    public void deleteBill(Bill bill) {
        repository.delete(bill);
    }
}

