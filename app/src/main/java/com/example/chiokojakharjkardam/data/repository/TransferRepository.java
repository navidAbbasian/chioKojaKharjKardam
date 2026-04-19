package com.example.chiokojakharjkardam.data.repository;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.lifecycle.LiveData;

import com.example.chiokojakharjkardam.data.database.AppDatabase;
import com.example.chiokojakharjkardam.data.database.dao.BankCardDao;
import com.example.chiokojakharjkardam.data.database.dao.TransferDao;
import com.example.chiokojakharjkardam.data.database.entity.BankCard;
import com.example.chiokojakharjkardam.data.database.entity.Transfer;
import com.example.chiokojakharjkardam.data.remote.RemoteDataSource;
import com.example.chiokojakharjkardam.data.remote.model.RemoteTransfer;
import com.example.chiokojakharjkardam.utils.NetworkMonitor;
import com.example.chiokojakharjkardam.utils.SessionManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TransferRepository {

    private final TransferDao transferDao;
    private final BankCardDao bankCardDao;
    private final Application application;
    private final RemoteDataSource remote;
    private final SessionManager session;

    public TransferRepository(Application application) {
        this.application = application;
        AppDatabase db = AppDatabase.getDatabase(application);
        transferDao = db.transferDao();
        bankCardDao = db.bankCardDao();
        remote  = RemoteDataSource.getInstance();
        session = SessionManager.getInstance();
    }

    /**
     * ثبت انتقال بین کارت‌ها با به‌روزرسانی موجودی هر دو کارت.
     * Remote-first: ابتدا در Supabase ثبت می‌شود، سپس در Room.
     */
    public void insertWithBalanceUpdate(Transfer transfer, OnTransferInsertedListener listener) {
        if (!NetworkMonitor.getInstance().isOnline()) { showOfflineError(); return; }
        AppDatabase.databaseWriteExecutor.execute(() -> {
            // بررسی موجودی کارت مبدأ
            BankCard fromCard = bankCardDao.getCardByIdSync(transfer.getFromCardId());
            if (fromCard != null && fromCard.getBalance() < transfer.getAmount()) {
                new Handler(Looper.getMainLooper()).post(() ->
                        Toast.makeText(application,
                                "موجودی کارت مبدأ کافی نیست. موجودی فعلی: " + fromCard.getBalance(),
                                Toast.LENGTH_SHORT).show());
                return;
            }

            RemoteTransfer rt = new RemoteTransfer(transfer, session.getFamilyId(), session.getUserId());
            rt.id = null;
            remote.insertTransfer(rt, new RemoteDataSource.Callback<RemoteTransfer>() {
                @Override public void onSuccess(RemoteTransfer created) {
                    transfer.setId(created.id);
                    AppDatabase.databaseWriteExecutor.execute(() -> {
                        transferDao.upsert(transfer);
                        // به‌روزرسانی موجودی کارت‌ها
                        bankCardDao.updateBalance(transfer.getFromCardId(), -transfer.getAmount());
                        bankCardDao.updateBalance(transfer.getToCardId(),   transfer.getAmount());
                        // همگام‌سازی موجودی با Supabase
                        syncCardBalance(transfer.getFromCardId());
                        syncCardBalance(transfer.getToCardId());
                        if (listener != null) listener.onTransferInserted(created.id);
                    });
                }
                @Override public void onError(String msg) { showError(msg); }
            });
        });
    }

    public void doTransfer(Transfer transfer) {
        insertWithBalanceUpdate(transfer, null);
    }

    public void delete(Transfer transfer) {
        if (!NetworkMonitor.getInstance().isOnline()) { showOfflineError(); return; }
        remote.deleteTransfer(transfer.getId(), new RemoteDataSource.Callback<Void>() {
            @Override public void onSuccess(Void v) {
                AppDatabase.databaseWriteExecutor.execute(() -> {
                    // برگرداندن موجودی‌ها
                    bankCardDao.updateBalance(transfer.getFromCardId(),  transfer.getAmount());
                    bankCardDao.updateBalance(transfer.getToCardId(),   -transfer.getAmount());
                    syncCardBalance(transfer.getFromCardId());
                    syncCardBalance(transfer.getToCardId());
                    transferDao.delete(transfer);
                });
            }
            @Override public void onError(String msg) { showError(msg); }
        });
    }

    // ── Reads (local Room cache) ──────────────────────────────────

    public LiveData<List<Transfer>> getAllTransfers()              { return transferDao.getAllTransfers(); }
    public LiveData<List<Transfer>> getTransfersByCard(long id)   { return transferDao.getTransfersByCard(id); }
    public LiveData<Transfer>       getTransferById(long id)      { return transferDao.getTransferById(id); }
    public LiveData<List<Transfer>> getRecentTransfers(int limit) { return transferDao.getRecentTransfers(limit); }

    // ── Helpers ───────────────────────────────────────────────────

    private void syncCardBalance(long cardId) {
        BankCard card = bankCardDao.getCardByIdSync(cardId);
        if (card == null) return;
        Map<String, Object> upd = new HashMap<>();
        upd.put("balance", card.getBalance());
        remote.updateBankCard(cardId, upd, new RemoteDataSource.Callback<Void>() {
            @Override public void onSuccess(Void v) {}
            @Override public void onError(String m) {}
        });
    }

    private void showOfflineError() {
        new Handler(Looper.getMainLooper()).post(() ->
                Toast.makeText(application, "اتصال اینترنت وجود ندارد", Toast.LENGTH_SHORT).show());
    }
    private void showError(String msg) {
        new Handler(Looper.getMainLooper()).post(() ->
                Toast.makeText(application, msg, Toast.LENGTH_SHORT).show());
    }

    public interface OnTransferInsertedListener { void onTransferInserted(long transferId); }
}
