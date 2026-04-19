package com.example.chiokojakharjkardam.data.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.chiokojakharjkardam.data.database.entity.CategoryReport;
import com.example.chiokojakharjkardam.data.database.entity.CombinedReport;
import com.example.chiokojakharjkardam.data.database.entity.TagReport;
import com.example.chiokojakharjkardam.data.database.entity.Transaction;

import java.util.List;

@Dao
public interface TransactionDao {

    @Insert
    long insert(Transaction transaction);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long upsert(Transaction transaction);

    @Update
    void update(Transaction transaction);

    @Delete
    void delete(Transaction transaction);

    @Query("DELETE FROM transactions")
    void deleteAll();

    // ── Offline-sync helpers ──────────────────────────────────────

    @Query("SELECT * FROM transactions WHERE supabaseId = :supabaseId LIMIT 1")
    Transaction getBySupabaseId(String supabaseId);

    @Query("SELECT * FROM transactions WHERE pendingSync > 0")
    List<Transaction> getPendingTransactions();

    @Query("UPDATE transactions SET supabaseId = :supabaseId, pendingSync = 0 WHERE id = :localId")
    void updateSupabaseId(long localId, String supabaseId);

    @Query("UPDATE transactions SET pendingSync = :status WHERE id = :localId")
    void updateSyncStatus(long localId, int status);

    /**
     * Deletes synced transactions whose supabaseId is no longer in the remote set.
     * Used by smart-merge pull to remove records deleted on Supabase by other devices.
     */
    @Query("DELETE FROM transactions WHERE supabaseId IS NOT NULL AND pendingSync = 0 AND supabaseId NOT IN (:remoteIds)")
    void deleteObsoleteTransactions(List<String> remoteIds);

    @Query("SELECT * FROM transactions ORDER BY date DESC, createdAt DESC")
    LiveData<List<Transaction>> getAllTransactions();

    @Query("SELECT * FROM transactions ORDER BY date DESC, createdAt DESC")
    List<Transaction> getAllTransactionsSync();

    @Query("SELECT * FROM transactions WHERE cardId = :cardId ORDER BY date DESC")
    LiveData<List<Transaction>> getTransactionsByCard(long cardId);

    @Query("SELECT * FROM transactions WHERE categoryId = :categoryId ORDER BY date DESC")
    LiveData<List<Transaction>> getTransactionsByCategory(long categoryId);

    @Query("SELECT * FROM transactions WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    LiveData<List<Transaction>> getTransactionsByDateRange(long startDate, long endDate);

    @Query("SELECT * FROM transactions WHERE id = :id")
    LiveData<Transaction> getTransactionById(long id);

    @Query("SELECT * FROM transactions WHERE id = :id")
    Transaction getTransactionByIdSync(long id);

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 0 AND date BETWEEN :startDate AND :endDate")
    LiveData<Long> getTotalExpenseByDateRange(long startDate, long endDate);

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 1 AND date BETWEEN :startDate AND :endDate")
    LiveData<Long> getTotalIncomeByDateRange(long startDate, long endDate);

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 0")
    LiveData<Long> getTotalExpense();

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 1")
    LiveData<Long> getTotalIncome();

    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE type = 2 AND date BETWEEN :startDate AND :endDate")
    LiveData<Long> getTotalTransferByDateRange(long startDate, long endDate);

    @Query("SELECT * FROM transactions ORDER BY date DESC LIMIT :limit")
    LiveData<List<Transaction>> getRecentTransactions(int limit);

    @Query("SELECT COUNT(*) FROM transactions")
    int getTransactionCount();

    // ==================== گزارش‌گیری ====================

    /**
     * گزارش مجموع هزینه‌ها بر اساس دسته‌بندی در بازه زمانی
     */
    @Query("SELECT t.categoryId as categoryId, c.name as categoryName, c.color as categoryColor, " +
            "c.icon as categoryIcon, SUM(t.amount) as totalAmount, COUNT(DISTINCT t.id) as transactionCount " +
            "FROM transactions t " +
            "LEFT JOIN categories c ON t.categoryId = c.id " +
            "WHERE t.type = 0 AND t.date BETWEEN :startDate AND :endDate " +
            "GROUP BY t.categoryId " +
            "ORDER BY totalAmount DESC")
    LiveData<List<CategoryReport>> getExpenseReportByCategory(long startDate, long endDate);

    /**
     * گزارش مجموع درآمدها بر اساس دسته‌بندی در بازه زمانی
     */
    @Query("SELECT t.categoryId as categoryId, c.name as categoryName, c.color as categoryColor, " +
            "c.icon as categoryIcon, SUM(t.amount) as totalAmount, COUNT(DISTINCT t.id) as transactionCount " +
            "FROM transactions t " +
            "LEFT JOIN categories c ON t.categoryId = c.id " +
            "WHERE t.type = 1 AND t.date BETWEEN :startDate AND :endDate " +
            "GROUP BY t.categoryId " +
            "ORDER BY totalAmount DESC")
    LiveData<List<CategoryReport>> getIncomeReportByCategory(long startDate, long endDate);

    /**
     * گزارش مجموع هزینه‌ها بر اساس تگ در بازه زمانی
     */
    @Query("SELECT tg.id as tagId, tg.name as tagName, tg.color as tagColor, " +
            "SUM(t.amount) as totalAmount, COUNT(DISTINCT t.id) as transactionCount " +
            "FROM transactions t " +
            "INNER JOIN transaction_tags tt ON t.id = tt.transactionId " +
            "INNER JOIN tags tg ON tt.tagId = tg.id " +
            "WHERE t.type = 0 AND t.date BETWEEN :startDate AND :endDate " +
            "GROUP BY tg.id " +
            "ORDER BY totalAmount DESC")
    LiveData<List<TagReport>> getExpenseReportByTag(long startDate, long endDate);

    /**
     * گزارش مجموع درآمدها بر اساس تگ در بازه زمانی
     */
    @Query("SELECT tg.id as tagId, tg.name as tagName, tg.color as tagColor, " +
            "SUM(t.amount) as totalAmount, COUNT(DISTINCT t.id) as transactionCount " +
            "FROM transactions t " +
            "INNER JOIN transaction_tags tt ON t.id = tt.transactionId " +
            "INNER JOIN tags tg ON tt.tagId = tg.id " +
            "WHERE t.type = 1 AND t.date BETWEEN :startDate AND :endDate " +
            "GROUP BY tg.id " +
            "ORDER BY totalAmount DESC")
    LiveData<List<TagReport>> getIncomeReportByTag(long startDate, long endDate);

    /**
     * گزارش ترکیبی هزینه‌ها بر اساس دسته‌بندی و تگ در بازه زمانی
     */
    @Query("SELECT t.categoryId as categoryId, c.name as categoryName, c.color as categoryColor, " +
            "c.icon as categoryIcon, tg.id as tagId, tg.name as tagName, tg.color as tagColor, " +
            "SUM(t.amount) as totalAmount, COUNT(DISTINCT t.id) as transactionCount " +
            "FROM transactions t " +
            "LEFT JOIN categories c ON t.categoryId = c.id " +
            "LEFT JOIN transaction_tags tt ON t.id = tt.transactionId " +
            "LEFT JOIN tags tg ON tt.tagId = tg.id " +
            "WHERE t.type = 0 AND t.date BETWEEN :startDate AND :endDate " +
            "GROUP BY t.categoryId, tg.id " +
            "ORDER BY totalAmount DESC")
    LiveData<List<CombinedReport>> getExpenseReportByCategoryAndTag(long startDate, long endDate);

    /**
     * گزارش ترکیبی درآمدها بر اساس دسته‌بندی و تگ در بازه زمانی
     */
    @Query("SELECT t.categoryId as categoryId, c.name as categoryName, c.color as categoryColor, " +
            "c.icon as categoryIcon, tg.id as tagId, tg.name as tagName, tg.color as tagColor, " +
            "SUM(t.amount) as totalAmount, COUNT(DISTINCT t.id) as transactionCount " +
            "FROM transactions t " +
            "LEFT JOIN categories c ON t.categoryId = c.id " +
            "LEFT JOIN transaction_tags tt ON t.id = tt.transactionId " +
            "LEFT JOIN tags tg ON tt.tagId = tg.id " +
            "WHERE t.type = 1 AND t.date BETWEEN :startDate AND :endDate " +
            "GROUP BY t.categoryId, tg.id " +
            "ORDER BY totalAmount DESC")
    LiveData<List<CombinedReport>> getIncomeReportByCategoryAndTag(long startDate, long endDate);

    /**
     * گزارش هزینه‌ها برای یک دسته‌بندی خاص در بازه زمانی
     */
    @Query("SELECT SUM(amount) FROM transactions " +
            "WHERE type = 0 AND categoryId = :categoryId AND date BETWEEN :startDate AND :endDate")
    LiveData<Long> getExpenseByCategoryInRange(long categoryId, long startDate, long endDate);

    /**
     * گزارش هزینه‌ها برای یک تگ خاص در بازه زمانی
     */
    @Query("SELECT SUM(t.amount) FROM transactions t " +
            "INNER JOIN transaction_tags tt ON t.id = tt.transactionId " +
            "WHERE t.type = 0 AND tt.tagId = :tagId AND t.date BETWEEN :startDate AND :endDate")
    LiveData<Long> getExpenseByTagInRange(long tagId, long startDate, long endDate);

    /**
     * دریافت تراکنش‌های یک دسته‌بندی به صورت همزمان (برای حذف)
     */
    @Query("SELECT * FROM transactions WHERE categoryId = :categoryId")
    List<Transaction> getTransactionsByCategorySync(long categoryId);

    /**
     * دریافت تراکنش‌های مرتبط با یک تگ به صورت همزمان (برای حذف)
     */
    @Query("SELECT t.* FROM transactions t " +
            "INNER JOIN transaction_tags tt ON t.id = tt.transactionId " +
            "WHERE tt.tagId = :tagId")
    List<Transaction> getTransactionsByTagSync(long tagId);

    /**
     * شمارش تراکنش‌های یک دسته‌بندی
     */
    @Query("SELECT COUNT(*) FROM transactions WHERE categoryId = :categoryId")
    int getTransactionCountByCategory(long categoryId);

    /**
     * شمارش تراکنش‌های مرتبط با یک تگ
     */
    @Query("SELECT COUNT(DISTINCT t.id) FROM transactions t " +
            "INNER JOIN transaction_tags tt ON t.id = tt.transactionId " +
            "WHERE tt.tagId = :tagId")
    int getTransactionCountByTag(long tagId);

    /**
     * دریافت تراکنش‌های هزینه در بازه زمانی مشخص
     */
    @Query("SELECT * FROM transactions WHERE type = 0 AND date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    LiveData<List<Transaction>> getExpensesByDateRange(long startDate, long endDate);

    /**
     * دریافت تراکنش‌های درآمد در بازه زمانی مشخص
     */
    @Query("SELECT * FROM transactions WHERE type = 1 AND date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    LiveData<List<Transaction>> getIncomesByDateRange(long startDate, long endDate);

    /**
     * دریافت تراکنش‌های هزینه یک دسته‌بندی در بازه زمانی
     */
    @Query("SELECT * FROM transactions WHERE type = 0 AND categoryId = :categoryId AND date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    LiveData<List<Transaction>> getExpensesByCategoryAndDateRange(long categoryId, long startDate, long endDate);

    /**
     * دریافت تراکنش‌های درآمد یک دسته‌بندی در بازه زمانی
     */
    @Query("SELECT * FROM transactions WHERE type = 1 AND categoryId = :categoryId AND date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    LiveData<List<Transaction>> getIncomesByCategoryAndDateRange(long categoryId, long startDate, long endDate);

    /**
     * دریافت تراکنش‌های هزینه یک تگ در بازه زمانی
     */
    @Query("SELECT t.* FROM transactions t " +
            "INNER JOIN transaction_tags tt ON t.id = tt.transactionId " +
            "WHERE t.type = 0 AND tt.tagId = :tagId AND t.date BETWEEN :startDate AND :endDate ORDER BY t.date DESC")
    LiveData<List<Transaction>> getExpensesByTagAndDateRange(long tagId, long startDate, long endDate);

    /**
     * دریافت تراکنش‌های درآمد یک تگ در بازه زمانی
     */
    @Query("SELECT t.* FROM transactions t " +
            "INNER JOIN transaction_tags tt ON t.id = tt.transactionId " +
            "WHERE t.type = 1 AND tt.tagId = :tagId AND t.date BETWEEN :startDate AND :endDate ORDER BY t.date DESC")
    LiveData<List<Transaction>> getIncomesByTagAndDateRange(long tagId, long startDate, long endDate);

    /**
     * دریافت تراکنش‌های هزینه یک دسته‌بندی و تگ در بازه زمانی
     */
    @Query("SELECT t.* FROM transactions t " +
            "INNER JOIN transaction_tags tt ON t.id = tt.transactionId " +
            "WHERE t.type = 0 AND t.categoryId = :categoryId AND tt.tagId = :tagId AND t.date BETWEEN :startDate AND :endDate ORDER BY t.date DESC")
    LiveData<List<Transaction>> getExpensesByCategoryAndTagAndDateRange(long categoryId, long tagId, long startDate, long endDate);

    /**
     * دریافت تراکنش‌های درآمد یک دسته‌بندی و تگ در بازه زمانی
     */
    @Query("SELECT t.* FROM transactions t " +
            "INNER JOIN transaction_tags tt ON t.id = tt.transactionId " +
            "WHERE t.type = 1 AND t.categoryId = :categoryId AND tt.tagId = :tagId AND t.date BETWEEN :startDate AND :endDate ORDER BY t.date DESC")
    LiveData<List<Transaction>> getIncomesByCategoryAndTagAndDateRange(long categoryId, long tagId, long startDate, long endDate);

    /**
     * دریافت همه تراکنش‌ها (درآمد+خرج) یک دسته‌بندی در بازه زمانی
     */
    @Query("SELECT * FROM transactions WHERE categoryId = :categoryId AND date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    LiveData<List<Transaction>> getAllTransactionsByCategoryAndDateRange(long categoryId, long startDate, long endDate);

    /**
     * دریافت همه تراکنش‌ها (درآمد+خرج) یک تگ در بازه زمانی
     */
    @Query("SELECT t.* FROM transactions t " +
            "INNER JOIN transaction_tags tt ON t.id = tt.transactionId " +
            "WHERE tt.tagId = :tagId AND t.date BETWEEN :startDate AND :endDate ORDER BY t.date DESC")
    LiveData<List<Transaction>> getAllTransactionsByTagAndDateRange(long tagId, long startDate, long endDate);

    /**
     * دریافت همه تراکنش‌ها (درآمد+خرج) یک دسته‌بندی و تگ در بازه زمانی
     */
    @Query("SELECT t.* FROM transactions t " +
            "INNER JOIN transaction_tags tt ON t.id = tt.transactionId " +
            "WHERE t.categoryId = :categoryId AND tt.tagId = :tagId AND t.date BETWEEN :startDate AND :endDate ORDER BY t.date DESC")
    LiveData<List<Transaction>> getAllTransactionsByCategoryAndTagAndDateRange(long categoryId, long tagId, long startDate, long endDate);

    /**
     * دریافت تراکنش‌های کارت به کارت در بازه زمانی مشخص
     */
    @Query("SELECT * FROM transactions WHERE type = 2 AND date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    LiveData<List<Transaction>> getTransfersByDateRange(long startDate, long endDate);

    /**
     * دریافت تراکنش‌های کارت به کارت یک دسته‌بندی در بازه زمانی
     */
    @Query("SELECT * FROM transactions WHERE type = 2 AND categoryId = :categoryId AND date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    LiveData<List<Transaction>> getTransfersByCategoryAndDateRange(long categoryId, long startDate, long endDate);

    /**
     * دریافت تراکنش‌های کارت به کارت یک تگ در بازه زمانی
     */
    @Query("SELECT t.* FROM transactions t " +
            "INNER JOIN transaction_tags tt ON t.id = tt.transactionId " +
            "WHERE t.type = 2 AND tt.tagId = :tagId AND t.date BETWEEN :startDate AND :endDate ORDER BY t.date DESC")
    LiveData<List<Transaction>> getTransfersByTagAndDateRange(long tagId, long startDate, long endDate);

    /**
     * دریافت تراکنش‌های کارت به کارت یک دسته‌بندی و تگ در بازه زمانی
     */
    @Query("SELECT t.* FROM transactions t " +
            "INNER JOIN transaction_tags tt ON t.id = tt.transactionId " +
            "WHERE t.type = 2 AND t.categoryId = :categoryId AND tt.tagId = :tagId AND t.date BETWEEN :startDate AND :endDate ORDER BY t.date DESC")
    LiveData<List<Transaction>> getTransfersByCategoryAndTagAndDateRange(long categoryId, long tagId, long startDate, long endDate);

    /**
     * گزارش کارت به کارت بر اساس دسته‌بندی در بازه زمانی
     */
    @Query("SELECT t.categoryId as categoryId, c.name as categoryName, c.color as categoryColor, " +
            "c.icon as categoryIcon, SUM(t.amount) as totalAmount, COUNT(DISTINCT t.id) as transactionCount " +
            "FROM transactions t " +
            "LEFT JOIN categories c ON t.categoryId = c.id " +
            "WHERE t.type = 2 AND t.date BETWEEN :startDate AND :endDate " +
            "GROUP BY t.categoryId " +
            "ORDER BY totalAmount DESC")
    LiveData<List<CategoryReport>> getTransferReportByCategory(long startDate, long endDate);

    /**
     * گزارش کارت به کارت بر اساس تگ در بازه زمانی
     */
    @Query("SELECT tg.id as tagId, tg.name as tagName, tg.color as tagColor, " +
            "SUM(t.amount) as totalAmount, COUNT(DISTINCT t.id) as transactionCount " +
            "FROM transactions t " +
            "INNER JOIN transaction_tags tt ON t.id = tt.transactionId " +
            "INNER JOIN tags tg ON tt.tagId = tg.id " +
            "WHERE t.type = 2 AND t.date BETWEEN :startDate AND :endDate " +
            "GROUP BY tg.id " +
            "ORDER BY totalAmount DESC")
    LiveData<List<TagReport>> getTransferReportByTag(long startDate, long endDate);

    /**
     * گزارش ترکیبی کارت به کارت بر اساس دسته‌بندی و تگ در بازه زمانی
     */
    @Query("SELECT t.categoryId as categoryId, c.name as categoryName, c.color as categoryColor, " +
            "c.icon as categoryIcon, tg.id as tagId, tg.name as tagName, tg.color as tagColor, " +
            "SUM(t.amount) as totalAmount, COUNT(DISTINCT t.id) as transactionCount " +
            "FROM transactions t " +
            "LEFT JOIN categories c ON t.categoryId = c.id " +
            "LEFT JOIN transaction_tags tt ON t.id = tt.transactionId " +
            "LEFT JOIN tags tg ON tt.tagId = tg.id " +
            "WHERE t.type = 2 AND t.date BETWEEN :startDate AND :endDate " +
            "GROUP BY t.categoryId, tg.id " +
            "ORDER BY totalAmount DESC")
    LiveData<List<CombinedReport>> getTransferReportByCategoryAndTag(long startDate, long endDate);
}
