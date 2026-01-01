package com.example.chiokojakharjkardam.data.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.chiokojakharjkardam.data.database.entity.BankCard;

import java.util.List;

@Dao
public interface BankCardDao {

    @Insert
    long insert(BankCard bankCard);

    @Update
    void update(BankCard bankCard);

    @Delete
    void delete(BankCard bankCard);

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
}

