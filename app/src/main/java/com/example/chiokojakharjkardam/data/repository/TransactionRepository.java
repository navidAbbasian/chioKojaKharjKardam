package com.example.chiokojakharjkardam.data.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.example.chiokojakharjkardam.data.database.AppDatabase;
import com.example.chiokojakharjkardam.data.database.dao.BankCardDao;
import com.example.chiokojakharjkardam.data.database.dao.TransactionDao;
import com.example.chiokojakharjkardam.data.database.dao.TransactionTagDao;
import com.example.chiokojakharjkardam.data.database.entity.Transaction;
import com.example.chiokojakharjkardam.data.database.entity.TransactionTag;

import java.util.ArrayList;
import java.util.List;

public class TransactionRepository {

    private final TransactionDao transactionDao;
    private final TransactionTagDao transactionTagDao;
    private final BankCardDao bankCardDao;

    public TransactionRepository(Application application) {
        AppDatabase db = AppDatabase.getDatabase(application);
        transactionDao = db.transactionDao();
        transactionTagDao = db.transactionTagDao();
        bankCardDao = db.bankCardDao();
    }

    /**
     * ثبت تراکنش جدید همراه با به‌روزرسانی موجودی کارت
     */
    public void insertWithBalanceUpdate(Transaction transaction, List<Long> tagIds, OnTransactionInsertedListener listener) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            // ثبت تراکنش
            long transactionId = transactionDao.insert(transaction);

            // ثبت تگ‌ها
            if (tagIds != null && !tagIds.isEmpty()) {
                List<TransactionTag> transactionTags = new ArrayList<>();
                for (Long tagId : tagIds) {
                    transactionTags.add(new TransactionTag(transactionId, tagId));
                }
                transactionTagDao.insertAll(transactionTags);
            }

            // به‌روزرسانی موجودی کارت
            long balanceChange = transaction.getType() == Transaction.TYPE_INCOME
                    ? transaction.getAmount()
                    : -transaction.getAmount();
            bankCardDao.updateBalance(transaction.getCardId(), balanceChange);

            if (listener != null) {
                listener.onTransactionInserted(transactionId);
            }
        });
    }

    public void update(Transaction transaction, List<Long> tagIds) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            // دریافت تراکنش قبلی برای محاسبه تفاوت موجودی
            Transaction oldTransaction = transactionDao.getTransactionByIdSync(transaction.getId());

            if (oldTransaction != null) {
                // برگرداندن موجودی قبلی
                long oldBalanceChange = oldTransaction.getType() == Transaction.TYPE_INCOME
                        ? -oldTransaction.getAmount()
                        : oldTransaction.getAmount();
                bankCardDao.updateBalance(oldTransaction.getCardId(), oldBalanceChange);
            }

            // به‌روزرسانی تراکنش
            transactionDao.update(transaction);

            // به‌روزرسانی تگ‌ها
            transactionTagDao.deleteByTransaction(transaction.getId());
            if (tagIds != null && !tagIds.isEmpty()) {
                List<TransactionTag> transactionTags = new ArrayList<>();
                for (Long tagId : tagIds) {
                    transactionTags.add(new TransactionTag(transaction.getId(), tagId));
                }
                transactionTagDao.insertAll(transactionTags);
            }

            // اعمال موجودی جدید
            long newBalanceChange = transaction.getType() == Transaction.TYPE_INCOME
                    ? transaction.getAmount()
                    : -transaction.getAmount();
            bankCardDao.updateBalance(transaction.getCardId(), newBalanceChange);
        });
    }

    public void delete(Transaction transaction) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            // برگرداندن موجودی
            long balanceChange = transaction.getType() == Transaction.TYPE_INCOME
                    ? -transaction.getAmount()
                    : transaction.getAmount();
            bankCardDao.updateBalance(transaction.getCardId(), balanceChange);

            // حذف تراکنش (تگ‌ها خودکار حذف می‌شوند)
            transactionDao.delete(transaction);
        });
    }

    public LiveData<List<Transaction>> getAllTransactions() {
        return transactionDao.getAllTransactions();
    }

    public LiveData<List<Transaction>> getTransactionsByCard(long cardId) {
        return transactionDao.getTransactionsByCard(cardId);
    }

    public LiveData<List<Transaction>> getTransactionsByCategory(long categoryId) {
        return transactionDao.getTransactionsByCategory(categoryId);
    }

    public LiveData<List<Transaction>> getTransactionsByDateRange(long startDate, long endDate) {
        return transactionDao.getTransactionsByDateRange(startDate, endDate);
    }

    public LiveData<Transaction> getTransactionById(long id) {
        return transactionDao.getTransactionById(id);
    }

    public LiveData<List<Transaction>> getRecentTransactions(int limit) {
        return transactionDao.getRecentTransactions(limit);
    }

    public LiveData<Long> getTotalExpense() {
        return transactionDao.getTotalExpense();
    }

    public LiveData<Long> getTotalIncome() {
        return transactionDao.getTotalIncome();
    }

    public LiveData<Long> getTotalExpenseByDateRange(long startDate, long endDate) {
        return transactionDao.getTotalExpenseByDateRange(startDate, endDate);
    }

    public LiveData<Long> getTotalIncomeByDateRange(long startDate, long endDate) {
        return transactionDao.getTotalIncomeByDateRange(startDate, endDate);
    }

    public LiveData<Long> getTotalExpenseInRange(long startDate, long endDate) {
        return transactionDao.getTotalExpenseByDateRange(startDate, endDate);
    }

    public LiveData<Long> getTotalIncomeInRange(long startDate, long endDate) {
        return transactionDao.getTotalIncomeByDateRange(startDate, endDate);
    }

    public List<Long> getTagIdsByTransaction(long transactionId) {
        return transactionTagDao.getTagIdsByTransaction(transactionId);
    }

    public interface OnTransactionInsertedListener {
        void onTransactionInserted(long transactionId);
    }
}

