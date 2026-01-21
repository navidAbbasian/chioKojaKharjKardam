package com.example.chiokojakharjkardam.data.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.example.chiokojakharjkardam.data.database.AppDatabase;
import com.example.chiokojakharjkardam.data.database.dao.BankCardDao;
import com.example.chiokojakharjkardam.data.database.dao.TagDao;
import com.example.chiokojakharjkardam.data.database.dao.TransactionDao;
import com.example.chiokojakharjkardam.data.database.entity.Tag;
import com.example.chiokojakharjkardam.data.database.entity.Transaction;

import java.util.List;

public class TagRepository {

    private final TagDao tagDao;
    private final TransactionDao transactionDao;
    private final BankCardDao bankCardDao;

    public TagRepository(Application application) {
        AppDatabase db = AppDatabase.getDatabase(application);
        tagDao = db.tagDao();
        transactionDao = db.transactionDao();
        bankCardDao = db.bankCardDao();
    }

    public void insert(Tag tag) {
        AppDatabase.databaseWriteExecutor.execute(() -> tagDao.insert(tag));
    }

    public void insertAndGetId(Tag tag, OnTagInsertedListener listener) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            long id = tagDao.insert(tag);
            if (listener != null) {
                listener.onTagInserted(id);
            }
        });
    }

    public void update(Tag tag) {
        AppDatabase.databaseWriteExecutor.execute(() -> tagDao.update(tag));
    }

    public void delete(Tag tag) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            // دریافت تراکنش‌های مرتبط با این تگ
            List<Transaction> transactions = transactionDao.getTransactionsByTagSync(tag.getId());

            // برگرداندن موجودی و حذف تراکنش‌ها
            for (Transaction transaction : transactions) {
                long balanceChange = transaction.getType() == Transaction.TYPE_INCOME
                        ? -transaction.getAmount()
                        : transaction.getAmount();
                bankCardDao.updateBalance(transaction.getCardId(), balanceChange);
                transactionDao.delete(transaction);
            }

            // حذف تگ
            tagDao.delete(tag);
        });
    }

    /**
     * دریافت تعداد تراکنش‌های مرتبط با یک تگ
     */
    public void getTransactionCount(long tagId, OnTransactionCountListener listener) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            int count = transactionDao.getTransactionCountByTag(tagId);
            if (listener != null) {
                listener.onCountReceived(count);
            }
        });
    }

    public LiveData<List<Tag>> getAllTags() {
        return tagDao.getAllTags();
    }

    public LiveData<Tag> getTagById(long id) {
        return tagDao.getTagById(id);
    }

    public LiveData<List<Tag>> getTagsByTransactionId(long transactionId) {
        return tagDao.getTagsByTransactionId(transactionId);
    }

    public interface OnTagInsertedListener {
        void onTagInserted(long tagId);
    }

    public interface OnTransactionCountListener {
        void onCountReceived(int count);
    }
}

