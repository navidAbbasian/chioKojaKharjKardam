package com.example.chiokojakharjkardam.data.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.chiokojakharjkardam.data.database.entity.Tag;

import java.util.List;

@Dao
public interface TagDao {

    @Insert
    long insert(Tag tag);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long upsert(Tag tag);

    @Update
    void update(Tag tag);

    @Delete
    void delete(Tag tag);

    @Query("DELETE FROM tags")
    void deleteAll();

    // ── Offline-sync helpers ──────────────────────────────────────

    @Query("SELECT * FROM tags WHERE supabaseId = :supabaseId LIMIT 1")
    Tag getBySupabaseId(long supabaseId);

    /** Fallback match by name when supabaseId lookup fails. */
    @Query("SELECT * FROM tags WHERE name = :name LIMIT 1")
    Tag getByName(String name);

    @Query("SELECT * FROM tags WHERE pendingSync > 0 OR supabaseId = 0")
    List<Tag> getPendingTags();

    @Query("UPDATE tags SET supabaseId = :supabaseId, pendingSync = 0 WHERE id = :localId")
    void updateSupabaseId(long localId, long supabaseId);

    @Query("UPDATE tags SET pendingSync = :status WHERE id = :localId")
    void updateSyncStatus(long localId, int status);

    /** Protects pending tags from deletion so they can be uploaded later. */
    @Query("DELETE FROM tags WHERE supabaseId > 0 AND pendingSync = 0 AND supabaseId NOT IN (:remoteIds)")
    void deleteObsoleteTags(List<Long> remoteIds);

    @Query("SELECT * FROM tags ORDER BY name ASC")
    LiveData<List<Tag>> getAllTags();

    @Query("SELECT * FROM tags ORDER BY name ASC")
    List<Tag> getAllTagsSync();

    @Query("SELECT * FROM tags WHERE id = :id")
    LiveData<Tag> getTagById(long id);

    @Query("SELECT * FROM tags WHERE id = :id")
    Tag getTagByIdSync(long id);

    @Query("SELECT * FROM tags WHERE id IN (:ids)")
    List<Tag> getTagsByIds(List<Long> ids);

    @Query("SELECT t.* FROM tags t INNER JOIN transaction_tags tt ON t.id = tt.tagId WHERE tt.transactionId = :transactionId")
    LiveData<List<Tag>> getTagsByTransactionId(long transactionId);

    @Query("SELECT COUNT(*) FROM tags")
    int getTagCount();
}
