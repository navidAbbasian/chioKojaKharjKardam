package com.example.chiokojakharjkardam.ui.transactions;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.chiokojakharjkardam.data.database.entity.Transaction;
import com.example.chiokojakharjkardam.data.repository.TransactionRepository;

public class TransactionDetailViewModel extends AndroidViewModel {

    private final TransactionRepository repository;

    public TransactionDetailViewModel(@NonNull Application application) {
        super(application);
        repository = new TransactionRepository(application);
    }

    public LiveData<Transaction> getTransactionById(long id) {
        return repository.getTransactionById(id);
    }

    public void deleteTransaction(Transaction transaction) {
        repository.delete(transaction);
    }
}

