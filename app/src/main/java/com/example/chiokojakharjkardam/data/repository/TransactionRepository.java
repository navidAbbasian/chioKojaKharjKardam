package com.example.chiokojakharjkardam.data.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.example.chiokojakharjkardam.data.database.AppDatabase;
import com.example.chiokojakharjkardam.data.database.dao.BankCardDao;
import com.example.chiokojakharjkardam.data.database.dao.TransactionDao;
import com.example.chiokojakharjkardam.data.database.dao.TransactionTagDao;
import com.example.chiokojakharjkardam.data.database.entity.BankCard;
import com.example.chiokojakharjkardam.data.database.entity.CategoryReport;
import com.example.chiokojakharjkardam.data.database.entity.CombinedReport;
import com.example.chiokojakharjkardam.data.database.entity.TagReport;
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
    public void insertWithBalanceUpdate(Transaction transaction, List<Long> tagIds,
                                         OnTransactionInsertedListener listener,
                                         OnTransactionErrorListener errorListener) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            // بررسی موجودی کارت برای تراکنش‌های خرج
            if (transaction.getType() == Transaction.TYPE_EXPENSE) {
                BankCard card = bankCardDao.getCardByIdSync(transaction.getCardId());
                if (card != null && card.getBalance() < transaction.getAmount()) {
                    if (errorListener != null) {
                        errorListener.onError("موجودی کارت کافی نیست. موجودی فعلی: " + card.getBalance());
                    }
                    return;
                }
            }

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

    public void update(Transaction transaction, List<Long> tagIds,
                       OnTransactionUpdatedListener listener,
                       OnTransactionErrorListener errorListener) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            // دریافت تراکنش قبلی برای محاسبه تفاوت موجودی
            Transaction oldTransaction = transactionDao.getTransactionByIdSync(transaction.getId());

            if (oldTransaction != null) {
                // محاسبه تغییر موجودی
                long oldBalanceChange = oldTransaction.getType() == Transaction.TYPE_INCOME
                        ? -oldTransaction.getAmount()
                        : oldTransaction.getAmount();

                // بررسی موجودی برای تراکنش جدید
                if (transaction.getType() == Transaction.TYPE_EXPENSE) {
                    BankCard card = bankCardDao.getCardByIdSync(transaction.getCardId());
                    if (card != null) {
                        // موجودی بعد از برگرداندن تراکنش قبلی
                        long availableBalance = card.getBalance();
                        if (oldTransaction.getCardId() == transaction.getCardId()) {
                            availableBalance = card.getBalance() + oldBalanceChange;
                        }
                        if (availableBalance < transaction.getAmount()) {
                            if (errorListener != null) {
                                errorListener.onError("موجودی کارت کافی نیست. موجودی فعلی: " + availableBalance);
                            }
                            return;
                        }
                    }
                }

                // برگرداندن موجودی قبلی
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

            if (listener != null) {
                listener.onTransactionUpdated();
            }
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

    public List<Long> getTransactionIdsByTag(long tagId) {
        return transactionTagDao.getTransactionIdsByTag(tagId);
    }

    public LiveData<List<TransactionTag>> getAllTransactionTags() {
        return transactionTagDao.getAllTransactionTags();
    }

    // ==================== گزارش‌گیری ====================

    /**
     * گزارش هزینه‌ها بر اساس دسته‌بندی
     */
    public LiveData<List<CategoryReport>> getExpenseReportByCategory(long startDate, long endDate) {
        return transactionDao.getExpenseReportByCategory(startDate, endDate);
    }

    /**
     * گزارش درآمدها بر اساس دسته‌بندی
     */
    public LiveData<List<CategoryReport>> getIncomeReportByCategory(long startDate, long endDate) {
        return transactionDao.getIncomeReportByCategory(startDate, endDate);
    }

    /**
     * گزارش هزینه‌ها بر اساس تگ
     */
    public LiveData<List<TagReport>> getExpenseReportByTag(long startDate, long endDate) {
        return transactionDao.getExpenseReportByTag(startDate, endDate);
    }

    /**
     * گزارش درآمدها بر اساس تگ
     */
    public LiveData<List<TagReport>> getIncomeReportByTag(long startDate, long endDate) {
        return transactionDao.getIncomeReportByTag(startDate, endDate);
    }

    /**
     * گزارش ترکیبی هزینه‌ها بر اساس دسته‌بندی و تگ
     */
    public LiveData<List<CombinedReport>> getExpenseReportByCategoryAndTag(long startDate, long endDate) {
        return transactionDao.getExpenseReportByCategoryAndTag(startDate, endDate);
    }

    /**
     * گزارش ترکیبی درآمدها بر اساس دسته‌بندی و تگ
     */
    public LiveData<List<CombinedReport>> getIncomeReportByCategoryAndTag(long startDate, long endDate) {
        return transactionDao.getIncomeReportByCategoryAndTag(startDate, endDate);
    }

    /**
     * گزارش هزینه برای یک دسته‌بندی خاص
     */
    public LiveData<Long> getExpenseByCategoryInRange(long categoryId, long startDate, long endDate) {
        return transactionDao.getExpenseByCategoryInRange(categoryId, startDate, endDate);
    }

    /**
     * گزارش هزینه برای یک تگ خاص
     */
    public LiveData<Long> getExpenseByTagInRange(long tagId, long startDate, long endDate) {
        return transactionDao.getExpenseByTagInRange(tagId, startDate, endDate);
    }

    public interface OnTransactionInsertedListener {
        void onTransactionInserted(long transactionId);
    }

    public interface OnTransactionUpdatedListener {
        void onTransactionUpdated();
    }

    public interface OnTransactionErrorListener {
        void onError(String errorMessage);
    }
}

