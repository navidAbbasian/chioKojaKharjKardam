package com.example.chiokojakharjkardam.data.repository;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.lifecycle.LiveData;

import com.example.chiokojakharjkardam.data.database.AppDatabase;
import com.example.chiokojakharjkardam.data.database.dao.BankCardDao;
import com.example.chiokojakharjkardam.data.database.dao.MemberDao;
import com.example.chiokojakharjkardam.data.database.dao.PendingDeleteDao;
import com.example.chiokojakharjkardam.data.database.entity.BankCard;
import com.example.chiokojakharjkardam.data.database.entity.Member;
import com.example.chiokojakharjkardam.data.database.entity.PendingDelete;
import com.example.chiokojakharjkardam.data.remote.RemoteDataSource;
import com.example.chiokojakharjkardam.data.remote.model.RemoteBankCard;
import com.example.chiokojakharjkardam.utils.NetworkMonitor;
import com.example.chiokojakharjkardam.utils.SessionManager;
import com.example.chiokojakharjkardam.utils.SyncManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BankCardRepository {

    private final BankCardDao bankCardDao;
    private final MemberDao memberDao;
    private final PendingDeleteDao pendingDeleteDao;
    private final Application application;
    private final RemoteDataSource remote;
    private final SessionManager session;

    public BankCardRepository(Application application) {
        this.application = application;
        AppDatabase db = AppDatabase.getDatabase(application);
        bankCardDao      = db.bankCardDao();
        memberDao        = db.memberDao();
        pendingDeleteDao = db.pendingDeleteDao();
        remote  = RemoteDataSource.getInstance();
        session = SessionManager.getInstance();
    }

    // ── Writes (offline-first) ──────────────────────────────────

    public void insertAndGetId(BankCard card, OnCardInsertedListener listener) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            // ۱. ذخیره محلی
            card.setPendingSync(1);
            long localId = bankCardDao.insert(card);
            card.setId(localId);
            if (listener != null) listener.onCardInserted(localId);

            // Auto-sync trigger
            SyncManager.getInstance().triggerAutoSync();
            if (NetworkMonitor.getInstance().isOnline() && session.hasFamilyId()) {
                String memberUserId = getMemberUserId(card.getMemberId());
                RemoteBankCard rc = new RemoteBankCard(card, session.getFamilyId(), memberUserId);
                rc.id = null;
                remote.insertBankCard(rc, new RemoteDataSource.Callback<RemoteBankCard>() {
                    @Override public void onSuccess(RemoteBankCard created) {
                        AppDatabase.databaseWriteExecutor.execute(() ->
                                bankCardDao.updateSupabaseId(localId, created.id));
                    }
                    @Override public void onError(String msg) { /* SYNC_NEW باقی می‌ماند */ }
                });
            }
        });
    }

    public void insert(BankCard card) { insertAndGetId(card, null); }

    public void update(BankCard card) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            // Always mark as pending update first; only clear after confirmed remote success
            card.setPendingSync(2);
            bankCardDao.update(card);

            if (NetworkMonitor.getInstance().isOnline() && card.getSupabaseId() > 0) {
                Map<String, Object> upd = new HashMap<>();
                upd.put("bank_name", card.getBankName());
                upd.put("card_number", card.getCardNumber());
                upd.put("card_holder_name", card.getCardHolderName());
                upd.put("balance", card.getBalance());
                upd.put("initial_balance", card.getInitialBalance());
                upd.put("color", card.getColor());
                remote.updateBankCard(card.getSupabaseId(), upd, new RemoteDataSource.Callback<Void>() {
                    @Override public void onSuccess(Void v) {
                        AppDatabase.databaseWriteExecutor.execute(() ->
                                bankCardDao.updateSyncStatus(card.getId(), 0));
                    }
                    @Override public void onError(String msg) {
                        // stays at pendingSync=2 for next sync attempt
                    }
                });
            }
            // Auto-sync trigger (after remote call is set up to avoid race with download)
            SyncManager.getInstance().triggerAutoSync();
        });
    }

    public void delete(BankCard card) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            bankCardDao.delete(card);

            // Auto-sync trigger
            SyncManager.getInstance().triggerAutoSync();

            long supabaseId = card.getSupabaseId();
            if (supabaseId > 0) {
                if (NetworkMonitor.getInstance().isOnline()) {
                    remote.deleteBankCard(supabaseId, new RemoteDataSource.Callback<Void>() {
                        @Override public void onSuccess(Void v) {}
                        @Override public void onError(String msg) {
                            AppDatabase.databaseWriteExecutor.execute(() ->
                                    pendingDeleteDao.insert(new PendingDelete(
                                            PendingDelete.TYPE_BANK_CARD, supabaseId)));
                        }
                    });
                } else {
                    pendingDeleteDao.insert(new PendingDelete(PendingDelete.TYPE_BANK_CARD, supabaseId));
                }
            }
        });
    }

    public void updateBalance(long cardId, long amount) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            bankCardDao.updateBalance(cardId, amount);
            BankCard updated = bankCardDao.getCardByIdSync(cardId);
            if (updated != null && updated.getSupabaseId() > 0
                    && NetworkMonitor.getInstance().isOnline()) {
                Map<String, Object> upd = new HashMap<>();
                upd.put("balance", updated.getBalance());
                remote.updateBankCard(updated.getSupabaseId(), upd,
                        new RemoteDataSource.Callback<Void>() {
                            @Override public void onSuccess(Void v) {}
                            @Override public void onError(String m) {}
                        });
            }
        });
    }

    // ── Reads (local Room cache) ──────────────────────────────────
    public LiveData<List<BankCard>> getAllCards()              { return bankCardDao.getAllCards(); }
    public LiveData<List<BankCard>> getCardsByMember(long id) { return bankCardDao.getCardsByMember(id); }
    public LiveData<BankCard> getCardById(long id)            { return bankCardDao.getCardById(id); }
    /** Legacy: reads the stored balance column (may be stale). Use getComputedTotalBalance() instead. */
    public LiveData<Long> getTotalBalance()                   { return bankCardDao.getTotalBalance(); }
    /**
     * Reactively computes total balance directly from transactions:
     * total = SUM(income) - SUM(expense). Always accurate, never drifts.
     */
    public LiveData<Long> getComputedTotalBalance()           { return bankCardDao.getComputedTotalBalance(); }

    private String getMemberUserId(long memberId) {
        Member m = memberDao.getMemberByIdSync(memberId);
        return (m != null && m.getUserId() != null) ? m.getUserId() : session.getUserId();
    }
    private void showError(String msg) {
        new Handler(Looper.getMainLooper()).post(() ->
                Toast.makeText(application, msg, Toast.LENGTH_SHORT).show());
    }

    public interface OnCardInsertedListener { void onCardInserted(long cardId); }
}
