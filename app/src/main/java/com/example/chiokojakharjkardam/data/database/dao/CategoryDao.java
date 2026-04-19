package com.example.chiokojakharjkardam.data.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.chiokojakharjkardam.data.database.entity.Category;

import java.util.List;

@Dao
public interface CategoryDao {

    @Insert
    long insert(Category category);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long upsert(Category category);

    @Insert
    void insertAll(List<Category> categories);

    @Update
    void update(Category category);

    @Delete
    void delete(Category category);

    @Query("DELETE FROM categories")
    void deleteAll();

    // ── Offline-sync helpers ──────────────────────────────────────

    @Query("SELECT * FROM categories WHERE supabaseId = :supabaseId LIMIT 1")
    Category getBySupabaseId(long supabaseId);

    /** Fallback match by name when supabaseId lookup fails. */
    @Query("SELECT * FROM categories WHERE name = :name LIMIT 1")
    Category getByName(String name);

    @Query("SELECT * FROM categories WHERE pendingSync > 0 OR supabaseId = 0")
    List<Category> getPendingCategories();

    @Query("UPDATE categories SET supabaseId = :supabaseId, pendingSync = 0 WHERE id = :localId")
    void updateSupabaseId(long localId, long supabaseId);

    @Query("UPDATE categories SET pendingSync = :status WHERE id = :localId")
    void updateSyncStatus(long localId, int status);

    /** Protects pending categories from deletion so they can be uploaded later. */
    @Query("DELETE FROM categories WHERE supabaseId > 0 AND pendingSync = 0 AND supabaseId NOT IN (:remoteIds)")
    void deleteObsoleteCategories(List<Long> remoteIds);

    @Query("SELECT * FROM categories ORDER BY name ASC")
    LiveData<List<Category>> getAllCategories();

    @Query("SELECT * FROM categories ORDER BY name ASC")
    List<Category> getAllCategoriesSync();

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
