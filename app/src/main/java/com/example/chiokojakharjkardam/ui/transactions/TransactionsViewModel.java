package com.example.chiokojakharjkardam.ui.transactions;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
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
import java.util.stream.Collectors;

public class TransactionsViewModel extends AndroidViewModel {

    public static final int FILTER_ALL = 0;
    public static final int FILTER_EXPENSE = 1;
    public static final int FILTER_INCOME = 2;

    private final TransactionRepository repository;
    private final CategoryRepository categoryRepository;
    private final BankCardRepository cardRepository;
    private final TagRepository tagRepository;

    private final LiveData<List<Transaction>> allTransactions;
    private final LiveData<List<Category>> allCategories;
    private final LiveData<List<BankCard>> allCards;
    private final LiveData<List<Tag>> allTags;

    private final MutableLiveData<Integer> currentFilter = new MutableLiveData<>(FILTER_ALL);
    private final MutableLiveData<Long> categoryFilter = new MutableLiveData<>(-1L); // -1 = همه
    private final MutableLiveData<Long> cardFilter = new MutableLiveData<>(-1L); // -1 = همه
    private final MutableLiveData<Long> tagFilter = new MutableLiveData<>(-1L); // -1 = همه

    private final MediatorLiveData<List<Transaction>> filteredTransactions = new MediatorLiveData<>();

    public TransactionsViewModel(@NonNull Application application) {
        super(application);
        repository = new TransactionRepository(application);
        categoryRepository = new CategoryRepository(application);
        cardRepository = new BankCardRepository(application);
        tagRepository = new TagRepository(application);

        allTransactions = repository.getAllTransactions();
        allCategories = categoryRepository.getAllCategories();
        allCards = cardRepository.getAllCards();
        allTags = tagRepository.getAllTags();

        filteredTransactions.addSource(allTransactions, transactions -> applyFilters());
        filteredTransactions.addSource(currentFilter, filter -> applyFilters());
        filteredTransactions.addSource(categoryFilter, filter -> applyFilters());
        filteredTransactions.addSource(cardFilter, filter -> applyFilters());
        filteredTransactions.addSource(tagFilter, filter -> applyFilters());
    }

    private void applyFilters() {
        List<Transaction> transactions = allTransactions.getValue();
        Integer typeFilter = currentFilter.getValue();
        Long catFilter = categoryFilter.getValue();
        Long crdFilter = cardFilter.getValue();
        Long tgFilter = tagFilter.getValue();

        if (transactions == null) {
            filteredTransactions.setValue(null);
            return;
        }

        List<Transaction> result = transactions;

        // فیلتر بر اساس نوع (خرج/درآمد)
        if (typeFilter != null && typeFilter != FILTER_ALL) {
            int type = typeFilter == FILTER_EXPENSE ? Transaction.TYPE_EXPENSE : Transaction.TYPE_INCOME;
            result = result.stream()
                    .filter(t -> t.getType() == type)
                    .collect(Collectors.toList());
        }

        // فیلتر بر اساس دسته‌بندی
        if (catFilter != null && catFilter != -1) {
            result = result.stream()
                    .filter(t -> t.getCategoryId() == catFilter)
                    .collect(Collectors.toList());
        }

        // فیلتر بر اساس کارت
        if (crdFilter != null && crdFilter != -1) {
            result = result.stream()
                    .filter(t -> t.getCardId() == crdFilter)
                    .collect(Collectors.toList());
        }

        // فیلتر بر اساس تگ
        if (tgFilter != null && tgFilter != -1) {
            List<Long> transactionIdsWithTag = repository.getTransactionIdsByTag(tgFilter);
            if (transactionIdsWithTag != null) {
                result = result.stream()
                        .filter(t -> transactionIdsWithTag.contains(t.getId()))
                        .collect(Collectors.toList());
            }
        }

        filteredTransactions.setValue(result);
    }

    public void setFilter(int filter) {
        currentFilter.setValue(filter);
    }

    public void setCategoryFilter(long categoryId) {
        categoryFilter.setValue(categoryId);
    }

    public void setCardFilter(long cardId) {
        cardFilter.setValue(cardId);
    }

    public void setTagFilter(long tagId) {
        tagFilter.setValue(tagId);
    }

    public void clearFilters() {
        currentFilter.setValue(FILTER_ALL);
        categoryFilter.setValue(-1L);
        cardFilter.setValue(-1L);
        tagFilter.setValue(-1L);
    }

    public LiveData<List<Transaction>> getFilteredTransactions() {
        return filteredTransactions;
    }

    public LiveData<List<Category>> getAllCategories() {
        return allCategories;
    }

    public LiveData<List<BankCard>> getAllCards() {
        return allCards;
    }

    public LiveData<List<Tag>> getAllTags() {
        return allTags;
    }

    public void deleteTransaction(Transaction transaction) {
        repository.delete(transaction);
    }
}

