package com.example.chiokojakharjkardam.ui.settings;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.chiokojakharjkardam.data.database.AppDatabase;
import com.example.chiokojakharjkardam.data.database.entity.Family;
import com.example.chiokojakharjkardam.data.repository.FamilyRepository;

public class SettingsViewModel extends AndroidViewModel {

    private final FamilyRepository familyRepository;
    private final LiveData<Family> family;

    public SettingsViewModel(@NonNull Application application) {
        super(application);
        familyRepository = new FamilyRepository(application);
        family = familyRepository.getFirstFamily();
    }

    public LiveData<Family> getFamily() {
        return family;
    }

    public interface ClearDataCallback {
        void onSuccess();
        void onError(String error);
    }

    public void clearTransactionsOnly(ClearDataCallback callback) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                AppDatabase db = AppDatabase.getDatabase(getApplication());
                db.transactionTagDao().deleteAll();
                db.transactionDao().deleteAll();
                db.transferDao().deleteAll();
                if (callback != null) callback.onSuccess();
            } catch (Exception e) {
                if (callback != null) callback.onError(e.getMessage());
            }
        });
    }

    public void clearAllData(ClearDataCallback callback) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                AppDatabase db = AppDatabase.getDatabase(getApplication());
                db.transactionTagDao().deleteAll();
                db.transactionDao().deleteAll();
                db.transferDao().deleteAll();
                db.billDao().deleteAll();
                db.bankCardDao().deleteAll();
                db.tagDao().deleteAll();
                db.categoryDao().deleteAll();
                db.memberDao().deleteAll();
                db.familyDao().deleteAll();
                if (callback != null) callback.onSuccess();
            } catch (Exception e) {
                if (callback != null) callback.onError(e.getMessage());
            }
        });
    }
}

