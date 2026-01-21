package com.example.chiokojakharjkardam.ui.bills;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.chiokojakharjkardam.data.database.entity.Bill;
import com.example.chiokojakharjkardam.data.repository.BillRepository;
import com.example.chiokojakharjkardam.utils.BillReminderScheduler;

import java.util.List;

public class AddBillViewModel extends AndroidViewModel {

    private final Application application;
    private final BillRepository repository;

    public AddBillViewModel(@NonNull Application application) {
        super(application);
        this.application = application;
        repository = new BillRepository(application);
    }

    public LiveData<Bill> getBillById(long id) {
        return repository.getBillById(id);
    }

    public void insertBill(Bill bill) {
        repository.insert(bill, insertedBill -> {
            // زمان‌بندی یادآوری
            BillReminderScheduler.scheduleReminder(application, insertedBill);
        });
    }

    public void insertBills(List<Bill> bills) {
        repository.insertAll(bills, insertedBills -> {
            // زمان‌بندی یادآوری برای همه قبض‌ها
            for (Bill bill : insertedBills) {
                BillReminderScheduler.scheduleReminder(application, bill);
            }
        });
    }

    public void updateBill(Bill bill) {
        repository.update(bill);
        // زمان‌بندی مجدد یا��آوری
        BillReminderScheduler.scheduleReminder(application, bill);
    }
}

