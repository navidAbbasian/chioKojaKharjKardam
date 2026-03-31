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
            // بررسی موجودی کارت برای تراکنش‌های خرج و کارت به کارت
            if (transaction.getType() == Transaction.TYPE_EXPENSE
                    || transaction.getType() == Transaction.TYPE_TRANSFER) {
                BankCard card = bankCardDao.getCardByIdSync(transaction.getCardId());
                if (card != null && card.getBalance() < transaction.getAmount()) {
                    if (errorListener != null) {
                        errorListener.onError("موجودی کارت کافی نیست. موجودی فعلی: " + card.getBalance());
                    }
                    return;
                }
                // بررسی کارت مقصد برای کارت به کارت
                if (transaction.getType() == Transaction.TYPE_TRANSFER
                        && (transaction.getToCardId() == null || transaction.getToCardId() == transaction.getCardId())) {
                    if (errorListener != null) {
                        errorListener.onError("کارت مقصد باید متفاوت از کارت مبدا باشد");
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

            // به‌روزرسانی موجودی
            if (transaction.getType() == Transaction.TYPE_TRANSFER) {
                // کارت به کارت: از مبدا کم کن، به مقصد اضافه کن
                bankCardDao.updateBalance(transaction.getCardId(), -transaction.getAmount());
                bankCardDao.updateBalance(transaction.getToCardId(), transaction.getAmount());
            } else {
                long balanceChange = transaction.getType() == Transaction.TYPE_INCOME
                        ? transaction.getAmount()
                        : -transaction.getAmount();
                bankCardDao.updateBalance(transaction.getCardId(), balanceChange);
            }

            if (listener != null) {
                listener.onTransactionInserted(transactionId);
            }
        });
    }

    public void update(Transaction transaction, List<Long> tagIds,
                       OnTransactionUpdatedListener listener,
                       OnTransactionErrorListener errorListener) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            Transaction oldTransaction = transactionDao.getTransactionByIdSync(transaction.getId());

            if (oldTransaction != null) {
                // برگرداندن موجودی قبلی
                if (oldTransaction.getType() == Transaction.TYPE_TRANSFER) {
                    bankCardDao.updateBalance(oldTransaction.getCardId(), oldTransaction.getAmount());
                    if (oldTransaction.getToCardId() != null) {
                        bankCardDao.updateBalance(oldTransaction.getToCardId(), -oldTransaction.getAmount());
                    }
                } else {
                    long oldBalanceChange = oldTransaction.getType() == Transaction.TYPE_INCOME
                            ? -oldTransaction.getAmount()
                            : oldTransaction.getAmount();
                    bankCardDao.updateBalance(oldTransaction.getCardId(), oldBalanceChange);
                }

                // بررسی موجودی برای تراکنش جدید
                if (transaction.getType() == Transaction.TYPE_EXPENSE
                        || transaction.getType() == Transaction.TYPE_TRANSFER) {
                    BankCard card = bankCardDao.getCardByIdSync(transaction.getCardId());
                    if (card != null) {
                        long availableBalance = card.getBalance();
                        if (availableBalance < transaction.getAmount()) {
                            // برگرداندن تغییر قبلی که دادیم
                            if (oldTransaction.getType() == Transaction.TYPE_TRANSFER) {
                                bankCardDao.updateBalance(oldTransaction.getCardId(), -oldTransaction.getAmount());
                                if (oldTransaction.getToCardId() != null)
                                    bankCardDao.updateBalance(oldTransaction.getToCardId(), oldTransaction.getAmount());
                            } else {
                                long revert = oldTransaction.getType() == Transaction.TYPE_INCOME
                                        ? oldTransaction.getAmount()
                                        : -oldTransaction.getAmount();
                                bankCardDao.updateBalance(oldTransaction.getCardId(), revert);
                            }
                            if (errorListener != null)
                                errorListener.onError("موجودی کارت کافی نیست. موجودی فعلی: " + availableBalance);
                            return;
                        }
                    }
                }
            }

            transactionDao.update(transaction);

            transactionTagDao.deleteByTransaction(transaction.getId());
            if (tagIds != null && !tagIds.isEmpty()) {
                List<TransactionTag> transactionTags = new ArrayList<>();
                for (Long tagId : tagIds) {
                    transactionTags.add(new TransactionTag(transaction.getId(), tagId));
                }
                transactionTagDao.insertAll(transactionTags);
            }

            // اعمال موجودی جدید
            if (transaction.getType() == Transaction.TYPE_TRANSFER) {
                bankCardDao.updateBalance(transaction.getCardId(), -transaction.getAmount());
                if (transaction.getToCardId() != null)
                    bankCardDao.updateBalance(transaction.getToCardId(), transaction.getAmount());
            } else {
                long newBalanceChange = transaction.getType() == Transaction.TYPE_INCOME
                        ? transaction.getAmount()
                        : -transaction.getAmount();
                bankCardDao.updateBalance(transaction.getCardId(), newBalanceChange);
            }

            if (listener != null) listener.onTransactionUpdated();
        });
    }

    public void delete(Transaction transaction) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            // برگرداندن موجودی
            if (transaction.getType() == Transaction.TYPE_TRANSFER) {
                bankCardDao.updateBalance(transaction.getCardId(), transaction.getAmount());
                if (transaction.getToCardId() != null)
                    bankCardDao.updateBalance(transaction.getToCardId(), -transaction.getAmount());
            } else {
                long balanceChange = transaction.getType() == Transaction.TYPE_INCOME
                        ? -transaction.getAmount()
                        : transaction.getAmount();
                bankCardDao.updateBalance(transaction.getCardId(), balanceChange);
            }
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

    /**
     * دریافت تراکنش‌های هزینه در بازه زمانی
     */
    public LiveData<List<Transaction>> getExpensesByDateRange(long startDate, long endDate) {
        return transactionDao.getExpensesByDateRange(startDate, endDate);
    }

    /**
     * دریافت تراکنش‌های درآمد در بازه زمانی
     */
    public LiveData<List<Transaction>> getIncomesByDateRange(long startDate, long endDate) {
        return transactionDao.getIncomesByDateRange(startDate, endDate);
    }

    /**
     * دریافت تراکنش‌های هزینه یک دسته‌بندی در بازه زمانی
     */
    public LiveData<List<Transaction>> getExpensesByCategoryAndDateRange(long categoryId, long startDate, long endDate) {
        return transactionDao.getExpensesByCategoryAndDateRange(categoryId, startDate, endDate);
    }

    /**
     * دریافت تراکنش‌های درآمد یک دسته‌بندی در بازه زمانی
     */
    public LiveData<List<Transaction>> getIncomesByCategoryAndDateRange(long categoryId, long startDate, long endDate) {
        return transactionDao.getIncomesByCategoryAndDateRange(categoryId, startDate, endDate);
    }

    /**
     * دریافت تراکنش‌های هزینه یک تگ در بازه زمانی
     */
    public LiveData<List<Transaction>> getExpensesByTagAndDateRange(long tagId, long startDate, long endDate) {
        return transactionDao.getExpensesByTagAndDateRange(tagId, startDate, endDate);
    }

    /**
     * دریافت تراکنش‌های درآمد یک تگ در بازه زمانی
     */
    public LiveData<List<Transaction>> getIncomesByTagAndDateRange(long tagId, long startDate, long endDate) {
        return transactionDao.getIncomesByTagAndDateRange(tagId, startDate, endDate);
    }

    /**
     * دریافت تراکنش‌های هزینه یک دسته‌بندی و تگ در بازه زمانی
     */
    public LiveData<List<Transaction>> getExpensesByCategoryAndTagAndDateRange(long categoryId, long tagId, long startDate, long endDate) {
        return transactionDao.getExpensesByCategoryAndTagAndDateRange(categoryId, tagId, startDate, endDate);
    }

    /**
     * دریافت تراکنش‌های درآمد یک دسته‌بندی و تگ در بازه زمانی
     */
    public LiveData<List<Transaction>> getIncomesByCategoryAndTagAndDateRange(long categoryId, long tagId, long startDate, long endDate) {
        return transactionDao.getIncomesByCategoryAndTagAndDateRange(categoryId, tagId, startDate, endDate);
    }

    /**
     * دریافت همه تراکنش‌ها (درآمد+خرج) یک دسته‌بندی در بازه زمانی
     */
    public LiveData<List<Transaction>> getAllTransactionsByCategoryAndDateRange(long categoryId, long startDate, long endDate) {
        return transactionDao.getAllTransactionsByCategoryAndDateRange(categoryId, startDate, endDate);
    }

    /**
     * دریافت همه تراکنش‌ها (درآمد+خرج) یک تگ در بازه زمانی
     */
    public LiveData<List<Transaction>> getAllTransactionsByTagAndDateRange(long tagId, long startDate, long endDate) {
        return transactionDao.getAllTransactionsByTagAndDateRange(tagId, startDate, endDate);
    }

    /**
     * دریافت همه تراکنش‌ها (درآمد+خرج) یک دسته‌بندی و تگ در بازه زمانی
     */
    public LiveData<List<Transaction>> getAllTransactionsByCategoryAndTagAndDateRange(long categoryId, long tagId, long startDate, long endDate) {
        return transactionDao.getAllTransactionsByCategoryAndTagAndDateRange(categoryId, tagId, startDate, endDate);
    }

    // ==================== کارت به کارت ====================

    public LiveData<List<Transaction>> getTransfersByDateRange(long startDate, long endDate) {
        return transactionDao.getTransfersByDateRange(startDate, endDate);
    }

    public LiveData<List<Transaction>> getTransfersByCategoryAndDateRange(long categoryId, long startDate, long endDate) {
        return transactionDao.getTransfersByCategoryAndDateRange(categoryId, startDate, endDate);
    }

    public LiveData<List<Transaction>> getTransfersByTagAndDateRange(long tagId, long startDate, long endDate) {
        return transactionDao.getTransfersByTagAndDateRange(tagId, startDate, endDate);
    }

    public LiveData<List<Transaction>> getTransfersByCategoryAndTagAndDateRange(long categoryId, long tagId, long startDate, long endDate) {
        return transactionDao.getTransfersByCategoryAndTagAndDateRange(categoryId, tagId, startDate, endDate);
    }

    public LiveData<List<CategoryReport>> getTransferReportByCategory(long startDate, long endDate) {
        return transactionDao.getTransferReportByCategory(startDate, endDate);
    }

    public LiveData<List<TagReport>> getTransferReportByTag(long startDate, long endDate) {
        return transactionDao.getTransferReportByTag(startDate, endDate);
    }

    public LiveData<List<CombinedReport>> getTransferReportByCategoryAndTag(long startDate, long endDate) {
        return transactionDao.getTransferReportByCategoryAndTag(startDate, endDate);
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

