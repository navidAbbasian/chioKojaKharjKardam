package com.example.chiokojakharjkardam.data.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.example.chiokojakharjkardam.data.database.AppDatabase;
import com.example.chiokojakharjkardam.data.database.dao.BankCardDao;
import com.example.chiokojakharjkardam.data.database.dao.TransferDao;
import com.example.chiokojakharjkardam.data.database.entity.Transfer;

import java.util.List;

public class TransferRepository {

    private final TransferDao transferDao;
    private final BankCardDao bankCardDao;

    public TransferRepository(Application application) {
        AppDatabase db = AppDatabase.getDatabase(application);
        transferDao = db.transferDao();
        bankCardDao = db.bankCardDao();
    }

    /**
     * ثبت انتقال بین کارت‌ها با به‌روزرسانی موجودی هر دو کارت
     */
    public void insertWithBalanceUpdate(Transfer transfer, OnTransferInsertedListener listener) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            // ثبت انتقال
            long transferId = transferDao.insert(transfer);

            // کاهش موجودی کارت مبدأ
            bankCardDao.updateBalance(transfer.getFromCardId(), -transfer.getAmount());

            // افزایش موجودی کارت مقصد
            bankCardDao.updateBalance(transfer.getToCardId(), transfer.getAmount());

            if (listener != null) {
                listener.onTransferInserted(transferId);
            }
        });
    }

    public void delete(Transfer transfer) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            // برگرداندن موجودی‌ها
            bankCardDao.updateBalance(transfer.getFromCardId(), transfer.getAmount());
            bankCardDao.updateBalance(transfer.getToCardId(), -transfer.getAmount());

            transferDao.delete(transfer);
        });
    }

    public LiveData<List<Transfer>> getAllTransfers() {
        return transferDao.getAllTransfers();
    }

    public LiveData<List<Transfer>> getTransfersByCard(long cardId) {
        return transferDao.getTransfersByCard(cardId);
    }

    public LiveData<Transfer> getTransferById(long id) {
        return transferDao.getTransferById(id);
    }

    public LiveData<List<Transfer>> getRecentTransfers(int limit) {
        return transferDao.getRecentTransfers(limit);
    }

    public void doTransfer(Transfer transfer) {
        insertWithBalanceUpdate(transfer, null);
    }

    public interface OnTransferInsertedListener {
        void onTransferInserted(long transferId);
    }
}

