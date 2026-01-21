package com.example.chiokojakharjkardam.data.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.example.chiokojakharjkardam.data.database.AppDatabase;
import com.example.chiokojakharjkardam.data.database.dao.BankCardDao;
import com.example.chiokojakharjkardam.data.database.dao.CategoryDao;
import com.example.chiokojakharjkardam.data.database.dao.TransactionDao;
import com.example.chiokojakharjkardam.data.database.entity.Category;
import com.example.chiokojakharjkardam.data.database.entity.Transaction;

import java.util.List;

public class CategoryRepository {

    private final CategoryDao categoryDao;
    private final TransactionDao transactionDao;
    private final BankCardDao bankCardDao;

    public CategoryRepository(Application application) {
        AppDatabase db = AppDatabase.getDatabase(application);
        categoryDao = db.categoryDao();
        transactionDao = db.transactionDao();
        bankCardDao = db.bankCardDao();
    }

    public void insert(Category category) {
        AppDatabase.databaseWriteExecutor.execute(() -> categoryDao.insert(category));
    }

    public void update(Category category) {
        AppDatabase.databaseWriteExecutor.execute(() -> categoryDao.update(category));
    }

    public void delete(Category category) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            // دریافت تراکنش‌های مرتبط با این دسته‌بندی
            List<Transaction> transactions = transactionDao.getTransactionsByCategorySync(category.getId());

            // برگرداندن موجودی و حذف تراکنش‌ها
            for (Transaction transaction : transactions) {
                long balanceChange = transaction.getType() == Transaction.TYPE_INCOME
                        ? -transaction.getAmount()
                        : transaction.getAmount();
                bankCardDao.updateBalance(transaction.getCardId(), balanceChange);
                transactionDao.delete(transaction);
            }

            // حذف دسته‌بندی
            categoryDao.delete(category);
        });
    }

    /**
     * دریافت تعداد تراکنش‌های مرتبط با یک دسته‌بندی
     */
    public void getTransactionCount(long categoryId, OnTransactionCountListener listener) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            int count = transactionDao.getTransactionCountByCategory(categoryId);
            if (listener != null) {
                listener.onCountReceived(count);
            }
        });
    }

    public LiveData<List<Category>> getAllCategories() {
        return categoryDao.getAllCategories();
    }

    public LiveData<List<Category>> getCategoriesByType(int type) {
        return categoryDao.getCategoriesByType(type);
    }

    public LiveData<Category> getCategoryById(long id) {
        return categoryDao.getCategoryById(id);
    }

    public interface OnTransactionCountListener {
        void onCountReceived(int count);
    }
}

