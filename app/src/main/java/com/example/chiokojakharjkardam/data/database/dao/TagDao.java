package com.example.chiokojakharjkardam.data.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.chiokojakharjkardam.data.database.entity.Tag;

import java.util.List;

@Dao
public interface TagDao {

    @Insert
    long insert(Tag tag);

    @Update
    void update(Tag tag);

    @Delete
    void delete(Tag tag);

    @Query("SELECT * FROM tags ORDER BY name ASC")
    LiveData<List<Tag>> getAllTags();

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

