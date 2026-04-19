package com.example.chiokojakharjkardam.data.repository;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.lifecycle.LiveData;

import com.example.chiokojakharjkardam.data.database.AppDatabase;
import com.example.chiokojakharjkardam.data.database.dao.BillDao;
import com.example.chiokojakharjkardam.data.database.entity.Bill;
import com.example.chiokojakharjkardam.data.remote.RemoteDataSource;
import com.example.chiokojakharjkardam.data.remote.model.RemoteBill;
import com.example.chiokojakharjkardam.utils.NetworkMonitor;
import com.example.chiokojakharjkardam.utils.SessionManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BillRepository {

    private final BillDao billDao;
    private final Application application;
    private final RemoteDataSource remote;
    private final SessionManager session;

    public BillRepository(Application application) {
        this.application = application;
        AppDatabase db = AppDatabase.getDatabase(application);
        billDao = db.billDao();
        remote  = RemoteDataSource.getInstance();
        session = SessionManager.getInstance();
    }

    // ── Writes ────────────────────────────────────────────────────

    public void insert(Bill bill) { insertAndGetId(bill, null); }

    public void insertAndGetId(Bill bill, OnBillInsertedListener listener) {
        if (!NetworkMonitor.getInstance().isOnline()) { showOfflineError(); return; }
        RemoteBill rb = new RemoteBill(bill, session.getFamilyId(), session.getUserId());
        rb.id = null;
        remote.insertBill(rb, new RemoteDataSource.Callback<RemoteBill>() {
            @Override public void onSuccess(RemoteBill created) {
                bill.setId(created.id);
                AppDatabase.databaseWriteExecutor.execute(() -> {
                    billDao.upsert(bill);
                    if (listener != null) listener.onBillInserted(created.id);
                });
            }
            @Override public void onError(String msg) { showError(msg); }
        });
    }

    public void insertAll(List<Bill> bills, OnBillsInsertedListener listener) {
        if (bills == null || bills.isEmpty()) {
            if (listener != null) listener.onBillsInserted(bills);
            return;
        }
        if (!NetworkMonitor.getInstance().isOnline()) { showOfflineError(); return; }
        final java.util.List<Bill> inserted = new java.util.ArrayList<>();
        final int[] remaining = {bills.size()};
        final Object lock = new Object();
        for (Bill bill : bills) {
            RemoteBill rb = new RemoteBill(bill, session.getFamilyId(), session.getUserId());
            rb.id = null;
            remote.insertBill(rb, new RemoteDataSource.Callback<RemoteBill>() {
                @Override public void onSuccess(RemoteBill created) {
                    bill.setId(created.id);
                    AppDatabase.databaseWriteExecutor.execute(() -> billDao.upsert(bill));
                    synchronized (lock) {
                        inserted.add(bill);
                        remaining[0]--;
                        if (remaining[0] == 0 && listener != null) listener.onBillsInserted(inserted);
                    }
                }
                @Override public void onError(String msg) {
                    showError(msg);
                    synchronized (lock) {
                        remaining[0]--;
                        if (remaining[0] == 0 && listener != null) listener.onBillsInserted(inserted);
                    }
                }
            });
        }
    }

    public void insert(Bill bill, OnBillInsertedWithBillListener listener) {
        if (!NetworkMonitor.getInstance().isOnline()) { showOfflineError(); return; }
        RemoteBill rb = new RemoteBill(bill, session.getFamilyId(), session.getUserId());
        rb.id = null;
        remote.insertBill(rb, new RemoteDataSource.Callback<RemoteBill>() {
            @Override public void onSuccess(RemoteBill created) {
                bill.setId(created.id);
                AppDatabase.databaseWriteExecutor.execute(() -> {
                    billDao.upsert(bill);
                    if (listener != null) listener.onBillInserted(bill);
                });
            }
            @Override public void onError(String msg) { showError(msg); }
        });
    }

    public void update(Bill bill) {
        if (!NetworkMonitor.getInstance().isOnline()) { showOfflineError(); return; }
        Map<String, Object> upd = new HashMap<>();
        upd.put("title", bill.getTitle());
        upd.put("amount", bill.getAmount());
        upd.put("due_date", bill.getDueDate());
        upd.put("is_paid", bill.isPaid());
        upd.put("notify_before", bill.getNotifyBefore());
        upd.put("is_recurring", bill.isRecurring());
        upd.put("recurring_type", bill.getRecurringType());
        upd.put("card_id", bill.getCardId());
        remote.updateBill(bill.getId(), upd, new RemoteDataSource.Callback<Void>() {
            @Override public void onSuccess(Void v) {
                AppDatabase.databaseWriteExecutor.execute(() -> billDao.update(bill));
            }
            @Override public void onError(String msg) { showError(msg); }
        });
    }

    public void delete(Bill bill) {
        if (!NetworkMonitor.getInstance().isOnline()) { showOfflineError(); return; }
        remote.deleteBill(bill.getId(), new RemoteDataSource.Callback<Void>() {
            @Override public void onSuccess(Void v) {
                AppDatabase.databaseWriteExecutor.execute(() -> billDao.delete(bill));
            }
            @Override public void onError(String msg) { showError(msg); }
        });
    }

    public void markAsPaid(long billId, boolean isPaid) {
        if (!NetworkMonitor.getInstance().isOnline()) { showOfflineError(); return; }
        Map<String, Object> upd = new HashMap<>();
        upd.put("is_paid", isPaid);
        remote.updateBill(billId, upd, new RemoteDataSource.Callback<Void>() {
            @Override public void onSuccess(Void v) {
                AppDatabase.databaseWriteExecutor.execute(() -> billDao.updatePaidStatus(billId, isPaid));
            }
            @Override public void onError(String msg) { showError(msg); }
        });
    }

    // ── Reads ─────────────────────────────────────────────────────

    public LiveData<List<Bill>> getAllBills()     { return billDao.getAllBills(); }
    public LiveData<List<Bill>> getUnpaidBills()  { return billDao.getUnpaidBills(); }
    public LiveData<List<Bill>> getPaidBills()    { return billDao.getPaidBills(); }
    public LiveData<Bill> getBillById(long id)    { return billDao.getBillById(id); }
    public LiveData<Integer> getUnpaidBillCount() { return billDao.getUnpaidBillCount(); }

    public void getBillsDueForNotification(long startDate, long endDate, OnBillsLoadedListener listener) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<Bill> bills = billDao.getBillsDueBetween(startDate, endDate);
            if (listener != null) listener.onBillsLoaded(bills);
        });
    }

    // ── Helpers ───────────────────────────────────────────────────

    private void showOfflineError() {
        new Handler(Looper.getMainLooper()).post(() ->
                Toast.makeText(application, "اتصال اینترنت وجود ندارد", Toast.LENGTH_SHORT).show());
    }
    private void showError(String msg) {
        new Handler(Looper.getMainLooper()).post(() ->
                Toast.makeText(application, msg, Toast.LENGTH_SHORT).show());
    }

    // ── Interfaces ────────────────────────────────────────────────

    public interface OnBillInsertedListener        { void onBillInserted(long billId); }
    public interface OnBillInsertedWithBillListener { void onBillInserted(Bill bill); }
    public interface OnBillsInsertedListener       { void onBillsInserted(List<Bill> bills); }
    public interface OnBillsLoadedListener         { void onBillsLoaded(List<Bill> bills); }
}
