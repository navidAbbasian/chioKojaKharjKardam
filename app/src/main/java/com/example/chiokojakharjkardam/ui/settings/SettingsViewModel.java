package com.example.chiokojakharjkardam.ui.settings;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.chiokojakharjkardam.data.database.AppDatabase;
import com.example.chiokojakharjkardam.data.database.entity.Family;
import com.example.chiokojakharjkardam.data.repository.FamilyRepository;
import com.example.chiokojakharjkardam.utils.NetworkMonitor;
import com.example.chiokojakharjkardam.utils.SessionManager;
import com.example.chiokojakharjkardam.utils.SyncManager;

public class SettingsViewModel extends AndroidViewModel {

    private final FamilyRepository familyRepository;
    private final LiveData<Family> family;
    private final SessionManager session;

    public SettingsViewModel(@NonNull Application application) {
        super(application);
        familyRepository = new FamilyRepository(application);
        family  = familyRepository.getFirstFamily();
        session = SessionManager.getInstance();
    }

    public LiveData<Family> getFamily() { return family; }

    // ── User / Family info ────────────────────────────────────────

    public String getUserFullName() { return session.getFullName(); }
    public String getUserEmail()    { return session.getUserEmail(); }
    public String getInviteCode()   { return session.getInviteCode(); }
    public boolean isOwner()        { return session.isOwner(); }

    // ── Sync ──────────────────────────────────────────────────────

    public void syncNow() {
        SyncManager.getInstance().syncAll();
    }

    // ── Logout ────────────────────────────────────────────────────

    public void logout(ClearDataCallback callback) {
        // Phase 1: if online, upload every unsynced local record to Supabase first
        // so data created/edited offline is not permanently lost.
        if (session.hasFamilyId() && NetworkMonitor.getInstance().isOnline()) {
            SyncManager.getInstance().uploadPending(
                    session.getFamilyId(),
                    () -> wipeLocalDataAndSession(callback));
        } else {
            // Offline: wipe immediately (user was warned in the dialog)
            wipeLocalDataAndSession(callback);
        }
    }

    /** Deletes every local table and clears the Supabase session. */
    private void wipeLocalDataAndSession(ClearDataCallback callback) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                AppDatabase db = AppDatabase.getDatabase(getApplication());
                db.transactionTagDao().deleteAll();
                db.transactionDao().deleteAll();
                db.transferDao().deleteAll();
                db.billDao().deleteAll();
                db.bankCardDao().deleteAll();
                db.tagDao().deleteAll();
                db.categoryDao().deleteAll();
                db.memberDao().deleteAll();
                db.familyDao().deleteAll();
                db.pendingDeleteDao().deleteAll(); // clear tombstones for next login

                session.clearSession();

                if (callback != null) callback.onSuccess();
            } catch (Exception e) {
                if (callback != null) callback.onError(e.getMessage());
            }
        });
    }

    // ── Data clear ────────────────────────────────────────────────

    public interface ClearDataCallback {
        void onSuccess();
        void onError(String error);
    }

    public void clearTransactionsOnly(ClearDataCallback callback) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                AppDatabase db = AppDatabase.getDatabase(getApplication());
                db.transactionTagDao().deleteAll();
                db.transactionDao().deleteAll();
                db.transferDao().deleteAll();
                if (callback != null) callback.onSuccess();
            } catch (Exception e) {
                if (callback != null) callback.onError(e.getMessage());
            }
        });
    }

    public void clearAllData(ClearDataCallback callback) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                AppDatabase db = AppDatabase.getDatabase(getApplication());
                db.transactionTagDao().deleteAll();
                db.transactionDao().deleteAll();
                db.transferDao().deleteAll();
                db.billDao().deleteAll();
                db.bankCardDao().deleteAll();
                db.tagDao().deleteAll();
                db.categoryDao().deleteAll();
                db.memberDao().deleteAll();
                db.familyDao().deleteAll();
                if (callback != null) callback.onSuccess();
            } catch (Exception e) {
                if (callback != null) callback.onError(e.getMessage());
            }
        });
    }
}
