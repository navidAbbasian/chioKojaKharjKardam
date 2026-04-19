package com.example.chiokojakharjkardam.data.repository;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.lifecycle.LiveData;

import com.example.chiokojakharjkardam.data.database.AppDatabase;
import com.example.chiokojakharjkardam.data.database.dao.BankCardDao;
import com.example.chiokojakharjkardam.data.database.dao.PendingDeleteDao;
import com.example.chiokojakharjkardam.data.database.dao.TagDao;
import com.example.chiokojakharjkardam.data.database.dao.TransactionDao;
import com.example.chiokojakharjkardam.data.database.entity.PendingDelete;
import com.example.chiokojakharjkardam.data.database.entity.Tag;
import com.example.chiokojakharjkardam.data.database.entity.Transaction;
import com.example.chiokojakharjkardam.data.remote.RemoteDataSource;
import com.example.chiokojakharjkardam.data.remote.model.RemoteTag;
import com.example.chiokojakharjkardam.utils.NetworkMonitor;
import com.example.chiokojakharjkardam.utils.SessionManager;
import com.example.chiokojakharjkardam.utils.SyncManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TagRepository {

    private final TagDao tagDao;
    private final TransactionDao transactionDao;
    private final BankCardDao bankCardDao;
    private final PendingDeleteDao pendingDeleteDao;
    private final Application application;
    private final RemoteDataSource remote;
    private final SessionManager session;

    public TagRepository(Application application) {
        this.application = application;
        AppDatabase db = AppDatabase.getDatabase(application);
        tagDao           = db.tagDao();
        transactionDao   = db.transactionDao();
        bankCardDao      = db.bankCardDao();
        pendingDeleteDao = db.pendingDeleteDao();
        remote  = RemoteDataSource.getInstance();
        session = SessionManager.getInstance();
    }

    public void insert(Tag tag) {
        AppDatabase.databaseWriteExecutor.execute(() -> tagDao.insert(tag));
    }

    public void insertAndGetId(Tag tag, OnTagInsertedListener listener) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            tag.setPendingSync(1);
            long localId = tagDao.insert(tag);
            tag.setId(localId);
            if (listener != null) listener.onTagInserted(localId);

            // Auto-sync trigger
            SyncManager.getInstance().triggerAutoSync();

            if (NetworkMonitor.getInstance().isOnline() && session.hasFamilyId()) {
                RemoteTag rt = new RemoteTag(tag, session.getFamilyId());
                rt.id = null;
                remote.insertTag(rt, new RemoteDataSource.Callback<RemoteTag>() {
                    @Override public void onSuccess(RemoteTag created) {
                        AppDatabase.databaseWriteExecutor.execute(() ->
                                tagDao.updateSupabaseId(localId, created.id));
                    }
                    @Override public void onError(String msg) {}
                });
            }
        });
    }

    public void update(Tag tag) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            if (NetworkMonitor.getInstance().isOnline() && tag.getSupabaseId() > 0) {
                tag.setPendingSync(0);
            } else {
                tag.setPendingSync(2);
            }
            tagDao.update(tag);

            // Auto-sync trigger
            SyncManager.getInstance().triggerAutoSync();

            if (NetworkMonitor.getInstance().isOnline() && tag.getSupabaseId() > 0) {
                Map<String, Object> upd = new HashMap<>();
                upd.put("name", tag.getName());
                upd.put("color", tag.getColor());
                remote.updateTag(tag.getSupabaseId(), upd, new RemoteDataSource.Callback<Void>() {
                    @Override public void onSuccess(Void v) {}
                    @Override public void onError(String msg) {
                        AppDatabase.databaseWriteExecutor.execute(() ->
                                tagDao.updateSyncStatus(tag.getId(), 2));
                    }
                });
            }
        });
    }

    public void delete(Tag tag) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<Transaction> txs = transactionDao.getTransactionsByTagSync(tag.getId());
            for (Transaction tx : txs) {
                long change = tx.getType() == Transaction.TYPE_INCOME
                        ? -tx.getAmount() : tx.getAmount();
                bankCardDao.updateBalance(tx.getCardId(), change);
                transactionDao.delete(tx);
            }
            tagDao.delete(tag);

            // Auto-sync trigger
            SyncManager.getInstance().triggerAutoSync();

            long supabaseId = tag.getSupabaseId();
            if (supabaseId > 0) {
                if (NetworkMonitor.getInstance().isOnline()) {
                    remote.deleteTag(supabaseId, new RemoteDataSource.Callback<Void>() {
                        @Override public void onSuccess(Void v) {}
                        @Override public void onError(String msg) {
                            AppDatabase.databaseWriteExecutor.execute(() ->
                                    pendingDeleteDao.insert(new PendingDelete(
                                            PendingDelete.TYPE_TAG, supabaseId)));
                        }
                    });
                } else {
                    pendingDeleteDao.insert(new PendingDelete(PendingDelete.TYPE_TAG, supabaseId));
                }
            }
        });
    }

    public void getTransactionCount(long tagId, OnTransactionCountListener listener) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            int count = transactionDao.getTransactionCountByTag(tagId);
            if (listener != null) listener.onCountReceived(count);
        });
    }

    public LiveData<List<Tag>> getAllTags()                                    { return tagDao.getAllTags(); }
    public LiveData<Tag> getTagById(long id)                                  { return tagDao.getTagById(id); }
    public LiveData<List<Tag>> getTagsByTransactionId(long transactionId)     { return tagDao.getTagsByTransactionId(transactionId); }

    public interface OnTagInsertedListener { void onTagInserted(long tagId); }
    public interface OnTransactionCountListener { void onCountReceived(int count); }

    private void showOfflineError() {
        new Handler(Looper.getMainLooper()).post(() ->
                Toast.makeText(application, "اتصال اینترنت وجود ندارد", Toast.LENGTH_SHORT).show());
    }
    private void showError(String msg) {
        new Handler(Looper.getMainLooper()).post(() ->
                Toast.makeText(application, msg, Toast.LENGTH_SHORT).show());
    }
}
