package com.example.chiokojakharjkardam.data.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.example.chiokojakharjkardam.data.database.AppDatabase;
import com.example.chiokojakharjkardam.data.database.dao.BankCardDao;
import com.example.chiokojakharjkardam.data.database.entity.BankCard;

import java.util.List;

public class BankCardRepository {

    private final BankCardDao bankCardDao;

    public BankCardRepository(Application application) {
        AppDatabase db = AppDatabase.getDatabase(application);
        bankCardDao = db.bankCardDao();
    }

    public void insert(BankCard bankCard) {
        AppDatabase.databaseWriteExecutor.execute(() -> bankCardDao.insert(bankCard));
    }

    public void insertAndGetId(BankCard bankCard, OnCardInsertedListener listener) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            long id = bankCardDao.insert(bankCard);
            if (listener != null) {
                listener.onCardInserted(id);
            }
        });
    }

    public void update(BankCard bankCard) {
        AppDatabase.databaseWriteExecutor.execute(() -> bankCardDao.update(bankCard));
    }

    public void delete(BankCard bankCard) {
        AppDatabase.databaseWriteExecutor.execute(() -> bankCardDao.delete(bankCard));
    }

    public void updateBalance(long cardId, long amount) {
        AppDatabase.databaseWriteExecutor.execute(() -> bankCardDao.updateBalance(cardId, amount));
    }

    public LiveData<List<BankCard>> getAllCards() {
        return bankCardDao.getAllCards();
    }

    public LiveData<List<BankCard>> getCardsByMember(long memberId) {
        return bankCardDao.getCardsByMember(memberId);
    }

    public LiveData<BankCard> getCardById(long id) {
        return bankCardDao.getCardById(id);
    }

    public LiveData<Long> getTotalBalance() {
        return bankCardDao.getTotalBalance();
    }

    public interface OnCardInsertedListener {
        void onCardInserted(long cardId);
    }
}

