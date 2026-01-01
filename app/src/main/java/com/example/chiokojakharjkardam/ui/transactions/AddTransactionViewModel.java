package com.example.chiokojakharjkardam.ui.transactions;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.chiokojakharjkardam.data.database.entity.BankCard;
import com.example.chiokojakharjkardam.data.database.entity.Category;
import com.example.chiokojakharjkardam.data.database.entity.Tag;
import com.example.chiokojakharjkardam.data.database.entity.Transaction;
import com.example.chiokojakharjkardam.data.repository.BankCardRepository;
import com.example.chiokojakharjkardam.data.repository.CategoryRepository;
import com.example.chiokojakharjkardam.data.repository.TagRepository;
import com.example.chiokojakharjkardam.data.repository.TransactionRepository;

import java.util.List;

public class AddTransactionViewModel extends AndroidViewModel {

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final BankCardRepository bankCardRepository;
    private final TagRepository tagRepository;

    private final MutableLiveData<List<Category>> categories = new MutableLiveData<>();
    private final LiveData<List<BankCard>> cards;
    private final LiveData<List<Tag>> tags;

    public AddTransactionViewModel(@NonNull Application application) {
        super(application);
        transactionRepository = new TransactionRepository(application);
        categoryRepository = new CategoryRepository(application);
        bankCardRepository = new BankCardRepository(application);
        tagRepository = new TagRepository(application);

        cards = bankCardRepository.getAllCards();
        tags = tagRepository.getAllTags();
    }

    public void loadCategoriesByType(int type) {
        categoryRepository.getCategoriesByType(type).observeForever(categories::setValue);
    }

    public LiveData<List<Category>> getCategories() {
        return categories;
    }

    public LiveData<List<BankCard>> getCards() {
        return cards;
    }

    public LiveData<List<Tag>> getTags() {
        return tags;
    }

    public void insertTransaction(Transaction transaction, List<Long> tagIds) {
        transactionRepository.insertWithBalanceUpdate(transaction, tagIds, null);
    }

    public void updateTransaction(Transaction transaction, List<Long> tagIds) {
        transactionRepository.update(transaction, tagIds);
    }
}

