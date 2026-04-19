package com.example.chiokojakharjkardam.data.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.chiokojakharjkardam.data.database.entity.BankCard;

import java.util.List;

@Dao
public interface BankCardDao {

    @Insert
    long insert(BankCard bankCard);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long upsert(BankCard bankCard);

    @Update
    void update(BankCard bankCard);

    @Delete
    void delete(BankCard bankCard);

    @Query("DELETE FROM bank_cards")
    void deleteAll();

    // ── Offline-sync helpers ──────────────────────────────────────

    @Query("SELECT * FROM bank_cards WHERE supabaseId = :supabaseId LIMIT 1")
    BankCard getBySupabaseId(long supabaseId);

    @Query("SELECT * FROM bank_cards WHERE supabaseId = :supabaseId OR bankName = :bankName LIMIT 1")
    BankCard getBySupabaseIdOrName(long supabaseId, String bankName);

    /**
     * Fallback match when supabaseId lookup fails (e.g. card created before sync).
     * Matches on cardNumber + bankName within the same member.
     */
    @Query("SELECT * FROM bank_cards WHERE cardNumber = :cardNumber AND bankName = :bankName LIMIT 1")
    BankCard getByCardNumberAndBank(String cardNumber, String bankName);

    @Query("SELECT * FROM bank_cards WHERE pendingSync > 0 OR supabaseId = 0")
    List<BankCard> getPendingCards();

    @Query("UPDATE bank_cards SET supabaseId = :supabaseId, pendingSync = 0 WHERE id = :localId")
    void updateSupabaseId(long localId, long supabaseId);

    @Query("UPDATE bank_cards SET pendingSync = :status WHERE id = :localId")
    void updateSyncStatus(long localId, int status);

    /**
     * Deletes synced cards no longer present on Supabase.
     * Protects cards that still have pending transactions to avoid CASCADE-deleting user data.
     */
    @Query("DELETE FROM bank_cards WHERE supabaseId > 0 AND pendingSync = 0 " +
           "AND supabaseId NOT IN (:remoteIds) " +
           "AND id NOT IN (SELECT DISTINCT cardId FROM transactions WHERE pendingSync > 0)")
    void deleteObsoleteCards(List<Long> remoteIds);

    @Query("SELECT * FROM bank_cards WHERE memberId = :memberId ORDER BY createdAt DESC")
    LiveData<List<BankCard>> getCardsByMember(long memberId);

    @Query("SELECT * FROM bank_cards ORDER BY createdAt DESC")
    LiveData<List<BankCard>> getAllCards();

    @Query("SELECT * FROM bank_cards ORDER BY createdAt DESC")
    List<BankCard> getAllCardsSync();

    @Query("SELECT * FROM bank_cards WHERE id = :id")
    LiveData<BankCard> getCardById(long id);

    @Query("SELECT * FROM bank_cards WHERE id = :id")
    BankCard getCardByIdSync(long id);

    @Query("UPDATE bank_cards SET balance = balance + :amount WHERE id = :cardId")
    void updateBalance(long cardId, long amount);

    @Query("SELECT SUM(balance) FROM bank_cards")
    LiveData<Long> getTotalBalance();

    @Query("SELECT SUM(balance) FROM bank_cards")
    Long getTotalBalanceSync();

    @Query("SELECT COUNT(*) FROM bank_cards")
    int getCardCount();

    /**
     * Recalculates balance for a single card from initial balance + all its transactions.
     * Call after sync to ensure local balances are correct.
     */
    @Query("UPDATE bank_cards SET balance = :initialBalance " +
           "+ COALESCE((SELECT SUM(CASE " +
           "WHEN t.type = 1 THEN t.amount " +
           "WHEN t.type = 0 THEN -t.amount " +
           "WHEN t.type = 2 THEN -t.amount " +
           "ELSE 0 END) FROM transactions t WHERE t.cardId = :cardId), 0) " +
           "+ COALESCE((SELECT SUM(t.amount) FROM transactions t WHERE t.type = 2 AND t.toCardId = :cardId), 0) " +
           "WHERE id = :cardId")
    void recalculateBalance(long cardId, long initialBalance);

    /**
     * Recalculates balance for a single card purely from its transactions:
     * balance = SUM(income) - SUM(expense) - SUM(transfer_out) + SUM(transfer_in)
     * Does NOT use initialBalance — matches the formula: درآمد - خرج
     */
    @Query("UPDATE bank_cards SET balance = " +
           "COALESCE((SELECT SUM(CASE " +
           "  WHEN t.type = 1 THEN t.amount " +
           "  WHEN t.type = 0 THEN -t.amount " +
           "  WHEN t.type = 2 THEN -t.amount " +
           "  ELSE 0 END) FROM transactions t WHERE t.cardId = bank_cards.id), 0) " +
           "+ COALESCE((SELECT SUM(t.amount) FROM transactions t WHERE t.type = 2 AND t.toCardId = bank_cards.id), 0) " +
           "WHERE id = :cardId")
    void recalculateBalanceFromTransactions(long cardId);

    /**
     * Recalculates balance for ALL cards purely from transactions.
     * Call after sync or on app start to ensure correct state.
     */
    @Query("UPDATE bank_cards SET balance = " +
           "COALESCE((SELECT SUM(CASE " +
           "  WHEN t.type = 1 THEN t.amount " +
           "  WHEN t.type = 0 THEN -t.amount " +
           "  WHEN t.type = 2 THEN -t.amount " +
           "  ELSE 0 END) FROM transactions t WHERE t.cardId = bank_cards.id), 0) " +
           "+ COALESCE((SELECT SUM(t.amount) FROM transactions t WHERE t.type = 2 AND t.toCardId = bank_cards.id), 0)")
    void recalculateAllBalancesFromTransactions();

    /**
     * Reactively computes the total balance across all cards directly from transactions:
     * total = SUM(all income) - SUM(all expense)
     * Transfers cancel out (money moves between cards, total unchanged).
     * This LiveData updates automatically whenever the transactions table changes.
     */
    @Query("SELECT COALESCE((SELECT SUM(amount) FROM transactions WHERE type = 1), 0) " +
           "- COALESCE((SELECT SUM(amount) FROM transactions WHERE type = 0), 0)")
    LiveData<Long> getComputedTotalBalance();
}
