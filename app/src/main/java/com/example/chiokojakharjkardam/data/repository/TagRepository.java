package com.example.chiokojakharjkardam.data.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.example.chiokojakharjkardam.data.database.AppDatabase;
import com.example.chiokojakharjkardam.data.database.dao.TagDao;
import com.example.chiokojakharjkardam.data.database.entity.Tag;

import java.util.List;

public class TagRepository {

    private final TagDao tagDao;

    public TagRepository(Application application) {
        AppDatabase db = AppDatabase.getDatabase(application);
        tagDao = db.tagDao();
    }

    public void insert(Tag tag) {
        AppDatabase.databaseWriteExecutor.execute(() -> tagDao.insert(tag));
    }

    public void insertAndGetId(Tag tag, OnTagInsertedListener listener) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            long id = tagDao.insert(tag);
            if (listener != null) {
                listener.onTagInserted(id);
            }
        });
    }

    public void update(Tag tag) {
        AppDatabase.databaseWriteExecutor.execute(() -> tagDao.update(tag));
    }

    public void delete(Tag tag) {
        AppDatabase.databaseWriteExecutor.execute(() -> tagDao.delete(tag));
    }

    public LiveData<List<Tag>> getAllTags() {
        return tagDao.getAllTags();
    }

    public LiveData<Tag> getTagById(long id) {
        return tagDao.getTagById(id);
    }

    public interface OnTagInsertedListener {
        void onTagInserted(long tagId);
    }
}

