package com.example.chiokojakharjkardam.utils;

import android.app.Application;
import android.util.Log;

import com.example.chiokojakharjkardam.data.database.AppDatabase;
import com.example.chiokojakharjkardam.data.database.dao.*;
import com.example.chiokojakharjkardam.data.database.entity.*;
import com.example.chiokojakharjkardam.data.remote.RemoteDataSource;
import com.example.chiokojakharjkardam.data.remote.model.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class SyncManager {

    private static final String TAG = "SyncManager";
    private static volatile SyncManager instance;

    private AppDatabase db;
    private RemoteDataSource remote;
    private SessionManager session;
    private PendingDeleteDao pendingDeleteDao;
    private volatile boolean manualSyncInProgress = false;

    private SyncManager() {}

    public static void init(Application app) {
        if (instance == null) {
            synchronized (SyncManager.class) {
                if (instance == null) {
                    instance = new SyncManager();
                    instance.db = AppDatabase.getDatabase(app);
                    instance.remote = RemoteDataSource.getInstance();
                    instance.session = SessionManager.getInstance();
                    instance.pendingDeleteDao = instance.db.pendingDeleteDao();
                }
            }
        }
    }

    public static SyncManager getInstance() {
        if (instance == null) throw new IllegalStateException("SyncManager not initialised");
        return instance;
    }

    // ======== Inner classes & interfaces ========

    public static class SyncStatusReport {
        public int pendingUploadCategories;
        public int pendingUploadTags;
        public int pendingUploadCards;
        public int pendingUploadTransactions;
        public int pendingDeletes;
        public int remoteOnlyCategories;
        public int remoteOnlyTags;
        public int remoteOnlyCards;
        public int remoteOnlyTransactions;
        public int totalPendingUpload() {
            return pendingUploadCategories + pendingUploadTags + pendingUploadCards + pendingUploadTransactions + pendingDeletes;
        }
        public int totalRemoteOnly() {
            return remoteOnlyCategories + remoteOnlyTags + remoteOnlyCards + remoteOnlyTransactions;
        }
        public int totalPendingDownload() { return totalRemoteOnly(); }
    }

    public static class SyncResult {
        public boolean success;
        public int uploadedCategories, uploadedTags, uploadedCards, uploadedTransactions, uploadedDeletes;
        public int downloadedMembers, downloadedCards, downloadedCategories, downloadedTags;
        public int downloadedTransactions, downloadedBills, downloadedTransfers;
        public boolean alreadySynced;
        public String errorMessage;
        public boolean needsRelogin;
        public int totalUploaded() {
            return uploadedCategories + uploadedTags + uploadedCards + uploadedTransactions + uploadedDeletes;
        }
        public int totalDownloaded() {
            return downloadedMembers + downloadedCards + downloadedCategories + downloadedTags
                    + downloadedTransactions + downloadedBills + downloadedTransfers;
        }
    }

    public interface SyncStatusCallback {
        void onResult(SyncStatusReport report);
        void onError(String message);
    }

    public interface SyncProgressCallback {
        void onProgress(int current, int total, String message);
    }

    public interface SyncResultCallback {
        void onResult(SyncResult result);
    }

    public interface PendingDataCallback {
        void onResult(boolean hasPending);
    }

    // ======== Quick pending check ========

    public void hasPendingDataSync(PendingDataCallback callback) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            boolean has = !db.categoryDao().getPendingCategories().isEmpty()
                    || !db.tagDao().getPendingTags().isEmpty()
                    || !db.bankCardDao().getPendingCards().isEmpty()
                    || !db.transactionDao().getPendingTransactions().isEmpty()
                    || !pendingDeleteDao.getByType(PendingDelete.TYPE_TRANSACTION).isEmpty()
                    || !pendingDeleteDao.getByType(PendingDelete.TYPE_BANK_CARD).isEmpty()
                    || !pendingDeleteDao.getByType(PendingDelete.TYPE_CATEGORY).isEmpty()
                    || !pendingDeleteDao.getByType(PendingDelete.TYPE_TAG).isEmpty();
            callback.onResult(has);
        });
    }

    // ======== Check local pending status ========

    public void checkLocalPendingStatus(java.util.function.Consumer<SyncStatusReport> consumer) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            SyncStatusReport r = new SyncStatusReport();
            r.pendingUploadCategories = db.categoryDao().getPendingCategories().size();
            r.pendingUploadTags = db.tagDao().getPendingTags().size();
            r.pendingUploadCards = db.bankCardDao().getPendingCards().size();
            r.pendingUploadTransactions = db.transactionDao().getPendingTransactions().size();
            List<PendingDelete> allDel = new ArrayList<>();
            allDel.addAll(pendingDeleteDao.getByType(PendingDelete.TYPE_TRANSACTION));
            allDel.addAll(pendingDeleteDao.getByType(PendingDelete.TYPE_BANK_CARD));
            allDel.addAll(pendingDeleteDao.getByType(PendingDelete.TYPE_CATEGORY));
            allDel.addAll(pendingDeleteDao.getByType(PendingDelete.TYPE_TAG));
            r.pendingDeletes = allDel.size();
            consumer.accept(r);
        });
    }

    // ======== Check sync status (local + remote) ========

    public void checkSyncStatus(SyncStatusCallback callback) {
        if (!session.hasFamilyId()) { callback.onError("No family"); return; }
        if (!NetworkMonitor.getInstance().isOnline()) { callback.onError("Offline"); return; }
        String familyId = session.getFamilyId();
        checkLocalPendingStatus(report -> {
            AtomicInteger remaining = new AtomicInteger(4);
            final boolean[] hadError = {false};
            Runnable done = () -> { if (remaining.decrementAndGet() == 0 && !hadError[0]) callback.onResult(report); };

            remote.getCategories(familyId, new RemoteDataSource.Callback<List<RemoteCategory>>() {
                public void onSuccess(List<RemoteCategory> list) {
                    AppDatabase.databaseWriteExecutor.execute(() -> {
                        int s = 0; for (Category c : db.categoryDao().getAllCategoriesSync()) if (c.getSupabaseId() > 0) s++;
                        report.remoteOnlyCategories = Math.max(0, list.size() - s); done.run();
                    });
                }
                public void onError(String m) { hadError[0] = true; callback.onError(m); }
            });
            remote.getTags(familyId, new RemoteDataSource.Callback<List<RemoteTag>>() {
                public void onSuccess(List<RemoteTag> list) {
                    AppDatabase.databaseWriteExecutor.execute(() -> {
                        int s = 0; for (Tag t : db.tagDao().getAllTagsSync()) if (t.getSupabaseId() > 0) s++;
                        report.remoteOnlyTags = Math.max(0, list.size() - s); done.run();
                    });
                }
                public void onError(String m) { hadError[0] = true; callback.onError(m); }
            });
            remote.getBankCards(familyId, new RemoteDataSource.Callback<List<RemoteBankCard>>() {
                public void onSuccess(List<RemoteBankCard> list) {
                    AppDatabase.databaseWriteExecutor.execute(() -> {
                        int s = 0; for (BankCard c : db.bankCardDao().getAllCardsSync()) if (c.getSupabaseId() > 0) s++;
                        report.remoteOnlyCards = Math.max(0, list.size() - s); done.run();
                    });
                }
                public void onError(String m) { hadError[0] = true; callback.onError(m); }
            });
            remote.getTransactions(familyId, new RemoteDataSource.Callback<List<RemoteTransaction>>() {
                public void onSuccess(List<RemoteTransaction> list) {
                    AppDatabase.databaseWriteExecutor.execute(() -> {
                        int s = 0; for (Transaction t : db.transactionDao().getAllTransactionsSync()) if (t.getSupabaseId() != null) s++;
                        report.remoteOnlyTransactions = Math.max(0, list.size() - s); done.run();
                    });
                }
                public void onError(String m) { hadError[0] = true; callback.onError(m); }
            });
        });
    }

    // ======== Upload with progress ========

    public void uploadWithProgress(SyncProgressCallback progress, SyncResultCallback resultCb) {
        if (!session.hasFamilyId() || !NetworkMonitor.getInstance().isOnline()) {
            SyncResult r = new SyncResult(); r.success = false; r.errorMessage = "Offline or no family"; resultCb.onResult(r); return;
        }
        manualSyncInProgress = true;
        String familyId = session.getFamilyId();
        SyncResult result = new SyncResult();
        int[] step = {0};
        int totalSteps = 5;

        progress.onProgress(step[0], totalSteps, "Processing deletes...");
        processPendingDeletesWithResult(result, () -> {
            step[0]++;
            progress.onProgress(step[0], totalSteps, "Uploading categories...");
            uploadCategoriesWithResult(familyId, result, () -> {
                step[0]++;
                progress.onProgress(step[0], totalSteps, "Uploading tags...");
                uploadTagsWithResult(familyId, result, () -> {
                    step[0]++;
                    progress.onProgress(step[0], totalSteps, "Uploading cards...");
                    uploadCardsWithResult(familyId, result, () -> {
                        step[0]++;
                        progress.onProgress(step[0], totalSteps, "Uploading transactions...");
                        uploadTransactionsWithResult(familyId, result, () -> {
                            step[0]++;
                            progress.onProgress(step[0], totalSteps, "Done");
                            result.success = result.errorMessage == null;
                            manualSyncInProgress = false;
                            resultCb.onResult(result);
                        });
                    });
                });
            });
        });
    }

    // ======== Download with progress ========

    public void downloadWithProgress(SyncProgressCallback progress, SyncResultCallback resultCb) {
        if (!session.hasFamilyId() || !NetworkMonitor.getInstance().isOnline()) {
            SyncResult r = new SyncResult(); r.success = false; r.errorMessage = "Offline or no family"; resultCb.onResult(r); return;
        }
        manualSyncInProgress = true;
        String familyId = session.getFamilyId();
        SyncResult result = new SyncResult();
        int[] step = {0};
        int totalSteps = 8;

        progress.onProgress(step[0], totalSteps, "Syncing family...");
        ensureFamilyInRoom(familyId, () -> {
            step[0]++;
            progress.onProgress(step[0], totalSteps, "Syncing members...");
            syncMembers(familyId, result, () -> {
                step[0]++;
                progress.onProgress(step[0], totalSteps, "Syncing categories...");
                syncCategories(familyId, result, () -> {
                    step[0]++;
                    progress.onProgress(step[0], totalSteps, "Syncing tags...");
                    syncTags(familyId, result, () -> {
                        step[0]++;
                        progress.onProgress(step[0], totalSteps, "Syncing cards...");
                        syncBankCards(familyId, result, () -> {
                            step[0]++;
                            progress.onProgress(step[0], totalSteps, "Syncing transactions...");
                            syncTransactions(familyId, result, () -> {
                                step[0]++;
                                progress.onProgress(step[0], totalSteps, "Syncing bills...");
                                syncBills(familyId, result, () -> {
                                    step[0]++;
                                    progress.onProgress(step[0], totalSteps, "Syncing transfers...");
                                    syncTransfers(familyId, result, () -> {
                                        step[0]++;
                                        // Recalculate all card balances after full download
                                        recalculateAllCardBalances();
                                        progress.onProgress(step[0], totalSteps, "Done");
                                        result.success = result.errorMessage == null;
                                        manualSyncInProgress = false;
                                        resultCb.onResult(result);
                                    });
                                });
                            });
                        });
                    });
                });
            });
        });
    }
    // ======== Auto sync (called on every onResume) ========

    public void syncAll() {
        if (!session.hasFamilyId() || !NetworkMonitor.getInstance().isOnline() || manualSyncInProgress) return;
        String familyId = session.getFamilyId();
        Log.d(TAG, "syncAll start");
        uploadPending(familyId, () -> {
            ensureFamilyInRoom(familyId, () -> {
                SyncResult dummy = new SyncResult();
                syncMembers(familyId, dummy, () ->
                syncCategories(familyId, dummy, () ->
                syncTags(familyId, dummy, () ->
                syncBankCards(familyId, dummy, () ->
                syncTransactions(familyId, dummy, () ->
                syncBills(familyId, dummy, () ->
                syncTransfers(familyId, dummy, () -> {
                    // Recalculate all card balances after full sync
                    recalculateAllCardBalances();
                    Log.d(TAG, "syncAll complete");
                })))))));
            });
        });
    }

    public void uploadPending(String familyId, Runnable onDone) {
        processPendingDeletes(() ->
            uploadPendingCategories(familyId, () ->
                uploadPendingTags(familyId, () ->
                    uploadPendingBankCards(familyId, () ->
                        uploadPendingTransactions(familyId, onDone)))));
    }

    private void processPendingDeletes(Runnable onDone) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<PendingDelete> all = new ArrayList<>();
            all.addAll(pendingDeleteDao.getByType(PendingDelete.TYPE_TRANSACTION));
            all.addAll(pendingDeleteDao.getByType(PendingDelete.TYPE_BANK_CARD));
            all.addAll(pendingDeleteDao.getByType(PendingDelete.TYPE_CATEGORY));
            all.addAll(pendingDeleteDao.getByType(PendingDelete.TYPE_TAG));
            if (all.isEmpty()) { onDone.run(); return; }
            AtomicInteger rem = new AtomicInteger(all.size());
            for (PendingDelete pd : all) {
                RemoteDataSource.Callback<Void> cb = new RemoteDataSource.Callback<Void>() {
                    public void onSuccess(Void v) {
                        AppDatabase.databaseWriteExecutor.execute(() -> {
                            pendingDeleteDao.deleteByTypeAndId(pd.getEntityType(), pd.getSupabaseId());
                            if (rem.decrementAndGet() == 0) onDone.run();
                        });
                    }
                    public void onError(String m) {
                        Log.e(TAG, "Delete failed: " + pd.getEntityType() + "#" + pd.getSupabaseId() + ": " + m);
                        if (rem.decrementAndGet() == 0) onDone.run();
                    }
                };
                switch (pd.getEntityType()) {
                    case PendingDelete.TYPE_TRANSACTION: remote.deleteTransaction(pd.getSupabaseId(), cb); break;
                    case PendingDelete.TYPE_BANK_CARD: remote.deleteBankCard(Long.parseLong(pd.getSupabaseId()), cb); break;
                    case PendingDelete.TYPE_CATEGORY: remote.deleteCategory(Long.parseLong(pd.getSupabaseId()), cb); break;
                    case PendingDelete.TYPE_TAG: remote.deleteTag(Long.parseLong(pd.getSupabaseId()), cb); break;
                    default: if (rem.decrementAndGet() == 0) onDone.run(); break;
                }
            }
        });
    }

    private void processPendingDeletesWithResult(SyncResult result, Runnable onDone) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<PendingDelete> all = new ArrayList<>();
            all.addAll(pendingDeleteDao.getByType(PendingDelete.TYPE_TRANSACTION));
            all.addAll(pendingDeleteDao.getByType(PendingDelete.TYPE_BANK_CARD));
            all.addAll(pendingDeleteDao.getByType(PendingDelete.TYPE_CATEGORY));
            all.addAll(pendingDeleteDao.getByType(PendingDelete.TYPE_TAG));
            if (all.isEmpty()) { onDone.run(); return; }
            AtomicInteger rem = new AtomicInteger(all.size());
            for (PendingDelete pd : all) {
                RemoteDataSource.Callback<Void> cb = new RemoteDataSource.Callback<Void>() {
                    public void onSuccess(Void v) {
                        result.uploadedDeletes++;
                        AppDatabase.databaseWriteExecutor.execute(() -> {
                            pendingDeleteDao.deleteByTypeAndId(pd.getEntityType(), pd.getSupabaseId());
                            if (rem.decrementAndGet() == 0) onDone.run();
                        });
                    }
                    public void onError(String m) {
                        Log.e(TAG, "Delete failed: " + m);
                        if (rem.decrementAndGet() == 0) onDone.run();
                    }
                };
                switch (pd.getEntityType()) {
                    case PendingDelete.TYPE_TRANSACTION: remote.deleteTransaction(pd.getSupabaseId(), cb); break;
                    case PendingDelete.TYPE_BANK_CARD: remote.deleteBankCard(Long.parseLong(pd.getSupabaseId()), cb); break;
                    case PendingDelete.TYPE_CATEGORY: remote.deleteCategory(Long.parseLong(pd.getSupabaseId()), cb); break;
                    case PendingDelete.TYPE_TAG: remote.deleteTag(Long.parseLong(pd.getSupabaseId()), cb); break;
                    default: if (rem.decrementAndGet() == 0) onDone.run(); break;
                }
            }
        });
    }

    private void uploadPendingCategories(String familyId, Runnable onDone) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<Category> pending = db.categoryDao().getPendingCategories();
            if (pending.isEmpty()) { onDone.run(); return; }
            AtomicInteger rem = new AtomicInteger(pending.size());
            for (Category c : pending) {
                if (c.getPendingSync() == Transaction.SYNC_NEW || c.getSupabaseId() == 0) {
                    remote.insertCategory(new RemoteCategory(c, familyId), new RemoteDataSource.Callback<RemoteCategory>() {
                        public void onSuccess(RemoteCategory rc) {
                            if (rc != null && rc.id != null) {
                                AppDatabase.databaseWriteExecutor.execute(() -> {
                                    db.categoryDao().updateSupabaseId(c.getId(), rc.id);
                                    if (rem.decrementAndGet() == 0) onDone.run();
                                });
                            } else { if (rem.decrementAndGet() == 0) onDone.run(); }
                        }
                        public void onError(String m) {
                            Log.e(TAG, "Upload category failed: " + m);
                            if (rem.decrementAndGet() == 0) onDone.run();
                        }
                    });
                } else {
                    Map<String, Object> upd = new HashMap<>();
                    upd.put("name", c.getName()); upd.put("icon", c.getIcon());
                    upd.put("color", c.getColor()); upd.put("type", c.getType());
                    remote.updateCategory(c.getSupabaseId(), upd, new RemoteDataSource.Callback<Void>() {
                        public void onSuccess(Void v) {
                            AppDatabase.databaseWriteExecutor.execute(() -> {
                                db.categoryDao().updateSyncStatus(c.getId(), Transaction.SYNC_DONE);
                                if (rem.decrementAndGet() == 0) onDone.run();
                            });
                        }
                        public void onError(String m) {
                            Log.e(TAG, "Update category failed: " + m);
                            if (rem.decrementAndGet() == 0) onDone.run();
                        }
                    });
                }
            }
        });
    }

    private void uploadPendingTags(String familyId, Runnable onDone) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<Tag> pending = db.tagDao().getPendingTags();
            if (pending.isEmpty()) { onDone.run(); return; }
            AtomicInteger rem = new AtomicInteger(pending.size());
            for (Tag t : pending) {
                if (t.getPendingSync() == Transaction.SYNC_NEW || t.getSupabaseId() == 0) {
                    remote.insertTag(new RemoteTag(t, familyId), new RemoteDataSource.Callback<RemoteTag>() {
                        public void onSuccess(RemoteTag rt) {
                            if (rt != null && rt.id != null) {
                                AppDatabase.databaseWriteExecutor.execute(() -> {
                                    db.tagDao().updateSupabaseId(t.getId(), rt.id);
                                    if (rem.decrementAndGet() == 0) onDone.run();
                                });
                            } else { if (rem.decrementAndGet() == 0) onDone.run(); }
                        }
                        public void onError(String m) {
                            Log.e(TAG, "Upload tag failed: " + m);
                            if (rem.decrementAndGet() == 0) onDone.run();
                        }
                    });
                } else {
                    Map<String, Object> upd = new HashMap<>();
                    upd.put("name", t.getName()); upd.put("color", t.getColor());
                    remote.updateTag(t.getSupabaseId(), upd, new RemoteDataSource.Callback<Void>() {
                        public void onSuccess(Void v) {
                            AppDatabase.databaseWriteExecutor.execute(() -> {
                                db.tagDao().updateSyncStatus(t.getId(), Transaction.SYNC_DONE);
                                if (rem.decrementAndGet() == 0) onDone.run();
                            });
                        }
                        public void onError(String m) {
                            Log.e(TAG, "Update tag failed: " + m);
                            if (rem.decrementAndGet() == 0) onDone.run();
                        }
                    });
                }
            }
        });
    }

    private void uploadPendingBankCards(String familyId, Runnable onDone) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<BankCard> pending = db.bankCardDao().getPendingCards();
            if (pending.isEmpty()) { onDone.run(); return; }
            String userId = session.getUserId();
            AtomicInteger rem = new AtomicInteger(pending.size());
            for (BankCard card : pending) {
                if (card.getPendingSync() == Transaction.SYNC_NEW || card.getSupabaseId() == 0) {
                    remote.insertBankCard(new RemoteBankCard(card, familyId, userId), new RemoteDataSource.Callback<RemoteBankCard>() {
                        public void onSuccess(RemoteBankCard rc) {
                            if (rc != null && rc.id != null) {
                                AppDatabase.databaseWriteExecutor.execute(() -> {
                                    db.bankCardDao().updateSupabaseId(card.getId(), rc.id);
                                    if (rem.decrementAndGet() == 0) onDone.run();
                                });
                            } else { if (rem.decrementAndGet() == 0) onDone.run(); }
                        }
                        public void onError(String m) {
                            Log.e(TAG, "Upload card failed: " + m);
                            if (rem.decrementAndGet() == 0) onDone.run();
                        }
                    });
                } else {
                    Map<String, Object> upd = new HashMap<>();
                    upd.put("bank_name", card.getBankName()); upd.put("card_number", card.getCardNumber());
                    upd.put("card_holder_name", card.getCardHolderName()); upd.put("balance", card.getBalance());
                    upd.put("initial_balance", card.getInitialBalance());
                    upd.put("color", card.getColor());
                    remote.updateBankCard(card.getSupabaseId(), upd, new RemoteDataSource.Callback<Void>() {
                        public void onSuccess(Void v) {
                            AppDatabase.databaseWriteExecutor.execute(() -> {
                                db.bankCardDao().updateSyncStatus(card.getId(), Transaction.SYNC_DONE);
                                if (rem.decrementAndGet() == 0) onDone.run();
                            });
                        }
                        public void onError(String m) {
                            Log.e(TAG, "Update card failed: " + m);
                            if (rem.decrementAndGet() == 0) onDone.run();
                        }
                    });
                }
            }
        });
    }
    private void uploadPendingTransactions(String familyId, Runnable onDone) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<Transaction> pending = db.transactionDao().getPendingTransactions();
            if (pending.isEmpty()) { onDone.run(); return; }
            String userId = session.getUserId();
            AtomicInteger rem = new AtomicInteger(pending.size());
            for (Transaction tx : pending) {
                RemoteTransaction rtx = buildRemoteTransaction(tx, familyId, userId);
                if (tx.getPendingSync() == Transaction.SYNC_NEW || tx.getSupabaseId() == null) {
                    remote.insertTransaction(rtx, new RemoteDataSource.Callback<RemoteTransaction>() {
                        public void onSuccess(RemoteTransaction created) {
                            if (created != null && created.id != null && !created.id.isEmpty()) {
                                AppDatabase.databaseWriteExecutor.execute(() -> {
                                    db.transactionDao().updateSupabaseId(tx.getId(), created.id);
                                    uploadTransactionTags(tx.getId(), created.id, () -> {
                                        if (rem.decrementAndGet() == 0) onDone.run();
                                    });
                                });
                            } else { if (rem.decrementAndGet() == 0) onDone.run(); }
                        }
                        public void onError(String m) {
                            Log.e(TAG, "Upload tx failed: " + m);
                            if (rem.decrementAndGet() == 0) onDone.run();
                        }
                    });
                } else {
                    Map<String, Object> upd = new HashMap<>();
                    upd.put("card_id", rtx.cardId); upd.put("category_id", rtx.categoryId);
                    upd.put("amount", rtx.amount); upd.put("type", rtx.type);
                    upd.put("to_card_id", rtx.toCardId); upd.put("description", rtx.description);
                    upd.put("date", rtx.date);
                    remote.updateTransaction(tx.getSupabaseId(), upd, new RemoteDataSource.Callback<Void>() {
                        public void onSuccess(Void v) {
                            AppDatabase.databaseWriteExecutor.execute(() -> {
                                db.transactionDao().updateSyncStatus(tx.getId(), Transaction.SYNC_DONE);
                                uploadTransactionTags(tx.getId(), tx.getSupabaseId(), () -> {
                                    if (rem.decrementAndGet() == 0) onDone.run();
                                });
                            });
                        }
                        public void onError(String m) {
                            Log.e(TAG, "Update tx failed: " + m);
                            if (rem.decrementAndGet() == 0) onDone.run();
                        }
                    });
                }
            }
        });
    }

    private void uploadTransactionTags(long localTxId, String supabaseTxId, Runnable onDone) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<Long> tagIds = db.transactionTagDao().getTagIdsByTransaction(localTxId);
            if (tagIds.isEmpty()) { onDone.run(); return; }
            remote.deleteTransactionTags(supabaseTxId, new RemoteDataSource.Callback<Void>() {
                public void onSuccess(Void v) {
                    AtomicInteger rem = new AtomicInteger(tagIds.size());
                    for (Long tagLocalId : tagIds) {
                        Tag tag = db.tagDao().getTagByIdSync(tagLocalId);
                        if (tag == null || tag.getSupabaseId() == 0) {
                            if (rem.decrementAndGet() == 0) onDone.run(); continue;
                        }
                        remote.insertTransactionTag(supabaseTxId, tag.getSupabaseId(), new RemoteDataSource.Callback<Void>() {
                            public void onSuccess(Void v) { if (rem.decrementAndGet() == 0) onDone.run(); }
                            public void onError(String m) {
                                Log.e(TAG, "Insert tx tag failed: " + m);
                                if (rem.decrementAndGet() == 0) onDone.run();
                            }
                        });
                    }
                }
                public void onError(String m) {
                    Log.e(TAG, "Delete tx tags failed: " + m);
                    onDone.run();
                }
            });
        });
    }

    private RemoteTransaction buildRemoteTransaction(Transaction tx, String familyId, String userId) {
        RemoteTransaction rtx = new RemoteTransaction(tx, familyId, userId);
        // Map local cardId -> supabase cardId
        BankCard card = db.bankCardDao().getCardByIdSync(tx.getCardId());
        if (card != null && card.getSupabaseId() > 0) rtx.cardId = card.getSupabaseId();
        // Map local categoryId -> supabase categoryId
        if (tx.getCategoryId() != null) {
            Category cat = db.categoryDao().getCategoryByIdSync(tx.getCategoryId());
            if (cat != null && cat.getSupabaseId() > 0) rtx.categoryId = cat.getSupabaseId();
        }
        // Map toCardId
        if (tx.getToCardId() != null) {
            BankCard toCard = db.bankCardDao().getCardByIdSync(tx.getToCardId());
            if (toCard != null && toCard.getSupabaseId() > 0) rtx.toCardId = toCard.getSupabaseId();
        }
        return rtx;
    }

    private void uploadCategoriesWithResult(String familyId, SyncResult result, Runnable onDone) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            int count = db.categoryDao().getPendingCategories().size();
            uploadPendingCategories(familyId, () -> { result.uploadedCategories = count; onDone.run(); });
        });
    }

    private void uploadTagsWithResult(String familyId, SyncResult result, Runnable onDone) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            int count = db.tagDao().getPendingTags().size();
            uploadPendingTags(familyId, () -> { result.uploadedTags = count; onDone.run(); });
        });
    }

    private void uploadCardsWithResult(String familyId, SyncResult result, Runnable onDone) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            int count = db.bankCardDao().getPendingCards().size();
            uploadPendingBankCards(familyId, () -> { result.uploadedCards = count; onDone.run(); });
        });
    }

    private void uploadTransactionsWithResult(String familyId, SyncResult result, Runnable onDone) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            int count = db.transactionDao().getPendingTransactions().size();
            uploadPendingTransactions(familyId, () -> { result.uploadedTransactions = count; onDone.run(); });
        });
    }
    // ======== Pull / Download methods ========

    private void ensureFamilyInRoom(String familyId, Runnable onDone) {
        remote.getFamilyById(familyId, new RemoteDataSource.Callback<RemoteFamily>() {
            public void onSuccess(RemoteFamily rf) {
                if (rf == null) { onDone.run(); return; }
                AppDatabase.databaseWriteExecutor.execute(() -> {
                    Family local = db.familyDao().getFamilySync();
                    if (local == null) {
                        Family f = rf.toEntity();
                        db.familyDao().insert(f);
                    } else {
                        local.setName(rf.name);
                        local.setSupabaseId(rf.id);
                        local.setInviteCode(rf.inviteCode);
                        db.familyDao().update(local);
                    }
                    onDone.run();
                });
            }
            public void onError(String m) { Log.e(TAG, "ensureFamilyInRoom: " + m); onDone.run(); }
        });
    }

    private Map<String, Long> buildMemberMap(List<Member> members) {
        Map<String, Long> map = new HashMap<>();
        for (Member m : members) if (m.getUserId() != null) map.put(m.getUserId(), m.getId());
        return map;
    }

    private void syncMembers(String familyId, SyncResult result, Runnable onDone) {
        remote.getProfilesByFamily(familyId, new RemoteDataSource.Callback<List<RemoteProfile>>() {
            public void onSuccess(List<RemoteProfile> profiles) {
                AppDatabase.databaseWriteExecutor.execute(() -> {
                    Family family = db.familyDao().getFamilySync();
                    long localFamilyId = family != null ? family.getId() : 1;
                    List<String> remoteUserIds = new ArrayList<>();
                    for (RemoteProfile p : profiles) {
                        remoteUserIds.add(p.id);
                        Member existing = db.memberDao().getByUserIdSync(p.id);
                        if (existing != null) {
                            existing.setName(p.fullName);
                            existing.setOwner(p.isOwner);
                            if (p.avatarColor != null) existing.setAvatarColor(p.avatarColor);
                            db.memberDao().update(existing);
                        } else {
                            Member m = p.toMember(localFamilyId);
                            long id = db.memberDao().insert(m);
                            result.downloadedMembers++;
                            if (p.id.equals(session.getUserId())) session.saveLocalMemberId(id);
                        }
                    }
                    if (!remoteUserIds.isEmpty()) db.memberDao().deleteObsoleteMembers(remoteUserIds);
                    onDone.run();
                });
            }
            public void onError(String m) { Log.e(TAG, "syncMembers: " + m); onDone.run(); }
        });
    }

    private void syncBankCards(String familyId, SyncResult result, Runnable onDone) {
        remote.getBankCards(familyId, new RemoteDataSource.Callback<List<RemoteBankCard>>() {
            public void onSuccess(List<RemoteBankCard> remoteCards) {
                AppDatabase.databaseWriteExecutor.execute(() -> {
                    List<Member> members = db.memberDao().getAllMembersSync();
                    Map<String, Long> memberMap = buildMemberMap(members);
                    long fallbackMemberId = session.getLocalMemberId();
                    List<Long> remoteIds = new ArrayList<>();
                    for (RemoteBankCard rc : remoteCards) {
                        if (rc.id != null) remoteIds.add(rc.id);
                        BankCard existing = rc.id != null ? db.bankCardDao().getBySupabaseId(rc.id) : null;
                        if (existing == null) existing = db.bankCardDao().getByCardNumberAndBank(rc.cardNumber, rc.bankName);
                        Long localMemberId = rc.memberId != null ? memberMap.get(rc.memberId) : null;
                        if (localMemberId == null) localMemberId = fallbackMemberId;
                        if (existing != null) {
                            existing.setBankName(rc.bankName);
                            existing.setCardNumber(rc.cardNumber);
                            existing.setCardHolderName(rc.cardHolderName);
                            // Don't overwrite balance here - recalculateAllCardBalances will fix it
                            // But update initialBalance from remote if available
                            if (rc.initialBalance != null) {
                                existing.setInitialBalance(rc.initialBalance);
                            }
                            existing.setColor(rc.color);
                            existing.setSupabaseId(rc.id != null ? rc.id : 0);
                            existing.setMemberId(localMemberId);
                            existing.setPendingSync(0);
                            db.bankCardDao().update(existing);
                        } else {
                            BankCard card = rc.toEntity(localMemberId);
                            db.bankCardDao().insert(card);
                            result.downloadedCards++;
                        }
                    }
                    if (!remoteIds.isEmpty()) db.bankCardDao().deleteObsoleteCards(remoteIds);
                    onDone.run();
                });
            }
            public void onError(String m) { Log.e(TAG, "syncBankCards: " + m); onDone.run(); }
        });
    }

    private void syncCategories(String familyId, SyncResult result, Runnable onDone) {
        remote.getCategories(familyId, new RemoteDataSource.Callback<List<RemoteCategory>>() {
            public void onSuccess(List<RemoteCategory> remoteCats) {
                AppDatabase.databaseWriteExecutor.execute(() -> {
                    Family family = db.familyDao().getFamilySync();
                    long localFamilyId = family != null ? family.getId() : 1;
                    List<Long> remoteIds = new ArrayList<>();
                    for (RemoteCategory rc : remoteCats) {
                        if (rc.id != null) remoteIds.add(rc.id);
                        Category existing = rc.id != null ? db.categoryDao().getBySupabaseId(rc.id) : null;
                        // Only fallback to name match for local records that have never been synced
                        if (existing == null) {
                            Category byName = db.categoryDao().getByName(rc.name);
                            if (byName != null && byName.getSupabaseId() == 0) {
                                existing = byName; // Match unsynced local record by name
                            }
                        }
                        if (existing != null) {
                            existing.setName(rc.name); existing.setIcon(rc.icon);
                            existing.setColor(rc.color); existing.setType(rc.type);
                            existing.setSupabaseId(rc.id != null ? rc.id : 0);
                            existing.setFamilyId(localFamilyId);
                            existing.setPendingSync(0);
                            db.categoryDao().update(existing);
                        } else {
                            Category c = rc.toEntity(localFamilyId);
                            db.categoryDao().insert(c);
                            result.downloadedCategories++;
                        }
                    }
                    if (!remoteIds.isEmpty()) db.categoryDao().deleteObsoleteCategories(remoteIds);
                    onDone.run();
                });
            }
            public void onError(String m) { Log.e(TAG, "syncCategories: " + m); onDone.run(); }
        });
    }

    private void syncTags(String familyId, SyncResult result, Runnable onDone) {
        remote.getTags(familyId, new RemoteDataSource.Callback<List<RemoteTag>>() {
            public void onSuccess(List<RemoteTag> remoteTags) {
                AppDatabase.databaseWriteExecutor.execute(() -> {
                    Family family = db.familyDao().getFamilySync();
                    long localFamilyId = family != null ? family.getId() : 1;
                    List<Long> remoteIds = new ArrayList<>();
                    for (RemoteTag rt : remoteTags) {
                        if (rt.id != null) remoteIds.add(rt.id);
                        Tag existing = rt.id != null ? db.tagDao().getBySupabaseId(rt.id) : null;
                        // Only fallback to name match for local records that have never been synced
                        if (existing == null) {
                            Tag byName = db.tagDao().getByName(rt.name);
                            if (byName != null && byName.getSupabaseId() == 0) {
                                existing = byName; // Match unsynced local record by name
                            }
                        }
                        if (existing != null) {
                            existing.setName(rt.name); existing.setColor(rt.color);
                            existing.setSupabaseId(rt.id != null ? rt.id : 0);
                            existing.setFamilyId(localFamilyId);
                            existing.setPendingSync(0);
                            db.tagDao().update(existing);
                        } else {
                            Tag t = rt.toEntity(localFamilyId);
                            db.tagDao().insert(t);
                            result.downloadedTags++;
                        }
                    }
                    if (!remoteIds.isEmpty()) db.tagDao().deleteObsoleteTags(remoteIds);
                    onDone.run();
                });
            }
            public void onError(String m) { Log.e(TAG, "syncTags: " + m); onDone.run(); }
        });
    }
    private void syncTransactions(String familyId, SyncResult result, Runnable onDone) {
        remote.getTransactions(familyId, new RemoteDataSource.Callback<List<RemoteTransaction>>() {
            public void onSuccess(List<RemoteTransaction> remoteTxs) {
                AppDatabase.databaseWriteExecutor.execute(() -> {
                    // Build supabaseId->local card/category maps for FK remapping
                    Map<Long, Long> cardSupaToLocal = new HashMap<>();
                    for (BankCard c : db.bankCardDao().getAllCardsSync())
                        if (c.getSupabaseId() > 0) cardSupaToLocal.put(c.getSupabaseId(), c.getId());
                    Map<Long, Long> catSupaToLocal = new HashMap<>();
                    for (Category c : db.categoryDao().getAllCategoriesSync())
                        if (c.getSupabaseId() > 0) catSupaToLocal.put(c.getSupabaseId(), c.getId());

                    List<String> remoteIds = new ArrayList<>();
                    for (RemoteTransaction rt : remoteTxs) {
                        if (rt.id != null) remoteIds.add(rt.id);
                        Transaction existing = rt.id != null ? db.transactionDao().getBySupabaseId(rt.id) : null;
                        // Remap FKs from supabase IDs to local IDs
                        Long localCardId = cardSupaToLocal.get(rt.cardId);
                        Long localCatId = rt.categoryId != null ? catSupaToLocal.get(rt.categoryId) : null;
                        Long localToCardId = rt.toCardId != null ? cardSupaToLocal.get(rt.toCardId) : null;

                        if (existing != null && existing.getPendingSync() == 0) {
                            // Update from remote only if not locally modified
                            if (localCardId != null) existing.setCardId(localCardId);
                            existing.setCategoryId(localCatId);
                            existing.setAmount(rt.amount);
                            existing.setType(rt.type);
                            existing.setToCardId(localToCardId);
                            existing.setDescription(rt.description);
                            existing.setDate(rt.date);
                            existing.setCreatedAt(rt.createdAt);
                            existing.setPendingSync(0);
                            db.transactionDao().update(existing);
                        } else if (existing == null) {
                            Transaction tx = rt.toEntity();
                            if (localCardId != null) tx.setCardId(localCardId);
                            else tx.setCardId(rt.cardId); // fallback
                            tx.setCategoryId(localCatId);
                            tx.setToCardId(localToCardId);
                            db.transactionDao().insert(tx);
                            result.downloadedTransactions++;
                        }
                        // else: existing with pendingSync > 0 => keep local version
                    }
                    if (!remoteIds.isEmpty()) db.transactionDao().deleteObsoleteTransactions(remoteIds);
                    // Sync transaction tags
                    syncTransactionTagsSmartMerge(remoteTxs, onDone);
                });
            }
            public void onError(String m) { Log.e(TAG, "syncTransactions: " + m); onDone.run(); }
        });
    }

    private void syncTransactionTagsSmartMerge(List<RemoteTransaction> remoteTxs, Runnable onDone) {
        if (remoteTxs.isEmpty()) { onDone.run(); return; }
        // Build supabase tag id -> local tag id map
        Map<Long, Long> tagSupaToLocal = new HashMap<>();
        for (Tag t : db.tagDao().getAllTagsSync())
            if (t.getSupabaseId() > 0) tagSupaToLocal.put(t.getSupabaseId(), t.getId());

        AtomicInteger rem = new AtomicInteger(remoteTxs.size());
        for (RemoteTransaction rt : remoteTxs) {
            if (rt.id == null) { if (rem.decrementAndGet() == 0) onDone.run(); continue; }
            Transaction local = db.transactionDao().getBySupabaseId(rt.id);
            if (local == null) { if (rem.decrementAndGet() == 0) onDone.run(); continue; }
            // Fetch remote tags for this transaction
            remote.getTransactionTags(rt.id, new RemoteDataSource.Callback<List<RemoteTransactionTag>>() {
                public void onSuccess(List<RemoteTransactionTag> remoteTags) {
                    AppDatabase.databaseWriteExecutor.execute(() -> {
                        db.transactionTagDao().deleteByTransaction(local.getId());
                        for (RemoteTransactionTag rtt : remoteTags) {
                            Long localTagId = tagSupaToLocal.get(rtt.tagId);
                            if (localTagId != null) {
                                db.transactionTagDao().insert(new TransactionTag(local.getId(), localTagId));
                            }
                        }
                        if (rem.decrementAndGet() == 0) onDone.run();
                    });
                }
                public void onError(String m) {
                    Log.e(TAG, "syncTxTags: " + m);
                    if (rem.decrementAndGet() == 0) onDone.run();
                }
            });
        }
    }

    private void syncBills(String familyId, SyncResult result, Runnable onDone) {
        remote.getBills(familyId, new RemoteDataSource.Callback<List<RemoteBill>>() {
            public void onSuccess(List<RemoteBill> remoteBills) {
                AppDatabase.databaseWriteExecutor.execute(() -> {
                    // Build card FK map
                    Map<Long, Long> cardSupaToLocal = new HashMap<>();
                    for (BankCard c : db.bankCardDao().getAllCardsSync())
                        if (c.getSupabaseId() > 0) cardSupaToLocal.put(c.getSupabaseId(), c.getId());

                    db.billDao().deleteAll();
                    for (RemoteBill rb : remoteBills) {
                        Bill b = rb.toEntity();
                        if (rb.cardId != null) {
                            Long localCardId = cardSupaToLocal.get(rb.cardId);
                            if (localCardId != null) b.setCardId(localCardId);
                        }
                        db.billDao().insert(b);
                        result.downloadedBills++;
                    }
                    onDone.run();
                });
            }
            public void onError(String m) { Log.e(TAG, "syncBills: " + m); onDone.run(); }
        });
    }

    private void syncTransfers(String familyId, SyncResult result, Runnable onDone) {
        remote.getTransfers(familyId, new RemoteDataSource.Callback<List<RemoteTransfer>>() {
            public void onSuccess(List<RemoteTransfer> remoteTransfers) {
                AppDatabase.databaseWriteExecutor.execute(() -> {
                    Map<Long, Long> cardSupaToLocal = new HashMap<>();
                    for (BankCard c : db.bankCardDao().getAllCardsSync())
                        if (c.getSupabaseId() > 0) cardSupaToLocal.put(c.getSupabaseId(), c.getId());

                    db.transferDao().deleteAll();
                    for (RemoteTransfer rt : remoteTransfers) {
                        Transfer t = rt.toEntity();
                        Long localFrom = cardSupaToLocal.get(rt.fromCardId);
                        Long localTo = cardSupaToLocal.get(rt.toCardId);
                        if (localFrom != null) t.setFromCardId(localFrom);
                        if (localTo != null) t.setToCardId(localTo);
                        db.transferDao().insert(t);
                        result.downloadedTransfers++;
                    }
                    onDone.run();
                });
            }
            public void onError(String m) { Log.e(TAG, "syncTransfers: " + m); onDone.run(); }
        });
    }

    // ======== Balance recalculation ========

    /**
     * Recalculates the balance of every card directly from transactions:
     * balance = SUM(income) - SUM(expense) - SUM(transfer_out) + SUM(transfer_in)
     * Called after sync completes to ensure local balances are consistent across devices.
     */
    private void recalculateAllCardBalances() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            // Single SQL UPDATE recalculates all cards at once
            db.bankCardDao().recalculateAllBalancesFromTransactions();
            Log.d(TAG, "recalculateAllCardBalances: recalculated all cards from transactions");

            // Push updated balances to Supabase
            if (NetworkMonitor.getInstance().isOnline()) {
                List<BankCard> allCards = db.bankCardDao().getAllCardsSync();
                for (BankCard card : allCards) {
                    if (card.getSupabaseId() > 0) {
                        Map<String, Object> upd = new HashMap<>();
                        upd.put("balance", card.getBalance());
                        RemoteDataSource.getInstance().updateBankCard(card.getSupabaseId(), upd,
                                new RemoteDataSource.Callback<Void>() {
                                    public void onSuccess(Void v) {}
                                    public void onError(String m) {}
                                });
                    }
                }
            }
        });
    }

    // ======== Auto-sync trigger (debounced) ========

    private static final long SYNC_DEBOUNCE_MS = 2000;
    private final android.os.Handler syncHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable pendingSyncRunnable;

    /**
     * Triggers a debounced auto-sync. Call after every local create/edit/delete.
     * Waits 2 seconds before syncing to batch rapid mutations.
     */
    public void triggerAutoSync() {
        if (pendingSyncRunnable != null) {
            syncHandler.removeCallbacks(pendingSyncRunnable);
        }
        pendingSyncRunnable = () -> {
            if (!manualSyncInProgress) {
                syncAll();
            }
            pendingSyncRunnable = null;
        };
        syncHandler.postDelayed(pendingSyncRunnable, SYNC_DEBOUNCE_MS);
    }
}
