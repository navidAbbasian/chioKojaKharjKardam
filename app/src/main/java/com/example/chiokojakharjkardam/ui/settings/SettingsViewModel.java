package com.example.chiokojakharjkardam.ui.settings;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

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
}

