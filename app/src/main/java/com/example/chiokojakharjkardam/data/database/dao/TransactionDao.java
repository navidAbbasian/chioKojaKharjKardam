package com.example.chiokojakharjkardam.data.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
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

    @Update
    void update(Transaction transaction);

    @Delete
    void delete(Transaction transaction);

    @Query("SELECT * FROM transactions ORDER BY date DESC, createdAt DESC")
    LiveData<List<Transaction>> getAllTransactions();

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

    @Query("SELECT * FROM transactions ORDER BY date DESC LIMIT :limit")
    LiveData<List<Transaction>> getRecentTransactions(int limit);

    @Query("SELECT COUNT(*) FROM transactions")
    int getTransactionCount();

    // ==================== گزارش‌گیری ====================

    /**
     * گزارش مجموع هزینه‌ها بر اساس دسته‌بندی در بازه زمانی
     */
    @Query("SELECT t.categoryId as categoryId, c.name as categoryName, c.color as categoryColor, " +
            "c.icon as categoryIcon, SUM(t.amount) as totalAmount, COUNT(*) as transactionCount " +
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
            "c.icon as categoryIcon, SUM(t.amount) as totalAmount, COUNT(*) as transactionCount " +
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
            "INNER JOIN transaction_tags tt ON t.id = tt.transactionId " +
            "INNER JOIN tags tg ON tt.tagId = tg.id " +
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
            "INNER JOIN transaction_tags tt ON t.id = tt.transactionId " +
            "INNER JOIN tags tg ON tt.tagId = tg.id " +
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
}

