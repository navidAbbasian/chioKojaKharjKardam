package com.example.chiokojakharjkardam.ui.tags;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.chiokojakharjkardam.data.database.entity.Tag;
import com.example.chiokojakharjkardam.data.repository.TagRepository;

import java.util.List;

public class TagsViewModel extends AndroidViewModel {

    private final TagRepository repository;
    private final LiveData<List<Tag>> allTags;

    public TagsViewModel(@NonNull Application application) {
        super(application);
        repository = new TagRepository(application);
        allTags = repository.getAllTags();
    }

    public LiveData<List<Tag>> getAllTags() {
        return allTags;
    }

    public void insertTag(Tag tag) {
        repository.insert(tag);
    }

    public void updateTag(Tag tag) {
        repository.update(tag);
    }

    public void deleteTag(Tag tag) {
        repository.delete(tag);
    }
}

