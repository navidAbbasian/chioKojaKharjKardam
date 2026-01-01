package com.example.chiokojakharjkardam.data.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.chiokojakharjkardam.data.database.entity.Transfer;

import java.util.List;

@Dao
public interface TransferDao {

    @Insert
    long insert(Transfer transfer);

    @Update
    void update(Transfer transfer);

    @Delete
    void delete(Transfer transfer);

    @Query("SELECT * FROM transfers ORDER BY date DESC")
    LiveData<List<Transfer>> getAllTransfers();

    @Query("SELECT * FROM transfers WHERE fromCardId = :cardId OR toCardId = :cardId ORDER BY date DESC")
    LiveData<List<Transfer>> getTransfersByCard(long cardId);

    @Query("SELECT * FROM transfers WHERE id = :id")
    LiveData<Transfer> getTransferById(long id);

    @Query("SELECT * FROM transfers WHERE id = :id")
    Transfer getTransferByIdSync(long id);

    @Query("SELECT * FROM transfers ORDER BY date DESC LIMIT :limit")
    LiveData<List<Transfer>> getRecentTransfers(int limit);
}

