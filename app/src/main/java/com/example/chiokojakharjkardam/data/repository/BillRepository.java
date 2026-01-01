package com.example.chiokojakharjkardam.data.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.example.chiokojakharjkardam.data.database.AppDatabase;
import com.example.chiokojakharjkardam.data.database.dao.BillDao;
import com.example.chiokojakharjkardam.data.database.entity.Bill;

import java.util.List;

public class BillRepository {

    private final BillDao billDao;

    public BillRepository(Application application) {
        AppDatabase db = AppDatabase.getDatabase(application);
        billDao = db.billDao();
    }

    public void insert(Bill bill) {
        AppDatabase.databaseWriteExecutor.execute(() -> billDao.insert(bill));
    }

    public void insert(Bill bill, OnBillInsertedWithBillListener listener) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            long id = billDao.insert(bill);
            bill.setId(id);
            if (listener != null) {
                listener.onBillInserted(bill);
            }
        });
    }

    public void insertAndGetId(Bill bill, OnBillInsertedListener listener) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            long id = billDao.insert(bill);
            if (listener != null) {
                listener.onBillInserted(id);
            }
        });
    }

    public void update(Bill bill) {
        AppDatabase.databaseWriteExecutor.execute(() -> billDao.update(bill));
    }

    public void delete(Bill bill) {
        AppDatabase.databaseWriteExecutor.execute(() -> billDao.delete(bill));
    }

    public void markAsPaid(long billId, boolean isPaid) {
        AppDatabase.databaseWriteExecutor.execute(() -> billDao.updatePaidStatus(billId, isPaid));
    }

    public LiveData<List<Bill>> getAllBills() {
        return billDao.getAllBills();
    }

    public LiveData<List<Bill>> getUnpaidBills() {
        return billDao.getUnpaidBills();
    }

    public LiveData<List<Bill>> getPaidBills() {
        return billDao.getPaidBills();
    }

    public LiveData<Bill> getBillById(long id) {
        return billDao.getBillById(id);
    }

    public LiveData<Integer> getUnpaidBillCount() {
        return billDao.getUnpaidBillCount();
    }

    public void getBillsDueForNotification(long startDate, long endDate, OnBillsLoadedListener listener) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<Bill> bills = billDao.getBillsDueBetween(startDate, endDate);
            if (listener != null) {
                listener.onBillsLoaded(bills);
            }
        });
    }

    public interface OnBillInsertedListener {
        void onBillInserted(long billId);
    }

    public interface OnBillInsertedWithBillListener {
        void onBillInserted(Bill bill);
    }

    public interface OnBillsLoadedListener {
        void onBillsLoaded(List<Bill> bills);
    }
}

