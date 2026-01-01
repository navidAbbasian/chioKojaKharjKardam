package com.example.chiokojakharjkardam.ui.categories;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.chiokojakharjkardam.data.database.entity.Category;
import com.example.chiokojakharjkardam.data.repository.CategoryRepository;

import java.util.List;

public class CategoriesViewModel extends AndroidViewModel {

    private final CategoryRepository repository;
    private final LiveData<List<Category>> allCategories;

    public CategoriesViewModel(@NonNull Application application) {
        super(application);
        repository = new CategoryRepository(application);
        allCategories = repository.getAllCategories();
    }

    public LiveData<List<Category>> getAllCategories() {
        return allCategories;
    }

    public void insertCategory(Category category) {
        repository.insert(category);
    }

    public void updateCategory(Category category) {
        repository.update(category);
    }

    public void deleteCategory(Category category) {
        repository.delete(category);
    }
}

