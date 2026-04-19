package com.example.chiokojakharjkardam.data.database.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.chiokojakharjkardam.data.database.entity.PendingDelete;

import java.util.List;

@Dao
public interface PendingDeleteDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(PendingDelete pendingDelete);

    @Delete
    void delete(PendingDelete pendingDelete);

    @Query("SELECT * FROM pending_deletes WHERE entityType = :entityType")
    List<PendingDelete> getByType(String entityType);

    @Query("DELETE FROM pending_deletes WHERE entityType = :entityType AND supabaseId = :supabaseId")
    void deleteByTypeAndId(String entityType, String supabaseId);

    @Query("DELETE FROM pending_deletes WHERE entityType = :entityType")
    void deleteAllByType(String entityType);

    @Query("DELETE FROM pending_deletes")
    void deleteAll();
}

