package com.example.chiokojakharjkardam.data.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.chiokojakharjkardam.data.database.entity.Family;

import java.util.List;

@Dao
public interface FamilyDao {

    @Insert
    long insert(Family family);

    @Update
    void update(Family family);

    @Delete
    void delete(Family family);

    @Query("SELECT * FROM families ORDER BY id ASC LIMIT 1")
    LiveData<Family> getFamily();

    @Query("SELECT * FROM families ORDER BY id ASC LIMIT 1")
    Family getFamilySync();

    @Query("SELECT * FROM families")
    LiveData<List<Family>> getAllFamilies();
}

