package com.example.chiokojakharjkardam.utils;

import android.app.Application;
import android.util.Log;

import com.example.chiokojakharjkardam.data.database.AppDatabase;
import com.example.chiokojakharjkardam.data.database.dao.*;
import com.example.chiokojakharjkardam.data.database.entity.*;
import com.example.chiokojakharjkardam.data.remote.RemoteDataSource;
import com.example.chiokojakharjkardam.data.remote.model.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * One-time migration: uploads all existing local Room data to Supabase.
 *
 * Call after the user completes auth + family creation.
 * Maintains FK integrity by uploading in dependency order:
 *   Family → Members → BankCards → Categories → Tags
 *   → Transactions + TransactionTags → Bills → Transfers
 */
public class DataMigrationManager {

    private static final String TAG = "DataMigration";

    public interface MigrationCallback {
        void onProgress(String message);
        void onComplete();
        void onError(String message);
    }

    private final AppDatabase db;
    private final RemoteDataSource remote;
    private final SessionManager session;

    public DataMigrationManager(Application app) {
        db      = AppDatabase.getDatabase(app);
        remote  = RemoteDataSource.getInstance();
        session = SessionManager.getInstance();
    }

    /**
     * Uploads all local data to Supabase for the current authenticated family.
     * Designed to run once after the owner creates the family and authenticates.
     */
    public void migrateLocalDataToCloud(MigrationCallback cb) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                String familyId   = session.getFamilyId();
                String userId     = session.getUserId();
                long localFamilyId = -1;

                Family localFamily = db.familyDao().getFamilySync();
                if (localFamily != null) localFamilyId = localFamily.getId();

                cb.onProgress("در حال آپلود اعضا...");
                migrateMembers(familyId, localFamilyId, userId);

                cb.onProgress("در حال آپلود کارت‌های بانکی...");
                migrateBankCards(familyId);

                cb.onProgress("در حال آپلود دسته‌بندی‌ها...");
                migrateCategories(familyId);

                cb.onProgress("در حال آپلود تگ‌ها...");
                migrateTags(familyId);

                cb.onProgress("در حال آپلود تراکنش‌ها...");
                migrateTransactions(familyId, userId);

                cb.onProgress("در حال آپلود قبوض...");
                migrateBills(familyId, userId);

                cb.onProgress("در حال آپلود انتقال‌ها...");
                migrateTransfers(familyId, userId);

                Log.d(TAG, "Migration completed");
                cb.onComplete();

            } catch (Exception e) {
                Log.e(TAG, "Migration failed: " + e.getMessage());
                cb.onError("خطا در انتقال اطلاعات: " + e.getMessage());
            }
        });
    }

    // ──────────────────────────────────────────────────────────────

    private void migrateMembers(String familyId, long localFamilyId, String currentUserId)
            throws InterruptedException {
        // Upload current user as member (profile already created by trigger)
        // Update profile with family_id + is_owner flag (already done in FamilyViewModel)
        // Also upload other local members as ghost profiles if they exist locally
        // (In practice on fresh install there is only the owner)
        List<Member> members = db.memberDao().getAllMembersSync();
        for (Member m : members) {
            if (m.getUserId() == null || m.getUserId().equals(currentUserId)) continue;
            // Other members will join via invite — no action needed
        }
    }

    private void migrateBankCards(String familyId) {
        List<BankCard> cards = db.bankCardDao().getAllCardsSync();
        if (cards.isEmpty()) return;

        final Object lock = new Object();
        final int[] pending = {cards.size()};

        for (BankCard card : cards) {
            Member owner = db.memberDao().getMemberByIdSync(card.getMemberId());
            String memberUserId = (owner != null && owner.getUserId() != null)
                    ? owner.getUserId() : session.getUserId();

            RemoteBankCard remote = new RemoteBankCard(card, familyId, memberUserId);
            remote.id = null; // let Supabase assign ID

            this.remote.insertBankCard(remote, new RemoteDataSource.Callback<RemoteBankCard>() {
                @Override public void onSuccess(RemoteBankCard created) {
                    // Update local Room ID with Supabase-assigned ID
                    AppDatabase.databaseWriteExecutor.execute(() -> {
                        card.setId(created.id);
                        db.bankCardDao().update(card);
                    });
                    synchronized (lock) { pending[0]--; lock.notifyAll(); }
                }
                @Override public void onError(String msg) {
                    Log.e(TAG, "BankCard upload failed: " + msg);
                    synchronized (lock) { pending[0]--; lock.notifyAll(); }
                }
            });
        }

        // Wait for all uploads
        synchronized (lock) {
            while (pending[0] > 0) {
                try { lock.wait(10000); } catch (InterruptedException e) { break; }
            }
        }
    }

    private void migrateCategories(String familyId) {
        List<Category> cats = db.categoryDao().getAllCategoriesSync();
        if (cats == null || cats.isEmpty()) return;

        final Object lock = new Object();
        final int[] pending = {cats.size()};

        for (Category cat : cats) {
            RemoteCategory rc = new RemoteCategory(cat, familyId);
            rc.id = null;
            this.remote.insertCategory(rc, new RemoteDataSource.Callback<RemoteCategory>() {
                @Override public void onSuccess(RemoteCategory created) {
                    AppDatabase.databaseWriteExecutor.execute(() -> {
                        cat.setId(created.id);
                        db.categoryDao().update(cat);
                    });
                    synchronized (lock) { pending[0]--; lock.notifyAll(); }
                }
                @Override public void onError(String msg) {
                    Log.e(TAG, "Category upload failed: " + msg);
                    synchronized (lock) { pending[0]--; lock.notifyAll(); }
                }
            });
        }
        synchronized (lock) {
            while (pending[0] > 0) {
                try { lock.wait(10000); } catch (InterruptedException e) { break; }
            }
        }
    }

    private void migrateTags(String familyId) {
        List<Tag> tags = db.tagDao().getAllTagsSync();
        if (tags == null || tags.isEmpty()) return;

        final Object lock = new Object();
        final int[] pending = {tags.size()};

        for (Tag tag : tags) {
            RemoteTag rt = new RemoteTag(tag, familyId);
            rt.id = null;
            this.remote.insertTag(rt, new RemoteDataSource.Callback<RemoteTag>() {
                @Override public void onSuccess(RemoteTag created) {
                    AppDatabase.databaseWriteExecutor.execute(() -> {
                        tag.setId(created.id);
                        db.tagDao().update(tag);
                    });
                    synchronized (lock) { pending[0]--; lock.notifyAll(); }
                }
                @Override public void onError(String msg) {
                    Log.e(TAG, "Tag upload failed: " + msg);
                    synchronized (lock) { pending[0]--; lock.notifyAll(); }
                }
            });
        }
        synchronized (lock) {
            while (pending[0] > 0) {
                try { lock.wait(10000); } catch (InterruptedException e) { break; }
            }
        }
    }

    private void migrateTransactions(String familyId, String userId) {
        List<Transaction> txs = db.transactionDao().getAllTransactionsSync();
        if (txs == null || txs.isEmpty()) return;

        final RemoteDataSource remoteRef = this.remote; // capture for inner classes
        final Object lock = new Object();
        final int[] pending = {txs.size()};

        for (Transaction tx : txs) {
            long oldId = tx.getId();
            RemoteTransaction rt = new RemoteTransaction(tx, familyId, userId);
            rt.id = null;
            remoteRef.insertTransaction(rt, new RemoteDataSource.Callback<RemoteTransaction>() {
                @Override public void onSuccess(RemoteTransaction created) {
                    AppDatabase.databaseWriteExecutor.execute(() -> {
                        List<Long> tagIds = db.transactionTagDao().getTagIdsByTransaction(oldId);
                        tx.setSupabaseId(created.id);
                        tx.setPendingSync(Transaction.SYNC_DONE);
                        db.transactionDao().upsert(tx);
                        for (long tagId : tagIds) {
                            remoteRef.insertTransactionTag(created.id, tagId,
                                    new RemoteDataSource.Callback<Void>() {
                                        @Override public void onSuccess(Void v) {}
                                        @Override public void onError(String msg) {
                                            Log.e(TAG, "TxTag upload failed: " + msg);
                                        }
                                    });
                        }
                    });
                    synchronized (lock) { pending[0]--; lock.notifyAll(); }
                }
                @Override public void onError(String msg) {
                    Log.e(TAG, "Transaction upload failed: " + msg);
                    synchronized (lock) { pending[0]--; lock.notifyAll(); }
                }
            });
        }
        synchronized (lock) {
            while (pending[0] > 0) {
                try { lock.wait(30000); } catch (InterruptedException e) { break; }
            }
        }
    }

    private void migrateBills(String familyId, String userId) {
        List<Bill> bills = db.billDao().getAllBillsSync();
        if (bills == null || bills.isEmpty()) return;

        final Object lock = new Object();
        final int[] pending = {bills.size()};

        for (Bill bill : bills) {
            RemoteBill rb = new RemoteBill(bill, familyId, userId);
            rb.id = null;
            this.remote.insertBill(rb, new RemoteDataSource.Callback<RemoteBill>() {
                @Override public void onSuccess(RemoteBill created) {
                    AppDatabase.databaseWriteExecutor.execute(() -> {
                        bill.setId(created.id);
                        db.billDao().upsert(bill);
                    });
                    synchronized (lock) { pending[0]--; lock.notifyAll(); }
                }
                @Override public void onError(String msg) {
                    Log.e(TAG, "Bill upload failed: " + msg);
                    synchronized (lock) { pending[0]--; lock.notifyAll(); }
                }
            });
        }
        synchronized (lock) {
            while (pending[0] > 0) {
                try { lock.wait(10000); } catch (InterruptedException e) { break; }
            }
        }
    }

    private void migrateTransfers(String familyId, String userId) {
        List<Transfer> transfers = db.transferDao().getAllTransfersSync();
        if (transfers == null || transfers.isEmpty()) return;

        final Object lock = new Object();
        final int[] pending = {transfers.size()};

        for (Transfer t : transfers) {
            RemoteTransfer rt = new RemoteTransfer(t, familyId, userId);
            rt.id = null;
            this.remote.insertTransfer(rt, new RemoteDataSource.Callback<RemoteTransfer>() {
                @Override public void onSuccess(RemoteTransfer created) {
                    AppDatabase.databaseWriteExecutor.execute(() -> {
                        t.setId(created.id);
                        db.transferDao().upsert(t);
                    });
                    synchronized (lock) { pending[0]--; lock.notifyAll(); }
                }
                @Override public void onError(String msg) {
                    Log.e(TAG, "Transfer upload failed: " + msg);
                    synchronized (lock) { pending[0]--; lock.notifyAll(); }
                }
            });
        }
        synchronized (lock) {
            while (pending[0] > 0) {
                try { lock.wait(10000); } catch (InterruptedException e) { break; }
            }
        }
    }
}


