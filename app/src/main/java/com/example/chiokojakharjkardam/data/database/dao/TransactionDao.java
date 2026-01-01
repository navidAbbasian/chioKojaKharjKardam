package com.example.chiokojakharjkardam.data.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

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
}

