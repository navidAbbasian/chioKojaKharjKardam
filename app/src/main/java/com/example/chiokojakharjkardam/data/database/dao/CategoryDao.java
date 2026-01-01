package com.example.chiokojakharjkardam.data.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.chiokojakharjkardam.data.database.entity.Category;

import java.util.List;

@Dao
public interface CategoryDao {

    @Insert
    long insert(Category category);

    @Insert
    void insertAll(List<Category> categories);

    @Update
    void update(Category category);

    @Delete
    void delete(Category category);

    @Query("SELECT * FROM categories ORDER BY name ASC")
    LiveData<List<Category>> getAllCategories();

    @Query("SELECT * FROM categories WHERE type = :type OR type = 2 ORDER BY name ASC")
    LiveData<List<Category>> getCategoriesByType(int type);

    @Query("SELECT * FROM categories WHERE id = :id")
    LiveData<Category> getCategoryById(long id);

    @Query("SELECT * FROM categories WHERE id = :id")
    Category getCategoryByIdSync(long id);

    @Query("SELECT COUNT(*) FROM categories")
    int getCategoryCount();

    @Query("DELETE FROM categories WHERE isDefault = 0")
    void deleteCustomCategories();
}

