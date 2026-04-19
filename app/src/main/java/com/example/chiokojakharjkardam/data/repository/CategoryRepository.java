package com.example.chiokojakharjkardam.data.repository;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.lifecycle.LiveData;

import com.example.chiokojakharjkardam.data.database.AppDatabase;
import com.example.chiokojakharjkardam.data.database.dao.BankCardDao;
import com.example.chiokojakharjkardam.data.database.dao.CategoryDao;
import com.example.chiokojakharjkardam.data.database.dao.PendingDeleteDao;
import com.example.chiokojakharjkardam.data.database.dao.TransactionDao;
import com.example.chiokojakharjkardam.data.database.entity.Category;
import com.example.chiokojakharjkardam.data.database.entity.PendingDelete;
import com.example.chiokojakharjkardam.data.database.entity.Transaction;
import com.example.chiokojakharjkardam.data.remote.RemoteDataSource;
import com.example.chiokojakharjkardam.data.remote.model.RemoteCategory;
import com.example.chiokojakharjkardam.utils.NetworkMonitor;
import com.example.chiokojakharjkardam.utils.SessionManager;
import com.example.chiokojakharjkardam.utils.SyncManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CategoryRepository {

    private final CategoryDao categoryDao;
    private final TransactionDao transactionDao;
    private final BankCardDao bankCardDao;
    private final PendingDeleteDao pendingDeleteDao;
    private final Application application;
    private final RemoteDataSource remote;
    private final SessionManager session;

    public CategoryRepository(Application application) {
        this.application = application;
        AppDatabase db = AppDatabase.getDatabase(application);
        categoryDao      = db.categoryDao();
        transactionDao   = db.transactionDao();
        bankCardDao      = db.bankCardDao();
        pendingDeleteDao = db.pendingDeleteDao();
        remote  = RemoteDataSource.getInstance();
        session = SessionManager.getInstance();
    }

    public void insert(Category category) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            // Only family owner can create categories
            if (session.hasFamilyId() && !session.isOwner()) {
                new Handler(Looper.getMainLooper()).post(() ->
                    Toast.makeText(application, "فقط سرپرست خانواده می‌تواند دسته‌بندی بسازد", Toast.LENGTH_SHORT).show());
                return;
            }
            category.setPendingSync(1);
            long localId = categoryDao.insert(category);
            category.setId(localId);

            // Auto-sync trigger
            SyncManager.getInstance().triggerAutoSync();

            if (NetworkMonitor.getInstance().isOnline() && session.hasFamilyId()) {
                RemoteCategory rc = new RemoteCategory(category, session.getFamilyId());
                rc.id = null;
                remote.insertCategory(rc, new RemoteDataSource.Callback<RemoteCategory>() {
                    @Override public void onSuccess(RemoteCategory created) {
                        AppDatabase.databaseWriteExecutor.execute(() ->
                                categoryDao.updateSupabaseId(localId, created.id));
                    }
                    @Override public void onError(String msg) {}
                });
            }
        });
    }

    public void update(Category category) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            if (NetworkMonitor.getInstance().isOnline() && category.getSupabaseId() > 0) {
                category.setPendingSync(0);
            } else {
                category.setPendingSync(2);
            }
            categoryDao.update(category);

            // Auto-sync trigger
            SyncManager.getInstance().triggerAutoSync();

            if (NetworkMonitor.getInstance().isOnline() && category.getSupabaseId() > 0) {
                Map<String, Object> upd = new HashMap<>();
                upd.put("name", category.getName());
                upd.put("icon", category.getIcon());
                upd.put("color", category.getColor());
                upd.put("type", category.getType());
                remote.updateCategory(category.getSupabaseId(), upd,
                        new RemoteDataSource.Callback<Void>() {
                            @Override public void onSuccess(Void v) {}
                            @Override public void onError(String msg) {
                                AppDatabase.databaseWriteExecutor.execute(() ->
                                        categoryDao.updateSyncStatus(category.getId(), 2));
                            }
                        });
            }
        });
    }

    public void delete(Category category) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            // حذف تراکنش‌های مرتبط با برگرداندن موجودی
            List<Transaction> txs = transactionDao.getTransactionsByCategorySync(category.getId());
            for (Transaction tx : txs) {
                long change = tx.getType() == Transaction.TYPE_INCOME
                        ? -tx.getAmount() : tx.getAmount();
                bankCardDao.updateBalance(tx.getCardId(), change);
                transactionDao.delete(tx);
            }
            categoryDao.delete(category);

            // Auto-sync trigger
            SyncManager.getInstance().triggerAutoSync();

            long supabaseId = category.getSupabaseId();
            if (supabaseId > 0) {
                if (NetworkMonitor.getInstance().isOnline()) {
                    remote.deleteCategory(supabaseId, new RemoteDataSource.Callback<Void>() {
                        @Override public void onSuccess(Void v) {}
                        @Override public void onError(String msg) {
                            AppDatabase.databaseWriteExecutor.execute(() ->
                                    pendingDeleteDao.insert(new PendingDelete(
                                            PendingDelete.TYPE_CATEGORY, supabaseId)));
                        }
                    });
                } else {
                    pendingDeleteDao.insert(new PendingDelete(PendingDelete.TYPE_CATEGORY, supabaseId));
                }
            }
        });
    }

    public void getTransactionCount(long categoryId, OnTransactionCountListener listener) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            int count = transactionDao.getTransactionCountByCategory(categoryId);
            if (listener != null) listener.onCountReceived(count);
        });
    }

    public LiveData<List<Category>> getAllCategories()           { return categoryDao.getAllCategories(); }
    public LiveData<List<Category>> getCategoriesByType(int t)  { return categoryDao.getCategoriesByType(t); }
    public LiveData<Category> getCategoryById(long id)          { return categoryDao.getCategoryById(id); }

    public interface OnTransactionCountListener { void onCountReceived(int count); }

    private void showError(String msg) {
        new Handler(Looper.getMainLooper()).post(() ->
                Toast.makeText(application, msg, Toast.LENGTH_SHORT).show());
    }
}
