package com.example.chiokojakharjkardam.ui.transactions;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.chiokojakharjkardam.data.database.entity.BankCard;
import com.example.chiokojakharjkardam.data.database.entity.Category;
import com.example.chiokojakharjkardam.data.database.entity.Tag;
import com.example.chiokojakharjkardam.data.database.entity.Transaction;
import com.example.chiokojakharjkardam.data.repository.BankCardRepository;
import com.example.chiokojakharjkardam.data.repository.CategoryRepository;
import com.example.chiokojakharjkardam.data.repository.TagRepository;
import com.example.chiokojakharjkardam.data.repository.TransactionRepository;

import java.util.List;

public class TransactionDetailViewModel extends AndroidViewModel {

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final BankCardRepository bankCardRepository;
    private final TagRepository tagRepository;

    public TransactionDetailViewModel(@NonNull Application application) {
        super(application);
        transactionRepository = new TransactionRepository(application);
        categoryRepository = new CategoryRepository(application);
        bankCardRepository = new BankCardRepository(application);
        tagRepository = new TagRepository(application);
    }

    public LiveData<Transaction> getTransactionById(long id) {
        return transactionRepository.getTransactionById(id);
    }

    public LiveData<Category> getCategoryById(long id) {
        return categoryRepository.getCategoryById(id);
    }

    public LiveData<BankCard> getCardById(long id) {
        return bankCardRepository.getCardById(id);
    }

    public LiveData<List<Tag>> getTagsByTransactionId(long transactionId) {
        return tagRepository.getTagsByTransactionId(transactionId);
    }

    public void deleteTransaction(Transaction transaction) {
        transactionRepository.delete(transaction);
    }
}

