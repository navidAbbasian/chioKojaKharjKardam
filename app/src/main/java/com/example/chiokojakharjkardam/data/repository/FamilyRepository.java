package com.example.chiokojakharjkardam.data.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.example.chiokojakharjkardam.data.database.AppDatabase;
import com.example.chiokojakharjkardam.data.database.dao.FamilyDao;
import com.example.chiokojakharjkardam.data.database.entity.Family;

import java.util.List;

public class FamilyRepository {

    private final FamilyDao familyDao;

    public FamilyRepository(Application application) {
        AppDatabase db = AppDatabase.getDatabase(application);
        familyDao = db.familyDao();
    }

    public void insert(Family family) {
        AppDatabase.databaseWriteExecutor.execute(() -> familyDao.insert(family));
    }

    public void insertAndGetId(Family family, OnFamilyInsertedListener listener) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            long id = familyDao.insert(family);
            if (listener != null) {
                listener.onFamilyInserted(id);
            }
        });
    }

    public void update(Family family) {
        AppDatabase.databaseWriteExecutor.execute(() -> familyDao.update(family));
    }

    public void delete(Family family) {
        AppDatabase.databaseWriteExecutor.execute(() -> familyDao.delete(family));
    }

    public LiveData<Family> getFamily() {
        return familyDao.getFamily();
    }

    public LiveData<Family> getFirstFamily() {
        return familyDao.getFamily();
    }

    public LiveData<List<Family>> getAllFamilies() {
        return familyDao.getAllFamilies();
    }

    public interface OnFamilyInsertedListener {
        void onFamilyInserted(long familyId);
    }
}
