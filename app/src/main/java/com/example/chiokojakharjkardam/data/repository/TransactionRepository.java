package com.example.chiokojakharjkardam.data.repository;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.lifecycle.LiveData;

import com.example.chiokojakharjkardam.data.database.AppDatabase;
import com.example.chiokojakharjkardam.data.database.dao.BankCardDao;
import com.example.chiokojakharjkardam.data.database.dao.TagDao;
import com.example.chiokojakharjkardam.data.database.dao.TransactionDao;
import com.example.chiokojakharjkardam.data.database.dao.TransactionTagDao;
import com.example.chiokojakharjkardam.data.database.dao.PendingDeleteDao;
import com.example.chiokojakharjkardam.data.database.entity.BankCard;
import com.example.chiokojakharjkardam.data.database.entity.CategoryReport;
import com.example.chiokojakharjkardam.data.database.entity.CombinedReport;
import com.example.chiokojakharjkardam.data.database.entity.PendingDelete;
import com.example.chiokojakharjkardam.data.database.entity.Tag;
import com.example.chiokojakharjkardam.data.database.entity.TagReport;
import com.example.chiokojakharjkardam.data.database.entity.Transaction;
import com.example.chiokojakharjkardam.data.database.entity.TransactionTag;
import com.example.chiokojakharjkardam.data.remote.RemoteDataSource;
import com.example.chiokojakharjkardam.data.remote.model.RemoteTransaction;
import com.example.chiokojakharjkardam.utils.NetworkMonitor;
import com.example.chiokojakharjkardam.utils.SessionManager;
import com.example.chiokojakharjkardam.utils.SyncManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TransactionRepository {

    private final TransactionDao transactionDao;
    private final TransactionTagDao transactionTagDao;
    private final BankCardDao bankCardDao;
    private final TagDao tagDao;
    private final PendingDeleteDao pendingDeleteDao;
    private final Application application;
    private final RemoteDataSource remote;
    private final SessionManager session;

    public TransactionRepository(Application application) {
        this.application = application;
        AppDatabase db = AppDatabase.getDatabase(application);
        transactionDao    = db.transactionDao();
        transactionTagDao = db.transactionTagDao();
        bankCardDao       = db.bankCardDao();
        tagDao            = db.tagDao();
        pendingDeleteDao  = db.pendingDeleteDao();
        remote  = RemoteDataSource.getInstance();
        session = SessionManager.getInstance();
    }

    /**
     * ثبت تراکنش جدید همراه با به‌روزرسانی موجودی کارت (offline-first)
     * همیشه ابتدا در Room ذخیره می‌شود؛ اگر آنلاین بود، فوری به Supabase ارسال می‌شود.
     */
    public void insertWithBalanceUpdate(Transaction transaction, List<Long> tagIds,
                                         OnTransactionInsertedListener listener,
                                         OnTransactionErrorListener errorListener) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            // بررسی موجودی از Room (بدون نیاز به اینترنت)
            if (transaction.getType() == Transaction.TYPE_EXPENSE
                    || transaction.getType() == Transaction.TYPE_TRANSFER) {
                BankCard card = bankCardDao.getCardByIdSync(transaction.getCardId());
                if (card != null && card.getBalance() < transaction.getAmount()) {
                    if (errorListener != null)
                        errorListener.onError("موجودی کارت کافی نیست. موجودی فعلی: " + card.getBalance());
                    return;
                }
                if (transaction.getType() == Transaction.TYPE_TRANSFER
                        && (transaction.getToCardId() == null
                            || transaction.getToCardId() == transaction.getCardId())) {
                    if (errorListener != null)
                        errorListener.onError("کارت مقصد باید متفاوت از کارت مبدا باشد");
                    return;
                }
            }

            // ۱. ذخیره در Room (offline-first) با وضعیت SYNC_NEW
            transaction.setPendingSync(Transaction.SYNC_NEW);
            long localId = transactionDao.insert(transaction);
            transaction.setId(localId);

            // ۲. ذخیره تگ‌ها در Room
            if (tagIds != null && !tagIds.isEmpty()) {
                List<TransactionTag> txTags = new ArrayList<>();
                for (Long tagId : tagIds) txTags.add(new TransactionTag(localId, tagId));
                transactionTagDao.insertAll(txTags);
            }

            // ۳. به‌روزرسانی موجودی محلی با محاسبه مجدد از تراکنش‌ها
            bankCardDao.recalculateBalanceFromTransactions(transaction.getCardId());
            if (transaction.getType() == Transaction.TYPE_TRANSFER) {
                bankCardDao.recalculateBalanceFromTransactions(transaction.getToCardId());
            }

            // ۴. اطلاع‌رسانی به UI
            if (listener != null) listener.onTransactionInserted(localId);

            // Auto-sync trigger
            SyncManager.getInstance().triggerAutoSync();

            // ۵. اگر آنلاین بود، فوراً به Supabase ارسال کن
            if (NetworkMonitor.getInstance().isOnline() && session.hasFamilyId()) {
                RemoteTransaction rt = new RemoteTransaction(transaction, session.getFamilyId(), session.getUserId());
                rt.id = null;
                // Remap local Room FK IDs to Supabase IDs
                BankCard card = bankCardDao.getCardByIdSync(transaction.getCardId());
                if (card != null && card.getSupabaseId() > 0) rt.cardId = card.getSupabaseId();
                if (transaction.getCategoryId() != null) {
                    com.example.chiokojakharjkardam.data.database.entity.Category cat =
                            AppDatabase.getDatabase(application).categoryDao().getCategoryByIdSync(transaction.getCategoryId());
                    if (cat != null && cat.getSupabaseId() > 0) rt.categoryId = cat.getSupabaseId();
                }
                if (transaction.getToCardId() != null) {
                    BankCard toCard = bankCardDao.getCardByIdSync(transaction.getToCardId());
                    if (toCard != null && toCard.getSupabaseId() > 0) rt.toCardId = toCard.getSupabaseId();
                }
                remote.insertTransaction(rt, new RemoteDataSource.Callback<RemoteTransaction>() {
                    @Override public void onSuccess(RemoteTransaction created) {
                        AppDatabase.databaseWriteExecutor.execute(() -> {
                            transactionDao.updateSupabaseId(localId, created.id);
                            // ارسال تگ‌ها به Supabase
                            if (tagIds != null) {
                                for (Long tagId : tagIds) {
                                    Tag tag = tagDao.getTagByIdSync(tagId);
                                    long supabaseTagId = (tag != null && tag.getSupabaseId() > 0)
                                            ? tag.getSupabaseId() : 0;
                                    if (supabaseTagId > 0) {
                                        remote.insertTransactionTag(created.id, supabaseTagId,
                                                new RemoteDataSource.Callback<Void>() {
                                                    @Override public void onSuccess(Void v) {}
                                                    @Override public void onError(String m) {}
                                                });
                                    }
                                }
                            }
                            syncCardBalanceRemote(transaction.getCardId());
                            if (transaction.getType() == Transaction.TYPE_TRANSFER) {
                                syncCardBalanceRemote(transaction.getToCardId());
                            }
                        });
                    }
                    @Override public void onError(String msg) {
                        // تراکنش در Room ذخیره شده با SYNC_NEW؛ SyncManager بعداً آپلود می‌کند
                    }
                });
            }
        });
    }

    private void syncCardBalanceRemote(long cardId) {
        BankCard card = bankCardDao.getCardByIdSync(cardId);
        if (card == null || card.getSupabaseId() == 0) return;
        Map<String, Object> upd = new HashMap<>();
        upd.put("balance", card.getBalance());
        remote.updateBankCard(card.getSupabaseId(), upd, new RemoteDataSource.Callback<Void>() {
            @Override public void onSuccess(Void v) {}
            @Override public void onError(String m) {}
        });
    }

    public void update(Transaction transaction, List<Long> tagIds,
                       OnTransactionUpdatedListener listener,
                       OnTransactionErrorListener errorListener) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            Transaction oldTransaction = transactionDao.getTransactionByIdSync(transaction.getId());

            if (oldTransaction != null) {
                // بررسی موجودی برای تراکنش جدید بدون تغییر موقت در DB
                if (transaction.getType() == Transaction.TYPE_EXPENSE
                        || transaction.getType() == Transaction.TYPE_TRANSFER) {
                    BankCard card = bankCardDao.getCardByIdSync(transaction.getCardId());
                    if (card != null) {
                        long available = card.getBalance();
                        // اگر کارت همان کارت قبلی است، اثر تراکنش قدیمی را از موجودی حذف می‌کنیم
                        if (transaction.getCardId() == oldTransaction.getCardId()) {
                            if (oldTransaction.getType() == Transaction.TYPE_INCOME) {
                                available -= oldTransaction.getAmount();
                            } else if (oldTransaction.getType() == Transaction.TYPE_EXPENSE
                                    || oldTransaction.getType() == Transaction.TYPE_TRANSFER) {
                                available += oldTransaction.getAmount();
                            }
                        }
                        if (available < transaction.getAmount()) {
                            if (errorListener != null)
                                errorListener.onError("موجودی کارت کافی نیست. موجودی فعلی: " + card.getBalance());
                            return;
                        }
                    }
                }
            }

            // حفظ supabaseId از رکورد قدیمی
            if (oldTransaction != null && oldTransaction.getSupabaseId() != null) {
                transaction.setSupabaseId(oldTransaction.getSupabaseId());
            }

            // ذخیره در Room
            if (NetworkMonitor.getInstance().isOnline() && transaction.getSupabaseId() != null) {
                transaction.setPendingSync(Transaction.SYNC_DONE);
            } else {
                transaction.setPendingSync(Transaction.SYNC_UPDATE);
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

            // محاسبه مجدد موجودی از تراکنش‌ها (جایگزین updateBalance دلتایی)
            bankCardDao.recalculateBalanceFromTransactions(transaction.getCardId());
            if (oldTransaction != null && oldTransaction.getCardId() != transaction.getCardId()) {
                bankCardDao.recalculateBalanceFromTransactions(oldTransaction.getCardId());
            }
            if (transaction.getType() == Transaction.TYPE_TRANSFER && transaction.getToCardId() != null) {
                bankCardDao.recalculateBalanceFromTransactions(transaction.getToCardId());
            }
            if (oldTransaction != null && oldTransaction.getType() == Transaction.TYPE_TRANSFER
                    && oldTransaction.getToCardId() != null
                    && !oldTransaction.getToCardId().equals(transaction.getToCardId())) {
                bankCardDao.recalculateBalanceFromTransactions(oldTransaction.getToCardId());
            }

            if (listener != null) listener.onTransactionUpdated();

            // Auto-sync trigger
            SyncManager.getInstance().triggerAutoSync();
            if (NetworkMonitor.getInstance().isOnline() && transaction.getSupabaseId() != null) {
                // Remap local category ID to Supabase ID
                Long supabaseCatId = null;
                if (transaction.getCategoryId() != null) {
                    com.example.chiokojakharjkardam.data.database.entity.Category cat =
                            AppDatabase.getDatabase(application).categoryDao().getCategoryByIdSync(transaction.getCategoryId());
                    if (cat != null && cat.getSupabaseId() > 0) supabaseCatId = cat.getSupabaseId();
                }
                // Remap local card ID to Supabase ID
                long supabaseCardId = transaction.getCardId();
                BankCard cardForSync = bankCardDao.getCardByIdSync(transaction.getCardId());
                if (cardForSync != null && cardForSync.getSupabaseId() > 0) supabaseCardId = cardForSync.getSupabaseId();
                Long supabaseToCardId = null;
                if (transaction.getToCardId() != null) {
                    BankCard toCardForSync = bankCardDao.getCardByIdSync(transaction.getToCardId());
                    if (toCardForSync != null && toCardForSync.getSupabaseId() > 0) supabaseToCardId = toCardForSync.getSupabaseId();
                }

                Map<String, Object> upd = new HashMap<>();
                upd.put("amount", transaction.getAmount());
                upd.put("type", transaction.getType());
                upd.put("description", transaction.getDescription());
                upd.put("date", transaction.getDate());
                upd.put("category_id", supabaseCatId);
                upd.put("card_id", supabaseCardId);
                upd.put("to_card_id", supabaseToCardId);
                remote.updateTransaction(transaction.getSupabaseId(), upd,
                        new RemoteDataSource.Callback<Void>() {
                            @Override public void onSuccess(Void v) {
                                syncCardBalanceRemote(transaction.getCardId());
                                if (transaction.getType() == Transaction.TYPE_TRANSFER
                                        && transaction.getToCardId() != null) {
                                    syncCardBalanceRemote(transaction.getToCardId());
                                }
                            }
                            @Override public void onError(String msg) {
                                AppDatabase.databaseWriteExecutor.execute(() ->
                                        transactionDao.updateSyncStatus(
                                                transaction.getId(), Transaction.SYNC_UPDATE));
                            }
                        });
            }
        });
    }

    public void delete(Transaction transaction) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            // حذف تراکنش اول، سپس محاسبه مجدد موجودی از تراکنش‌ها
            transactionDao.delete(transaction);

            bankCardDao.recalculateBalanceFromTransactions(transaction.getCardId());
            if (transaction.getType() == Transaction.TYPE_TRANSFER && transaction.getToCardId() != null) {
                bankCardDao.recalculateBalanceFromTransactions(transaction.getToCardId());
            }

            // Auto-sync trigger
            SyncManager.getInstance().triggerAutoSync();

            String supabaseId = transaction.getSupabaseId();
            if (supabaseId != null) {
                if (NetworkMonitor.getInstance().isOnline()) {
                    remote.deleteTransaction(supabaseId, new RemoteDataSource.Callback<Void>() {
                        @Override public void onSuccess(Void v) {
                            syncCardBalanceRemote(transaction.getCardId());
                        }
                        @Override public void onError(String m) {
                            // ذخیره در صف حذف برای بعد
                            AppDatabase.databaseWriteExecutor.execute(() ->
                                    pendingDeleteDao.insert(new PendingDelete(
                                            PendingDelete.TYPE_TRANSACTION, supabaseId)));
                        }
                    });
                } else {
                    // آفلاین: ثبت در صف حذف
                    pendingDeleteDao.insert(new PendingDelete(PendingDelete.TYPE_TRANSACTION, supabaseId));
                }
            }
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

    public LiveData<Long> getTotalTransferInRange(long startDate, long endDate) {
        return transactionDao.getTotalTransferByDateRange(startDate, endDate);
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





