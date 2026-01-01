package com.example.chiokojakharjkardam.data.database.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.chiokojakharjkardam.data.database.entity.TransactionTag;

import java.util.List;

@Dao
public interface TransactionTagDao {

    @Insert
    void insert(TransactionTag transactionTag);

    @Insert
    void insertAll(List<TransactionTag> transactionTags);

    @Delete
    void delete(TransactionTag transactionTag);

    @Query("DELETE FROM transaction_tags WHERE transactionId = :transactionId")
    void deleteByTransaction(long transactionId);

    @Query("SELECT tagId FROM transaction_tags WHERE transactionId = :transactionId")
    List<Long> getTagIdsByTransaction(long transactionId);

    @Query("SELECT transactionId FROM transaction_tags WHERE tagId = :tagId")
    List<Long> getTransactionIdsByTag(long tagId);
}

