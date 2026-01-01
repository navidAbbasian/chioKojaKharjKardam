package com.example.chiokojakharjkardam.ui.cards;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.chiokojakharjkardam.data.database.entity.BankCard;
import com.example.chiokojakharjkardam.data.repository.BankCardRepository;

import java.util.List;

public class CardsViewModel extends AndroidViewModel {

    private final BankCardRepository repository;
    private final LiveData<List<BankCard>> allCards;

    public CardsViewModel(@NonNull Application application) {
        super(application);
        repository = new BankCardRepository(application);
        allCards = repository.getAllCards();
    }

    public LiveData<List<BankCard>> getAllCardsWithMember() {
        return allCards;
    }

    public void deleteCard(BankCard card) {
        repository.delete(card);
    }
}

